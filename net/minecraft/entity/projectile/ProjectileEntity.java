package net.minecraft.entity.projectile;

import com.google.common.base.MoreObjects;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;

public abstract class ProjectileEntity extends Entity {
   @Nullable
   private UUID ownerUuid;
   @Nullable
   private Entity owner;
   private boolean leftOwner;
   private boolean shot;

   ProjectileEntity(EntityType<? extends ProjectileEntity> entityType, World world) {
      super(entityType, world);
   }

   public void setOwner(@Nullable Entity entity) {
      if (entity != null) {
         this.ownerUuid = entity.getUuid();
         this.owner = entity;
      }

   }

   @Nullable
   public Entity getOwner() {
      if (this.owner != null) {
         return this.owner;
      } else if (this.ownerUuid != null && this.world instanceof ServerWorld) {
         this.owner = ((ServerWorld)this.world).getEntity(this.ownerUuid);
         return this.owner;
      } else {
         return null;
      }
   }

   /**
    * {@return the cause entity of any effect applied by this projectile} If this
    * projectile has an owner, the effect is attributed to the owner; otherwise, it
    * is attributed to this projectile itself.
    */
   public Entity getEffectCause() {
      return (Entity)MoreObjects.firstNonNull(this.getOwner(), this);
   }

   protected void writeCustomDataToNbt(NbtCompound nbt) {
      if (this.ownerUuid != null) {
         nbt.putUuid("Owner", this.ownerUuid);
      }

      if (this.leftOwner) {
         nbt.putBoolean("LeftOwner", true);
      }

      nbt.putBoolean("HasBeenShot", this.shot);
   }

   protected boolean isOwner(Entity entity) {
      return entity.getUuid().equals(this.ownerUuid);
   }

   protected void readCustomDataFromNbt(NbtCompound nbt) {
      if (nbt.containsUuid("Owner")) {
         this.ownerUuid = nbt.getUuid("Owner");
      }

      this.leftOwner = nbt.getBoolean("LeftOwner");
      this.shot = nbt.getBoolean("HasBeenShot");
   }

   public void tick() {
      if (!this.shot) {
         this.emitGameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner(), this.getBlockPos());
         this.shot = true;
      }

      if (!this.leftOwner) {
         this.leftOwner = this.shouldLeaveOwner();
      }

      super.tick();
   }

   private boolean shouldLeaveOwner() {
      Entity entity = this.getOwner();
      if (entity != null) {
         Iterator var2 = this.world.getOtherEntities(this, this.getBoundingBox().stretch(this.getVelocity()).expand(1.0D), (entityx) -> {
            return !entityx.isSpectator() && entityx.collides();
         }).iterator();

         while(var2.hasNext()) {
            Entity entity2 = (Entity)var2.next();
            if (entity2.getRootVehicle() == entity.getRootVehicle()) {
               return false;
            }
         }
      }

      return true;
   }

   public void setVelocity(double x, double y, double z, float speed, float divergence) {
      Vec3d vec3d = (new Vec3d(x, y, z)).normalize().add(this.random.nextGaussian() * 0.007499999832361937D * (double)divergence, this.random.nextGaussian() * 0.007499999832361937D * (double)divergence, this.random.nextGaussian() * 0.007499999832361937D * (double)divergence).multiply((double)speed);
      this.setVelocity(vec3d);
      double d = vec3d.horizontalLength();
      this.setYaw((float)(MathHelper.atan2(vec3d.x, vec3d.z) * 57.2957763671875D));
      this.setPitch((float)(MathHelper.atan2(vec3d.y, d) * 57.2957763671875D));
      this.prevYaw = this.getYaw();
      this.prevPitch = this.getPitch();
   }

   public void setProperties(Entity user, float pitch, float yaw, float roll, float modifierZ, float modifierXYZ) {
      float f = -MathHelper.sin(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
      float g = -MathHelper.sin((pitch + roll) * 0.017453292F);
      float h = MathHelper.cos(yaw * 0.017453292F) * MathHelper.cos(pitch * 0.017453292F);
      this.setVelocity((double)f, (double)g, (double)h, modifierZ, modifierXYZ);
      Vec3d vec3d = user.getVelocity();
      this.setVelocity(this.getVelocity().add(vec3d.x, user.isOnGround() ? 0.0D : vec3d.y, vec3d.z));
   }

   protected void onCollision(HitResult hitResult) {
      HitResult.Type type = hitResult.getType();
      if (type == HitResult.Type.ENTITY) {
         this.onEntityHit((EntityHitResult)hitResult);
      } else if (type == HitResult.Type.BLOCK) {
         this.onBlockHit((BlockHitResult)hitResult);
      }

      if (type != HitResult.Type.MISS) {
         this.emitGameEvent(GameEvent.PROJECTILE_LAND, this.getOwner());
      }

   }

   protected void onEntityHit(EntityHitResult entityHitResult) {
   }

   protected void onBlockHit(BlockHitResult blockHitResult) {
      BlockState blockState = this.world.getBlockState(blockHitResult.getBlockPos());
      blockState.onProjectileHit(this.world, blockState, blockHitResult, this);
   }

   public void setVelocityClient(double x, double y, double z) {
      this.setVelocity(x, y, z);
      if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
         double d = Math.sqrt(x * x + z * z);
         this.setPitch((float)(MathHelper.atan2(y, d) * 57.2957763671875D));
         this.setYaw((float)(MathHelper.atan2(x, z) * 57.2957763671875D));
         this.prevPitch = this.getPitch();
         this.prevYaw = this.getYaw();
         this.refreshPositionAndAngles(this.getX(), this.getY(), this.getZ(), this.getYaw(), this.getPitch());
      }

   }

   protected boolean canHit(Entity entity) {
      if (!entity.isSpectator() && entity.isAlive() && entity.collides()) {
         Entity entity2 = this.getOwner();
         return entity2 == null || this.leftOwner || !entity2.isConnectedThroughVehicle(entity);
      } else {
         return false;
      }
   }

   protected void updateRotation() {
      Vec3d vec3d = this.getVelocity();
      double d = vec3d.horizontalLength();
      this.setPitch(updateRotation(this.prevPitch, (float)(MathHelper.atan2(vec3d.y, d) * 57.2957763671875D)));
      this.setYaw(updateRotation(this.prevYaw, (float)(MathHelper.atan2(vec3d.x, vec3d.z) * 57.2957763671875D)));
   }

   protected static float updateRotation(float prevRot, float newRot) {
      while(newRot - prevRot < -180.0F) {
         prevRot -= 360.0F;
      }

      while(newRot - prevRot >= 180.0F) {
         prevRot += 360.0F;
      }

      return MathHelper.lerp(0.2F, prevRot, newRot);
   }

   public Packet<?> createSpawnPacket() {
      Entity entity = this.getOwner();
      return new EntitySpawnS2CPacket(this, entity == null ? 0 : entity.getId());
   }

   public void onSpawnPacket(EntitySpawnS2CPacket packet) {
      super.onSpawnPacket(packet);
      Entity entity = this.world.getEntityById(packet.getEntityData());
      if (entity != null) {
         this.setOwner(entity);
      }

   }

   public boolean canModifyAt(World world, BlockPos pos) {
      Entity entity = this.getOwner();
      if (entity instanceof PlayerEntity) {
         return entity.canModifyAt(world, pos);
      } else {
         return entity == null || world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
      }
   }
}
