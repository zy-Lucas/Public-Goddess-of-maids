package com.myx00x.maid.combat.health;

import java.io.InputStream;
import java.lang.invoke.CallSite;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import sun.misc.Unsafe;

public final class ASMHealthMethodHelper {
   private static final String[] GET_HEALTH_NAMES = new String[]{"getHealth", "getHealth", "func_110143_aJ"};
   private static final String GET_HEALTH_DESC = "()F";
   private static final String GET_FLOAT_DESC = "()F";
   private static final String ENTITY_DATA_ACCESSOR_DESC = "Lnet/minecraft/network/syncher/EntityDataAccessor;";
   private static final Map<Class<?>, ASMHealthMethodHelper.HealthFieldInfo> CACHE = new ConcurrentHashMap<>();
   private static final Unsafe UNSAFE = obtainUnsafe();

   private ASMHealthMethodHelper() {
   }

   public static boolean analyzeAndSetHealth(LivingEntity entity, float targetHealth) {
      if (entity == null) {
         return false;
      }

      ASMHealthMethodHelper.HealthFieldInfo info = CACHE.computeIfAbsent(entity.getClass(), ASMHealthMethodHelper::analyzeClass);
      if (info == null) {
         return false;
      }

      return switch (info.storageType) {
         case COMPUTED -> writeComputed(entity, info, targetHealth);
         case PLAIN_FIELD -> writePlainField(entity, info, targetHealth);
         case ENTITY_DATA -> writeEntityData(entity, info, targetHealth);
         case STATIC_MAP -> writeStaticMap(entity, info, targetHealth);
      };
   }

   private static ASMHealthMethodHelper.HealthFieldInfo analyzeClass(Class<?> entityClass) {
      Class<?> owner = findGetHealthOwner(entityClass);
      if (owner == null) {
         return null;
      }

      byte[] bytes = readClassBytes(owner);
      if (bytes == null) {
         return null;
      }

      ClassNode cn = new ClassNode();
      new ClassReader(bytes).accept(cn, 8);
      MethodNode targetMethod = null;

      for (MethodNode mn : cn.methods) {
         for (String name : GET_HEALTH_NAMES) {
            if (mn.name.equals(name) && mn.desc.equals("()F")) {
               targetMethod = mn;
               break;
            }
         }

         if (targetMethod != null) {
            break;
         }
      }

      if (targetMethod == null) {
         return null;
      }

      LinkedHashSet<String> numericFields = new LinkedHashSet<>();
      String entityDataAccessorField = null;
      String entityDataAccessorOwnerInternal = null;
      boolean sawGetMaxHealth = false;
      boolean sawMathMax = false;
      boolean sawSub = false;
      boolean sawAdd = false;
      boolean sawInvokeFromThis = false;

      for (AbstractInsnNode insn : targetMethod.instructions) {
         int op = insn.getOpcode();
         if (insn instanceof FieldInsnNode fin) {
            if (fin.getOpcode() == 178 && fin.desc.equals("Lnet/minecraft/network/syncher/EntityDataAccessor;")) {
               entityDataAccessorField = fin.name;
               entityDataAccessorOwnerInternal = fin.owner;
            }

            if (fin.getOpcode() == 180 && isNumericDesc(fin.desc)) {
               numericFields.add(fin.name);
            }
         } else {
            MethodInsnNode min;
            if (insn instanceof MethodInsnNode && (min = (MethodInsnNode)insn).getOpcode() == 182) {
               if (isGetMaxHealthCall(min)) {
                  sawGetMaxHealth = true;
               } else if (min.owner.equals("java/lang/Math") && min.name.equals("max")) {
                  sawMathMax = true;
               } else if (min.desc.equals("()F") && min.owner.equals(internalName(cn))) {
                  sawInvokeFromThis = true;
               }
            }
         }

         if (op == 102 || op == 100 || op == 103 || op == 101) {
            sawSub = true;
         } else if (op == 98 || op == 96 || op == 99 || op == 97) {
            sawAdd = true;
         }
      }

      if (entityDataAccessorField != null) {
         Class<?> accessorOwner;
         try {
            accessorOwner = Class.forName(entityDataAccessorOwnerInternal.replace('/', '.'), false, Thread.currentThread().getContextClassLoader());
         } catch (ClassNotFoundException e) {
            accessorOwner = owner;
         }

         return new ASMHealthMethodHelper.HealthFieldInfo(
            ASMHealthMethodHelper.StorageType.ENTITY_DATA, entityDataAccessorField, accessorOwner, sawGetMaxHealth, sawSub, sawMathMax
         );
      } else {
         List<String> staticMapOwners = collectStaticMapOwners(entityClass, cn, targetMethod);
         if (!staticMapOwners.isEmpty()) {
            return new ASMHealthMethodHelper.HealthFieldInfo(staticMapOwners.toArray(new String[0]));
         }

         numericFields.removeIf(ASMHealthMethodHelper::isUtilityField);
         if (!numericFields.isEmpty()) {
            String fieldName = numericFields.iterator().next();
            Class<?> fieldOwner = findFieldOwner(owner, fieldName);
            if (fieldOwner == null) {
               fieldOwner = owner;
            }

            return new ASMHealthMethodHelper.HealthFieldInfo(
               ASMHealthMethodHelper.StorageType.PLAIN_FIELD, fieldName, fieldOwner, sawGetMaxHealth, sawSub, sawMathMax
            );
         } else {
            return sawInvokeFromThis ? analyzeComputedGetHealth(cn, targetMethod, owner) : null;
         }
      }
   }

   private static List<String> collectStaticMapOwners(Class<?> entityClass, ClassNode cn, MethodNode root) {
      LinkedHashSet<String> owners = new LinkedHashSet<>();
      ArrayDeque<MethodNode> queue = new ArrayDeque<>();
      HashSet<CallSite> visited = new HashSet<>();
      queue.add(root);

      while (!queue.isEmpty()) {
         MethodNode methodNode = queue.poll();
         if (methodNode != null && visited.add((CallSite)(methodNode.name + methodNode.desc))) {
            for (AbstractInsnNode insn : methodNode.instructions) {
               FieldInsnNode fin;
               if (insn instanceof FieldInsnNode && (fin = (FieldInsnNode)insn).getOpcode() == 178) {
                  if (isMapTypeDesc(fin.desc, entityClass.getClassLoader())) {
                     owners.add(fin.owner);
                  }
               } else if (insn instanceof MethodInsnNode min) {
                  if (min.getOpcode() == 184 && isEntityHealthAccessor(min, entityClass)) {
                     owners.add(min.owner);
                  } else if (isMapGetterCall(min, entityClass.getClassLoader())) {
                     owners.add(min.owner);
                  } else {
                     MethodNode next;
                     if (min.getOpcode() == 182
                        && min.desc.equals("()F")
                        && min.owner.equals(cn.name)
                        && (next = findMethodNode(cn, min.name, min.desc)) != null) {
                        queue.add(next);
                     }
                  }
               }
            }
         }
      }

      return new ArrayList<>(owners);
   }

   private static boolean isMapTypeDesc(String desc, ClassLoader loader) {
      if (!desc.startsWith("L") || !desc.endsWith(";")) {
         return false;
      }

      if (desc.startsWith("Ljava/util/Map;")) {
         return true;
      }

      try {
         Class<?> type = Class.forName(desc.substring(1, desc.length() - 1).replace('/', '.'), false, loader);
         return Map.class.isAssignableFrom(type);
      } catch (Throwable ignored) {
         return false;
      }
   }

   private static boolean isEntityHealthAccessor(MethodInsnNode min, Class<?> entityClass) {
      String desc = min.desc;
      if (desc.startsWith("(L") && desc.endsWith(")F")) {
         int close = desc.indexOf(41);
         if (close <= 2) {
            return false;
         }

         String params = desc.substring(1, close);
         if (params.startsWith("L") && params.endsWith(";") && params.indexOf(59) == params.length() - 1) {
            try {
               Class<?> paramClass = Class.forName(params.substring(1, params.length() - 1).replace('/', '.'), false, entityClass.getClassLoader());
               return paramClass.isAssignableFrom(entityClass);
            } catch (Throwable ignored) {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean writeStaticMap(LivingEntity entity, ASMHealthMethodHelper.HealthFieldInfo info, float targetHealth) {
      if (info.staticMapOwners != null && info.staticMapOwners.length != 0) {
         ClassLoader loader = entity.getClass().getClassLoader();

         for (String ownerInternal : info.staticMapOwners) {
            try {
               Class<?> owner2 = Class.forName(ownerInternal.replace('/', '.'), false, loader);
               writeStaticMapFields(owner2, entity, targetHealth);

               for (Class<?> nested : owner2.getDeclaredClasses()) {
                  writeStaticMapFields(nested, entity, targetHealth);
               }
            } catch (Throwable var14) {
            }
         }

         if (!exactHealthReached(entity, targetHealth)) {
            tryPrivilegedWriters(entity, targetHealth, info, loader);

            for (String ownerInternal : info.staticMapOwners) {
               try {
                  Class<?> owner2 = Class.forName(ownerInternal.replace('/', '.'), false, loader);
                  writeStaticMapFields(owner2, entity, targetHealth);

                  for (Class<?> nested : owner2.getDeclaredClasses()) {
                     writeStaticMapFields(nested, entity, targetHealth);
                  }
               } catch (Throwable var13) {
               }
            }
         }

         return exactHealthReached(entity, targetHealth);
      } else {
         return false;
      }
   }

   private static boolean writeStaticMapFields(Class<?> clazz, LivingEntity entity, float targetHealth) {
      boolean written = false;

      for (Field field : clazz.getDeclaredFields()) {
         if (Modifier.isStatic(field.getModifiers()) && Map.class.isAssignableFrom(field.getType())) {
            try {
               field.setAccessible(true);
               Object mapObject = field.get(null);
               if (mapObject instanceof Map && writeEntityKeyedMap((Map<Object, Object>)mapObject, entity, targetHealth, new HashSet<>())) {
                  written = true;
               }
            } catch (Throwable var9) {
            }
         }
      }

      return written;
   }

   private static void putHealthIntoMap(Map<Object, Object> map, LivingEntity entity, float value) {
      Map<Object, Object> raw = map;

      try {
         raw.put(entity, value);
      } catch (Throwable throwable) {
         try {
            raw.put(entity, (double)value);
         } catch (Throwable throwable2) {
            try {
               raw.put(entity, (int)value);
            } catch (Throwable throwable3) {
               try {
                  raw.put(entity, (long)value);
               } catch (Throwable var8) {
               }
            }
         }
      }
   }

   private static boolean isMapGetterCall(MethodInsnNode min, ClassLoader loader) {
      return min.desc.startsWith("()L") && min.desc.endsWith(";") ? isMapTypeDesc(min.desc.substring(2), loader) : false;
   }

   private static boolean writeEntityKeyedMap(Map<Object, Object> map, LivingEntity entity, float targetHealth, Set<Object> visited) {
      if (map != null && visited.add(map)) {
         if (isEntityKeyedMap(map, entity) && writeMapHard(map, entity, targetHealth)) {
            return true;
         }

         for (Field field : map.getClass().getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && Map.class.isAssignableFrom(field.getType())) {
               try {
                  field.setAccessible(true);
                  Object nested = field.get(map);
                  if (nested instanceof Map && writeEntityKeyedMap((Map<Object, Object>)nested, entity, targetHealth, visited)) {
                     return true;
                  }
               } catch (Throwable var9) {
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static Unsafe obtainUnsafe() {
      try {
         Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
         theUnsafe.setAccessible(true);
         return (Unsafe)theUnsafe.get(null);
      } catch (Throwable ignored) {
         return null;
      }
   }

   private static boolean writeMapHard(Map<Object, Object> map, LivingEntity entity, float targetHealth) {
      boolean changed = false;
      ArrayList<Entry<Object, Object>> entries = new ArrayList<>();

      for (Entry<Object, Object> entry : map.entrySet()) {
         if (isEntityKeyMatch(entry.getKey(), entity)) {
            entries.add(entry);
         }
      }

      if (entries.isEmpty()) {
         putHealthIntoMap(map, entity, targetHealth);
         return numbersEqual(map.get(entity), targetHealth);
      }

      for (Entry<Object, Object> entry : entries) {
         Object boxed = boxNumeric(entry.getValue(), targetHealth);
         if (boxed != null) {
            try {
               entry.setValue(boxed);
            } catch (Throwable var11) {
            }

            if (numbersEqual(entry.getValue(), targetHealth)) {
               changed = true;
            } else {
               long offset = entryValueFieldOffset(entry);
               if (offset != -1L && UNSAFE != null) {
                  try {
                     UNSAFE.putObject(entry, offset, boxed);
                  } catch (Throwable var12) {
                  }
               }

               if (numbersEqual(entry.getValue(), targetHealth)) {
                  changed = true;
               }
            }
         }
      }

      return changed;
   }

   private static boolean isEntityKeyMatch(Object key, LivingEntity entity) {
      if (key == entity) {
         return true;
      } else if (key instanceof Integer && (Integer)key == entity.getId()) {
         return true;
      } else {
         return key instanceof UUID && key.equals(entity.getUUID()) ? true : entity.equals(key);
      }
   }

   private static Object boxNumeric(Object reference, float value) {
      if (reference instanceof Double) {
         return (double)value;
      } else if (reference instanceof Integer) {
         return (int)value;
      } else if (reference instanceof Long) {
         return (long)value;
      } else if (reference instanceof Short) {
         return (short)value;
      } else if (reference instanceof Byte) {
         return (byte)value;
      } else {
         return reference instanceof Float ? value : null;
      }
   }

   private static long entryValueFieldOffset(Entry<?, ?> entry) {
      for (Class<?> cls = entry.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
         for (Field field : cls.getDeclaredFields()) {
            String name = field.getName();
            if (name.equals("value") || name.equals("val")) {
               try {
                  return UNSAFE.objectFieldOffset(field);
               } catch (Throwable var8) {
               }
            }
         }
      }

      return -1L;
   }

   private static void tryPrivilegedWriters(LivingEntity entity, float targetHealth, ASMHealthMethodHelper.HealthFieldInfo info, ClassLoader loader) {
      if (info.staticMapOwners != null) {
         Class<?> entityClass = entity.getClass();

         for (String ownerInternal : info.staticMapOwners) {
            try {
               Class<?> owner = Class.forName(ownerInternal.replace('/', '.'), false, loader);
               Method runner = findPrivilegedRunner(owner);
               if (runner != null) {
                  for (Method writer : owner.getDeclaredMethods()) {
                     Class<?>[] params;
                     if (Modifier.isStatic(writer.getModifiers())
                        && writer.getReturnType() == void.class
                        && (params = writer.getParameterTypes()).length == 2
                        && params[0].isAssignableFrom(entityClass)
                        && isNumeric(params[1])) {
                        invokePrivilegedWriter(runner, writer, entity, targetHealth);
                        if (exactHealthReached(entity, targetHealth)) {
                           return;
                        }
                     }
                  }
               }
            } catch (Throwable var16) {
            }
         }
      }
   }

   private static Method findPrivilegedRunner(Class<?> owner) {
      for (Method method : owner.getDeclaredMethods()) {
         Class<?> param;
         String name;
         if (Modifier.isStatic(method.getModifiers())
            && method.getParameterCount() == 1
            && method.getReturnType() == void.class
            && ((name = method.getName().toLowerCase(Locale.ROOT)).contains("privileged") || name.contains("access"))
            && ((param = method.getParameterTypes()[0]) == Runnable.class || param.isInterface() && param.getDeclaredMethods().length == 1)) {
            return method;
         }
      }

      return null;
   }

   private static void invokePrivilegedWriter(Method runner, Method writer, LivingEntity entity, float targetHealth) {
      Class<?> valueType = writer.getParameterTypes()[1];
      Runnable action = () -> {
         try {
            writer.setAccessible(true);
            writer.invoke(null, entity, coerceTo(valueType, targetHealth));
         } catch (Throwable var5x) {
         }
      };

      try {
         Class<?> param = runner.getParameterTypes()[0];
         if (param == Runnable.class) {
            runner.invoke(null, action);
         } else if (param.isInterface()) {
            Object proxy = Proxy.newProxyInstance(param.getClassLoader(), new Class[]{param}, (proxyObject, method, args) -> {
               action.run();
               return defaultValue(method.getReturnType());
            });
            runner.invoke(null, proxy);
         }
      } catch (Throwable var8) {
      }
   }

   private static Object coerceTo(Class<?> type, float value) {
      if (type == float.class || type == Float.class) {
         return value;
      } else if (type == double.class || type == Double.class) {
         return (double)value;
      } else if (type == int.class || type == Integer.class) {
         return (int)value;
      } else if (type == long.class || type == Long.class) {
         return (long)value;
      } else if (type == short.class || type == Short.class) {
         return (short)value;
      } else {
         return type != byte.class && type != Byte.class ? value : (byte)value;
      }
   }

   private static Object defaultValue(Class<?> type) {
      if (!type.isPrimitive()) {
         return null;
      } else if (type == boolean.class) {
         return Boolean.FALSE;
      } else if (type == char.class) {
         return '\u0000';
      } else if (type == byte.class) {
         return (byte)0;
      } else if (type == short.class) {
         return (short)0;
      } else if (type == int.class) {
         return 0;
      } else if (type == long.class) {
         return 0L;
      } else if (type == float.class) {
         return 0.0F;
      } else {
         return type == double.class ? 0.0 : null;
      }
   }

   private static boolean isNumeric(Class<?> type) {
      return type == float.class
         || type == Float.class
         || type == double.class
         || type == Double.class
         || type == int.class
         || type == Integer.class
         || type == long.class
         || type == Long.class
         || type == short.class
         || type == Short.class
         || type == byte.class
         || type == Byte.class
         || Number.class.isAssignableFrom(type);
   }

   private static boolean isEntityKeyedMap(Map<?, Object> map, LivingEntity entity) {
      if (map.containsKey(entity)) {
         return true;
      }

      if (map.isEmpty()) {
         return false;
      }

      for (Entry<?, ?> entry : map.entrySet()) {
         Object key = entry.getKey();
         if (key == null || !entity.getClass().isInstance(key) || !(entry.getValue() instanceof Number)) {
            return false;
         }
      }

      return true;
   }

   private static boolean numbersEqual(Object value, float target) {
      return value instanceof Number && Math.abs(((Number)value).floatValue() - target) <= 0.001F;
   }

   private static boolean exactHealthReached(LivingEntity entity, float target) {
      try {
         return Math.abs(entity.getHealth() - target) <= 0.001F;
      } catch (Throwable ignored) {
         return false;
      }
   }

   private static boolean isGetMaxHealthCall(MethodInsnNode min) {
      String lower = min.name.toLowerCase();
      return lower.contains("maxhealth") || min.name.equals("getMaxHealth") || min.name.equals("func_110138_aK");
   }

   private static ASMHealthMethodHelper.HealthFieldInfo analyzeComputedGetHealth(ClassNode cn, MethodNode getHealthMethod, Class<?> owner) {
      ArrayList<String> delegateNames = new ArrayList<>();
      int arithmeticOp = 0;
      boolean sawMathMax = false;

      for (AbstractInsnNode insn : getHealthMethod.instructions) {
         MethodInsnNode min;
         if (!(insn instanceof MethodInsnNode) || (min = (MethodInsnNode)insn).getOpcode() != 182) {
            int op = insn.getOpcode();
            if (op == 102 || op == 100 || op == 103 || op == 101) {
               arithmeticOp = 45;
            } else if (op == 98 || op == 96 || op == 99 || op == 97) {
               arithmeticOp = 43;
            }
         } else if (min.desc.equals("()F") && min.owner.equals(internalName(cn))) {
            delegateNames.add(min.name);
         } else if (min.owner.equals("java/lang/Math") && min.name.equals("max")) {
            sawMathMax = true;
         }
      }

      if (delegateNames.isEmpty()) {
         return null;
      }

      String[] dataFields = new String[delegateNames.size()];
      String[] dataOwners = new String[delegateNames.size()];

      for (int i = 0; i < delegateNames.size(); i++) {
         String delegateMethod = delegateNames.get(i);

         try {
            MethodNode mn = findMethodNode(cn, delegateMethod, "()F");
            if (mn == null) {
               return null;
            }

            String foundDataField = null;
            String foundDataOwner = null;

            for (AbstractInsnNode insn : mn.instructions) {
               FieldInsnNode fin;
               if (insn instanceof FieldInsnNode
                  && (fin = (FieldInsnNode)insn).getOpcode() == 178
                  && fin.desc.equals("Lnet/minecraft/network/syncher/EntityDataAccessor;")) {
                  foundDataField = fin.name;
                  foundDataOwner = fin.owner;
                  break;
               }
            }

            if (foundDataField == null) {
               return null;
            }

            dataFields[i] = foundDataField;
            dataOwners[i] = foundDataOwner;
         } catch (Exception e) {
            return null;
         }
      }

      int writeTarget;
      if (arithmeticOp == 45 && delegateNames.size() >= 2) {
         writeTarget = 1;
      } else {
         if (arithmeticOp != 43 || delegateNames.size() < 2) {
            Class<?> accessorOwner;
            try {
               accessorOwner = Class.forName(dataOwners[0].replace('/', '.'), false, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
               accessorOwner = owner;
            }

            return new ASMHealthMethodHelper.HealthFieldInfo(ASMHealthMethodHelper.StorageType.ENTITY_DATA, dataFields[0], accessorOwner, false, false, false);
         }

         writeTarget = 1;
      }

      return new ASMHealthMethodHelper.HealthFieldInfo(
         delegateNames.toArray(new String[0]), dataFields, dataOwners, writeTarget, arithmeticOp == 45, sawMathMax
      );
   }

   private static String internalName(ClassNode cn) {
      return cn.name;
   }

   private static MethodNode findMethodNode(ClassNode cn, String name, String desc) {
      for (MethodNode mn : cn.methods) {
         if (mn.name.equals(name) && mn.desc.equals(desc)) {
            return mn;
         }
      }

      return null;
   }

   private static boolean writeComputed(LivingEntity entity, ASMHealthMethodHelper.HealthFieldInfo info, float targetHealth) {
      if (info.delegateMethodNames == null || info.delegateMethodNames.length < 2) {
         return false;
      }

      if (info.writeTargetIndex >= 0 && info.writeTargetIndex < info.delegateDataFieldNames.length) {
         int baseIndex = 1 - info.writeTargetIndex;
         String baseFieldName = info.delegateDataFieldNames[baseIndex];
         String baseFieldOwnerInternal = info.delegateDataFieldOwners[baseIndex];
         String writeFieldName = info.delegateDataFieldNames[info.writeTargetIndex];
         String writeFieldOwnerInternal = info.delegateDataFieldOwners[info.writeTargetIndex];

         try {
            Class<?> baseOwner = Class.forName(baseFieldOwnerInternal.replace('/', '.'), false, Thread.currentThread().getContextClassLoader());
            Field baseAccessorField = baseOwner.getDeclaredField(baseFieldName);
            baseAccessorField.setAccessible(true);
            EntityDataAccessor baseAccessor = (EntityDataAccessor)baseAccessorField.get(null);
            float baseValue = 0.0F;
            Object raw;
            if (baseAccessor != null && (raw = entity.getEntityData().get(baseAccessor)) instanceof Number) {
               baseValue = ((Number)raw).floatValue();
            }

            float writeValue = info.isSubtract ? baseValue - targetHealth : targetHealth - baseValue;
            if (info.usesMathMax) {
               writeValue = Math.max(0.0F, writeValue);
            }

            Class<?> writeOwner = Class.forName(writeFieldOwnerInternal.replace('/', '.'), false, Thread.currentThread().getContextClassLoader());
            Field writeAccessorField = writeOwner.getDeclaredField(writeFieldName);
            writeAccessorField.setAccessible(true);
            EntityDataAccessor writeAccessor = (EntityDataAccessor)writeAccessorField.get(null);
            if (writeAccessor == null) {
               return false;
            }

            SynchedEntityData ed = entity.getEntityData();
            EntityDataSerializer serializer = writeAccessor.getSerializer();
            if (serializer == EntityDataSerializers.FLOAT) {
               ed.set(writeAccessor, writeValue);
            } else {
               if (serializer != EntityDataSerializers.INT) {
                  return false;
               }

               ed.set(writeAccessor, (int)writeValue);
            }

            return true;
         } catch (Exception e) {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean writePlainField(LivingEntity entity, ASMHealthMethodHelper.HealthFieldInfo info, float targetHealth) {
      try {
         Field field = findField(info.fieldOwner, info.fieldName);
         if (field == null) {
            return false;
         }

         field.setAccessible(true);
         float value = computeFieldValue(entity, info, targetHealth);
         setFieldValue(field, entity, value);
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   private static boolean writeEntityData(LivingEntity entity, ASMHealthMethodHelper.HealthFieldInfo info, float targetHealth) {
      try {
         Field accessorField = info.fieldOwner.getDeclaredField(info.fieldName);
         accessorField.setAccessible(true);
         EntityDataAccessor rawAccessor = (EntityDataAccessor)accessorField.get(null);
         if (rawAccessor == null) {
            return false;
         }

         float storeValue = computeEntityDataStoreValue(entity, rawAccessor, targetHealth);
         SynchedEntityData ed = entity.getEntityData();
         EntityDataSerializer serializer = rawAccessor.getSerializer();
         boolean success = false;
         if (serializer == EntityDataSerializers.FLOAT) {
            ed.set(rawAccessor, storeValue);
            success = true;
         } else {
            if (serializer != EntityDataSerializers.INT) {
               return false;
            }

            ed.set(rawAccessor, (int)storeValue);
            success = true;
         }

         return success;
      } catch (Exception e) {
         return false;
      }
   }

   private static float computeEntityDataStoreValue(LivingEntity entity, EntityDataAccessor<?> accessor, float targetHealth) {
      float currentHealth = entity.getHealth();
      float maxHealth = entity.getMaxHealth();
      float currentAccessorVal = 0.0F;
      Object currentAccessorObj = entity.getEntityData().get(accessor);
      if (currentAccessorObj instanceof Number) {
         currentAccessorVal = ((Number)currentAccessorObj).floatValue();
      }

      float EPSILON = 0.5F;
      if (Math.abs(currentHealth - (maxHealth - currentAccessorVal)) < 0.5F) {
         float targetAccessor = maxHealth - targetHealth;
         return Math.max(0.0F, targetAccessor);
      }

      if (Math.abs(currentHealth - currentAccessorVal) < 0.5F) {
         return targetHealth;
      }

      if (Math.abs(currentHealth - (maxHealth + currentAccessorVal)) < 0.5F) {
         return targetHealth - maxHealth;
      }

      ASMHealthMethodHelper.HealthFieldInfo cachedInfo = CACHE.get(entity.getClass());
      return cachedInfo != null ? computeFieldValueStatic(entity, cachedInfo, targetHealth) : targetHealth;
   }

   private static float computeFieldValueStatic(LivingEntity entity, ASMHealthMethodHelper.HealthFieldInfo info, float targetHealth) {
      float value = info.usesMaxHealth && info.isSubtract
         ? entity.getMaxHealth() - targetHealth
         : (info.usesMaxHealth && !info.isSubtract ? targetHealth - entity.getMaxHealth() : targetHealth);
      if (info.usesMathMax) {
         value = Math.max(0.0F, value);
      }

      return value;
   }

   private static float computeFieldValue(LivingEntity entity, ASMHealthMethodHelper.HealthFieldInfo info, float targetHealth) {
      return computeFieldValueStatic(entity, info, targetHealth);
   }

   private static byte[] readClassBytes(Class<?> clazz) {
      String path = clazz.getName().replace('.', '/') + ".class";
      ClassLoader cl = clazz.getClassLoader();
      if (cl != null) {
         try (InputStream is2222 = cl.getResourceAsStream(path)) {
            if (is2222 != null) {
               return is2222.readAllBytes();
            }
         } catch (Exception var17) {
         }
      }

      try (InputStream is3222 = ASMHealthMethodHelper.class.getClassLoader().getResourceAsStream(path)) {
         if (is3222 != null) {
            return is3222.readAllBytes();
         }
      } catch (Exception var15) {
      }

      try (InputStream is4222 = clazz.getResourceAsStream("/" + path)) {
         if (is4222 != null) {
            return is4222.readAllBytes();
         }
      } catch (Exception var13) {
      }

      try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
         return is == null ? null : is.readAllBytes();
      } catch (Exception var11) {
         return null;
      }
   }

   private static Class<?> findGetHealthOwner(Class<?> start) {
      for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
         try {
            c.getDeclaredMethod("getHealth");
            return c;
         } catch (NoSuchMethodException e) {
            try {
               c.getDeclaredMethod("getHealth");
               return c;
            } catch (NoSuchMethodException noSuchMethodException) {
            }
         }
      }

      return null;
   }

   private static Class<?> findFieldOwner(Class<?> start, String fieldName) {
      for (Class<?> c = start; c != null && c != Object.class; c = c.getSuperclass()) {
         try {
            Field f = c.getDeclaredField(fieldName);
            if (!Modifier.isStatic(f.getModifiers())) {
               return c;
            }
         } catch (NoSuchFieldException var4) {
         }
      }

      return null;
   }

   private static Field findField(Class<?> owner, String name) {
      for (Class<?> c = owner; c != null && c != Object.class; c = c.getSuperclass()) {
         try {
            Field f = c.getDeclaredField(name);
            if (!Modifier.isStatic(f.getModifiers())) {
               return f;
            }
         } catch (NoSuchFieldException var4) {
         }
      }

      return null;
   }

   private static void setFieldValue(Field field, Object target, float value) throws Exception {
      Class<?> type = field.getType();
      if (type == float.class) {
         field.setFloat(target, value);
      } else if (type == double.class) {
         field.setDouble(target, value);
      } else if (type == int.class) {
         field.setInt(target, (int)value);
      } else if (type == long.class) {
         field.setLong(target, (long)value);
      } else {
         field.set(target, value);
      }
   }

   public static boolean isNumericDesc(String desc) {
      return desc.equals("F") || desc.equals("I") || desc.equals("D") || desc.equals("J");
   }

   private static boolean isUtilityField(String name) {
      String l = name.toLowerCase();
      return l.contains("level")
         || l.contains("random")
         || l.contains("tick")
         || l.equals("id")
         || l.contains("pos")
         || l.contains("rot")
         || l.contains("motion")
         || l.contains("speed")
         || l.contains("noactiontime")
         || l.contains("last")
         || l.contains("time")
         || l.contains("cooldown")
         || l.contains("counter")
         || l.contains("handler");
   }

   public static final class HealthFieldInfo {
      public final ASMHealthMethodHelper.StorageType storageType;
      public final String fieldName;
      public final Class<?> fieldOwner;
      public final boolean usesMaxHealth;
      public final boolean isSubtract;
      public final boolean usesMathMax;
      public final String[] delegateMethodNames;
      public final String[] delegateDataFieldNames;
      public final String[] delegateDataFieldOwners;
      public final int writeTargetIndex;
      public final String[] staticMapOwners;

      HealthFieldInfo(ASMHealthMethodHelper.StorageType st, String fn, Class<?> fo, boolean umh, boolean is, boolean umm) {
         this.storageType = st;
         this.fieldName = fn;
         this.fieldOwner = fo;
         this.usesMaxHealth = umh;
         this.isSubtract = is;
         this.usesMathMax = umm;
         this.delegateMethodNames = null;
         this.delegateDataFieldNames = null;
         this.delegateDataFieldOwners = null;
         this.writeTargetIndex = -1;
         this.staticMapOwners = null;
      }

      HealthFieldInfo(String[] delegateMethods, String[] dataFields, String[] dataOwners, int writeIdx, boolean sawSub, boolean sawMathMax) {
         this.storageType = ASMHealthMethodHelper.StorageType.COMPUTED;
         this.fieldName = null;
         this.fieldOwner = null;
         this.usesMaxHealth = false;
         this.isSubtract = sawSub;
         this.usesMathMax = sawMathMax;
         this.delegateMethodNames = delegateMethods;
         this.delegateDataFieldNames = dataFields;
         this.delegateDataFieldOwners = dataOwners;
         this.writeTargetIndex = writeIdx;
         this.staticMapOwners = null;
      }

      HealthFieldInfo(String[] staticMapOwners) {
         this.storageType = ASMHealthMethodHelper.StorageType.STATIC_MAP;
         this.fieldName = null;
         this.fieldOwner = null;
         this.usesMaxHealth = false;
         this.isSubtract = false;
         this.usesMathMax = false;
         this.delegateMethodNames = null;
         this.delegateDataFieldNames = null;
         this.delegateDataFieldOwners = null;
         this.writeTargetIndex = -1;
         this.staticMapOwners = staticMapOwners;
      }

      @Override
      public String toString() {
         if (this.storageType == ASMHealthMethodHelper.StorageType.STATIC_MAP) {
            return "HealthFieldInfo[type=" + this.storageType + ", maps=" + Arrays.toString(this.staticMapOwners) + "]";
         } else {
            return this.storageType == ASMHealthMethodHelper.StorageType.COMPUTED
               ? "HealthFieldInfo[type="
                  + this.storageType
                  + ", delegates="
                  + Arrays.toString(this.delegateMethodNames)
                  + ", dataFields="
                  + Arrays.toString(this.delegateDataFieldNames)
                  + ", writeTarget="
                  + this.writeTargetIndex
                  + ", sub="
                  + this.isSubtract
                  + ", max0="
                  + this.usesMathMax
                  + "]"
               : "HealthFieldInfo[type="
                  + this.storageType
                  + ", field="
                  + this.fieldOwner.getSimpleName()
                  + "."
                  + this.fieldName
                  + ", usesMax="
                  + this.usesMaxHealth
                  + ", sub="
                  + this.isSubtract
                  + ", max0="
                  + this.usesMathMax
                  + "]";
         }
      }

      boolean isComputed() {
         return this.storageType == ASMHealthMethodHelper.StorageType.COMPUTED;
      }
   }

   public enum StorageType {
      COMPUTED,
      PLAIN_FIELD,
      ENTITY_DATA,
      STATIC_MAP;
   }
}

