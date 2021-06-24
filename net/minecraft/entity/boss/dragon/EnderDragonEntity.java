package net.minecraft.entity.boss.dragon;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathMinHeap;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.dragon.phase.Phase;
import net.minecraft.entity.boss.dragon.phase.PhaseManager;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.EntityDamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.MobSpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.predicate.entity.EntityPredicates;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class EnderDragonEntity extends MobEntity implements Monster {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final TrackedData<Integer> PHASE_TYPE;
   private static final TargetPredicate CLOSE_PLAYER_PREDICATE;
   private static final int MAX_HEALTH = 200;
   private static final int field_30429 = 400;
   /**
    * The damage the dragon can take before it takes off, represented as a ratio to the full health.
    */
   private static final float TAKEOFF_THRESHOLD = 0.25F;
   private static final String DRAGON_DEATH_TIME_KEY = "DragonDeathTime";
   private static final String DRAGON_PHASE_KEY = "DragonPhase";
   /**
    * (yaw, y, ?)
    */
   public final double[][] segmentCircularBuffer = new double[64][3];
   public int latestSegment = -1;
   private final EnderDragonPart[] parts;
   public final EnderDragonPart head = new EnderDragonPart(this, "head", 1.0F, 1.0F);
   private final EnderDragonPart neck = new EnderDragonPart(this, "neck", 3.0F, 3.0F);
   private final EnderDragonPart body = new EnderDragonPart(this, "body", 5.0F, 3.0F);
   private final EnderDragonPart tail1 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
   private final EnderDragonPart tail2 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
   private final EnderDragonPart tail3 = new EnderDragonPart(this, "tail", 2.0F, 2.0F);
   private final EnderDragonPart rightWing = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
   private final EnderDragonPart leftWing = new EnderDragonPart(this, "wing", 4.0F, 2.0F);
   public float prevWingPosition;
   public float wingPosition;
   public boolean slowedDownByBlock;
   public int ticksSinceDeath;
   public float yawAcceleration;
   @Nullable
   public EndCrystalEntity connectedCrystal;
   @Nullable
   private final EnderDragonFight fight;
   private final PhaseManager phaseManager;
   private int ticksUntilNextGrowl = 100;
   private int damageDuringSitting;
   /**
    * The first 12 path nodes are used for end crystals; the others are not tied to them.
    */
   private final PathNode[] pathNodes = new PathNode[24];
   /**
    * An array of 24 bitflags, where node #i leads to #j if and only if
    * {@code (pathNodeConnections[i] & (1 << j)) != 0}.
    */
   private final int[] pathNodeConnections = new int[24];
   private final PathMinHeap pathHeap = new PathMinHeap();

   public EnderDragonEntity(EntityType<? extends EnderDragonEntity> entityType, World world) {
      super(EntityType.ENDER_DRAGON, world);
      this.parts = new EnderDragonPart[]{this.head, this.neck, this.body, this.tail1, this.tail2, this.tail3, this.rightWing, this.leftWing};
      this.setHealth(this.getMaxHealth());
      this.noClip = true;
      this.ignoreCameraFrustum = true;
      if (world instanceof ServerWorld) {
         this.fight = ((ServerWorld)world).getEnderDragonFight();
      } else {
         this.fight = null;
      }

      this.phaseManager = new PhaseManager(this);
   }

   public static DefaultAttributeContainer.Builder createEnderDragonAttributes() {
      return MobEntity.createMobAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, 200.0D);
   }

   public boolean hasWings() {
      float f = MathHelper.cos(this.wingPosition * 6.2831855F);
      float g = MathHelper.cos(this.prevWingPosition * 6.2831855F);
      return g <= -0.3F && f >= -0.3F;
   }

   public void addFlapEffects() {
      if (this.world.isClient && !this.isSilent()) {
         this.world.playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, this.getSoundCategory(), 5.0F, 0.8F + this.random.nextFloat() * 0.3F, false);
      }

   }

   protected void initDataTracker() {
      super.initDataTracker();
      this.getDataTracker().startTracking(PHASE_TYPE, PhaseType.HOVER.getTypeId());
   }

   public double[] getSegmentProperties(int segmentNumber, float tickDelta) {
      if (this.isDead()) {
         tickDelta = 0.0F;
      }

      tickDelta = 1.0F - tickDelta;
      int i = this.latestSegment - segmentNumber & 63;
      int j = this.latestSegment - segmentNumber - 1 & 63;
      double[] ds = new double[3];
      double d = this.segmentCircularBuffer[i][0];
      double e = MathHelper.wrapDegrees(this.segmentCircularBuffer[j][0] - d);
      ds[0] = d + e * (double)tickDelta;
      d = this.segmentCircularBuffer[i][1];
      e = this.segmentCircularBuffer[j][1] - d;
      ds[1] = d + e * (double)tickDelta;
      ds[2] = MathHelper.lerp((double)tickDelta, this.segmentCircularBuffer[i][2], this.segmentCircularBuffer[j][2]);
      return ds;
   }

   public void tickMovement() {
      this.addAirTravelEffects();
      if (this.world.isClient) {
         this.setHealth(this.getHealth());
         if (!this.isSilent() && !this.phaseManager.getCurrent().isSittingOrHovering() && --this.ticksUntilNextGrowl < 0) {
            this.world.playSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, this.getSoundCategory(), 2.5F, 0.8F + this.random.nextFloat() * 0.3F, false);
            this.ticksUntilNextGrowl = 200 + this.random.nextInt(200);
         }
      }

      this.prevWingPosition = this.wingPosition;
      float i;
      if (this.isDead()) {
         float f = (this.random.nextFloat() - 0.5F) * 8.0F;
         i = (this.random.nextFloat() - 0.5F) * 4.0F;
         float h = (this.random.nextFloat() - 0.5F) * 8.0F;
         this.world.addParticle(ParticleTypes.EXPLOSION, this.getX() + (double)f, this.getY() + 2.0D + (double)i, this.getZ() + (double)h, 0.0D, 0.0D, 0.0D);
      } else {
         this.tickWithEndCrystals();
         Vec3d vec3d = this.getVelocity();
         i = 0.2F / ((float)vec3d.horizontalLength() * 10.0F + 1.0F);
         i *= (float)Math.pow(2.0D, vec3d.y);
         if (this.phaseManager.getCurrent().isSittingOrHovering()) {
            this.wingPosition += 0.1F;
         } else if (this.slowedDownByBlock) {
            this.wingPosition += i * 0.5F;
         } else {
            this.wingPosition += i;
         }

         this.setYaw(MathHelper.wrapDegrees(this.getYaw()));
         if (this.isAiDisabled()) {
            this.wingPosition = 0.5F;
         } else {
            if (this.latestSegment < 0) {
               for(int j = 0; j < this.segmentCircularBuffer.length; ++j) {
                  this.segmentCircularBuffer[j][0] = (double)this.getYaw();
                  this.segmentCircularBuffer[j][1] = this.getY();
               }
            }

            if (++this.latestSegment == this.segmentCircularBuffer.length) {
               this.latestSegment = 0;
            }

            this.segmentCircularBuffer[this.latestSegment][0] = (double)this.getYaw();
            this.segmentCircularBuffer[this.latestSegment][1] = this.getY();
            double m;
            double n;
            double o;
            float aj;
            float al;
            float ak;
            if (this.world.isClient) {
               if (this.bodyTrackingIncrements > 0) {
                  double d = this.getX() + (this.serverX - this.getX()) / (double)this.bodyTrackingIncrements;
                  m = this.getY() + (this.serverY - this.getY()) / (double)this.bodyTrackingIncrements;
                  n = this.getZ() + (this.serverZ - this.getZ()) / (double)this.bodyTrackingIncrements;
                  o = MathHelper.wrapDegrees(this.serverYaw - (double)this.getYaw());
                  this.setYaw(this.getYaw() + (float)o / (float)this.bodyTrackingIncrements);
                  this.setPitch(this.getPitch() + (float)(this.serverPitch - (double)this.getPitch()) / (float)this.bodyTrackingIncrements);
                  --this.bodyTrackingIncrements;
                  this.setPosition(d, m, n);
                  this.setRotation(this.getYaw(), this.getPitch());
               }

               this.phaseManager.getCurrent().clientTick();
            } else {
               Phase phase = this.phaseManager.getCurrent();
               phase.serverTick();
               if (this.phaseManager.getCurrent() != phase) {
                  phase = this.phaseManager.getCurrent();
                  phase.serverTick();
               }

               Vec3d vec3d2 = phase.getPathTarget();
               if (vec3d2 != null) {
                  m = vec3d2.x - this.getX();
                  n = vec3d2.y - this.getY();
                  o = vec3d2.z - this.getZ();
                  double p = m * m + n * n + o * o;
                  float q = phase.getMaxYAcceleration();
                  double r = Math.sqrt(m * m + o * o);
                  if (r > 0.0D) {
                     n = MathHelper.clamp(n / r, (double)(-q), (double)q);
                  }

                  this.setVelocity(this.getVelocity().add(0.0D, n * 0.01D, 0.0D));
                  this.setYaw(MathHelper.wrapDegrees(this.getYaw()));
                  Vec3d vec3d3 = vec3d2.subtract(this.getX(), this.getY(), this.getZ()).normalize();
                  Vec3d vec3d4 = (new Vec3d((double)MathHelper.sin(this.getYaw() * 0.017453292F), this.getVelocity().y, (double)(-MathHelper.cos(this.getYaw() * 0.017453292F)))).normalize();
                  aj = Math.max(((float)vec3d4.dotProduct(vec3d3) + 0.5F) / 1.5F, 0.0F);
                  if (Math.abs(m) > 9.999999747378752E-6D || Math.abs(o) > 9.999999747378752E-6D) {
                     double t = MathHelper.clamp(MathHelper.wrapDegrees(180.0D - MathHelper.atan2(m, o) * 57.2957763671875D - (double)this.getYaw()), -50.0D, 50.0D);
                     this.yawAcceleration *= 0.8F;
                     this.yawAcceleration = (float)((double)this.yawAcceleration + t * (double)phase.getYawAcceleration());
                     this.setYaw(this.getYaw() + this.yawAcceleration * 0.1F);
                  }

                  ak = (float)(2.0D / (p + 1.0D));
                  al = 0.06F;
                  this.updateVelocity(0.06F * (aj * ak + (1.0F - ak)), new Vec3d(0.0D, 0.0D, -1.0D));
                  if (this.slowedDownByBlock) {
                     this.move(MovementType.SELF, this.getVelocity().multiply(0.800000011920929D));
                  } else {
                     this.move(MovementType.SELF, this.getVelocity());
                  }

                  Vec3d vec3d5 = this.getVelocity().normalize();
                  double w = 0.8D + 0.15D * (vec3d5.dotProduct(vec3d4) + 1.0D) / 2.0D;
                  this.setVelocity(this.getVelocity().multiply(w, 0.9100000262260437D, w));
               }
            }

            this.bodyYaw = this.getYaw();
            Vec3d[] vec3ds = new Vec3d[this.parts.length];

            for(int x = 0; x < this.parts.length; ++x) {
               vec3ds[x] = new Vec3d(this.parts[x].getX(), this.parts[x].getY(), this.parts[x].getZ());
            }

            float y = (float)(this.getSegmentProperties(5, 1.0F)[1] - this.getSegmentProperties(10, 1.0F)[1]) * 10.0F * 0.017453292F;
            float z = MathHelper.cos(y);
            float aa = MathHelper.sin(y);
            float ab = this.getYaw() * 0.017453292F;
            float ac = MathHelper.sin(ab);
            float ad = MathHelper.cos(ab);
            this.movePart(this.body, (double)(ac * 0.5F), 0.0D, (double)(-ad * 0.5F));
            this.movePart(this.rightWing, (double)(ad * 4.5F), 2.0D, (double)(ac * 4.5F));
            this.movePart(this.leftWing, (double)(ad * -4.5F), 2.0D, (double)(ac * -4.5F));
            if (!this.world.isClient && this.hurtTime == 0) {
               this.launchLivingEntities(this.world.getOtherEntities(this, this.rightWing.getBoundingBox().expand(4.0D, 2.0D, 4.0D).offset(0.0D, -2.0D, 0.0D), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR));
               this.launchLivingEntities(this.world.getOtherEntities(this, this.leftWing.getBoundingBox().expand(4.0D, 2.0D, 4.0D).offset(0.0D, -2.0D, 0.0D), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR));
               this.damageLivingEntities(this.world.getOtherEntities(this, this.head.getBoundingBox().expand(1.0D), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR));
               this.damageLivingEntities(this.world.getOtherEntities(this, this.neck.getBoundingBox().expand(1.0D), EntityPredicates.EXCEPT_CREATIVE_OR_SPECTATOR));
            }

            float ae = MathHelper.sin(this.getYaw() * 0.017453292F - this.yawAcceleration * 0.01F);
            float af = MathHelper.cos(this.getYaw() * 0.017453292F - this.yawAcceleration * 0.01F);
            float ag = this.getHeadVerticalMovement();
            this.movePart(this.head, (double)(ae * 6.5F * z), (double)(ag + aa * 6.5F), (double)(-af * 6.5F * z));
            this.movePart(this.neck, (double)(ae * 5.5F * z), (double)(ag + aa * 5.5F), (double)(-af * 5.5F * z));
            double[] ds = this.getSegmentProperties(5, 1.0F);

            int ah;
            for(ah = 0; ah < 3; ++ah) {
               EnderDragonPart enderDragonPart = null;
               if (ah == 0) {
                  enderDragonPart = this.tail1;
               }

               if (ah == 1) {
                  enderDragonPart = this.tail2;
               }

               if (ah == 2) {
                  enderDragonPart = this.tail3;
               }

               double[] es = this.getSegmentProperties(12 + ah * 2, 1.0F);
               float ai = this.getYaw() * 0.017453292F + this.wrapYawChange(es[0] - ds[0]) * 0.017453292F;
               aj = MathHelper.sin(ai);
               ak = MathHelper.cos(ai);
               al = 1.5F;
               float am = (float)(ah + 1) * 2.0F;
               this.movePart(enderDragonPart, (double)(-(ac * 1.5F + aj * am) * z), es[1] - ds[1] - (double)((am + 1.5F) * aa) + 1.5D, (double)((ad * 1.5F + ak * am) * z));
            }

            if (!this.world.isClient) {
               this.slowedDownByBlock = this.destroyBlocks(this.head.getBoundingBox()) | this.destroyBlocks(this.neck.getBoundingBox()) | this.destroyBlocks(this.body.getBoundingBox());
               if (this.fight != null) {
                  this.fight.updateFight(this);
               }
            }

            for(ah = 0; ah < this.parts.length; ++ah) {
               this.parts[ah].prevX = vec3ds[ah].x;
               this.parts[ah].prevY = vec3ds[ah].y;
               this.parts[ah].prevZ = vec3ds[ah].z;
               this.parts[ah].lastRenderX = vec3ds[ah].x;
               this.parts[ah].lastRenderY = vec3ds[ah].y;
               this.parts[ah].lastRenderZ = vec3ds[ah].z;
            }

         }
      }
   }

   private void movePart(EnderDragonPart enderDragonPart, double dx, double dy, double dz) {
      enderDragonPart.setPosition(this.getX() + dx, this.getY() + dy, this.getZ() + dz);
   }

   private float getHeadVerticalMovement() {
      if (this.phaseManager.getCurrent().isSittingOrHovering()) {
         return -1.0F;
      } else {
         double[] ds = this.getSegmentProperties(5, 1.0F);
         double[] es = this.getSegmentProperties(0, 1.0F);
         return (float)(ds[1] - es[1]);
      }
   }

   /**
    * Things to do every tick related to end crystals. The Ender Dragon:
    * 
    * * Disconnects from its crystal if it is removed
    * * If it is connected to a crystal, then heals every 10 ticks
    * * With a 1 in 10 chance each tick, searches for the nearest crystal and connects to it if present
    */
   private void tickWithEndCrystals() {
      if (this.connectedCrystal != null) {
         if (this.connectedCrystal.isRemoved()) {
            this.connectedCrystal = null;
         } else if (this.age % 10 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.setHealth(this.getHealth() + 1.0F);
         }
      }

      if (this.random.nextInt(10) == 0) {
         List<EndCrystalEntity> list = this.world.getNonSpectatingEntities(EndCrystalEntity.class, this.getBoundingBox().expand(32.0D));
         EndCrystalEntity endCrystalEntity = null;
         double d = Double.MAX_VALUE;
         Iterator var5 = list.iterator();

         while(var5.hasNext()) {
            EndCrystalEntity endCrystalEntity2 = (EndCrystalEntity)var5.next();
            double e = endCrystalEntity2.squaredDistanceTo(this);
            if (e < d) {
               d = e;
               endCrystalEntity = endCrystalEntity2;
            }
         }

         this.connectedCrystal = endCrystalEntity;
      }

   }

   private void launchLivingEntities(List<Entity> entities) {
      double d = (this.body.getBoundingBox().minX + this.body.getBoundingBox().maxX) / 2.0D;
      double e = (this.body.getBoundingBox().minZ + this.body.getBoundingBox().maxZ) / 2.0D;
      Iterator var6 = entities.iterator();

      while(var6.hasNext()) {
         Entity entity = (Entity)var6.next();
         if (entity instanceof LivingEntity) {
            double f = entity.getX() - d;
            double g = entity.getZ() - e;
            double h = Math.max(f * f + g * g, 0.1D);
            entity.addVelocity(f / h * 4.0D, 0.20000000298023224D, g / h * 4.0D);
            if (!this.phaseManager.getCurrent().isSittingOrHovering() && ((LivingEntity)entity).getLastAttackedTime() < entity.age - 2) {
               entity.damage(DamageSource.mob(this), 5.0F);
               this.applyDamageEffects(this, entity);
            }
         }
      }

   }

   private void damageLivingEntities(List<Entity> entities) {
      Iterator var2 = entities.iterator();

      while(var2.hasNext()) {
         Entity entity = (Entity)var2.next();
         if (entity instanceof LivingEntity) {
            entity.damage(DamageSource.mob(this), 10.0F);
            this.applyDamageEffects(this, entity);
         }
      }

   }

   private float wrapYawChange(double yawDegrees) {
      return (float)MathHelper.wrapDegrees(yawDegrees);
   }

   private boolean destroyBlocks(Box box) {
      int i = MathHelper.floor(box.minX);
      int j = MathHelper.floor(box.minY);
      int k = MathHelper.floor(box.minZ);
      int l = MathHelper.floor(box.maxX);
      int m = MathHelper.floor(box.maxY);
      int n = MathHelper.floor(box.maxZ);
      boolean bl = false;
      boolean bl2 = false;

      for(int o = i; o <= l; ++o) {
         for(int p = j; p <= m; ++p) {
            for(int q = k; q <= n; ++q) {
               BlockPos blockPos = new BlockPos(o, p, q);
               BlockState blockState = this.world.getBlockState(blockPos);
               if (!blockState.isAir() && blockState.getMaterial() != Material.FIRE) {
                  if (this.world.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING) && !blockState.isIn(BlockTags.DRAGON_IMMUNE)) {
                     bl2 = this.world.removeBlock(blockPos, false) || bl2;
                  } else {
                     bl = true;
                  }
               }
            }
         }
      }

      if (bl2) {
         BlockPos blockPos2 = new BlockPos(i + this.random.nextInt(l - i + 1), j + this.random.nextInt(m - j + 1), k + this.random.nextInt(n - k + 1));
         this.world.syncWorldEvent(WorldEvents.ENDER_DRAGON_BREAKS_BLOCK, blockPos2, 0);
      }

      return bl;
   }

   public boolean damagePart(EnderDragonPart part, DamageSource source, float amount) {
      if (this.phaseManager.getCurrent().getType() == PhaseType.DYING) {
         return false;
      } else {
         amount = this.phaseManager.getCurrent().modifyDamageTaken(source, amount);
         if (part != this.head) {
            amount = amount / 4.0F + Math.min(amount, 1.0F);
         }

         if (amount < 0.01F) {
            return false;
         } else {
            if (source.getAttacker() instanceof PlayerEntity || source.isExplosive()) {
               float f = this.getHealth();
               this.parentDamage(source, amount);
               if (this.isDead() && !this.phaseManager.getCurrent().isSittingOrHovering()) {
                  this.setHealth(1.0F);
                  this.phaseManager.setPhase(PhaseType.DYING);
               }

               if (this.phaseManager.getCurrent().isSittingOrHovering()) {
                  this.damageDuringSitting = (int)((float)this.damageDuringSitting + (f - this.getHealth()));
                  if ((float)this.damageDuringSitting > 0.25F * this.getMaxHealth()) {
                     this.damageDuringSitting = 0;
                     this.phaseManager.setPhase(PhaseType.TAKEOFF);
                  }
               }
            }

            return true;
         }
      }
   }

   public boolean damage(DamageSource source, float amount) {
      if (source instanceof EntityDamageSource && ((EntityDamageSource)source).isThorns()) {
         this.damagePart(this.body, source, amount);
      }

      return false;
   }

   protected boolean parentDamage(DamageSource source, float amount) {
      return super.damage(source, amount);
   }

   public void kill() {
      this.remove(Entity.RemovalReason.KILLED);
      if (this.fight != null) {
         this.fight.updateFight(this);
         this.fight.dragonKilled(this);
      }

   }

   protected void updatePostDeath() {
      if (this.fight != null) {
         this.fight.updateFight(this);
      }

      ++this.ticksSinceDeath;
      if (this.ticksSinceDeath >= 180 && this.ticksSinceDeath <= 200) {
         float f = (this.random.nextFloat() - 0.5F) * 8.0F;
         float g = (this.random.nextFloat() - 0.5F) * 4.0F;
         float h = (this.random.nextFloat() - 0.5F) * 8.0F;
         this.world.addParticle(ParticleTypes.EXPLOSION_EMITTER, this.getX() + (double)f, this.getY() + 2.0D + (double)g, this.getZ() + (double)h, 0.0D, 0.0D, 0.0D);
      }

      boolean bl = this.world.getGameRules().getBoolean(GameRules.DO_MOB_LOOT);
      int i = 500;
      if (this.fight != null && !this.fight.hasPreviouslyKilled()) {
         i = 12000;
      }

      if (this.world instanceof ServerWorld) {
         if (this.ticksSinceDeath > 150 && this.ticksSinceDeath % 5 == 0 && bl) {
            ExperienceOrbEntity.spawn((ServerWorld)this.world, this.getPos(), MathHelper.floor((float)i * 0.08F));
         }

         if (this.ticksSinceDeath == 1 && !this.isSilent()) {
            this.world.syncGlobalEvent(WorldEvents.ENDER_DRAGON_DIES, this.getBlockPos(), 0);
         }
      }

      this.move(MovementType.SELF, new Vec3d(0.0D, 0.10000000149011612D, 0.0D));
      this.setYaw(this.getYaw() + 20.0F);
      this.bodyYaw = this.getYaw();
      if (this.ticksSinceDeath == 200 && this.world instanceof ServerWorld) {
         if (bl) {
            ExperienceOrbEntity.spawn((ServerWorld)this.world, this.getPos(), MathHelper.floor((float)i * 0.2F));
         }

         if (this.fight != null) {
            this.fight.dragonKilled(this);
         }

         this.remove(Entity.RemovalReason.KILLED);
      }

   }

   public int getNearestPathNodeIndex() {
      if (this.pathNodes[0] == null) {
         for(int i = 0; i < 24; ++i) {
            int j = 5;
            int n;
            int o;
            if (i < 12) {
               n = MathHelper.floor(60.0F * MathHelper.cos(2.0F * (-3.1415927F + 0.2617994F * (float)i)));
               o = MathHelper.floor(60.0F * MathHelper.sin(2.0F * (-3.1415927F + 0.2617994F * (float)i)));
            } else {
               int k;
               if (i < 20) {
                  k = i - 12;
                  n = MathHelper.floor(40.0F * MathHelper.cos(2.0F * (-3.1415927F + 0.3926991F * (float)k)));
                  o = MathHelper.floor(40.0F * MathHelper.sin(2.0F * (-3.1415927F + 0.3926991F * (float)k)));
                  j += 10;
               } else {
                  k = i - 20;
                  n = MathHelper.floor(20.0F * MathHelper.cos(2.0F * (-3.1415927F + 0.7853982F * (float)k)));
                  o = MathHelper.floor(20.0F * MathHelper.sin(2.0F * (-3.1415927F + 0.7853982F * (float)k)));
               }
            }

            int r = Math.max(this.world.getSeaLevel() + 10, this.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(n, 0, o)).getY() + j);
            this.pathNodes[i] = new PathNode(n, r, o);
         }

         this.pathNodeConnections[0] = 6146;
         this.pathNodeConnections[1] = 8197;
         this.pathNodeConnections[2] = 8202;
         this.pathNodeConnections[3] = 16404;
         this.pathNodeConnections[4] = 32808;
         this.pathNodeConnections[5] = 32848;
         this.pathNodeConnections[6] = 65696;
         this.pathNodeConnections[7] = 131392;
         this.pathNodeConnections[8] = 131712;
         this.pathNodeConnections[9] = 263424;
         this.pathNodeConnections[10] = 526848;
         this.pathNodeConnections[11] = 525313;
         this.pathNodeConnections[12] = 1581057;
         this.pathNodeConnections[13] = 3166214;
         this.pathNodeConnections[14] = 2138120;
         this.pathNodeConnections[15] = 6373424;
         this.pathNodeConnections[16] = 4358208;
         this.pathNodeConnections[17] = 12910976;
         this.pathNodeConnections[18] = 9044480;
         this.pathNodeConnections[19] = 9706496;
         this.pathNodeConnections[20] = 15216640;
         this.pathNodeConnections[21] = 13688832;
         this.pathNodeConnections[22] = 11763712;
         this.pathNodeConnections[23] = 8257536;
      }

      return this.getNearestPathNodeIndex(this.getX(), this.getY(), this.getZ());
   }

   public int getNearestPathNodeIndex(double x, double y, double z) {
      float f = 10000.0F;
      int i = 0;
      PathNode pathNode = new PathNode(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
      int j = 0;
      if (this.fight == null || this.fight.getAliveEndCrystals() == 0) {
         j = 12;
      }

      for(int k = j; k < 24; ++k) {
         if (this.pathNodes[k] != null) {
            float g = this.pathNodes[k].getSquaredDistance(pathNode);
            if (g < f) {
               f = g;
               i = k;
            }
         }
      }

      return i;
   }

   @Nullable
   public Path findPath(int from, int to, @Nullable PathNode pathNode) {
      PathNode pathNode4;
      for(int i = 0; i < 24; ++i) {
         pathNode4 = this.pathNodes[i];
         pathNode4.visited = false;
         pathNode4.heapWeight = 0.0F;
         pathNode4.penalizedPathLength = 0.0F;
         pathNode4.distanceToNearestTarget = 0.0F;
         pathNode4.previous = null;
         pathNode4.heapIndex = -1;
      }

      PathNode pathNode3 = this.pathNodes[from];
      pathNode4 = this.pathNodes[to];
      pathNode3.penalizedPathLength = 0.0F;
      pathNode3.distanceToNearestTarget = pathNode3.getDistance(pathNode4);
      pathNode3.heapWeight = pathNode3.distanceToNearestTarget;
      this.pathHeap.clear();
      this.pathHeap.push(pathNode3);
      PathNode pathNode5 = pathNode3;
      int j = 0;
      if (this.fight == null || this.fight.getAliveEndCrystals() == 0) {
         j = 12;
      }

      while(!this.pathHeap.isEmpty()) {
         PathNode pathNode6 = this.pathHeap.pop();
         if (pathNode6.equals(pathNode4)) {
            if (pathNode != null) {
               pathNode.previous = pathNode4;
               pathNode4 = pathNode;
            }

            return this.getPathOfAllPredecessors(pathNode3, pathNode4);
         }

         if (pathNode6.getDistance(pathNode4) < pathNode5.getDistance(pathNode4)) {
            pathNode5 = pathNode6;
         }

         pathNode6.visited = true;
         int k = 0;

         int m;
         for(m = 0; m < 24; ++m) {
            if (this.pathNodes[m] == pathNode6) {
               k = m;
               break;
            }
         }

         for(m = j; m < 24; ++m) {
            if ((this.pathNodeConnections[k] & 1 << m) > 0) {
               PathNode pathNode7 = this.pathNodes[m];
               if (!pathNode7.visited) {
                  float f = pathNode6.penalizedPathLength + pathNode6.getDistance(pathNode7);
                  if (!pathNode7.isInHeap() || f < pathNode7.penalizedPathLength) {
                     pathNode7.previous = pathNode6;
                     pathNode7.penalizedPathLength = f;
                     pathNode7.distanceToNearestTarget = pathNode7.getDistance(pathNode4);
                     if (pathNode7.isInHeap()) {
                        this.pathHeap.setNodeWeight(pathNode7, pathNode7.penalizedPathLength + pathNode7.distanceToNearestTarget);
                     } else {
                        pathNode7.heapWeight = pathNode7.penalizedPathLength + pathNode7.distanceToNearestTarget;
                        this.pathHeap.push(pathNode7);
                     }
                  }
               }
            }
         }
      }

      if (pathNode5 == pathNode3) {
         return null;
      } else {
         LOGGER.debug((String)"Failed to find path from {} to {}", (Object)from, (Object)to);
         if (pathNode != null) {
            pathNode.previous = pathNode5;
            pathNode5 = pathNode;
         }

         return this.getPathOfAllPredecessors(pathNode3, pathNode5);
      }
   }

   private Path getPathOfAllPredecessors(PathNode unused, PathNode node) {
      List<PathNode> list = Lists.newArrayList();
      PathNode pathNode = node;
      list.add(0, node);

      while(pathNode.previous != null) {
         pathNode = pathNode.previous;
         list.add(0, pathNode);
      }

      return new Path(list, new BlockPos(node.x, node.y, node.z), true);
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      nbt.putInt("DragonPhase", this.phaseManager.getCurrent().getType().getTypeId());
      nbt.putInt("DragonDeathTime", this.ticksSinceDeath);
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      if (nbt.contains("DragonPhase")) {
         this.phaseManager.setPhase(PhaseType.getFromId(nbt.getInt("DragonPhase")));
      }

      if (nbt.contains("DragonDeathTime")) {
         this.ticksSinceDeath = nbt.getInt("DragonDeathTime");
      }

   }

   public void checkDespawn() {
   }

   public EnderDragonPart[] getBodyParts() {
      return this.parts;
   }

   public boolean collides() {
      return false;
   }

   public SoundCategory getSoundCategory() {
      return SoundCategory.HOSTILE;
   }

   protected SoundEvent getAmbientSound() {
      return SoundEvents.ENTITY_ENDER_DRAGON_AMBIENT;
   }

   protected SoundEvent getHurtSound(DamageSource source) {
      return SoundEvents.ENTITY_ENDER_DRAGON_HURT;
   }

   protected float getSoundVolume() {
      return 5.0F;
   }

   public float getChangeInNeckPitch(int segmentOffset, double[] segment1, double[] segment2) {
      Phase phase = this.phaseManager.getCurrent();
      PhaseType<? extends Phase> phaseType = phase.getType();
      double h;
      if (phaseType != PhaseType.LANDING && phaseType != PhaseType.TAKEOFF) {
         if (phase.isSittingOrHovering()) {
            h = (double)segmentOffset;
         } else if (segmentOffset == 6) {
            h = 0.0D;
         } else {
            h = segment2[1] - segment1[1];
         }
      } else {
         BlockPos blockPos = this.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.ORIGIN);
         double d = Math.max(Math.sqrt(blockPos.getSquaredDistance(this.getPos(), true)) / 4.0D, 1.0D);
         h = (double)segmentOffset / d;
      }

      return (float)h;
   }

   public Vec3d getRotationVectorFromPhase(float tickDelta) {
      Phase phase = this.phaseManager.getCurrent();
      PhaseType<? extends Phase> phaseType = phase.getType();
      Vec3d vec3d2;
      float k;
      if (phaseType != PhaseType.LANDING && phaseType != PhaseType.TAKEOFF) {
         if (phase.isSittingOrHovering()) {
            float j = this.getPitch();
            k = 1.5F;
            this.setPitch(-45.0F);
            vec3d2 = this.getRotationVec(tickDelta);
            this.setPitch(j);
         } else {
            vec3d2 = this.getRotationVec(tickDelta);
         }
      } else {
         BlockPos blockPos = this.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.ORIGIN);
         k = Math.max((float)Math.sqrt(blockPos.getSquaredDistance(this.getPos(), true)) / 4.0F, 1.0F);
         float g = 6.0F / k;
         float h = this.getPitch();
         float i = 1.5F;
         this.setPitch(-g * 1.5F * 5.0F);
         vec3d2 = this.getRotationVec(tickDelta);
         this.setPitch(h);
      }

      return vec3d2;
   }

   public void crystalDestroyed(EndCrystalEntity crystal, BlockPos pos, DamageSource source) {
      PlayerEntity playerEntity2;
      if (source.getAttacker() instanceof PlayerEntity) {
         playerEntity2 = (PlayerEntity)source.getAttacker();
      } else {
         playerEntity2 = this.world.getClosestPlayer(CLOSE_PLAYER_PREDICATE, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ());
      }

      if (crystal == this.connectedCrystal) {
         this.damagePart(this.head, DamageSource.explosion((LivingEntity)playerEntity2), 10.0F);
      }

      this.phaseManager.getCurrent().crystalDestroyed(crystal, pos, source, playerEntity2);
   }

   public void onTrackedDataSet(TrackedData<?> data) {
      if (PHASE_TYPE.equals(data) && this.world.isClient) {
         this.phaseManager.setPhase(PhaseType.getFromId((Integer)this.getDataTracker().get(PHASE_TYPE)));
      }

      super.onTrackedDataSet(data);
   }

   public PhaseManager getPhaseManager() {
      return this.phaseManager;
   }

   @Nullable
   public EnderDragonFight getFight() {
      return this.fight;
   }

   public boolean addStatusEffect(StatusEffectInstance effect) {
      return false;
   }

   protected boolean canStartRiding(Entity entity) {
      return false;
   }

   public boolean canUsePortals() {
      return false;
   }

   public void readFromPacket(MobSpawnS2CPacket packet) {
      super.readFromPacket(packet);
      EnderDragonPart[] enderDragonParts = this.getBodyParts();

      for(int i = 0; i < enderDragonParts.length; ++i) {
         enderDragonParts[i].setId(i + packet.getId());
      }

   }

   public boolean canTarget(LivingEntity target) {
      return target.canTakeDamage();
   }

   static {
      PHASE_TYPE = DataTracker.registerData(EnderDragonEntity.class, TrackedDataHandlerRegistry.INTEGER);
      CLOSE_PLAYER_PREDICATE = TargetPredicate.createAttackable().setBaseMaxDistance(64.0D);
   }
}
