package com.myx00x.maid.client;

public final class EffectChannel {
   private static volatile long framePassAt;
   private static volatile boolean lastAtFrame;
   private static volatile long handPassAt;

   private EffectChannel() {
   }

   public static void markFramePass(long now) {
      framePassAt = now;
   }

   public static boolean framePassAlive(long now) {
      return now - framePassAt < 1000L;
   }

   public static void markHandPass(long now) {
      handPassAt = now;
   }

   public static boolean handPassAlive(long now) {
      return now - handPassAt < 1000L;
   }

   public static boolean drawAtFrame() {
      return !RayEffect.wantsNow()
            && !StarEffect.pending()
            && !ShockwaveEffect.pending()
            && !ShockWarpEffect.pending()
            && !BlackHoleEffect.pending()
            && !OrbitCannonEffect.pending()
            && !CannonShotEffect.pending()
            && !CannonSpinEffect.pending()
            && !SoloBeamEffect.pending()
         ? lastAtFrame
         : true;
   }

   public static void noteAtFrame(boolean frame) {
      lastAtFrame = frame;
   }

   public static boolean stampedAt(long now) {
      return framePassAt == now;
   }
}

