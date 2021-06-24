package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.feature.EndPortalFeature;
import org.jetbrains.annotations.Nullable;

public class LandingApproachPhase extends AbstractPhase {
   private static final TargetPredicate PLAYERS_IN_RANGE_PREDICATE = TargetPredicate.createAttackable().ignoreVisibility();
   private Path path;
   private Vec3d pathTarget;

   public LandingApproachPhase(EnderDragonEntity enderDragonEntity) {
      super(enderDragonEntity);
   }

   public PhaseType<LandingApproachPhase> getType() {
      return PhaseType.LANDING_APPROACH;
   }

   public void beginPhase() {
      this.path = null;
      this.pathTarget = null;
   }

   public void serverTick() {
      double d = this.pathTarget == null ? 0.0D : this.pathTarget.squaredDistanceTo(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
      if (d < 100.0D || d > 22500.0D || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
         this.updatePath();
      }

   }

   @Nullable
   public Vec3d getPathTarget() {
      return this.pathTarget;
   }

   private void updatePath() {
      if (this.path == null || this.path.isFinished()) {
         int i = this.dragon.getNearestPathNodeIndex();
         BlockPos blockPos = this.dragon.world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, EndPortalFeature.ORIGIN);
         PlayerEntity playerEntity = this.dragon.world.getClosestPlayer(PLAYERS_IN_RANGE_PREDICATE, this.dragon, (double)blockPos.getX(), (double)blockPos.getY(), (double)blockPos.getZ());
         int k;
         if (playerEntity != null) {
            Vec3d vec3d = (new Vec3d(playerEntity.getX(), 0.0D, playerEntity.getZ())).normalize();
            k = this.dragon.getNearestPathNodeIndex(-vec3d.x * 40.0D, 105.0D, -vec3d.z * 40.0D);
         } else {
            k = this.dragon.getNearestPathNodeIndex(40.0D, (double)blockPos.getY(), 0.0D);
         }

         PathNode pathNode = new PathNode(blockPos.getX(), blockPos.getY(), blockPos.getZ());
         this.path = this.dragon.findPath(i, k, pathNode);
         if (this.path != null) {
            this.path.next();
         }
      }

      this.followPath();
      if (this.path != null && this.path.isFinished()) {
         this.dragon.getPhaseManager().setPhase(PhaseType.LANDING);
      }

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
}
