package net.minecraft.entity.projectile;

import java.util.Iterator;
import java.util.List;
import net.minecraft.entity.AreaEffectCloudEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;

public class DragonFireballEntity extends ExplosiveProjectileEntity {
   public static final float field_30661 = 4.0F;

   public DragonFireballEntity(EntityType<? extends DragonFireballEntity> entityType, World world) {
      super(entityType, world);
   }

   public DragonFireballEntity(World world, LivingEntity owner, double directionX, double directionY, double directionZ) {
      super(EntityType.DRAGON_FIREBALL, owner, directionX, directionY, directionZ, world);
   }

   protected void onCollision(HitResult hitResult) {
      super.onCollision(hitResult);
      if (hitResult.getType() != HitResult.Type.ENTITY || !this.isOwner(((EntityHitResult)hitResult).getEntity())) {
         if (!this.world.isClient) {
            List<LivingEntity> list = this.world.getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox().expand(4.0D, 2.0D, 4.0D));
            AreaEffectCloudEntity areaEffectCloudEntity = new AreaEffectCloudEntity(this.world, this.getX(), this.getY(), this.getZ());
            Entity entity = this.getOwner();
            if (entity instanceof LivingEntity) {
               areaEffectCloudEntity.setOwner((LivingEntity)entity);
            }

            areaEffectCloudEntity.setParticleType(ParticleTypes.DRAGON_BREATH);
            areaEffectCloudEntity.setRadius(3.0F);
            areaEffectCloudEntity.setDuration(600);
            areaEffectCloudEntity.setRadiusGrowth((7.0F - areaEffectCloudEntity.getRadius()) / (float)areaEffectCloudEntity.getDuration());
            areaEffectCloudEntity.addEffect(new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 1));
            if (!list.isEmpty()) {
               Iterator var5 = list.iterator();

               while(var5.hasNext()) {
                  LivingEntity livingEntity = (LivingEntity)var5.next();
                  double d = this.squaredDistanceTo(livingEntity);
                  if (d < 16.0D) {
                     areaEffectCloudEntity.setPosition(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
                     break;
                  }
               }
            }

            this.world.syncWorldEvent(WorldEvents.DRAGON_BREATH_CLOUD_SPAWNS, this.getBlockPos(), this.isSilent() ? -1 : 1);
            this.world.spawnEntity(areaEffectCloudEntity);
            this.discard();
         }

      }
   }

   public boolean collides() {
      return false;
   }

   public boolean damage(DamageSource source, float amount) {
      return false;
   }

   protected ParticleEffect getParticleType() {
      return ParticleTypes.DRAGON_BREATH;
   }

   protected boolean isBurning() {
      return false;
   }
}
