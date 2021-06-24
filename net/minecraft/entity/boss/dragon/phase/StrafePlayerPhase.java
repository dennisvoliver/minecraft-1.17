package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.DragonFireballEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.WorldEvents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class StrafePlayerPhase extends AbstractPhase {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int field_30440 = 5;
   private int seenTargetTimes;
   private Path path;
   private Vec3d pathTarget;
   private LivingEntity target;
   private boolean shouldFindNewPath;

   public StrafePlayerPhase(EnderDragonEntity enderDragonEntity) {
      super(enderDragonEntity);
   }

   public void serverTick() {
      if (this.target == null) {
         LOGGER.warn("Skipping player strafe phase because no player was found");
         this.dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
      } else {
         double j;
         double k;
         double n;
         if (this.path != null && this.path.isFinished()) {
            j = this.target.getX();
            k = this.target.getZ();
            double f = j - this.dragon.getX();
            double g = k - this.dragon.getZ();
            n = Math.sqrt(f * f + g * g);
            double i = Math.min(0.4000000059604645D + n / 80.0D - 1.0D, 10.0D);
            this.pathTarget = new Vec3d(j, this.target.getY() + i, k);
         }

         j = this.pathTarget == null ? 0.0D : this.pathTarget.squaredDistanceTo(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
         if (j < 100.0D || j > 22500.0D) {
            this.updatePath();
         }

         k = 64.0D;
         if (this.target.squaredDistanceTo(this.dragon) < 4096.0D) {
            if (this.dragon.canSee(this.target)) {
               ++this.seenTargetTimes;
               Vec3d vec3d = (new Vec3d(this.target.getX() - this.dragon.getX(), 0.0D, this.target.getZ() - this.dragon.getZ())).normalize();
               Vec3d vec3d2 = (new Vec3d((double)MathHelper.sin(this.dragon.getYaw() * 0.017453292F), 0.0D, (double)(-MathHelper.cos(this.dragon.getYaw() * 0.017453292F)))).normalize();
               float l = (float)vec3d2.dotProduct(vec3d);
               float m = (float)(Math.acos((double)l) * 57.2957763671875D);
               m += 0.5F;
               if (this.seenTargetTimes >= 5 && m >= 0.0F && m < 10.0F) {
                  n = 1.0D;
                  Vec3d vec3d3 = this.dragon.getRotationVec(1.0F);
                  double o = this.dragon.head.getX() - vec3d3.x * 1.0D;
                  double p = this.dragon.head.getBodyY(0.5D) + 0.5D;
                  double q = this.dragon.head.getZ() - vec3d3.z * 1.0D;
                  double r = this.target.getX() - o;
                  double s = this.target.getBodyY(0.5D) - p;
                  double t = this.target.getZ() - q;
                  if (!this.dragon.isSilent()) {
                     this.dragon.world.syncWorldEvent((PlayerEntity)null, WorldEvents.ENDER_DRAGON_SHOOTS, this.dragon.getBlockPos(), 0);
                  }

                  DragonFireballEntity dragonFireballEntity = new DragonFireballEntity(this.dragon.world, this.dragon, r, s, t);
                  dragonFireballEntity.refreshPositionAndAngles(o, p, q, 0.0F, 0.0F);
                  this.dragon.world.spawnEntity(dragonFireballEntity);
                  this.seenTargetTimes = 0;
                  if (this.path != null) {
                     while(!this.path.isFinished()) {
                        this.path.next();
                     }
                  }

                  this.dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
               }
            } else if (this.seenTargetTimes > 0) {
               --this.seenTargetTimes;
            }
         } else if (this.seenTargetTimes > 0) {
            --this.seenTargetTimes;
         }

      }
   }

   private void updatePath() {
      if (this.path == null || this.path.isFinished()) {
         int i = this.dragon.getNearestPathNodeIndex();
         int j = i;
         if (this.dragon.getRandom().nextInt(8) == 0) {
            this.shouldFindNewPath = !this.shouldFindNewPath;
            j = i + 6;
         }

         if (this.shouldFindNewPath) {
            ++j;
         } else {
            --j;
         }

         if (this.dragon.getFight() != null && this.dragon.getFight().getAliveEndCrystals() > 0) {
            j %= 12;
            if (j < 0) {
               j += 12;
            }
         } else {
            j -= 12;
            j &= 7;
            j += 12;
         }

         this.path = this.dragon.findPath(i, j, (PathNode)null);
         if (this.path != null) {
            this.path.next();
         }
      }

      this.followPath();
   }

   private void followPath() {
      if (this.path != null && !this.path.isFinished()) {
         Vec3i vec3i = this.path.getCurrentNodePos();
         this.path.next();
         double d = (double)vec3i.getX();
         double e = (double)vec3i.getZ();

         double f;
         do {
            f = (double)((float)vec3i.getY() + this.dragon.getRandom().nextFloat() * 20.0F);
         } while(f < (double)vec3i.getY());

         this.pathTarget = new Vec3d(d, f, e);
      }

   }

   public void beginPhase() {
      this.seenTargetTimes = 0;
      this.pathTarget = null;
      this.path = null;
      this.target = null;
   }

   public void setTargetEntity(LivingEntity targetEntity) {
      this.target = targetEntity;
      int i = this.dragon.getNearestPathNodeIndex();
      int j = this.dragon.getNearestPathNodeIndex(this.target.getX(), this.target.getY(), this.target.getZ());
      int k = this.target.getBlockX();
      int l = this.target.getBlockZ();
      double d = (double)k - this.dragon.getX();
      double e = (double)l - this.dragon.getZ();
      double f = Math.sqrt(d * d + e * e);
      double g = Math.min(0.4000000059604645D + f / 80.0D - 1.0D, 10.0D);
      int m = MathHelper.floor(this.target.getY() + g);
      PathNode pathNode = new PathNode(k, m, l);
      this.path = this.dragon.findPath(i, j, pathNode);
      if (this.path != null) {
         this.path.next();
         this.followPath();
      }

   }

   @Nullable
   public Vec3d getPathTarget() {
      return this.pathTarget;
   }

   public PhaseType<StrafePlayerPhase> getType() {
      return PhaseType.STRAFE_PLAYER;
   }
}
