package net.minecraft.entity.mob;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class HoglinEntity extends AnimalEntity implements Monster, Hoglin {
   private static final TrackedData<Boolean> BABY;
   private static final float field_30525 = 0.2F;
   private static final int field_30526 = 40;
   private static final float field_30527 = 0.3F;
   private static final int field_30528 = 1;
   private static final float field_30529 = 0.6F;
   private static final int field_30530 = 6;
   private static final float field_30531 = 0.5F;
   private static final int field_30532 = 300;
   private int movementCooldownTicks;
   private int timeInOverworld;
   private boolean cannotBeHunted;
   protected static final ImmutableList<? extends SensorType<? extends Sensor<? super HoglinEntity>>> SENSOR_TYPES;
   protected static final ImmutableList<? extends MemoryModuleType<?>> MEMORY_MODULE_TYPES;

   public HoglinEntity(EntityType<? extends HoglinEntity> entityType, World world) {
      super(entityType, world);
      this.experiencePoints = 5;
   }

   public boolean canBeLeashedBy(PlayerEntity player) {
      return !this.isLeashed();
   }

   public static DefaultAttributeContainer.Builder createHoglinAttributes() {
      return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30000001192092896D).add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6000000238418579D).add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 1.0D).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D);
   }

   public boolean tryAttack(Entity target) {
      if (!(target instanceof LivingEntity)) {
         return false;
      } else {
         this.movementCooldownTicks = 10;
         this.world.sendEntityStatus(this, (byte)4);
         this.playSound(SoundEvents.ENTITY_HOGLIN_ATTACK, 1.0F, this.getSoundPitch());
         HoglinBrain.onAttacking(this, (LivingEntity)target);
         return Hoglin.tryAttack(this, (LivingEntity)target);
      }
   }

   protected void knockback(LivingEntity target) {
      if (this.isAdult()) {
         Hoglin.knockback(this, target);
      }

   }

   public boolean damage(DamageSource source, float amount) {
      boolean bl = super.damage(source, amount);
      if (this.world.isClient) {
         return false;
      } else {
         if (bl && source.getAttacker() instanceof LivingEntity) {
            HoglinBrain.onAttacked(this, (LivingEntity)source.getAttacker());
         }

         return bl;
      }
   }

   protected Brain.Profile<HoglinEntity> createBrainProfile() {
      return Brain.createProfile(MEMORY_MODULE_TYPES, SENSOR_TYPES);
   }

   protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
      return HoglinBrain.create(this.createBrainProfile().deserialize(dynamic));
   }

   public Brain<HoglinEntity> getBrain() {
      return super.getBrain();
   }

   protected void mobTick() {
      this.world.getProfiler().push("hoglinBrain");
      this.getBrain().tick((ServerWorld)this.world, this);
      this.world.getProfiler().pop();
      HoglinBrain.refreshActivities(this);
      if (this.canConvert()) {
         ++this.timeInOverworld;
         if (this.timeInOverworld > 300) {
            this.playSound(SoundEvents.ENTITY_HOGLIN_CONVERTED_TO_ZOMBIFIED);
            this.zombify((ServerWorld)this.world);
         }
      } else {
         this.timeInOverworld = 0;
      }

   }

   public void tickMovement() {
      if (this.movementCooldownTicks > 0) {
         --this.movementCooldownTicks;
      }

      super.tickMovement();
   }

   protected void onGrowUp() {
      if (this.isBaby()) {
         this.experiencePoints = 3;
         this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(0.5D);
      } else {
         this.experiencePoints = 5;
         this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(6.0D);
      }

   }

   public static boolean canSpawn(EntityType<HoglinEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
      return !world.getBlockState(pos.down()).isOf(Blocks.NETHER_WART_BLOCK);
   }

   @Nullable
   public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
      if (world.getRandom().nextFloat() < 0.2F) {
         this.setBaby(true);
      }

      return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
   }

   public boolean canImmediatelyDespawn(double distanceSquared) {
      return !this.isPersistent();
   }

   public float getPathfindingFavor(BlockPos pos, WorldView world) {
      if (HoglinBrain.isWarpedFungusAround(this, pos)) {
         return -1.0F;
      } else {
         return world.getBlockState(pos.down()).isOf(Blocks.CRIMSON_NYLIUM) ? 10.0F : 0.0F;
      }
   }

   public double getMountedHeightOffset() {
      return (double)this.getHeight() - (this.isBaby() ? 0.2D : 0.15D);
   }

   public ActionResult interactMob(PlayerEntity player, Hand hand) {
      ActionResult actionResult = super.interactMob(player, hand);
      if (actionResult.isAccepted()) {
         this.setPersistent();
      }

      return actionResult;
   }

   public void handleStatus(byte status) {
      if (status == 4) {
         this.movementCooldownTicks = 10;
         this.playSound(SoundEvents.ENTITY_HOGLIN_ATTACK, 1.0F, this.getSoundPitch());
      } else {
         super.handleStatus(status);
      }

   }

   public int getMovementCooldownTicks() {
      return this.movementCooldownTicks;
   }

   protected boolean shouldDropXp() {
      return true;
   }

   protected int getXpToDrop(PlayerEntity player) {
      return this.experiencePoints;
   }

   private void zombify(ServerWorld word) {
      ZoglinEntity zoglinEntity = (ZoglinEntity)this.convertTo(EntityType.ZOGLIN, true);
      if (zoglinEntity != null) {
         zoglinEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
      }

   }

   public boolean isBreedingItem(ItemStack stack) {
      return stack.isOf(Items.CRIMSON_FUNGUS);
   }

   public boolean isAdult() {
      return !this.isBaby();
   }

   protected void initDataTracker() {
      super.initDataTracker();
      this.dataTracker.startTracking(BABY, false);
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      if (this.isImmuneToZombification()) {
         nbt.putBoolean("IsImmuneToZombification", true);
      }

      nbt.putInt("TimeInOverworld", this.timeInOverworld);
      if (this.cannotBeHunted) {
         nbt.putBoolean("CannotBeHunted", true);
      }

   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      this.setImmuneToZombification(nbt.getBoolean("IsImmuneToZombification"));
      this.timeInOverworld = nbt.getInt("TimeInOverworld");
      this.setCannotBeHunted(nbt.getBoolean("CannotBeHunted"));
   }

   public void setImmuneToZombification(boolean immuneToZombification) {
      this.getDataTracker().set(BABY, immuneToZombification);
   }

   private boolean isImmuneToZombification() {
      return (Boolean)this.getDataTracker().get(BABY);
   }

   public boolean canConvert() {
      return !this.world.getDimension().isPiglinSafe() && !this.isImmuneToZombification() && !this.isAiDisabled();
   }

   private void setCannotBeHunted(boolean cannotBeHunted) {
      this.cannotBeHunted = cannotBeHunted;
   }

   public boolean canBeHunted() {
      return this.isAdult() && !this.cannotBeHunted;
   }

   @Nullable
   public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
      HoglinEntity hoglinEntity = (HoglinEntity)EntityType.HOGLIN.create(world);
      if (hoglinEntity != null) {
         hoglinEntity.setPersistent();
      }

      return hoglinEntity;
   }

   public boolean canEat() {
      return !HoglinBrain.isNearPlayer(this) && super.canEat();
   }

   public SoundCategory getSoundCategory() {
      return SoundCategory.HOSTILE;
   }

   protected SoundEvent getAmbientSound() {
      return this.world.isClient ? null : (SoundEvent)HoglinBrain.getSoundEvent(this).orElse((Object)null);
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_HOGLIN_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_HOGLIN_DEATH;
   }

   protected SoundEvent getSwimSound() {
      return SoundEvents.ENTITY_HOSTILE_SWIM;
   }

   protected SoundEvent getSplashSound() {
      return SoundEvents.ENTITY_HOSTILE_SPLASH;
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(SoundEvents.ENTITY_HOGLIN_STEP, 0.15F, 1.0F);
   }

   protected void playSound(SoundEvent sound) {
      this.playSound(sound, this.getSoundVolume(), this.getSoundPitch());
   }

   protected void sendAiDebugData() {
      super.sendAiDebugData();
      DebugInfoSender.sendBrainDebugData(this);
   }

   static {
      BABY = DataTracker.registerData(HoglinEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
      SENSOR_TYPES = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ADULT, SensorType.HOGLIN_SPECIFIC_SENSOR);
      MEMORY_MODULE_TYPES = ImmutableList.of(MemoryModuleType.BREED_TARGET, MemoryModuleType.MOBS, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.NEAREST_VISIBLE_TARGETABLE_PLAYER, MemoryModuleType.LOOK_TARGET, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATTACK_TARGET, MemoryModuleType.ATTACK_COOLING_DOWN, MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLIN, MemoryModuleType.AVOID_TARGET, MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT, MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT, MemoryModuleType.NEAREST_VISIBLE_ADULT_HOGLINS, MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.NEAREST_REPELLENT, MemoryModuleType.PACIFIED);
   }
}
