package net.minecraft.entity.boss;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.entity.feature.SkinOverlayOwner;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.FollowTargetGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

public class WitherEntity extends HostileEntity implements SkinOverlayOwner, RangedAttackMob {
   private static final TrackedData<Integer> TRACKED_ENTITY_ID_1;
   private static final TrackedData<Integer> TRACKED_ENTITY_ID_2;
   private static final TrackedData<Integer> TRACKED_ENTITY_ID_3;
   private static final List<TrackedData<Integer>> TRACKED_ENTITY_IDS;
   private static final TrackedData<Integer> INVUL_TIMER;
   private static final int DEFAULT_INVUL_TIMER = 220;
   private final float[] sideHeadPitches = new float[2];
   private final float[] sideHeadYaws = new float[2];
   private final float[] prevSideHeadPitches = new float[2];
   private final float[] prevSideHeadYaws = new float[2];
   private final int[] skullCooldowns = new int[2];
   private final int[] chargedSkullCooldowns = new int[2];
   private int blockBreakingCooldown;
   private final ServerBossBar bossBar;
   private static final Predicate<LivingEntity> CAN_ATTACK_PREDICATE;
   private static final TargetPredicate HEAD_TARGET_PREDICATE;

   public WitherEntity(EntityType<? extends WitherEntity> entityType, World world) {
      super(entityType, world);
      this.bossBar = (ServerBossBar)(new ServerBossBar(this.getDisplayName(), BossBar.Color.PURPLE, BossBar.Style.PROGRESS)).setDarkenSky(true);
      this.setHealth(this.getMaxHealth());
      this.getNavigation().setCanSwim(true);
      this.experiencePoints = 50;
   }

   protected void initGoals() {
      this.goalSelector.add(0, new WitherEntity.DescendAtHalfHealthGoal());
      this.goalSelector.add(2, new ProjectileAttackGoal(this, 1.0D, 40, 20.0F));
      this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0D));
      this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
      this.goalSelector.add(7, new LookAroundGoal(this));
      this.targetSelector.add(1, new RevengeGoal(this, new Class[0]));
      this.targetSelector.add(2, new FollowTargetGoal(this, MobEntity.class, 0, false, false, CAN_ATTACK_PREDICATE));
   }

   protected void initDataTracker() {
      super.initDataTracker();
      this.dataTracker.startTracking(TRACKED_ENTITY_ID_1, 0);
      this.dataTracker.startTracking(TRACKED_ENTITY_ID_2, 0);
      this.dataTracker.startTracking(TRACKED_ENTITY_ID_3, 0);
      this.dataTracker.startTracking(INVUL_TIMER, 0);
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      nbt.putInt("Invul", this.getInvulnerableTimer());
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      this.setInvulTimer(nbt.getInt("Invul"));
      if (this.hasCustomName()) {
         this.bossBar.setName(this.getDisplayName());
      }

   }

   public void setCustomName(@Nullable Text name) {
      super.setCustomName(name);
      this.bossBar.setName(this.getDisplayName());
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.ENTITY_WITHER_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_WITHER_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_WITHER_DEATH;
   }

   public void tickMovement() {
      Vec3d vec3d = this.getVelocity().multiply(1.0D, 0.6D, 1.0D);
      if (!this.world.isClient && this.getTrackedEntityId(0) > 0) {
         Entity entity = this.world.getEntityById(this.getTrackedEntityId(0));
         if (entity != null) {
            double d = vec3d.y;
            if (this.getY() < entity.getY() || !this.shouldRenderOverlay() && this.getY() < entity.getY() + 5.0D) {
               d = Math.max(0.0D, d);
               d += 0.3D - d * 0.6000000238418579D;
            }

            vec3d = new Vec3d(vec3d.x, d, vec3d.z);
            Vec3d vec3d2 = new Vec3d(entity.getX() - this.getX(), 0.0D, entity.getZ() - this.getZ());
            if (vec3d2.horizontalLengthSquared() > 9.0D) {
               Vec3d vec3d3 = vec3d2.normalize();
               vec3d = vec3d.add(vec3d3.x * 0.3D - vec3d.x * 0.6D, 0.0D, vec3d3.z * 0.3D - vec3d.z * 0.6D);
            }
         }
      }

      this.setVelocity(vec3d);
      if (vec3d.horizontalLengthSquared() > 0.05D) {
         this.setYaw((float)MathHelper.atan2(vec3d.z, vec3d.x) * 57.295776F - 90.0F);
      }

      super.tickMovement();

      int j;
      for(j = 0; j < 2; ++j) {
         this.prevSideHeadYaws[j] = this.sideHeadYaws[j];
         this.prevSideHeadPitches[j] = this.sideHeadPitches[j];
      }

      int u;
      for(j = 0; j < 2; ++j) {
         u = this.getTrackedEntityId(j + 1);
         Entity entity2 = null;
         if (u > 0) {
            entity2 = this.world.getEntityById(u);
         }

         if (entity2 != null) {
            double e = this.getHeadX(j + 1);
            double f = this.getHeadY(j + 1);
            double g = this.getHeadZ(j + 1);
            double h = entity2.getX() - e;
            double l = entity2.getEyeY() - f;
            double m = entity2.getZ() - g;
            double n = Math.sqrt(h * h + m * m);
            float o = (float)(MathHelper.atan2(m, h) * 57.2957763671875D) - 90.0F;
            float p = (float)(-(MathHelper.atan2(l, n) * 57.2957763671875D));
            this.sideHeadPitches[j] = this.getNextAngle(this.sideHeadPitches[j], p, 40.0F);
            this.sideHeadYaws[j] = this.getNextAngle(this.sideHeadYaws[j], o, 10.0F);
         } else {
            this.sideHeadYaws[j] = this.getNextAngle(this.sideHeadYaws[j], this.bodyYaw, 10.0F);
         }
      }

      boolean bl = this.shouldRenderOverlay();

      for(u = 0; u < 3; ++u) {
         double r = this.getHeadX(u);
         double s = this.getHeadY(u);
         double t = this.getHeadZ(u);
         this.world.addParticle(ParticleTypes.SMOKE, r + this.random.nextGaussian() * 0.30000001192092896D, s + this.random.nextGaussian() * 0.30000001192092896D, t + this.random.nextGaussian() * 0.30000001192092896D, 0.0D, 0.0D, 0.0D);
         if (bl && this.world.random.nextInt(4) == 0) {
            this.world.addParticle(ParticleTypes.ENTITY_EFFECT, r + this.random.nextGaussian() * 0.30000001192092896D, s + this.random.nextGaussian() * 0.30000001192092896D, t + this.random.nextGaussian() * 0.30000001192092896D, 0.699999988079071D, 0.699999988079071D, 0.5D);
         }
      }

      if (this.getInvulnerableTimer() > 0) {
         for(u = 0; u < 3; ++u) {
            this.world.addParticle(ParticleTypes.ENTITY_EFFECT, this.getX() + this.random.nextGaussian(), this.getY() + (double)(this.random.nextFloat() * 3.3F), this.getZ() + this.random.nextGaussian(), 0.699999988079071D, 0.699999988079071D, 0.8999999761581421D);
         }
      }

   }

   protected void mobTick() {
      int l;
      if (this.getInvulnerableTimer() > 0) {
         l = this.getInvulnerableTimer() - 1;
         this.bossBar.setPercent(1.0F - (float)l / 220.0F);
         if (l <= 0) {
            Explosion.DestructionType destructionType = this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING) ? Explosion.DestructionType.DESTROY : Explosion.DestructionType.NONE;
            this.world.createExplosion(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, destructionType);
            if (!this.isSilent()) {
               this.world.syncGlobalEvent(WorldEvents.WITHER_SPAWNS, this.getBlockPos(), 0);
            }
         }

         this.setInvulTimer(l);
         if (this.age % 10 == 0) {
            this.heal(10.0F);
         }

      } else {
         super.mobTick();

         int m;
         for(l = 1; l < 3; ++l) {
            if (this.age >= this.skullCooldowns[l - 1]) {
               this.skullCooldowns[l - 1] = this.age + 10 + this.random.nextInt(10);
               if (this.world.getDifficulty() == Difficulty.NORMAL || this.world.getDifficulty() == Difficulty.HARD) {
                  int[] var10000 = this.chargedSkullCooldowns;
                  int var10001 = l - 1;
                  int var10003 = var10000[l - 1];
                  var10000[var10001] = var10000[l - 1] + 1;
                  if (var10003 > 15) {
                     float f = 10.0F;
                     float g = 5.0F;
                     double d = MathHelper.nextDouble(this.random, this.getX() - 10.0D, this.getX() + 10.0D);
                     double e = MathHelper.nextDouble(this.random, this.getY() - 5.0D, this.getY() + 5.0D);
                     double h = MathHelper.nextDouble(this.random, this.getZ() - 10.0D, this.getZ() + 10.0D);
                     this.shootSkullAt(l + 1, d, e, h, true);
                     this.chargedSkullCooldowns[l - 1] = 0;
                  }
               }

               m = this.getTrackedEntityId(l);
               if (m > 0) {
                  LivingEntity livingEntity = (LivingEntity)this.world.getEntityById(m);
                  if (livingEntity != null && this.canTarget(livingEntity) && !(this.squaredDistanceTo(livingEntity) > 900.0D) && this.canSee(livingEntity)) {
                     this.shootSkullAt(l + 1, livingEntity);
                     this.skullCooldowns[l - 1] = this.age + 40 + this.random.nextInt(20);
                     this.chargedSkullCooldowns[l - 1] = 0;
                  } else {
                     this.setTrackedEntityId(l, 0);
                  }
               } else {
                  List<LivingEntity> list = this.world.getTargets(LivingEntity.class, HEAD_TARGET_PREDICATE, this, this.getBoundingBox().expand(20.0D, 8.0D, 20.0D));
                  if (!list.isEmpty()) {
                     LivingEntity livingEntity2 = (LivingEntity)list.get(this.random.nextInt(list.size()));
                     this.setTrackedEntityId(l, livingEntity2.getId());
                  }
               }
            }
         }

         if (this.getTarget() != null) {
            this.setTrackedEntityId(0, this.getTarget().getId());
         } else {
            this.setTrackedEntityId(0, 0);
         }

         if (this.blockBreakingCooldown > 0) {
            --this.blockBreakingCooldown;
            if (this.blockBreakingCooldown == 0 && this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
               l = MathHelper.floor(this.getY());
               m = MathHelper.floor(this.getX());
               int n = MathHelper.floor(this.getZ());
               boolean bl = false;

               for(int o = -1; o <= 1; ++o) {
                  for(int p = -1; p <= 1; ++p) {
                     for(int q = 0; q <= 3; ++q) {
                        int r = m + o;
                        int s = l + q;
                        int t = n + p;
                        BlockPos blockPos = new BlockPos(r, s, t);
                        BlockState blockState = this.world.getBlockState(blockPos);
                        if (canDestroy(blockState)) {
                           bl = this.world.breakBlock(blockPos, true, this) || bl;
                        }
                     }
                  }
               }

               if (bl) {
                  this.world.syncWorldEvent((PlayerEntity)null, WorldEvents.WITHER_BREAKS_BLOCK, this.getBlockPos(), 0);
               }
            }
         }

         if (this.age % 20 == 0) {
            this.heal(1.0F);
         }

         this.bossBar.setPercent(this.getHealth() / this.getMaxHealth());
      }
   }

   public static boolean canDestroy(BlockState block) {
      return !block.isAir() && !block.isIn(BlockTags.WITHER_IMMUNE);
   }

   public void onSummoned() {
      this.setInvulTimer(220);
      this.bossBar.setPercent(0.0F);
      this.setHealth(this.getMaxHealth() / 3.0F);
   }

   public void slowMovement(BlockState state, Vec3d multiplier) {
   }

   public void onStartedTrackingBy(ServerPlayerEntity player) {
      super.onStartedTrackingBy(player);
      this.bossBar.addPlayer(player);
   }

   public void onStoppedTrackingBy(ServerPlayerEntity player) {
      super.onStoppedTrackingBy(player);
      this.bossBar.removePlayer(player);
   }

   private double getHeadX(int headIndex) {
      if (headIndex <= 0) {
         return this.getX();
      } else {
         float f = (this.bodyYaw + (float)(180 * (headIndex - 1))) * 0.017453292F;
         float g = MathHelper.cos(f);
         return this.getX() + (double)g * 1.3D;
      }
   }

   private double getHeadY(int headIndex) {
      return headIndex <= 0 ? this.getY() + 3.0D : this.getY() + 2.2D;
   }

   private double getHeadZ(int headIndex) {
      if (headIndex <= 0) {
         return this.getZ();
      } else {
         float f = (this.bodyYaw + (float)(180 * (headIndex - 1))) * 0.017453292F;
         float g = MathHelper.sin(f);
         return this.getZ() + (double)g * 1.3D;
      }
   }

   private float getNextAngle(float prevAngle, float desiredAngle, float maxDifference) {
      float f = MathHelper.wrapDegrees(desiredAngle - prevAngle);
      if (f > maxDifference) {
         f = maxDifference;
      }

      if (f < -maxDifference) {
         f = -maxDifference;
      }

      return prevAngle + f;
   }

   private void shootSkullAt(int headIndex, LivingEntity target) {
      this.shootSkullAt(headIndex, target.getX(), target.getY() + (double)target.getStandingEyeHeight() * 0.5D, target.getZ(), headIndex == 0 && this.random.nextFloat() < 0.001F);
   }

   private void shootSkullAt(int headIndex, double targetX, double targetY, double targetZ, boolean charged) {
      if (!this.isSilent()) {
         this.world.syncWorldEvent((PlayerEntity)null, WorldEvents.WITHER_SHOOTS, this.getBlockPos(), 0);
      }

      double d = this.getHeadX(headIndex);
      double e = this.getHeadY(headIndex);
      double f = this.getHeadZ(headIndex);
      double g = targetX - d;
      double h = targetY - e;
      double i = targetZ - f;
      WitherSkullEntity witherSkullEntity = new WitherSkullEntity(this.world, this, g, h, i);
      witherSkullEntity.setOwner(this);
      if (charged) {
         witherSkullEntity.setCharged(true);
      }

      witherSkullEntity.setPos(d, e, f);
      this.world.spawnEntity(witherSkullEntity);
   }

   public void attack(LivingEntity target, float pullProgress) {
      this.shootSkullAt(0, target);
   }

   public boolean damage(DamageSource source, float amount) {
      if (this.isInvulnerableTo(source)) {
         return false;
      } else if (source != DamageSource.DROWN && !(source.getAttacker() instanceof WitherEntity)) {
         if (this.getInvulnerableTimer() > 0 && source != DamageSource.OUT_OF_WORLD) {
            return false;
         } else {
            Entity entity2;
            if (this.shouldRenderOverlay()) {
               entity2 = source.getSource();
               if (entity2 instanceof PersistentProjectileEntity) {
                  return false;
               }
            }

            entity2 = source.getAttacker();
            if (entity2 != null && !(entity2 instanceof PlayerEntity) && entity2 instanceof LivingEntity && ((LivingEntity)entity2).getGroup() == this.getGroup()) {
               return false;
            } else {
               if (this.blockBreakingCooldown <= 0) {
                  this.blockBreakingCooldown = 20;
               }

               for(int i = 0; i < this.chargedSkullCooldowns.length; ++i) {
                  int[] var10000 = this.chargedSkullCooldowns;
                  var10000[i] += 3;
               }

               return super.damage(source, amount);
            }
         }
      } else {
         return false;
      }
   }

   protected void dropEquipment(DamageSource source, int lootingMultiplier, boolean allowDrops) {
      super.dropEquipment(source, lootingMultiplier, allowDrops);
      ItemEntity itemEntity = this.dropItem(Items.NETHER_STAR);
      if (itemEntity != null) {
         itemEntity.setCovetedItem();
      }

   }

   public void checkDespawn() {
      if (this.world.getDifficulty() == Difficulty.PEACEFUL && this.isDisallowedInPeaceful()) {
         this.discard();
      } else {
         this.despawnCounter = 0;
      }
   }

   public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
      return false;
   }

   public boolean addStatusEffect(StatusEffectInstance effect) {
      return false;
   }

   public static DefaultAttributeContainer.Builder createWitherAttributes() {
      return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 300.0D).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.6000000238418579D).add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0D).add(EntityAttributes.GENERIC_ARMOR, 4.0D);
   }

   public float getHeadYaw(int headIndex) {
      return this.sideHeadYaws[headIndex];
   }

   public float getHeadPitch(int headIndex) {
      return this.sideHeadPitches[headIndex];
   }

   public int getInvulnerableTimer() {
      return (Integer)this.dataTracker.get(INVUL_TIMER);
   }

   public void setInvulTimer(int ticks) {
      this.dataTracker.set(INVUL_TIMER, ticks);
   }

   public int getTrackedEntityId(int headIndex) {
      return (Integer)this.dataTracker.get((TrackedData)TRACKED_ENTITY_IDS.get(headIndex));
   }

   public void setTrackedEntityId(int headIndex, int id) {
      this.dataTracker.set((TrackedData)TRACKED_ENTITY_IDS.get(headIndex), id);
   }

   public boolean shouldRenderOverlay() {
      return this.getHealth() <= this.getMaxHealth() / 2.0F;
   }

   public EntityGroup getGroup() {
      return EntityGroup.UNDEAD;
   }

   protected boolean canStartRiding(Entity entity) {
      return false;
   }

   public boolean canUsePortals() {
      return false;
   }

   public boolean canHaveStatusEffect(StatusEffectInstance effect) {
      return effect.getEffectType() == StatusEffects.WITHER ? false : super.canHaveStatusEffect(effect);
   }

   static {
      TRACKED_ENTITY_ID_1 = DataTracker.registerData(WitherEntity.class, TrackedDataHandlerRegistry.INTEGER);
      TRACKED_ENTITY_ID_2 = DataTracker.registerData(WitherEntity.class, TrackedDataHandlerRegistry.INTEGER);
      TRACKED_ENTITY_ID_3 = DataTracker.registerData(WitherEntity.class, TrackedDataHandlerRegistry.INTEGER);
      TRACKED_ENTITY_IDS = ImmutableList.of(TRACKED_ENTITY_ID_1, TRACKED_ENTITY_ID_2, TRACKED_ENTITY_ID_3);
      INVUL_TIMER = DataTracker.registerData(WitherEntity.class, TrackedDataHandlerRegistry.INTEGER);
      CAN_ATTACK_PREDICATE = (entity) -> {
         return entity.getGroup() != EntityGroup.UNDEAD && entity.isMobOrPlayer();
      };
      HEAD_TARGET_PREDICATE = TargetPredicate.createAttackable().setBaseMaxDistance(20.0D).setPredicate(CAN_ATTACK_PREDICATE);
   }

   class DescendAtHalfHealthGoal extends Goal {
      public DescendAtHalfHealthGoal() {
         this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.JUMP, Goal.Control.LOOK));
      }

      public boolean canStart() {
         return WitherEntity.this.getInvulnerableTimer() > 0;
      }
   }
}
