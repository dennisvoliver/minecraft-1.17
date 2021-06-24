package net.minecraft.entity.boss.dragon.phase;

import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class ChargingPlayerPhase extends AbstractPhase {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int field_30431 = 10;
   private Vec3d pathTarget;
   private int chargingTicks;

   public ChargingPlayerPhase(EnderDragonEntity enderDragonEntity) {
      super(enderDragonEntity);
   }

   public void serverTick() {
      if (this.pathTarget == null) {
         LOGGER.warn("Aborting charge player as no target was set.");
         this.dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
      } else if (this.chargingTicks > 0 && this.chargingTicks++ >= 10) {
         this.dragon.getPhaseManager().setPhase(PhaseType.HOLDING_PATTERN);
      } else {
         double d = this.pathTarget.squaredDistanceTo(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
         if (d < 100.0D || d > 22500.0D || this.dragon.horizontalCollision || this.dragon.verticalCollision) {
            ++this.chargingTicks;
         }

      }
   }

   public void beginPhase() {
      this.pathTarget = null;
      this.chargingTicks = 0;
   }

   public void setPathTarget(Vec3d pathTarget) {
      this.pathTarget = pathTarget;
   }

   public float getMaxYAcceleration() {
      return 3.0F;
   }

   @Nullable
   public Vec3d getPathTarget() {
      return this.pathTarget;
   }

   public PhaseType<ChargingPlayerPhase> getType() {
      return PhaseType.CHARGING_PLAYER;
   }
}
