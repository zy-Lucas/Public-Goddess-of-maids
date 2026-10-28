package com.myx00x.maid.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.SchedulePos;
import com.myx00x.maid.Config;
import com.myx00x.maid.ShenlinMod;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;

public final class HomeKeeper {
   private static final Map<UUID, HomeKeeper.Post> POSTS = new ConcurrentHashMap<>();

   private HomeKeeper() {
   }

   static void follow(EntityMaid maid, LivingEntity target) {
      HomeKeeper.Post post = POSTS.get(maid.getUUID());
      if (post == null) {
         post = new HomeKeeper.Post(maid);
         POSTS.put(maid.getUUID(), post);
      }

      post.target = target;
   }

   public static void tick(long now) {
      if (!POSTS.isEmpty()) {
         int every = (Integer)Config.HATE_MOVE_HOME_TICKS.get();

         for (Entry<UUID, HomeKeeper.Post> entry : POSTS.entrySet()) {
            HomeKeeper.Post post = entry.getValue();
            if (now >= post.next) {
               EntityMaid maid = post.maid;
               LivingEntity target = post.target;
               if (HateBoard.underOrders(maid) && !maid.isRemoved() && maid.level() != null && target != null && !target.isRemoved() && target.isAlive()) {
                  post.next = now + every;
                  if (maid.isHomeModeEnable()) {
                     BlockPos at = target.blockPosition();
                     move(maid, at);
                     if (!post.told) {
                        post.told = true;
                        ShenlinMod.LOGGER
                           .info("[shenlin] hate home maid {} -> {} {} {}", new Object[]{maid.getId(), at.getX(), at.getY(), at.getZ()});
                     }
                  }
               } else {
                  POSTS.remove(entry.getKey());
               }
            }
         }
      }
   }

   private static void move(EntityMaid maid, BlockPos at) {
      SchedulePos pos = maid.getSchedulePos();
      pos.setDimension(maid.level().dimension().location());
      pos.setWorkPos(at);
      pos.setIdlePos(at);
      pos.setSleepPos(at);
      pos.setConfigured(true);
      pos.restrictTo(maid);
   }

   private static final class Post {
      private final EntityMaid maid;
      private LivingEntity target;
      private long next;
      private boolean told;

      private Post(EntityMaid maid) {
         this.maid = maid;
      }
   }
}

