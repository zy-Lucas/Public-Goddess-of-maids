package com.myx00x.maid.boss;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public final class ShenlinBoss {
   private final UUID id = UUID.randomUUID();
   private final ServerLevel level;
   private double x;
   private double y;
   private double z;
   private float yRot;
   private float xRot;
   private int age;
   private int lifeTicks;
   private float damageMultiplier = 1.0F;
   private ShenlinBossEntity proxy;
   private boolean finished;
   private LivingEntity lastAttacker;
   private ChunkPos ticket;
   private boolean everHere;

   ShenlinBoss(ServerLevel level, double x, double y, double z, int lifeTicks) {
      this.level = level;
      this.x = x;
      this.y = y;
      this.z = z;
      this.lifeTicks = lifeTicks;
   }

   public UUID id() {
      return this.id;
   }

   public ServerLevel level() {
      return this.level;
   }

   public ChunkPos ticket() {
      return this.ticket;
   }

   public void setTicket(ChunkPos ticket) {
      this.ticket = ticket;
   }

   void found() {
      this.everHere = true;
   }

   boolean everHere() {
      return this.everHere;
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

   public int age() {
      return this.age;
   }

   public float damageMultiplier() {
      return this.damageMultiplier;
   }

   public boolean alive() {
      return !this.finished;
   }

   public void noteAttacker(LivingEntity attacker) {
      this.lastAttacker = attacker;
   }

   public LivingEntity lastAttacker() {
      return this.lastAttacker;
   }

   public void moveTo(double x, double y, double z, float yRot, float xRot) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yRot = yRot;
      this.xRot = xRot;
   }

   void tick() {
      if (!this.finished) {
         this.age++;
         if (this.lifeTicks > 0 && this.age >= this.lifeTicks) {
            ShenlinBossKeeper.dismiss(this);
         }
      }
   }

   void finish() {
      this.finished = true;
      this.proxy = null;
   }

   ShenlinBossEntity proxy() {
      return this.proxy;
   }

   void setProxy(ShenlinBossEntity proxy) {
      this.proxy = proxy;
   }
}

