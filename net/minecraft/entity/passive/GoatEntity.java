package net.minecraft.entity.passive;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.sensor.Sensor;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
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
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class GoatEntity extends AnimalEntity {
   public static final EntityDimensions LONG_JUMPING_DIMENSIONS = EntityDimensions.changing(0.9F, 1.3F).scaled(0.7F);
   protected static final ImmutableList<SensorType<? extends Sensor<? super GoatEntity>>> SENSORS;
   protected static final ImmutableList<MemoryModuleType<?>> MEMORY_MODULES;
   public static final int FALL_DAMAGE_SUBTRACTOR = 10;
   public static final double SCREAMING_CHANCE = 0.02D;
   private static final TrackedData<Boolean> SCREAMING;
   private boolean field_33487;
   private int field_33488;

   public GoatEntity(EntityType<? extends GoatEntity> entityType, World world) {
      super(entityType, world);
      this.getNavigation().setCanSwim(true);
   }

   protected Brain.Profile<GoatEntity> createBrainProfile() {
      return Brain.createProfile(MEMORY_MODULES, SENSORS);
   }

   protected Brain<?> deserializeBrain(Dynamic<?> dynamic) {
      return GoatBrain.create(this.createBrainProfile().deserialize(dynamic));
   }

   public static DefaultAttributeContainer.Builder createGoatAttributes() {
      return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.20000000298023224D).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1.0D);
   }

   protected int computeFallDamage(float fallDistance, float damageMultiplier) {
      return super.computeFallDamage(fallDistance, damageMultiplier) - 10;
   }

   protected SoundEvent getAmbientSound() {
      return this.isScreaming() ? SoundEvents.ENTITY_GOAT_SCREAMING_AMBIENT : SoundEvents.ENTITY_GOAT_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return this.isScreaming() ? SoundEvents.ENTITY_GOAT_SCREAMING_HURT : SoundEvents.ENTITY_GOAT_HURT;
   }

   protected SoundEvent getDeathSound() {
      return this.isScreaming() ? SoundEvents.ENTITY_GOAT_SCREAMING_DEATH : SoundEvents.ENTITY_GOAT_DEATH;
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
      this.playSound(SoundEvents.ENTITY_GOAT_STEP, 0.15F, 1.0F);
   }

   protected SoundEvent getMilkingSound() {
      return this.isScreaming() ? SoundEvents.ENTITY_GOAT_SCREAMING_MILK : SoundEvents.ENTITY_GOAT_MILK;
   }

   public GoatEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
      GoatEntity goatEntity = (GoatEntity)passiveEntity;
      GoatEntity goatEntity2 = (GoatEntity)EntityType.GOAT.create(serverWorld);
      if (goatEntity2 != null && goatEntity.isScreaming()) {
         goatEntity2.setScreaming(true);
      }

      return goatEntity2;
   }

   public Brain<GoatEntity> getBrain() {
      return super.getBrain();
   }

   protected void mobTick() {
      this.world.getProfiler().push("goatBrain");
      this.getBrain().tick((ServerWorld)this.world, this);
      this.world.getProfiler().pop();
      this.world.getProfiler().push("goatActivityUpdate");
      GoatBrain.updateActivities(this);
      this.world.getProfiler().pop();
      super.mobTick();
   }

   public int getBodyYawSpeed() {
      return 15;
   }

   public void setHeadYaw(float headYaw) {
      int i = this.getBodyYawSpeed();
      float f = MathHelper.subtractAngles(this.bodyYaw, headYaw);
      float g = MathHelper.clamp(f, (float)(-i), (float)i);
      super.setHeadYaw(this.bodyYaw + g);
   }

   public SoundEvent getEatSound(ItemStack stack) {
      return this.isScreaming() ? SoundEvents.ENTITY_GOAT_SCREAMING_EAT : SoundEvents.ENTITY_GOAT_EAT;
   }

   public ActionResult interactMob(PlayerEntity player, Hand hand) {
      ItemStack itemStack = player.getStackInHand(hand);
      if (itemStack.isOf(Items.BUCKET) && !this.isBaby()) {
         player.playSound(this.getMilkingSound(), 1.0F, 1.0F);
         ItemStack itemStack2 = ItemUsage.exchangeStack(itemStack, player, Items.MILK_BUCKET.getDefaultStack());
         player.setStackInHand(hand, itemStack2);
         return ActionResult.success(this.world.isClient);
      } else {
         ActionResult actionResult = super.interactMob(player, hand);
         if (actionResult.isAccepted() && this.isBreedingItem(itemStack)) {
            this.world.playSoundFromEntity((PlayerEntity)null, this, this.getEatSound(itemStack), SoundCategory.NEUTRAL, 1.0F, MathHelper.nextBetween(this.world.random, 0.8F, 1.2F));
         }

         return actionResult;
      }
   }

   public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData entityData, @Nullable NbtCompound entityNbt) {
      GoatBrain.resetLongJumpCooldown(this);
      this.setScreaming(world.getRandom().nextDouble() < 0.02D);
      return super.initialize(world, difficulty, spawnReason, entityData, entityNbt);
   }

   protected void sendAiDebugData() {
      super.sendAiDebugData();
      DebugInfoSender.sendBrainDebugData(this);
   }

   public EntityDimensions getDimensions(EntityPose pose) {
      return pose == EntityPose.LONG_JUMPING ? LONG_JUMPING_DIMENSIONS.scaled(this.getScaleFactor()) : super.getDimensions(pose);
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      nbt.putBoolean("IsScreamingGoat", this.isScreaming());
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      this.setScreaming(nbt.getBoolean("IsScreamingGoat"));
   }

   public void handleStatus(byte status) {
      if (status == 58) {
         this.field_33487 = true;
      } else if (status == 59) {
         this.field_33487 = false;
      } else {
         super.handleStatus(status);
      }

   }

   public void tickMovement() {
      if (this.field_33487) {
         ++this.field_33488;
      } else {
         this.field_33488 -= 2;
      }

      this.field_33488 = MathHelper.clamp((int)this.field_33488, (int)0, (int)20);
      super.tickMovement();
   }

   protected void initDataTracker() {
      super.initDataTracker();
      this.dataTracker.startTracking(SCREAMING, false);
   }

   public boolean isScreaming() {
      return (Boolean)this.dataTracker.get(SCREAMING);
   }

   public void setScreaming(boolean screaming) {
      this.dataTracker.set(SCREAMING, screaming);
   }

   public float method_36283() {
      return (float)this.field_33488 / 20.0F * 30.0F * 0.017453292F;
   }

   protected EntityNavigation createNavigation(World world) {
      return new GoatEntity.GoatNavigation(this, world);
   }

   static {
      SENSORS = ImmutableList.of(SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.NEAREST_ADULT, SensorType.HURT_BY, SensorType.GOAT_TEMPTATIONS);
      MEMORY_MODULES = ImmutableList.of(MemoryModuleType.LOOK_TARGET, MemoryModuleType.VISIBLE_MOBS, MemoryModuleType.WALK_TARGET, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryModuleType.PATH, MemoryModuleType.ATE_RECENTLY, MemoryModuleType.BREED_TARGET, MemoryModuleType.LONG_JUMP_COOLING_DOWN, MemoryModuleType.LONG_JUMP_MID_JUMP, MemoryModuleType.TEMPTING_PLAYER, MemoryModuleType.NEAREST_VISIBLE_ADULT, MemoryModuleType.TEMPTATION_COOLDOWN_TICKS, MemoryModuleType.IS_TEMPTED, MemoryModuleType.RAM_COOLDOWN_TICKS, MemoryModuleType.RAM_TARGET);
      SCREAMING = DataTracker.registerData(GoatEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
   }

   private static class GoatNavigation extends MobNavigation {
      GoatNavigation(GoatEntity goat, World world) {
         super(goat, world);
      }

      protected PathNodeNavigator createPathNodeNavigator(int range) {
         this.nodeMaker = new GoatEntity.GoatPathNodeMaker();
         return new PathNodeNavigator(this.nodeMaker, range);
      }
   }

   private static class GoatPathNodeMaker extends LandPathNodeMaker {
      private final BlockPos.Mutable pos = new BlockPos.Mutable();

      GoatPathNodeMaker() {
      }

      public PathNodeType getDefaultNodeType(BlockView world, int x, int y, int z) {
         this.pos.set(x, y - 1, z);
         PathNodeType pathNodeType = getCommonNodeType(world, this.pos);
         return pathNodeType == PathNodeType.POWDER_SNOW ? PathNodeType.BLOCKED : getLandNodeType(world, this.pos.move(Direction.UP));
      }
   }
}
