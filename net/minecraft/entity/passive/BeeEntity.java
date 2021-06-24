package net.minecraft.entity.passive;

import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CropBlock;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.StemBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.entity.BeehiveBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.Flutterer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.AboveGroundTargeting;
import net.minecraft.entity.ai.Durations;
import net.minecraft.entity.ai.NoPenaltySolidTargeting;
import net.minecraft.entity.ai.NoWaterTargeting;
import net.minecraft.entity.ai.control.FlightMoveControl;
import net.minecraft.entity.ai.control.LookControl;
import net.minecraft.entity.ai.goal.AnimalMateGoal;
import net.minecraft.entity.ai.goal.FollowParentGoal;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.ai.goal.UniversalAngerGoal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.Ingredient;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.IntProperty;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.WorldView;
import net.minecraft.world.poi.PointOfInterest;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestType;
import org.jetbrains.annotations.Nullable;

public class BeeEntity extends AnimalEntity implements Angerable, Flutterer {
   public static final float field_30271 = 120.32113F;
   public static final int field_28638 = MathHelper.ceil(1.4959966F);
   private static final TrackedData<Byte> BEE_FLAGS;
   private static final TrackedData<Integer> ANGER;
   private static final int NEAR_TARGET_FLAG = 2;
   private static final int HAS_STUNG_FLAG = 4;
   private static final int HAS_NECTAR_FLAG = 8;
   private static final int field_30284 = 1200;
   /**
    * A bee will start moving to a flower once this time in ticks has passed from a pollination.
    */
   private static final int FLOWER_NAVIGATION_START_TICKS = 2400;
   /**
    * The duration in ticks when a bee's pollination is considered failed.
    */
   private static final int POLLINATION_FAIL_TICKS = 3600;
   private static final int field_30287 = 4;
   private static final int MAX_POLLINATED_CROPS = 10;
   private static final int NORMAL_DIFFICULTY_STING_POISON_DURATION = 10;
   private static final int HARD_DIFFICULTY_STING_POISON_DURATION = 18;
   /**
    * The minimum distance that bees lose their hive or flower position at.
    */
   private static final int TOO_FAR_DISTANCE = 32;
   private static final int field_30292 = 2;
   /**
    * The minimum distance that bees will immediately return to their hive at.
    */
   private static final int MIN_HIVE_RETURN_DISTANCE = 16;
   private static final int field_30294 = 20;
   public static final String CROPS_GROWN_SINCE_POLLINATION_KEY = "CropsGrownSincePollination";
   public static final String CANNOT_ENTER_HIVE_TICKS_KEY = "CannotEnterHiveTicks";
   public static final String TICKS_SINCE_POLLINATION_KEY = "TicksSincePollination";
   public static final String HAS_STUNG_KEY = "HasStung";
   public static final String HAS_NECTAR_KEY = "HasNectar";
   public static final String FLOWER_POS_KEY = "FlowerPos";
   public static final String HIVE_POS_KEY = "HivePos";
   private static final UniformIntProvider ANGER_TIME_RANGE;
   private UUID targetUuid;
   private float currentPitch;
   private float lastPitch;
   private int ticksSinceSting;
   int ticksSincePollination;
   private int cannotEnterHiveTicks;
   private int cropsGrownSincePollination;
   private static final int field_30274 = 200;
   int ticksLeftToFindHive;
   private static final int field_30275 = 200;
   int ticksUntilCanPollinate;
   @Nullable
   BlockPos flowerPos;
   @Nullable
   BlockPos hivePos;
   BeeEntity.PollinateGoal pollinateGoal;
   BeeEntity.MoveToHiveGoal moveToHiveGoal;
   private BeeEntity.MoveToFlowerGoal moveToFlowerGoal;
   private int ticksInsideWater;

   public BeeEntity(EntityType<? extends BeeEntity> entityType, World world) {
      super(entityType, world);
      this.ticksUntilCanPollinate = MathHelper.nextInt(this.random, 20, 60);
      this.moveControl = new FlightMoveControl(this, 20, true);
      this.lookControl = new BeeEntity.BeeLookControl(this);
      this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, -1.0F);
      this.setPathfindingPenalty(PathNodeType.WATER, -1.0F);
      this.setPathfindingPenalty(PathNodeType.WATER_BORDER, 16.0F);
      this.setPathfindingPenalty(PathNodeType.COCOA, -1.0F);
      this.setPathfindingPenalty(PathNodeType.FENCE, -1.0F);
   }

   protected void initDataTracker() {
      super.initDataTracker();
      this.dataTracker.startTracking(BEE_FLAGS, (byte)0);
      this.dataTracker.startTracking(ANGER, 0);
   }

   public float getPathfindingFavor(BlockPos pos, WorldView world) {
      return world.getBlockState(pos).isAir() ? 10.0F : 0.0F;
   }

   protected void initGoals() {
      this.goalSelector.add(0, new BeeEntity.StingGoal(this, 1.399999976158142D, true));
      this.goalSelector.add(1, new BeeEntity.EnterHiveGoal());
      this.goalSelector.add(2, new AnimalMateGoal(this, 1.0D));
      this.goalSelector.add(3, new TemptGoal(this, 1.25D, Ingredient.fromTag(ItemTags.FLOWERS), false));
      this.pollinateGoal = new BeeEntity.PollinateGoal();
      this.goalSelector.add(4, this.pollinateGoal);
      this.goalSelector.add(5, new FollowParentGoal(this, 1.25D));
      this.goalSelector.add(5, new BeeEntity.FindHiveGoal());
      this.moveToHiveGoal = new BeeEntity.MoveToHiveGoal();
      this.goalSelector.add(5, this.moveToHiveGoal);
      this.moveToFlowerGoal = new BeeEntity.MoveToFlowerGoal();
      this.goalSelector.add(6, this.moveToFlowerGoal);
      this.goalSelector.add(7, new BeeEntity.GrowCropsGoal());
      this.goalSelector.add(8, new BeeEntity.BeeWanderAroundGoal());
      this.goalSelector.add(9, new SwimGoal(this));
      this.targetSelector.add(1, (new BeeEntity.BeeRevengeGoal(this)).setGroupRevenge(new Class[0]));
      this.targetSelector.add(2, new BeeEntity.BeeFollowTargetGoal(this));
      this.targetSelector.add(3, new UniversalAngerGoal(this, true));
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      if (this.hasHive()) {
         nbt.put("HivePos", NbtHelper.fromBlockPos(this.getHivePos()));
      }

      if (this.hasFlower()) {
         nbt.put("FlowerPos", NbtHelper.fromBlockPos(this.getFlowerPos()));
      }

      nbt.putBoolean("HasNectar", this.hasNectar());
      nbt.putBoolean("HasStung", this.hasStung());
      nbt.putInt("TicksSincePollination", this.ticksSincePollination);
      nbt.putInt("CannotEnterHiveTicks", this.cannotEnterHiveTicks);
      nbt.putInt("CropsGrownSincePollination", this.cropsGrownSincePollination);
      this.writeAngerToNbt(nbt);
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      this.hivePos = null;
      if (nbt.contains("HivePos")) {
         this.hivePos = NbtHelper.toBlockPos(nbt.getCompound("HivePos"));
      }

      this.flowerPos = null;
      if (nbt.contains("FlowerPos")) {
         this.flowerPos = NbtHelper.toBlockPos(nbt.getCompound("FlowerPos"));
      }

      super.readCustomDataFromNbt(nbt);
      this.setHasNectar(nbt.getBoolean("HasNectar"));
      this.setHasStung(nbt.getBoolean("HasStung"));
      this.ticksSincePollination = nbt.getInt("TicksSincePollination");
      this.cannotEnterHiveTicks = nbt.getInt("CannotEnterHiveTicks");
      this.cropsGrownSincePollination = nbt.getInt("CropsGrownSincePollination");
      this.readAngerFromNbt(this.world, nbt);
   }

   public boolean tryAttack(Entity target) {
      boolean bl = target.damage(DamageSource.sting(this), (float)((int)this.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE)));
      if (bl) {
         this.applyDamageEffects(this, target);
         if (target instanceof LivingEntity) {
            ((LivingEntity)target).setStingerCount(((LivingEntity)target).getStingerCount() + 1);
            int i = 0;
            if (this.world.getDifficulty() == Difficulty.NORMAL) {
               i = 10;
            } else if (this.world.getDifficulty() == Difficulty.HARD) {
               i = 18;
            }

            if (i > 0) {
               ((LivingEntity)target).addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, i * 20, 0), this);
            }
         }

         this.setHasStung(true);
         this.stopAnger();
         this.playSound(SoundEvents.ENTITY_BEE_STING, 1.0F, 1.0F);
      }

      return bl;
   }

   public void tick() {
      super.tick();
      if (this.hasNectar() && this.getCropsGrownSincePollination() < 10 && this.random.nextFloat() < 0.05F) {
         for(int i = 0; i < this.random.nextInt(2) + 1; ++i) {
            this.addParticle(this.world, this.getX() - 0.30000001192092896D, this.getX() + 0.30000001192092896D, this.getZ() - 0.30000001192092896D, this.getZ() + 0.30000001192092896D, this.getBodyY(0.5D), ParticleTypes.FALLING_NECTAR);
         }
      }

      this.updateBodyPitch();
   }

   private void addParticle(World world, double lastX, double x, double lastZ, double z, double y, ParticleEffect effect) {
      world.addParticle(effect, MathHelper.lerp(world.random.nextDouble(), lastX, x), y, MathHelper.lerp(world.random.nextDouble(), lastZ, z), 0.0D, 0.0D, 0.0D);
   }

   void startMovingTo(BlockPos pos) {
      Vec3d vec3d = Vec3d.ofBottomCenter(pos);
      int i = 0;
      BlockPos blockPos = this.getBlockPos();
      int j = (int)vec3d.y - blockPos.getY();
      if (j > 2) {
         i = 4;
      } else if (j < -2) {
         i = -4;
      }

      int k = 6;
      int l = 8;
      int m = blockPos.getManhattanDistance(pos);
      if (m < 15) {
         k = m / 2;
         l = m / 2;
      }

      Vec3d vec3d2 = NoWaterTargeting.find(this, k, l, i, vec3d, 0.3141592741012573D);
      if (vec3d2 != null) {
         this.navigation.setRangeMultiplier(0.5F);
         this.navigation.startMovingTo(vec3d2.x, vec3d2.y, vec3d2.z, 1.0D);
      }
   }

   @Nullable
   public BlockPos getFlowerPos() {
      return this.flowerPos;
   }

   public boolean hasFlower() {
      return this.flowerPos != null;
   }

   public void setFlowerPos(BlockPos pos) {
      this.flowerPos = pos;
   }

   @Debug
   public int getMoveGoalTicks() {
      return Math.max(this.moveToHiveGoal.ticks, this.moveToFlowerGoal.ticks);
   }

   @Debug
   public List<BlockPos> getPossibleHives() {
      return this.moveToHiveGoal.possibleHives;
   }

   private boolean failedPollinatingTooLong() {
      return this.ticksSincePollination > 3600;
   }

   boolean canEnterHive() {
      if (this.cannotEnterHiveTicks <= 0 && !this.pollinateGoal.isRunning() && !this.hasStung() && this.getTarget() == null) {
         boolean bl = this.failedPollinatingTooLong() || this.world.isRaining() || this.world.isNight() || this.hasNectar();
         return bl && !this.isHiveNearFire();
      } else {
         return false;
      }
   }

   public void setCannotEnterHiveTicks(int ticks) {
      this.cannotEnterHiveTicks = ticks;
   }

   public float getBodyPitch(float tickDelta) {
      return MathHelper.lerp(tickDelta, this.lastPitch, this.currentPitch);
   }

   private void updateBodyPitch() {
      this.lastPitch = this.currentPitch;
      if (this.isNearTarget()) {
         this.currentPitch = Math.min(1.0F, this.currentPitch + 0.2F);
      } else {
         this.currentPitch = Math.max(0.0F, this.currentPitch - 0.24F);
      }

   }

   protected void mobTick() {
      boolean bl = this.hasStung();
      if (this.isInsideWaterOrBubbleColumn()) {
         ++this.ticksInsideWater;
      } else {
         this.ticksInsideWater = 0;
      }

      if (this.ticksInsideWater > 20) {
         this.damage(DamageSource.DROWN, 1.0F);
      }

      if (bl) {
         ++this.ticksSinceSting;
         if (this.ticksSinceSting % 5 == 0 && this.random.nextInt(MathHelper.clamp((int)(1200 - this.ticksSinceSting), (int)1, (int)1200)) == 0) {
            this.damage(DamageSource.GENERIC, this.getHealth());
         }
      }

      if (!this.hasNectar()) {
         ++this.ticksSincePollination;
      }

      if (!this.world.isClient) {
         this.tickAngerLogic((ServerWorld)this.world, false);
      }

   }

   public void resetPollinationTicks() {
      this.ticksSincePollination = 0;
   }

   private boolean isHiveNearFire() {
      if (this.hivePos == null) {
         return false;
      } else {
         BlockEntity blockEntity = this.world.getBlockEntity(this.hivePos);
         return blockEntity instanceof BeehiveBlockEntity && ((BeehiveBlockEntity)blockEntity).isNearFire();
      }
   }

   public int getAngerTime() {
      return (Integer)this.dataTracker.get(ANGER);
   }

   public void setAngerTime(int ticks) {
      this.dataTracker.set(ANGER, ticks);
   }

   public UUID getAngryAt() {
      return this.targetUuid;
   }

   public void setAngryAt(@Nullable UUID uuid) {
      this.targetUuid = uuid;
   }

   public void chooseRandomAngerTime() {
      this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
   }

   private boolean doesHiveHaveSpace(BlockPos pos) {
      BlockEntity blockEntity = this.world.getBlockEntity(pos);
      if (blockEntity instanceof BeehiveBlockEntity) {
         return !((BeehiveBlockEntity)blockEntity).isFullOfBees();
      } else {
         return false;
      }
   }

   @Debug
   public boolean hasHive() {
      return this.hivePos != null;
   }

   @Nullable
   @Debug
   public BlockPos getHivePos() {
      return this.hivePos;
   }

   @Debug
   public GoalSelector getGoalSelector() {
      return this.goalSelector;
   }

   protected void sendAiDebugData() {
      super.sendAiDebugData();
      DebugInfoSender.sendBeeDebugData(this);
   }

   int getCropsGrownSincePollination() {
      return this.cropsGrownSincePollination;
   }

   private void resetCropCounter() {
      this.cropsGrownSincePollination = 0;
   }

   void addCropCounter() {
      ++this.cropsGrownSincePollination;
   }

   public void tickMovement() {
      super.tickMovement();
      if (!this.world.isClient) {
         if (this.cannotEnterHiveTicks > 0) {
            --this.cannotEnterHiveTicks;
         }

         if (this.ticksLeftToFindHive > 0) {
            --this.ticksLeftToFindHive;
         }

         if (this.ticksUntilCanPollinate > 0) {
            --this.ticksUntilCanPollinate;
         }

         boolean bl = this.hasAngerTime() && !this.hasStung() && this.getTarget() != null && this.getTarget().squaredDistanceTo(this) < 4.0D;
         this.setNearTarget(bl);
         if (this.age % 20 == 0 && !this.isHiveValid()) {
            this.hivePos = null;
         }
      }

   }

   boolean isHiveValid() {
      if (!this.hasHive()) {
         return false;
      } else {
         BlockEntity blockEntity = this.world.getBlockEntity(this.hivePos);
         return blockEntity != null && blockEntity.getType() == BlockEntityType.BEEHIVE;
      }
   }

   public boolean hasNectar() {
      return this.getBeeFlag(HAS_NECTAR_FLAG);
   }

   void setHasNectar(boolean hasNectar) {
      if (hasNectar) {
         this.resetPollinationTicks();
      }

      this.setBeeFlag(HAS_NECTAR_FLAG, hasNectar);
   }

   public boolean hasStung() {
      return this.getBeeFlag(HAS_STUNG_FLAG);
   }

   private void setHasStung(boolean hasStung) {
      this.setBeeFlag(HAS_STUNG_FLAG, hasStung);
   }

   private boolean isNearTarget() {
      return this.getBeeFlag(NEAR_TARGET_FLAG);
   }

   private void setNearTarget(boolean nearTarget) {
      this.setBeeFlag(NEAR_TARGET_FLAG, nearTarget);
   }

   boolean isTooFar(BlockPos pos) {
      return !this.isWithinDistance(pos, 32);
   }

   private void setBeeFlag(int bit, boolean value) {
      if (value) {
         this.dataTracker.set(BEE_FLAGS, (byte)((Byte)this.dataTracker.get(BEE_FLAGS) | bit));
      } else {
         this.dataTracker.set(BEE_FLAGS, (byte)((Byte)this.dataTracker.get(BEE_FLAGS) & ~bit));
      }

   }

   private boolean getBeeFlag(int location) {
      return ((Byte)this.dataTracker.get(BEE_FLAGS) & location) != 0;
   }

   public static DefaultAttributeContainer.Builder createBeeAttributes() {
      return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D).add(EntityAttributes.GENERIC_FLYING_SPEED, 0.6000000238418579D).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30000001192092896D).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 2.0D).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0D);
   }

   protected EntityNavigation createNavigation(World world) {
      BirdNavigation birdNavigation = new BirdNavigation(this, world) {
         public boolean isValidPosition(BlockPos pos) {
            return !this.world.getBlockState(pos.down()).isAir();
         }

         public void tick() {
            if (!BeeEntity.this.pollinateGoal.isRunning()) {
               super.tick();
            }
         }
      };
      birdNavigation.setCanPathThroughDoors(false);
      birdNavigation.setCanSwim(false);
      birdNavigation.setCanEnterOpenDoors(true);
      return birdNavigation;
   }

   public boolean isBreedingItem(ItemStack stack) {
      return stack.isIn(ItemTags.FLOWERS);
   }

   boolean isFlowers(BlockPos pos) {
      return this.world.canSetBlock(pos) && this.world.getBlockState(pos).isIn(BlockTags.FLOWERS);
   }

   protected void playStepSound(BlockPos pos, BlockState state) {
   }

   protected SoundEvent getAmbientSound() {
      return null;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_BEE_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_BEE_DEATH;
   }

   protected float getSoundVolume() {
      return 0.4F;
   }

   public BeeEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
      return (BeeEntity)EntityType.BEE.create(serverWorld);
   }

   protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
      return this.isBaby() ? dimensions.height * 0.5F : dimensions.height * 0.5F;
   }

   public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
      return false;
   }

   protected void fall(double heightDifference, boolean onGround, BlockState landedState, BlockPos landedPosition) {
   }

   public boolean hasWings() {
      return this.isInAir() && this.age % field_28638 == 0;
   }

   public boolean isInAir() {
      return !this.onGround;
   }

   public void onHoneyDelivered() {
      this.setHasNectar(false);
      this.resetCropCounter();
   }

   public boolean damage(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source)) {
         return false;
      } else {
         if (!this.world.isClient) {
            this.pollinateGoal.cancel();
         }

         return super.damage(source, amount);
      }
   }

   public EntityGroup getGroup() {
      return EntityGroup.ARTHROPOD;
   }

   protected void swimUpward(Tag<Fluid> fluid) {
      this.setVelocity(this.getVelocity().add(0.0D, 0.01D, 0.0D));
   }

   public Vec3d getLeashOffset() {
      return new Vec3d(0.0D, (double)(0.5F * this.getStandingEyeHeight()), (double)(this.getWidth() * 0.2F));
   }

   boolean isWithinDistance(BlockPos pos, int distance) {
      return pos.isWithinDistance(this.getBlockPos(), (double)distance);
   }

   static {
      BEE_FLAGS = DataTracker.registerData(BeeEntity.class, TrackedDataHandlerRegistry.BYTE);
      ANGER = DataTracker.registerData(BeeEntity.class, TrackedDataHandlerRegistry.INTEGER);
      ANGER_TIME_RANGE = Durations.betweenSeconds(20, 39);
   }

   class PollinateGoal extends BeeEntity.NotAngryGoal {
      private static final int field_30300 = 400;
      private static final int field_30301 = 20;
      private static final int field_30302 = 60;
      private final Predicate<BlockState> flowerPredicate = (state) -> {
         if (state.isIn(BlockTags.FLOWERS)) {
            if (state.isOf(Blocks.SUNFLOWER)) {
               return state.get(TallPlantBlock.HALF) == DoubleBlockHalf.UPPER;
            } else {
               return true;
            }
         } else {
            return false;
         }
      };
      private static final double field_30303 = 0.1D;
      private static final int field_30304 = 25;
      private static final float field_30305 = 0.35F;
      private static final float field_30306 = 0.6F;
      private static final float field_30307 = 0.33333334F;
      private int pollinationTicks;
      private int lastPollinationTick;
      private boolean running;
      private Vec3d nextTarget;
      private int ticks;
      private static final int field_30308 = 600;

      PollinateGoal() {
         super();
         this.setControls(EnumSet.of(Goal.Control.MOVE));
      }

      public boolean canBeeStart() {
         if (BeeEntity.this.ticksUntilCanPollinate > 0) {
            return false;
         } else if (BeeEntity.this.hasNectar()) {
            return false;
         } else if (BeeEntity.this.world.isRaining()) {
            return false;
         } else {
            Optional<BlockPos> optional = this.getFlower();
            if (optional.isPresent()) {
               BeeEntity.this.flowerPos = (BlockPos)optional.get();
               BeeEntity.this.navigation.startMovingTo((double)BeeEntity.this.flowerPos.getX() + 0.5D, (double)BeeEntity.this.flowerPos.getY() + 0.5D, (double)BeeEntity.this.flowerPos.getZ() + 0.5D, 1.2000000476837158D);
               return true;
            } else {
               BeeEntity.this.ticksUntilCanPollinate = MathHelper.nextInt(BeeEntity.this.random, 20, 60);
               return false;
            }
         }
      }

      public boolean canBeeContinue() {
         if (!this.running) {
            return false;
         } else if (!BeeEntity.this.hasFlower()) {
            return false;
         } else if (BeeEntity.this.world.isRaining()) {
            return false;
         } else if (this.completedPollination()) {
            return BeeEntity.this.random.nextFloat() < 0.2F;
         } else if (BeeEntity.this.age % 20 == 0 && !BeeEntity.this.isFlowers(BeeEntity.this.flowerPos)) {
            BeeEntity.this.flowerPos = null;
            return false;
         } else {
            return true;
         }
      }

      private boolean completedPollination() {
         return this.pollinationTicks > 400;
      }

      boolean isRunning() {
         return this.running;
      }

      void cancel() {
         this.running = false;
      }

      public void start() {
         this.pollinationTicks = 0;
         this.ticks = 0;
         this.lastPollinationTick = 0;
         this.running = true;
         BeeEntity.this.resetPollinationTicks();
      }

      public void stop() {
         if (this.completedPollination()) {
            BeeEntity.this.setHasNectar(true);
         }

         this.running = false;
         BeeEntity.this.navigation.stop();
         BeeEntity.this.ticksUntilCanPollinate = 200;
      }

      public void tick() {
         ++this.ticks;
         if (this.ticks > 600) {
            BeeEntity.this.flowerPos = null;
         } else {
            Vec3d vec3d = Vec3d.ofBottomCenter(BeeEntity.this.flowerPos).add(0.0D, 0.6000000238418579D, 0.0D);
            if (vec3d.distanceTo(BeeEntity.this.getPos()) > 1.0D) {
               this.nextTarget = vec3d;
               this.moveToNextTarget();
            } else {
               if (this.nextTarget == null) {
                  this.nextTarget = vec3d;
               }

               boolean bl = BeeEntity.this.getPos().distanceTo(this.nextTarget) <= 0.1D;
               boolean bl2 = true;
               if (!bl && this.ticks > 600) {
                  BeeEntity.this.flowerPos = null;
               } else {
                  if (bl) {
                     boolean bl3 = BeeEntity.this.random.nextInt(25) == 0;
                     if (bl3) {
                        this.nextTarget = new Vec3d(vec3d.getX() + (double)this.getRandomOffset(), vec3d.getY(), vec3d.getZ() + (double)this.getRandomOffset());
                        BeeEntity.this.navigation.stop();
                     } else {
                        bl2 = false;
                     }

                     BeeEntity.this.getLookControl().lookAt(vec3d.getX(), vec3d.getY(), vec3d.getZ());
                  }

                  if (bl2) {
                     this.moveToNextTarget();
                  }

                  ++this.pollinationTicks;
                  if (BeeEntity.this.random.nextFloat() < 0.05F && this.pollinationTicks > this.lastPollinationTick + 60) {
                     this.lastPollinationTick = this.pollinationTicks;
                     BeeEntity.this.playSound(SoundEvents.ENTITY_BEE_POLLINATE, 1.0F, 1.0F);
                  }

               }
            }
         }
      }

      private void moveToNextTarget() {
         BeeEntity.this.getMoveControl().moveTo(this.nextTarget.getX(), this.nextTarget.getY(), this.nextTarget.getZ(), 0.3499999940395355D);
      }

      private float getRandomOffset() {
         return (BeeEntity.this.random.nextFloat() * 2.0F - 1.0F) * 0.33333334F;
      }

      private Optional<BlockPos> getFlower() {
         return this.findFlower(this.flowerPredicate, 5.0D);
      }

      private Optional<BlockPos> findFlower(Predicate<BlockState> predicate, double searchDistance) {
         BlockPos blockPos = BeeEntity.this.getBlockPos();
         BlockPos.Mutable mutable = new BlockPos.Mutable();

         for(int i = 0; (double)i <= searchDistance; i = i > 0 ? -i : 1 - i) {
            for(int j = 0; (double)j < searchDistance; ++j) {
               for(int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
                  for(int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l) {
                     mutable.set((Vec3i)blockPos, k, i - 1, l);
                     if (blockPos.isWithinDistance(mutable, searchDistance) && predicate.test(BeeEntity.this.world.getBlockState(mutable))) {
                        return Optional.of(mutable);
                     }
                  }
               }
            }
         }

         return Optional.empty();
      }
   }

   private class BeeLookControl extends LookControl {
      BeeLookControl(MobEntity entity) {
         super(entity);
      }

      public void tick() {
         if (!BeeEntity.this.hasAngerTime()) {
            super.tick();
         }
      }

      protected boolean shouldStayHorizontal() {
         return !BeeEntity.this.pollinateGoal.isRunning();
      }
   }

   private class StingGoal extends MeleeAttackGoal {
      StingGoal(PathAwareEntity mob, double speed, boolean pauseWhenMobIdle) {
         super(mob, speed, pauseWhenMobIdle);
      }

      public boolean canStart() {
         return super.canStart() && BeeEntity.this.hasAngerTime() && !BeeEntity.this.hasStung();
      }

      public boolean shouldContinue() {
         return super.shouldContinue() && BeeEntity.this.hasAngerTime() && !BeeEntity.this.hasStung();
      }
   }

   class EnterHiveGoal extends BeeEntity.NotAngryGoal {
      EnterHiveGoal() {
         super();
      }

      public boolean canBeeStart() {
         if (BeeEntity.this.hasHive() && BeeEntity.this.canEnterHive() && BeeEntity.this.hivePos.isWithinDistance(BeeEntity.this.getPos(), 2.0D)) {
            BlockEntity blockEntity = BeeEntity.this.world.getBlockEntity(BeeEntity.this.hivePos);
            if (blockEntity instanceof BeehiveBlockEntity) {
               BeehiveBlockEntity beehiveBlockEntity = (BeehiveBlockEntity)blockEntity;
               if (!beehiveBlockEntity.isFullOfBees()) {
                  return true;
               }

               BeeEntity.this.hivePos = null;
            }
         }

         return false;
      }

      public boolean canBeeContinue() {
         return false;
      }

      public void start() {
         BlockEntity blockEntity = BeeEntity.this.world.getBlockEntity(BeeEntity.this.hivePos);
         if (blockEntity instanceof BeehiveBlockEntity) {
            BeehiveBlockEntity beehiveBlockEntity = (BeehiveBlockEntity)blockEntity;
            beehiveBlockEntity.tryEnterHive(BeeEntity.this, BeeEntity.this.hasNectar());
         }

      }
   }

   class FindHiveGoal extends BeeEntity.NotAngryGoal {
      FindHiveGoal() {
         super();
      }

      public boolean canBeeStart() {
         return BeeEntity.this.ticksLeftToFindHive == 0 && !BeeEntity.this.hasHive() && BeeEntity.this.canEnterHive();
      }

      public boolean canBeeContinue() {
         return false;
      }

      public void start() {
         BeeEntity.this.ticksLeftToFindHive = 200;
         List<BlockPos> list = this.getNearbyFreeHives();
         if (!list.isEmpty()) {
            Iterator var2 = list.iterator();

            BlockPos blockPos;
            do {
               if (!var2.hasNext()) {
                  BeeEntity.this.moveToHiveGoal.clearPossibleHives();
                  BeeEntity.this.hivePos = (BlockPos)list.get(0);
                  return;
               }

               blockPos = (BlockPos)var2.next();
            } while(BeeEntity.this.moveToHiveGoal.isPossibleHive(blockPos));

            BeeEntity.this.hivePos = blockPos;
         }
      }

      private List<BlockPos> getNearbyFreeHives() {
         BlockPos blockPos = BeeEntity.this.getBlockPos();
         PointOfInterestStorage pointOfInterestStorage = ((ServerWorld)BeeEntity.this.world).getPointOfInterestStorage();
         Stream<PointOfInterest> stream = pointOfInterestStorage.getInCircle((pointOfInterestType) -> {
            return pointOfInterestType == PointOfInterestType.BEEHIVE || pointOfInterestType == PointOfInterestType.BEE_NEST;
         }, blockPos, 20, PointOfInterestStorage.OccupationStatus.ANY);
         return (List)stream.map(PointOfInterest::getPos).filter(BeeEntity.this::doesHiveHaveSpace).sorted(Comparator.comparingDouble((blockPos2) -> {
            return blockPos2.getSquaredDistance(blockPos);
         })).collect(Collectors.toList());
      }
   }

   @Debug
   public class MoveToHiveGoal extends BeeEntity.NotAngryGoal {
      public static final int field_30295 = 600;
      int ticks;
      private static final int field_30296 = 3;
      final List<BlockPos> possibleHives;
      @Nullable
      private Path path;
      private static final int field_30297 = 60;
      private int ticksUntilLost;

      MoveToHiveGoal() {
         super();
         this.ticks = BeeEntity.this.world.random.nextInt(10);
         this.possibleHives = Lists.newArrayList();
         this.setControls(EnumSet.of(Goal.Control.MOVE));
      }

      public boolean canBeeStart() {
         return BeeEntity.this.hivePos != null && !BeeEntity.this.hasPositionTarget() && BeeEntity.this.canEnterHive() && !this.isCloseEnough(BeeEntity.this.hivePos) && BeeEntity.this.world.getBlockState(BeeEntity.this.hivePos).isIn(BlockTags.BEEHIVES);
      }

      public boolean canBeeContinue() {
         return this.canBeeStart();
      }

      public void start() {
         this.ticks = 0;
         this.ticksUntilLost = 0;
         super.start();
      }

      public void stop() {
         this.ticks = 0;
         this.ticksUntilLost = 0;
         BeeEntity.this.navigation.stop();
         BeeEntity.this.navigation.resetRangeMultiplier();
      }

      public void tick() {
         if (BeeEntity.this.hivePos != null) {
            ++this.ticks;
            if (this.ticks > 600) {
               this.makeChosenHivePossibleHive();
            } else if (!BeeEntity.this.navigation.isFollowingPath()) {
               if (!BeeEntity.this.isWithinDistance(BeeEntity.this.hivePos, 16)) {
                  if (BeeEntity.this.isTooFar(BeeEntity.this.hivePos)) {
                     this.setLost();
                  } else {
                     BeeEntity.this.startMovingTo(BeeEntity.this.hivePos);
                  }
               } else {
                  boolean bl = this.startMovingToFar(BeeEntity.this.hivePos);
                  if (!bl) {
                     this.makeChosenHivePossibleHive();
                  } else if (this.path != null && BeeEntity.this.navigation.getCurrentPath().equalsPath(this.path)) {
                     ++this.ticksUntilLost;
                     if (this.ticksUntilLost > 60) {
                        this.setLost();
                        this.ticksUntilLost = 0;
                     }
                  } else {
                     this.path = BeeEntity.this.navigation.getCurrentPath();
                  }

               }
            }
         }
      }

      private boolean startMovingToFar(BlockPos pos) {
         BeeEntity.this.navigation.setRangeMultiplier(10.0F);
         BeeEntity.this.navigation.startMovingTo((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), 1.0D);
         return BeeEntity.this.navigation.getCurrentPath() != null && BeeEntity.this.navigation.getCurrentPath().reachesTarget();
      }

      boolean isPossibleHive(BlockPos pos) {
         return this.possibleHives.contains(pos);
      }

      private void addPossibleHive(BlockPos pos) {
         this.possibleHives.add(pos);

         while(this.possibleHives.size() > 3) {
            this.possibleHives.remove(0);
         }

      }

      void clearPossibleHives() {
         this.possibleHives.clear();
      }

      private void makeChosenHivePossibleHive() {
         if (BeeEntity.this.hivePos != null) {
            this.addPossibleHive(BeeEntity.this.hivePos);
         }

         this.setLost();
      }

      private void setLost() {
         BeeEntity.this.hivePos = null;
         BeeEntity.this.ticksLeftToFindHive = 200;
      }

      private boolean isCloseEnough(BlockPos pos) {
         if (BeeEntity.this.isWithinDistance(pos, 2)) {
            return true;
         } else {
            Path path = BeeEntity.this.navigation.getCurrentPath();
            return path != null && path.getTarget().equals(pos) && path.reachesTarget() && path.isFinished();
         }
      }
   }

   public class MoveToFlowerGoal extends BeeEntity.NotAngryGoal {
      private static final int MAX_FLOWER_NAVIGATION_TICKS = 600;
      int ticks;

      MoveToFlowerGoal() {
         super();
         this.ticks = BeeEntity.this.world.random.nextInt(10);
         this.setControls(EnumSet.of(Goal.Control.MOVE));
      }

      public boolean canBeeStart() {
         return BeeEntity.this.flowerPos != null && !BeeEntity.this.hasPositionTarget() && this.shouldMoveToFlower() && BeeEntity.this.isFlowers(BeeEntity.this.flowerPos) && !BeeEntity.this.isWithinDistance(BeeEntity.this.flowerPos, 2);
      }

      public boolean canBeeContinue() {
         return this.canBeeStart();
      }

      public void start() {
         this.ticks = 0;
         super.start();
      }

      public void stop() {
         this.ticks = 0;
         BeeEntity.this.navigation.stop();
         BeeEntity.this.navigation.resetRangeMultiplier();
      }

      public void tick() {
         if (BeeEntity.this.flowerPos != null) {
            ++this.ticks;
            if (this.ticks > 600) {
               BeeEntity.this.flowerPos = null;
            } else if (!BeeEntity.this.navigation.isFollowingPath()) {
               if (BeeEntity.this.isTooFar(BeeEntity.this.flowerPos)) {
                  BeeEntity.this.flowerPos = null;
               } else {
                  BeeEntity.this.startMovingTo(BeeEntity.this.flowerPos);
               }
            }
         }
      }

      private boolean shouldMoveToFlower() {
         return BeeEntity.this.ticksSincePollination > 2400;
      }
   }

   private class GrowCropsGoal extends BeeEntity.NotAngryGoal {
      static final int field_30299 = 30;

      GrowCropsGoal() {
         super();
      }

      public boolean canBeeStart() {
         if (BeeEntity.this.getCropsGrownSincePollination() >= 10) {
            return false;
         } else if (BeeEntity.this.random.nextFloat() < 0.3F) {
            return false;
         } else {
            return BeeEntity.this.hasNectar() && BeeEntity.this.isHiveValid();
         }
      }

      public boolean canBeeContinue() {
         return this.canBeeStart();
      }

      public void tick() {
         if (BeeEntity.this.random.nextInt(30) == 0) {
            for(int i = 1; i <= 2; ++i) {
               BlockPos blockPos = BeeEntity.this.getBlockPos().down(i);
               BlockState blockState = BeeEntity.this.world.getBlockState(blockPos);
               Block block = blockState.getBlock();
               boolean bl = false;
               IntProperty intProperty = null;
               if (blockState.isIn(BlockTags.BEE_GROWABLES)) {
                  if (block instanceof CropBlock) {
                     CropBlock cropBlock = (CropBlock)block;
                     if (!cropBlock.isMature(blockState)) {
                        bl = true;
                        intProperty = cropBlock.getAgeProperty();
                     }
                  } else {
                     int k;
                     if (block instanceof StemBlock) {
                        k = (Integer)blockState.get(StemBlock.AGE);
                        if (k < 7) {
                           bl = true;
                           intProperty = StemBlock.AGE;
                        }
                     } else if (blockState.isOf(Blocks.SWEET_BERRY_BUSH)) {
                        k = (Integer)blockState.get(SweetBerryBushBlock.AGE);
                        if (k < 3) {
                           bl = true;
                           intProperty = SweetBerryBushBlock.AGE;
                        }
                     } else if (blockState.isOf(Blocks.CAVE_VINES) || blockState.isOf(Blocks.CAVE_VINES_PLANT)) {
                        ((Fertilizable)blockState.getBlock()).grow((ServerWorld)BeeEntity.this.world, BeeEntity.this.random, blockPos, blockState);
                     }
                  }

                  if (bl) {
                     BeeEntity.this.world.syncWorldEvent(WorldEvents.PLANT_FERTILIZED, blockPos, 0);
                     BeeEntity.this.world.setBlockState(blockPos, (BlockState)blockState.with(intProperty, (Integer)blockState.get(intProperty) + 1));
                     BeeEntity.this.addCropCounter();
                  }
               }
            }

         }
      }
   }

   private class BeeWanderAroundGoal extends Goal {
      private static final int field_30309 = 22;

      BeeWanderAroundGoal() {
         this.setControls(EnumSet.of(Goal.Control.MOVE));
      }

      public boolean canStart() {
         return BeeEntity.this.navigation.isIdle() && BeeEntity.this.random.nextInt(10) == 0;
      }

      public boolean shouldContinue() {
         return BeeEntity.this.navigation.isFollowingPath();
      }

      public void start() {
         Vec3d vec3d = this.getRandomLocation();
         if (vec3d != null) {
            BeeEntity.this.navigation.startMovingAlong(BeeEntity.this.navigation.findPathTo((BlockPos)(new BlockPos(vec3d)), 1), 1.0D);
         }

      }

      @Nullable
      private Vec3d getRandomLocation() {
         Vec3d vec3d3;
         if (BeeEntity.this.isHiveValid() && !BeeEntity.this.isWithinDistance(BeeEntity.this.hivePos, 22)) {
            Vec3d vec3d = Vec3d.ofCenter(BeeEntity.this.hivePos);
            vec3d3 = vec3d.subtract(BeeEntity.this.getPos()).normalize();
         } else {
            vec3d3 = BeeEntity.this.getRotationVec(0.0F);
         }

         int i = true;
         Vec3d vec3d4 = AboveGroundTargeting.find(BeeEntity.this, 8, 7, vec3d3.x, vec3d3.z, 1.5707964F, 3, 1);
         return vec3d4 != null ? vec3d4 : NoPenaltySolidTargeting.find(BeeEntity.this, 8, 4, -2, vec3d3.x, vec3d3.z, 1.5707963705062866D);
      }
   }

   private class BeeRevengeGoal extends RevengeGoal {
      BeeRevengeGoal(BeeEntity bee) {
         super(bee);
      }

      public boolean shouldContinue() {
         return BeeEntity.this.hasAngerTime() && super.shouldContinue();
      }

      protected void setMobEntityTarget(MobEntity mob, LivingEntity target) {
         if (mob instanceof BeeEntity && this.mob.canSee(target)) {
            mob.setTarget(target);
         }

      }
   }

   private static class BeeFollowTargetGoal extends FollowTargetGoal<PlayerEntity> {
      BeeFollowTargetGoal(BeeEntity bee) {
         Objects.requireNonNull(bee);
         super(bee, PlayerEntity.class, 10, true, false, bee::shouldAngerAt);
      }

      public boolean canStart() {
         return this.canSting() && super.canStart();
      }

      public boolean shouldContinue() {
         boolean bl = this.canSting();
         if (bl && this.mob.getTarget() != null) {
            return super.shouldContinue();
         } else {
            this.target = null;
            return false;
         }
      }

      private boolean canSting() {
         BeeEntity beeEntity = (BeeEntity)this.mob;
         return beeEntity.hasAngerTime() && !beeEntity.hasStung();
      }
   }

   private abstract class NotAngryGoal extends Goal {
      NotAngryGoal() {
      }

      public abstract boolean canBeeStart();

      public abstract boolean canBeeContinue();

      public boolean canStart() {
         return this.canBeeStart() && !BeeEntity.this.hasAngerTime();
      }

      public boolean shouldContinue() {
         return this.canBeeContinue() && !BeeEntity.this.hasAngerTime();
      }
   }
}
