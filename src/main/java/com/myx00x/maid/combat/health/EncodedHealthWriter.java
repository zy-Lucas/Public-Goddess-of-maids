package com.myx00x.maid.combat.health;

import com.myx00x.maid.ShenlinMod;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

final class EncodedHealthWriter {
   private static final Map<Class<?>, List<EncodedHealthWriter.Shape>> CACHE = new ConcurrentHashMap<>();
   private static final String[] GET_HEALTH = new String[]{"getHealth", "m_21223_", "func_110143_aJ"};
   private static final String[] DATA_GET = new String[]{"get", "m_135370_", "func_135370_a"};
   private static final String ACCESSOR_DESC = "Lnet/minecraft/network/syncher/EntityDataAccessor;";
   private static final int FSUB = 102;

   private EncodedHealthWriter() {
   }

   static boolean setHealth(LivingEntity entity, float target) {
      if (entity != null && !entity.level().isClientSide) {
         List<EncodedHealthWriter.Shape> shapes = CACHE.computeIfAbsent(entity.getClass(), EncodedHealthWriter::analyze);
         if (shapes.isEmpty()) {
            return false;
         }

         SynchedEntityData data = entity.getEntityData();

         for (EncodedHealthWriter.Shape shape : shapes) {
            try {
               if (shape.write(entity, data, target)) {
                  return true;
               }
            } catch (Throwable var7) {
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private static List<EncodedHealthWriter.Shape> analyze(Class<?> type) {
      List<EncodedHealthWriter.Shape> out = new ArrayList<>();
      MethodNode body = getHealthNode(type);
      if (body == null) {
         return out;
      }

      ClassLoader loader = type.getClassLoader() != null ? type.getClassLoader() : EncodedHealthWriter.class.getClassLoader();
      List<EncodedHealthWriter.Step> steps = new ArrayList<>();
      FieldInsnNode pendingAccessor = null;
      boolean pendingIsSecondOperand = false;

      for (AbstractInsnNode insn : body.instructions) {
         if (insn instanceof FieldInsnNode field && field.getOpcode() == 178 && "Lnet/minecraft/network/syncher/EntityDataAccessor;".equals(field.desc)) {
            pendingAccessor = field;
         } else {
            int op = insn.getOpcode();
            if (op == 102) {
               pendingIsSecondOperand = !steps.isEmpty();
            } else if ((!(pendingAccessor != null && insn instanceof MethodInsnNode call) || !isDataGet(call))
               && pendingAccessor != null
               && insn instanceof MethodInsnNode call
               && isDecoder(call)) {
               steps.add(new EncodedHealthWriter.Step(pendingAccessor, call, pendingIsSecondOperand));
               pendingAccessor = null;
               pendingIsSecondOperand = false;
            }
         }
      }

      if (steps.isEmpty()) {
         return out;
      }

      for (EncodedHealthWriter.Step step : steps) {
         try {
            Field accessorField = staticField(step.accessor, loader);
            Method decode = method(step.decode, type, loader);
            if (accessorField != null && decode != null) {
               Field otherField = null;
               Method otherDecode = null;
               if (steps.size() >= 2) {
                  EncodedHealthWriter.Step other = step == steps.get(0) ? steps.get(1) : steps.get(0);
                  otherField = staticField(other.accessor, loader);
                  otherDecode = method(other.decode, type, loader);
               }

               out.add(new EncodedHealthWriter.Shape(accessorField, decode, otherField, otherDecode, step.secondOperand));
            }
         } catch (Throwable var14) {
         }
      }

      return out;
   }

   private static boolean isDataGet(MethodInsnNode call) {
      for (String name : DATA_GET) {
         if (call.name.equals(name) && call.desc.endsWith("Ljava/lang/Object;")) {
            return true;
         }
      }

      return false;
   }

   private static boolean isDecoder(MethodInsnNode call) {
      if (!call.desc.endsWith(")F")) {
         return false;
      }

      String args = call.desc.substring(0, call.desc.indexOf(41));
      return args.equals("(J") || args.equals("(I") || args.equals("(F") || args.equals("(D");
   }

   private static MethodNode getHealthNode(Class<?> type) {
      byte[] bytes = classBytes(type);
      if (bytes == null) {
         return null;
      }

      ClassNode node = new ClassNode();
      new ClassReader(bytes).accept(node, 4);

      for (MethodNode method : node.methods) {
         if (method.desc.equals("()F")) {
            for (String name : GET_HEALTH) {
               if (method.name.equals(name)) {
                  return method;
               }
            }
         }
      }

      return null;
   }

   private static byte[] classBytes(Class<?> type) {
      String path = type.getName().replace('.', '/') + ".class";
      ClassLoader loader = type.getClassLoader() != null ? type.getClassLoader() : ClassLoader.getSystemClassLoader();

      try (InputStream in = loader.getResourceAsStream(path)) {
         return in != null ? in.readAllBytes() : null;
      } catch (Throwable t) {
         return null;
      }
   }

   private static Field staticField(FieldInsnNode node, ClassLoader loader) {
      try {
         Class<?> owner = Class.forName(node.owner.replace('/', '.'), false, loader);
         Field found = owner.getDeclaredField(node.name);
         found.setAccessible(true);
         return found;
      } catch (Throwable t) {
         return null;
      }
   }

   private static Method method(MethodInsnNode node, Class<?> entityType, ClassLoader loader) {
      try {
         Class<?> owner = Class.forName(node.owner.replace('/', '.'), false, loader);

         for (Method candidate : owner.getDeclaredMethods()) {
            if (candidate.getName().equals(node.name) && descriptorOf(candidate).equals(node.desc)) {
               candidate.setAccessible(true);
               return candidate;
            }
         }
      } catch (Throwable t) {
         ShenlinMod.LOGGER.debug("[shenlin] encoded health: 认不到 helper {}", node.name);
      }

      return null;
   }

   private static String descriptorOf(Method method) {
      StringBuilder sb = new StringBuilder("(");

      for (Class<?> p : method.getParameterTypes()) {
         sb.append(descriptorOf(p));
      }

      return sb.append(')').append(descriptorOf(method.getReturnType())).toString();
   }

   private static String descriptorOf(Class<?> type) {
      if (type == void.class) {
         return "V";
      } else if (type == boolean.class) {
         return "Z";
      } else if (type == byte.class) {
         return "B";
      } else if (type == char.class) {
         return "C";
      } else if (type == short.class) {
         return "S";
      } else if (type == int.class) {
         return "I";
      } else if (type == long.class) {
         return "J";
      } else if (type == float.class) {
         return "F";
      } else {
         return type == double.class ? "D" : "L" + type.getName().replace('.', '/') + ";";
      }
   }

   private static final class Shape {
      private final Field accessorField;
      private final Method decode;
      private final Field otherAccessorField;
      private final Method otherDecode;
      private final boolean thisIsSubtracted;
      private Method encode;

      private Shape(Field accessorField, Method decode, Field otherAccessorField, Method otherDecode, boolean thisIsSubtracted) {
         this.accessorField = accessorField;
         this.decode = decode;
         this.otherAccessorField = otherAccessorField;
         this.otherDecode = otherDecode;
         this.thisIsSubtracted = thisIsSubtracted;
      }

      private boolean write(LivingEntity entity, SynchedEntityData data, float target) throws Exception {
         EntityDataAccessor<?> accessor = this.accessor(data, this.accessorField);
         if (accessor == null) {
            return false;
         }

         Object raw = data.get(accessor);
         if (!(raw instanceof Number)) {
            return false;
         }

         float mine = this.decode(entity, this.decode, (Number)raw);
         float value;
         if (this.otherAccessorField == null) {
            value = target;
         } else {
            EntityDataAccessor<?> other = this.accessor(data, this.otherAccessorField);
            if (other == null) {
               return false;
            }

            Object rawOther = data.get(other);
            if (!(rawOther instanceof Number)) {
               return false;
            }

            float theirs = this.decode(entity, this.otherDecode, (Number)rawOther);
            value = this.thisIsSubtracted ? theirs - target : target - theirs;
         }

         if (mine == 0.0F && value == 0.0F) {
            return false;
         }

         Method enc = this.encoder(entity, this);
         if (enc == null) {
            return false;
         }

         Object boxed = this.invoke(enc, entity, value);
         if (boxed == null) {
            return false;
         }

         setRaw(data, accessor, boxed);
         return true;
      }

      private static void setRaw(SynchedEntityData data, EntityDataAccessor<?> accessor, Object value) {
         data.set(accessor, value);
      }

      private EntityDataAccessor<?> accessor(SynchedEntityData data, Field field) {
         try {
            return (EntityDataAccessor<?>)field.get(null);
         } catch (Throwable t) {
            return null;
         }
      }

      private float decode(LivingEntity entity, Method method, Number raw) {
         return this.invoke(method, entity, raw) instanceof Number num ? num.floatValue() : Float.NaN;
      }

      private Object invoke(Method method, LivingEntity entity, Object arg) {
         try {
            method.setAccessible(true);
            return method.invoke(Modifier.isStatic(method.getModifiers()) ? null : entity, arg);
         } catch (Throwable t) {
            return null;
         }
      }

      private Method encoder(LivingEntity entity, EncodedHealthWriter.Shape shape) {
         if (this.encode != null) {
            return this.encode;
         }

         Class<?> owner = shape.decode.getDeclaringClass();
         Class<?> want = returnTypeOf(shape.decode);
         float probe = 12.5F;

         for (Method candidate : owner.getDeclaredMethods()) {
            if (candidate.getParameterCount() == 1) {
               Class<?> param = candidate.getParameterTypes()[0];
               if (param == float.class || param == double.class) {
                  Class<?> back = candidate.getReturnType();
                  if ((back == want || isBoxed(back) && isBoxed(want)) && (!isBoxed(back) || !isBoxed(want) || back.equals(want))) {
                     try {
                        candidate.setAccessible(true);
                        Object encoded = candidate.invoke(
                           Modifier.isStatic(candidate.getModifiers()) ? null : entity, param == float.class ? probe : (double)probe
                        );
                        if (encoded instanceof Number) {
                           Method dec = shape.decode;
                           dec.setAccessible(true);
                           if (dec.invoke(Modifier.isStatic(dec.getModifiers()) ? null : entity, encoded) instanceof Number num
                              && Math.abs(num.floatValue() - probe) <= 0.01F) {
                              this.encode = candidate;
                              return candidate;
                           }
                        }
                     } catch (Throwable var16) {
                     }
                  }
               }
            }
         }

         return null;
      }

      private static Class<?> returnTypeOf(Method method) {
         Class<?> t = method.getReturnType();
         if (t == long.class) {
            return Long.class;
         } else if (t == int.class) {
            return Integer.class;
         } else if (t == float.class) {
            return Float.class;
         } else {
            return t == double.class ? Double.class : t;
         }
      }

      private static boolean isBoxed(Class<?> type) {
         return type == Long.class || type == Integer.class || type == Float.class || type == Double.class;
      }
   }

   private record Step(FieldInsnNode accessor, MethodInsnNode decode, boolean secondOperand) {
   }
}

