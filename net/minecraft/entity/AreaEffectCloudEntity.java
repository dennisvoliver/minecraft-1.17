package net.minecraft.entity;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.command.argument.ParticleEffectArgumentType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class AreaEffectCloudEntity extends Entity {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int field_29972 = 5;
   private static final TrackedData<Float> RADIUS;
   private static final TrackedData<Integer> COLOR;
   private static final TrackedData<Boolean> WAITING;
   private static final TrackedData<ParticleEffect> PARTICLE_ID;
   private static final float MAX_RADIUS = 32.0F;
   private Potion potion;
   private final List<StatusEffectInstance> effects;
   private final Map<Entity, Integer> affectedEntities;
   private int duration;
   private int waitTime;
   private int reapplicationDelay;
   private boolean customColor;
   private int durationOnUse;
   private float radiusOnUse;
   private float radiusGrowth;
   @Nullable
   private LivingEntity owner;
   @Nullable
   private UUID ownerUuid;

   public AreaEffectCloudEntity(EntityType<? extends AreaEffectCloudEntity> entityType, World world) {
      super(entityType, world);
      this.potion = Potions.EMPTY;
      this.effects = Lists.newArrayList();
      this.affectedEntities = Maps.newHashMap();
      this.duration = 600;
      this.waitTime = 20;
      this.reapplicationDelay = 20;
      this.noClip = true;
      this.setRadius(3.0F);
   }

   public AreaEffectCloudEntity(World world, double x, double y, double z) {
      this(EntityType.AREA_EFFECT_CLOUD, world);
      this.setPosition(x, y, z);
   }

   protected void initDataTracker() {
      this.getDataTracker().startTracking(COLOR, 0);
      this.getDataTracker().startTracking(RADIUS, 0.5F);
      this.getDataTracker().startTracking(WAITING, false);
      this.getDataTracker().startTracking(PARTICLE_ID, ParticleTypes.ENTITY_EFFECT);
   }

   public void setRadius(float radius) {
      if (!this.world.isClient) {
         this.getDataTracker().set(RADIUS, MathHelper.clamp(radius, 0.0F, 32.0F));
      }

   }

   public void calculateDimensions() {
      double d = this.getX();
      double e = this.getY();
      double f = this.getZ();
      super.calculateDimensions();
      this.setPosition(d, e, f);
   }

   public float getRadius() {
      return (Float)this.getDataTracker().get(RADIUS);
   }

   public void setPotion(Potion potion) {
      this.potion = potion;
      if (!this.customColor) {
         this.updateColor();
      }

   }

   private void updateColor() {
      if (this.potion == Potions.EMPTY && this.effects.isEmpty()) {
         this.getDataTracker().set(COLOR, 0);
      } else {
         this.getDataTracker().set(COLOR, PotionUtil.getColor((Collection)PotionUtil.getPotionEffects(this.potion, this.effects)));
      }

   }

   public void addEffect(StatusEffectInstance effect) {
      this.effects.add(effect);
      if (!this.customColor) {
         this.updateColor();
      }

   }

   public int getColor() {
      return (Integer)this.getDataTracker().get(COLOR);
   }

   public void setColor(int rgb) {
      this.customColor = true;
      this.getDataTracker().set(COLOR, rgb);
   }

   public ParticleEffect getParticleType() {
      return (ParticleEffect)this.getDataTracker().get(PARTICLE_ID);
   }

   public void setParticleType(ParticleEffect particle) {
      this.getDataTracker().set(PARTICLE_ID, particle);
   }

   protected void setWaiting(boolean waiting) {
      this.getDataTracker().set(WAITING, waiting);
   }

   public boolean isWaiting() {
      return (Boolean)this.getDataTracker().get(WAITING);
   }

   public int getDuration() {
      return this.duration;
   }

   public void setDuration(int duration) {
      this.duration = duration;
   }

   public void tick() {
      super.tick();
      boolean bl = this.isWaiting();
      float f = this.getRadius();
      if (this.world.isClient) {
         if (bl && this.random.nextBoolean()) {
            return;
         }

         ParticleEffect particleEffect = this.getParticleType();
         int j;
         float h;
         if (bl) {
            j = 2;
            h = 0.2F;
         } else {
            j = MathHelper.ceil(3.1415927F * f * f);
            h = f;
         }

         for(int k = 0; k < j; ++k) {
            float l = this.random.nextFloat() * 6.2831855F;
            float m = MathHelper.sqrt(this.random.nextFloat()) * h;
            double d = this.getX() + (double)(MathHelper.cos(l) * m);
            double e = this.getY();
            double n = this.getZ() + (double)(MathHelper.sin(l) * m);
            double s;
            double t;
            double x;
            if (particleEffect.getType() != ParticleTypes.ENTITY_EFFECT) {
               if (bl) {
                  s = 0.0D;
                  t = 0.0D;
                  x = 0.0D;
               } else {
                  s = (0.5D - this.random.nextDouble()) * 0.15D;
                  t = 0.009999999776482582D;
                  x = (0.5D - this.random.nextDouble()) * 0.15D;
               }
            } else {
               int o = bl && this.random.nextBoolean() ? 16777215 : this.getColor();
               s = (double)((float)(o >> 16 & 255) / 255.0F);
               t = (double)((float)(o >> 8 & 255) / 255.0F);
               x = (double)((float)(o & 255) / 255.0F);
            }

            this.world.addImportantParticle(particleEffect, d, e, n, s, t, x);
         }
      } else {
         if (this.age >= this.waitTime + this.duration) {
            this.discard();
            return;
         }

         boolean bl2 = this.age < this.waitTime;
         if (bl != bl2) {
            this.setWaiting(bl2);
         }

         if (bl2) {
            return;
         }

         if (this.radiusGrowth != 0.0F) {
            f += this.radiusGrowth;
            if (f < 0.5F) {
               this.discard();
               return;
            }

            this.setRadius(f);
         }

         if (this.age % 5 == 0) {
            this.affectedEntities.entrySet().removeIf((entry) -> {
               return this.age >= (Integer)entry.getValue();
            });
            List<StatusEffectInstance> list = Lists.newArrayList();
            Iterator var24 = this.potion.getEffects().iterator();

            while(var24.hasNext()) {
               StatusEffectInstance statusEffectInstance = (StatusEffectInstance)var24.next();
               list.add(new StatusEffectInstance(statusEffectInstance.getEffectType(), statusEffectInstance.getDuration() / 4, statusEffectInstance.getAmplifier(), statusEffectInstance.isAmbient(), statusEffectInstance.shouldShowParticles()));
            }

            list.addAll(this.effects);
            if (list.isEmpty()) {
               this.affectedEntities.clear();
            } else {
               List<LivingEntity> list2 = this.world.getNonSpectatingEntities(LivingEntity.class, this.getBoundingBox());
               if (!list2.isEmpty()) {
                  Iterator var27 = list2.iterator();

                  while(true) {
                     double aa;
                     LivingEntity livingEntity;
                     do {
                        do {
                           do {
                              if (!var27.hasNext()) {
                                 return;
                              }

                              livingEntity = (LivingEntity)var27.next();
                           } while(this.affectedEntities.containsKey(livingEntity));
                        } while(!livingEntity.isAffectedBySplashPotions());

                        double y = livingEntity.getX() - this.getX();
                        double z = livingEntity.getZ() - this.getZ();
                        aa = y * y + z * z;
                     } while(!(aa <= (double)(f * f)));

                     this.affectedEntities.put(livingEntity, this.age + this.reapplicationDelay);
                     Iterator var14 = list.iterator();

                     while(var14.hasNext()) {
                        StatusEffectInstance statusEffectInstance2 = (StatusEffectInstance)var14.next();
                        if (statusEffectInstance2.getEffectType().isInstant()) {
                           statusEffectInstance2.getEffectType().applyInstantEffect(this, this.getOwner(), livingEntity, statusEffectInstance2.getAmplifier(), 0.5D);
                        } else {
                           livingEntity.addStatusEffect(new StatusEffectInstance(statusEffectInstance2), this);
                        }
                     }

                     if (this.radiusOnUse != 0.0F) {
                        f += this.radiusOnUse;
                        if (f < 0.5F) {
                           this.discard();
                           return;
                        }

                        this.setRadius(f);
                     }

                     if (this.durationOnUse != 0) {
                        this.duration += this.durationOnUse;
                        if (this.duration <= 0) {
                           this.discard();
                           return;
                        }
                     }
                  }
               }
            }
         }
      }

   }

   public float getRadiusOnUse() {
      return this.radiusOnUse;
   }

   public void setRadiusOnUse(float radius) {
      this.radiusOnUse = radius;
   }

   public float getRadiusGrowth() {
      return this.radiusGrowth;
   }

   public void setRadiusGrowth(float growth) {
      this.radiusGrowth = growth;
   }

   public int getDurationOnUse() {
      return this.durationOnUse;
   }

   public void setDurationOnUse(int durationOnUse) {
      this.durationOnUse = durationOnUse;
   }

   public int getWaitTime() {
      return this.waitTime;
   }

   public void setWaitTime(int ticks) {
      this.waitTime = ticks;
   }

   public void setOwner(@Nullable LivingEntity owner) {
      this.owner = owner;
      this.ownerUuid = owner == null ? null : owner.getUuid();
   }

   @Nullable
   public LivingEntity getOwner() {
      if (this.owner == null && this.ownerUuid != null && this.world instanceof ServerWorld) {
         Entity entity = ((ServerWorld)this.world).getEntity(this.ownerUuid);
         if (entity instanceof LivingEntity) {
            this.owner = (LivingEntity)entity;
         }
      }

      return this.owner;
   }

   protected void readCustomDataFromNbt(NbtCompound nbt) {
      this.age = nbt.getInt("Age");
      this.duration = nbt.getInt("Duration");
      this.waitTime = nbt.getInt("WaitTime");
      this.reapplicationDelay = nbt.getInt("ReapplicationDelay");
      this.durationOnUse = nbt.getInt("DurationOnUse");
      this.radiusOnUse = nbt.getFloat("RadiusOnUse");
      this.radiusGrowth = nbt.getFloat("RadiusPerTick");
      this.setRadius(nbt.getFloat("Radius"));
      if (nbt.containsUuid("Owner")) {
         this.ownerUuid = nbt.getUuid("Owner");
      }

      if (nbt.contains("Particle", 8)) {
         try {
            this.setParticleType(ParticleEffectArgumentType.readParameters(new StringReader(nbt.getString("Particle"))));
         } catch (CommandSyntaxException var5) {
            LOGGER.warn((String)"Couldn't load custom particle {}", (Object)nbt.getString("Particle"), (Object)var5);
         }
      }

      if (nbt.contains("Color", 99)) {
         this.setColor(nbt.getInt("Color"));
      }

      if (nbt.contains("Potion", 8)) {
         this.setPotion(PotionUtil.getPotion(nbt));
      }

      if (nbt.contains("Effects", 9)) {
         NbtList nbtList = nbt.getList("Effects", 10);
         this.effects.clear();

         for(int i = 0; i < nbtList.size(); ++i) {
            StatusEffectInstance statusEffectInstance = StatusEffectInstance.fromNbt(nbtList.getCompound(i));
            if (statusEffectInstance != null) {
               this.addEffect(statusEffectInstance);
            }
         }
      }

   }

   protected void writeCustomDataToNbt(NbtCompound nbt) {
      nbt.putInt("Age", this.age);
      nbt.putInt("Duration", this.duration);
      nbt.putInt("WaitTime", this.waitTime);
      nbt.putInt("ReapplicationDelay", this.reapplicationDelay);
      nbt.putInt("DurationOnUse", this.durationOnUse);
      nbt.putFloat("RadiusOnUse", this.radiusOnUse);
      nbt.putFloat("RadiusPerTick", this.radiusGrowth);
      nbt.putFloat("Radius", this.getRadius());
      nbt.putString("Particle", this.getParticleType().asString());
      if (this.ownerUuid != null) {
         nbt.putUuid("Owner", this.ownerUuid);
      }

      if (this.customColor) {
         nbt.putInt("Color", this.getColor());
      }

      if (this.potion != Potions.EMPTY) {
         nbt.putString("Potion", Registry.POTION.getId(this.potion).toString());
      }

      if (!this.effects.isEmpty()) {
         NbtList nbtList = new NbtList();
         Iterator var3 = this.effects.iterator();

         while(var3.hasNext()) {
            StatusEffectInstance statusEffectInstance = (StatusEffectInstance)var3.next();
            nbtList.add(statusEffectInstance.writeNbt(new NbtCompound()));
         }

         nbt.put("Effects", nbtList);
      }

   }

   public void onTrackedDataSet(TrackedData<?> data) {
      if (RADIUS.equals(data)) {
         this.calculateDimensions();
      }

      super.onTrackedDataSet(data);
   }

   public Potion getPotion() {
      return this.potion;
   }

   public PistonBehavior getPistonBehavior() {
      return PistonBehavior.IGNORE;
   }

   public Packet<?> createSpawnPacket() {
      return new EntitySpawnS2CPacket(this);
   }

   public EntityDimensions getDimensions(EntityPose pose) {
      return EntityDimensions.changing(this.getRadius() * 2.0F, 0.5F);
   }

   static {
      RADIUS = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.FLOAT);
      COLOR = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.INTEGER);
      WAITING = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
      PARTICLE_ID = DataTracker.registerData(AreaEffectCloudEntity.class, TrackedDataHandlerRegistry.PARTICLE);
   }
}
