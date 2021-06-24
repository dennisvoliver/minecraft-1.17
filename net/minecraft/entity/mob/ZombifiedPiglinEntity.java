package net.minecraft.entity.mob;

import java.util.Random;
import java.util.UUID;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.Durations;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.UniversalAngerGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.world.Difficulty;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class ZombifiedPiglinEntity extends ZombieEntity implements Angerable {
   private static final UUID ATTACKING_SPEED_BOOST_ID = UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718");
   private static final EntityAttributeModifier ATTACKING_SPEED_BOOST;
   private static final UniformIntProvider ANGRY_SOUND_DELAY_RANGE;
   private int angrySoundDelay;
   private static final UniformIntProvider ANGER_TIME_RANGE;
   private int angerTime;
   private UUID targetUuid;
   private static final int field_30524 = 10;
   private static final UniformIntProvider ANGER_PASSING_COOLDOWN_RANGE;
   private int angerPassingCooldown;

   public ZombifiedPiglinEntity(EntityType<? extends ZombifiedPiglinEntity> entityType, World world) {
      super(entityType, world);
      this.setPathfindingPenalty(PathNodeType.LAVA, 8.0F);
   }

   public void setAngryAt(@Nullable UUID uuid) {
      this.targetUuid = uuid;
   }

   public double getHeightOffset() {
      return this.isBaby() ? -0.05D : -0.45D;
   }

   protected void initCustomGoals() {
      this.goalSelector.add(2, new ZombieAttackGoal(this, 1.0D, false));
      this.goalSelector.add(7, new WanderAroundFarGoal(this, 1.0D));
      this.targetSelector.add(1, (new RevengeGoal(this, new Class[0])).setGroupRevenge());
      this.targetSelector.add(2, new FollowTargetGoal(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
      this.targetSelector.add(3, new UniversalAngerGoal(this, true));
   }

   public static DefaultAttributeContainer.Builder createZombifiedPiglinAttributes() {
      return ZombieEntity.createZombieAttributes().add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS, 0.0D).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23000000417232513D).add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D);
   }

   protected boolean canConvertInWater() {
      return false;
   }

   protected void mobTick() {
      EntityAttributeInstance entityAttributeInstance = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
      if (this.hasAngerTime()) {
         if (!this.isBaby() && !entityAttributeInstance.hasModifier(ATTACKING_SPEED_BOOST)) {
            entityAttributeInstance.addTemporaryModifier(ATTACKING_SPEED_BOOST);
         }

         this.tickAngrySound();
      } else if (entityAttributeInstance.hasModifier(ATTACKING_SPEED_BOOST)) {
         entityAttributeInstance.removeModifier(ATTACKING_SPEED_BOOST);
      }

      this.tickAngerLogic((ServerWorld)this.world, true);
      if (this.getTarget() != null) {
         this.tickAngerPassing();
      }

      if (this.hasAngerTime()) {
         this.playerHitTimer = this.age;
      }

      super.mobTick();
   }

   private void tickAngrySound() {
      if (this.angrySoundDelay > 0) {
         --this.angrySoundDelay;
         if (this.angrySoundDelay == 0) {
            this.playAngrySound();
         }
      }

   }

   private void tickAngerPassing() {
      if (this.angerPassingCooldown > 0) {
         --this.angerPassingCooldown;
      } else {
         if (this.getVisibilityCache().canSee(this.getTarget())) {
            this.angerNearbyZombifiedPiglins();
         }

         this.angerPassingCooldown = ANGER_PASSING_COOLDOWN_RANGE.get(this.random);
      }
   }

   private void angerNearbyZombifiedPiglins() {
      double d = this.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE);
      Box box = Box.from(this.getPos()).expand(d, 10.0D, d);
      this.world.getEntitiesByClass(ZombifiedPiglinEntity.class, box, EntityPredicates.EXCEPT_SPECTATOR).stream().filter((zombifiedPiglin) -> {
         return zombifiedPiglin != this;
      }).filter((zombifiedPiglin) -> {
         return zombifiedPiglin.getTarget() == null;
      }).filter((zombifiedPiglin) -> {
         return !zombifiedPiglin.isTeammate(this.getTarget());
      }).forEach((zombifiedPiglin) -> {
         zombifiedPiglin.setTarget(this.getTarget());
      });
   }

   private void playAngrySound() {
      this.playSound(SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_ANGRY, this.getSoundVolume() * 2.0F, this.getSoundPitch() * 1.8F);
   }

   public void setTarget(@Nullable LivingEntity target) {
      if (this.getTarget() == null && target != null) {
         this.angrySoundDelay = ANGRY_SOUND_DELAY_RANGE.get(this.random);
         this.angerPassingCooldown = ANGER_PASSING_COOLDOWN_RANGE.get(this.random);
      }

      if (target instanceof PlayerEntity) {
         this.setAttacking((PlayerEntity)target);
      }

      super.setTarget(target);
   }

   public void chooseRandomAngerTime() {
      this.setAngerTime(ANGER_TIME_RANGE.get(this.random));
   }

   public static boolean canSpawn(EntityType<ZombifiedPiglinEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
      return world.getDifficulty() != Difficulty.PEACEFUL && !world.getBlockState(pos.down()).isOf(Blocks.NETHER_WART_BLOCK);
   }

   public boolean canSpawn(WorldView world) {
      return world.intersectsEntities(this) && !world.containsFluid(this.getBoundingBox());
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      this.writeAngerToNbt(nbt);
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      this.readAngerFromNbt(this.world, nbt);
   }

   public void setAngerTime(int ticks) {
      this.angerTime = ticks;
   }

   public int getAngerTime() {
      return this.angerTime;
   }

   protected SoundEvent getAmbientSound() {
      return this.hasAngerTime() ? SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_ANGRY : SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_ZOMBIFIED_PIGLIN_DEATH;
   }

   protected void initEquipment(LocalDifficulty difficulty) {
      this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
   }

   protected ItemStack getSkull() {
      return ItemStack.EMPTY;
   }

   protected void initAttributes() {
      this.getAttributeInstance(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS).setBaseValue(0.0D);
   }

   public UUID getAngryAt() {
      return this.targetUuid;
   }

   public boolean isAngryAt(PlayerEntity player) {
      return this.shouldAngerAt(player);
   }

   static {
      ATTACKING_SPEED_BOOST = new EntityAttributeModifier(ATTACKING_SPEED_BOOST_ID, "Attacking speed boost", 0.05D, EntityAttributeModifier.Operation.ADDITION);
      ANGRY_SOUND_DELAY_RANGE = Durations.betweenSeconds(0, 1);
      ANGER_TIME_RANGE = Durations.betweenSeconds(20, 39);
      ANGER_PASSING_COOLDOWN_RANGE = Durations.betweenSeconds(4, 6);
   }
}
