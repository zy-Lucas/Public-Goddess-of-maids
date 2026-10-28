package com.myx00x.maid.boss;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class RuinedPhantasmBoss {
   private final UUID id = UUID.randomUUID();
   private final ServerLevel level;
   private double x;
   private double y;
   private double z;
   private float yRot;
   private float xRot;
   private int musicTick;
   private int lastCastTick = -1;
   private RuinedPhantasmEntity proxy;
   private boolean finished;
   private LivingEntity lastAttacker;
   private boolean combatStarted;

   RuinedPhantasmBoss(ServerLevel level, double x, double y, double z) {
      this.level = level;
      this.x = x;
      this.y = y;
      this.z = z;
   }

   public UUID id() {
      return this.id;
   }

   public ServerLevel level() {
      return this.level;
   }

   public double x() {
      return this.x;
   }

   public double y() {
      return this.y;
   }

   public double z() {
      return this.z;
   }

   public Vec3 pos() {
      return new Vec3(this.x, this.y, this.z);
   }

   public float yRot() {
      return this.yRot;
   }

   public float xRot() {
      return this.xRot;
   }

   public int musicTick() {
      return this.musicTick;
   }

   public boolean combatStarted() {
      return this.combatStarted;
   }

   public boolean alive() {
      return !this.finished;
   }

   public LivingEntity lastAttacker() {
      return this.lastAttacker;
   }

   public void noteAttacker(LivingEntity attacker) {
      this.lastAttacker = attacker;
   }

   public void moveTo(double x, double y, double z, float yRot, float xRot) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yRot = yRot;
      this.xRot = xRot;
   }

   void startCombat() {
      this.combatStarted = true;
   }

   void onMusicTick() {
      if (!this.finished) {
         this.musicTick++;
         if (this.musicTick != this.lastCastTick) {
            this.lastCastTick = this.musicTick;
            RuinedPhantasmMoves.play(this, this.musicTick);
         }
      }
   }

   void tick() {
      if (!this.finished) {
         ;
      }
   }

   void finish() {
      this.finished = true;
      this.proxy = null;
      this.combatStarted = false;
   }

   RuinedPhantasmEntity proxy() {
      return this.proxy;
   }

   void setProxy(RuinedPhantasmEntity proxy) {
      this.proxy = proxy;
   }
}

