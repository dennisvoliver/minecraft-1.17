package net.minecraft.entity.passive;

import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.WaterCreatureEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class SquidEntity extends WaterCreatureEntity {
   public float tiltAngle;
   public float prevTiltAngle;
   public float rollAngle;
   public float prevRollAngle;
   /**
    * Timer between thrusts as the squid swims. Represented as an angle from 0 to 2PI.
    */
   public float thrustTimer;
   /**
    * This serves no real purpose.
    */
   public float prevThrustTimer;
   public float tentacleAngle;
   public float prevTentacleAngle;
   /**
    * A scale factor for the squid's swimming speed.
    * 
    * Gets reset to 1 at the beginning of each thrust and gradually decreases to make the squid lurch around.
    */
   private float swimVelocityScale;
   private float thrustTimerSpeed;
   private float turningSpeed;
   private float swimX;
   private float swimY;
   private float swimZ;

   public SquidEntity(EntityType<? extends SquidEntity> entityType, World world) {
      super(entityType, world);
      this.random.setSeed((long)this.getId());
      this.thrustTimerSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
   }

   protected void initGoals() {
      this.goalSelector.add(0, new SquidEntity.SwimGoal(this));
      this.goalSelector.add(1, new SquidEntity.EscapeAttackerGoal());
   }

   public static DefaultAttributeContainer.Builder createSquidAttributes() {
      return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D);
   }

   protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
      return dimensions.height * 0.5F;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.ENTITY_SQUID_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_SQUID_HURT;
   }

   protected SoundEvent getDeathSound() {
      return SoundEvents.ENTITY_SQUID_DEATH;
   }

   protected SoundEvent getSquirtSound() {
      return SoundEvents.ENTITY_SQUID_SQUIRT;
   }

   public boolean canBeLeashedBy(PlayerEntity player) {
      return !this.isLeashed();
   }

   protected float getSoundVolume() {
      return 0.4F;
   }

   protected Entity.MoveEffect getMoveEffect() {
      return Entity.MoveEffect.EVENTS;
   }

   public void tickMovement() {
      super.tickMovement();
      this.prevTiltAngle = this.tiltAngle;
      this.prevRollAngle = this.rollAngle;
      this.prevThrustTimer = this.thrustTimer;
      this.prevTentacleAngle = this.tentacleAngle;
      this.thrustTimer += this.thrustTimerSpeed;
      if ((double)this.thrustTimer > 6.283185307179586D) {
         if (this.world.isClient) {
            this.thrustTimer = 6.2831855F;
         } else {
            this.thrustTimer = (float)((double)this.thrustTimer - 6.283185307179586D);
            if (this.random.nextInt(10) == 0) {
               this.thrustTimerSpeed = 1.0F / (this.random.nextFloat() + 1.0F) * 0.2F;
            }

            this.world.sendEntityStatus(this, (byte)19);
         }
      }

      if (this.isInsideWaterOrBubbleColumn()) {
         if (this.thrustTimer < 3.1415927F) {
            float f = this.thrustTimer / 3.1415927F;
            this.tentacleAngle = MathHelper.sin(f * f * 3.1415927F) * 3.1415927F * 0.25F;
            if ((double)f > 0.75D) {
               this.swimVelocityScale = 1.0F;
               this.turningSpeed = 1.0F;
            } else {
               this.turningSpeed *= 0.8F;
            }
         } else {
            this.tentacleAngle = 0.0F;
            this.swimVelocityScale *= 0.9F;
            this.turningSpeed *= 0.99F;
         }

         if (!this.world.isClient) {
            this.setVelocity((double)(this.swimX * this.swimVelocityScale), (double)(this.swimY * this.swimVelocityScale), (double)(this.swimZ * this.swimVelocityScale));
         }

         Vec3d vec3d = this.getVelocity();
         double d = vec3d.horizontalLength();
         this.bodyYaw += (-((float)MathHelper.atan2(vec3d.x, vec3d.z)) * 57.295776F - this.bodyYaw) * 0.1F;
         this.setYaw(this.bodyYaw);
         this.rollAngle = (float)((double)this.rollAngle + 3.141592653589793D * (double)this.turningSpeed * 1.5D);
         this.tiltAngle += (-((float)MathHelper.atan2(d, vec3d.y)) * 57.295776F - this.tiltAngle) * 0.1F;
      } else {
         this.tentacleAngle = MathHelper.abs(MathHelper.sin(this.thrustTimer)) * 3.1415927F * 0.25F;
         if (!this.world.isClient) {
            double e = this.getVelocity().y;
            if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
               e = 0.05D * (double)(this.getStatusEffect(StatusEffects.LEVITATION).getAmplifier() + 1);
            } else if (!this.hasNoGravity()) {
               e -= 0.08D;
            }

            this.setVelocity(0.0D, e * 0.9800000190734863D, 0.0D);
         }

         this.tiltAngle = (float)((double)this.tiltAngle + (double)(-90.0F - this.tiltAngle) * 0.02D);
      }

   }

   public boolean damage(DamageSource source, float amount) {
      if (super.damage(source, amount) && this.getAttacker() != null) {
         this.squirt();
         return true;
      } else {
         return false;
      }
   }

   private Vec3d applyBodyRotations(Vec3d shootVector) {
      Vec3d vec3d = shootVector.rotateX(this.prevTiltAngle * 0.017453292F);
      vec3d = vec3d.rotateY(-this.prevBodyYaw * 0.017453292F);
      return vec3d;
   }

   private void squirt() {
      this.playSound(this.getSquirtSound(), this.getSoundVolume(), this.getSoundPitch());
      Vec3d vec3d = this.applyBodyRotations(new Vec3d(0.0D, -1.0D, 0.0D)).add(this.getX(), this.getY(), this.getZ());

      for(int i = 0; i < 30; ++i) {
         Vec3d vec3d2 = this.applyBodyRotations(new Vec3d((double)this.random.nextFloat() * 0.6D - 0.3D, -1.0D, (double)this.random.nextFloat() * 0.6D - 0.3D));
         Vec3d vec3d3 = vec3d2.multiply(0.3D + (double)(this.random.nextFloat() * 2.0F));
         ((ServerWorld)this.world).spawnParticles(this.getInkParticle(), vec3d.x, vec3d.y + 0.5D, vec3d.z, 0, vec3d3.x, vec3d3.y, vec3d3.z, 0.10000000149011612D);
      }

   }

   protected ParticleEffect getInkParticle() {
      return ParticleTypes.SQUID_INK;
   }

   public void travel(Vec3d movementInput) {
      this.move(MovementType.SELF, this.getVelocity());
   }

   public static boolean canSpawn(EntityType<SquidEntity> type, WorldAccess world, SpawnReason spawnReason, BlockPos pos, Random random) {
      return pos.getY() > 45 && pos.getY() < world.getSeaLevel();
   }

   public void handleStatus(byte status) {
      if (status == 19) {
         this.thrustTimer = 0.0F;
      } else {
         super.handleStatus(status);
      }

   }

   /**
    * Sets the direction and velocity the squid must go when fleeing an enemy. Only has an effect when in the water.
    */
   public void setSwimmingVector(float x, float y, float z) {
      this.swimX = x;
      this.swimY = y;
      this.swimZ = z;
   }

   public boolean hasSwimmingVector() {
      return this.swimX != 0.0F || this.swimY != 0.0F || this.swimZ != 0.0F;
   }

   private class SwimGoal extends Goal {
      private final SquidEntity squid;

      public SwimGoal(SquidEntity squid) {
         this.squid = squid;
      }

      public boolean canStart() {
         return true;
      }

      public void tick() {
         int i = this.squid.getDespawnCounter();
         if (i > 100) {
            this.squid.setSwimmingVector(0.0F, 0.0F, 0.0F);
         } else if (this.squid.getRandom().nextInt(50) == 0 || !this.squid.touchingWater || !this.squid.hasSwimmingVector()) {
            float f = this.squid.getRandom().nextFloat() * 6.2831855F;
            float g = MathHelper.cos(f) * 0.2F;
            float h = -0.1F + this.squid.getRandom().nextFloat() * 0.2F;
            float j = MathHelper.sin(f) * 0.2F;
            this.squid.setSwimmingVector(g, h, j);
         }

      }
   }

   class EscapeAttackerGoal extends Goal {
      private static final float field_30375 = 3.0F;
      private static final float field_30376 = 5.0F;
      private static final float field_30377 = 10.0F;
      private int timer;

      public boolean canStart() {
         LivingEntity livingEntity = SquidEntity.this.getAttacker();
         if (SquidEntity.this.isTouchingWater() && livingEntity != null) {
            return SquidEntity.this.squaredDistanceTo(livingEntity) < 100.0D;
         } else {
            return false;
         }
      }

      public void start() {
         this.timer = 0;
      }

      public void tick() {
         ++this.timer;
         LivingEntity livingEntity = SquidEntity.this.getAttacker();
         if (livingEntity != null) {
            Vec3d vec3d = new Vec3d(SquidEntity.this.getX() - livingEntity.getX(), SquidEntity.this.getY() - livingEntity.getY(), SquidEntity.this.getZ() - livingEntity.getZ());
            BlockState blockState = SquidEntity.this.world.getBlockState(new BlockPos(SquidEntity.this.getX() + vec3d.x, SquidEntity.this.getY() + vec3d.y, SquidEntity.this.getZ() + vec3d.z));
            FluidState fluidState = SquidEntity.this.world.getFluidState(new BlockPos(SquidEntity.this.getX() + vec3d.x, SquidEntity.this.getY() + vec3d.y, SquidEntity.this.getZ() + vec3d.z));
            if (fluidState.isIn(FluidTags.WATER) || blockState.isAir()) {
               double d = vec3d.length();
               if (d > 0.0D) {
                  vec3d.normalize();
                  float f = 3.0F;
                  if (d > 5.0D) {
                     f = (float)((double)f - (d - 5.0D) / 5.0D);
                  }

                  if (f > 0.0F) {
                     vec3d = vec3d.multiply((double)f);
                  }
               }

               if (blockState.isAir()) {
                  vec3d = vec3d.subtract(0.0D, vec3d.y, 0.0D);
               }

               SquidEntity.this.setSwimmingVector((float)vec3d.x / 20.0F, (float)vec3d.y / 20.0F, (float)vec3d.z / 20.0F);
            }

            if (this.timer % 10 == 5) {
               SquidEntity.this.world.addParticle(ParticleTypes.BUBBLE, SquidEntity.this.getX(), SquidEntity.this.getY(), SquidEntity.this.getZ(), 0.0D, 0.0D, 0.0D);
            }

         }
      }
   }
}
