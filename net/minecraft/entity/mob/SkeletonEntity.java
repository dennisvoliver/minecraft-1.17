package net.minecraft.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

public class SkeletonEntity extends AbstractSkeletonEntity {
   private static final TrackedData<Boolean> CONVERTING;
   public static final String STRAY_CONVERSION_TIME_KEY = "StrayConversionTime";
   private int inPowderSnowTime;
   private int conversionTime;

   public SkeletonEntity(EntityType<? extends SkeletonEntity> entityType, World world) {
      super(entityType, world);
   }

   protected void initDataTracker() {
      super.initDataTracker();
      this.getDataTracker().startTracking(CONVERTING, false);
   }

   /**
    * Returns whether this skeleton is currently converting to a stray.
    */
   public boolean isConverting() {
      return (Boolean)this.getDataTracker().get(CONVERTING);
   }

   public void setConverting(boolean converting) {
      this.dataTracker.set(CONVERTING, converting);
   }

   public boolean isShaking() {
      return this.isConverting();
   }

   public void tick() {
      if (!this.world.isClient && this.isAlive() && !this.isAiDisabled()) {
         if (this.isConverting()) {
            --this.conversionTime;
            if (this.conversionTime < 0) {
               this.convertToStray();
            }
         } else if (this.inPowderSnow) {
            ++this.inPowderSnowTime;
            if (this.inPowderSnowTime >= 140) {
               this.setConversionTime(300);
            }
         } else {
            this.inPowderSnowTime = -1;
         }
      }

      super.tick();
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      nbt.putInt("StrayConversionTime", this.isConverting() ? this.conversionTime : -1);
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      if (nbt.contains("StrayConversionTime", 99) && nbt.getInt("StrayConversionTime") > -1) {
         this.setConversionTime(nbt.getInt("StrayConversionTime"));
      }

   }

   private void setConversionTime(int time) {
      this.conversionTime = time;
      this.dataTracker.set(CONVERTING, true);
   }

   /**
    * Converts this skeleton to a stray and plays a sound if it is not silent.
    */
   protected void convertToStray() {
      this.convertTo(EntityType.STRAY, true);
      if (!this.isSilent()) {
         this.world.syncWorldEvent((PlayerEntity)null, WorldEvents.SKELETON_CONVERTS_TO_STRAY, this.getBlockPos(), 0);
      }

   }

   public boolean canFreeze() {
      return false;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.ENTITY_SKELETON_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_SKELETON_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_SKELETON_DEATH;
   }

   SoundEvent getStepSound() {
      return SoundEvents.ENTITY_SKELETON_STEP;
   }

   protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
      super.dropEquipment(source, lootingMultiplier, allowDrops);
      Entity entity = source.getAttacker();
      if (entity instanceof CreeperEntity) {
         CreeperEntity creeperEntity = (CreeperEntity)entity;
         if (creeperEntity.shouldDropHead()) {
            creeperEntity.onHeadDropped();
            this.dropItem(Items.SKELETON_SKULL);
         }
      }

   }

   static {
      CONVERTING = DataTracker.registerData(SkeletonEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
   }
}
