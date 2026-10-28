package com.myx00x.maid.combat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class SkinFix {
   private static final long KLASS_OFFSET = 8L;
   private static volatile boolean ready;
   private static Object unsafe;
   private static Method mAllocate;
   private static Method mGetInt;
   private static Method mPutInt;
   private static Method mGetLong;
   private static Method mPutLong;
   private static boolean compressed;

   private SkinFix() {
   }

   public static boolean repair(Object target, Class<?> expected) {
      if (target == null || expected == null || target.getClass() == expected) {
         return false;
      }

      if (!init()) {
         return false;
      }

      try {
         Object template = mAllocate.invoke(unsafe, expected);
         if (template.getClass() != expected) {
            return false;
         }

         if (compressed) {
            int kv = (Integer)mGetInt.invoke(unsafe, template, 8L);
            int old = (Integer)mGetInt.invoke(unsafe, target, 8L);
            if (kv == old) {
               return false;
            }

            mPutInt.invoke(unsafe, target, 8L, kv);
            int back = (Integer)mGetInt.invoke(unsafe, target, 8L);
            if (back == kv && target.getClass() == expected) {
               return true;
            }

            mPutInt.invoke(unsafe, target, 8L, old);
            return false;
         } else {
            long kv = (Long)mGetLong.invoke(unsafe, template, 8L);
            long old = (Long)mGetLong.invoke(unsafe, target, 8L);
            if (kv == old) {
               return false;
            }

            mPutLong.invoke(unsafe, target, 8L, kv);
            long back = (Long)mGetLong.invoke(unsafe, target, 8L);
            if (back == kv && target.getClass() == expected) {
               return true;
            }

            mPutLong.invoke(unsafe, target, 8L, old);
            return false;
         }
      } catch (Throwable t) {
         return false;
      }
   }

   private static boolean init() {
      if (ready) {
         return unsafe != null;
      }

      synchronized (SkinFix.class) {
         if (ready) {
            return unsafe != null;
         }

         ready = true;

         try {
            Class<?> uc = Class.forName("sun.misc.Unsafe");
            Field f = uc.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            Object u = f.get(null);
            mAllocate = uc.getMethod("allocateInstance", Class.class);
            mGetInt = uc.getMethod("getInt", Object.class, long.class);
            mPutInt = uc.getMethod("putInt", Object.class, long.class, int.class);
            mGetLong = uc.getMethod("getLong", Object.class, long.class);
            mPutLong = uc.getMethod("putLong", Object.class, long.class, long.class);
            Method scale = uc.getMethod("arrayIndexScale", Class.class);
            compressed = (Integer)scale.invoke(u, Object[].class) == 4;
            unsafe = u;
         } catch (Throwable t) {
            unsafe = null;
         }

         return unsafe != null;
      }
   }
}

