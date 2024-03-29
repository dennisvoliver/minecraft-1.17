package net.minecraft.entity.mob;

import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.world.World;

public abstract class IllagerEntity extends RaiderEntity {
   protected IllagerEntity(EntityType<? extends IllagerEntity> entityType, World world) {
      super(entityType, world);
   }

   protected void initGoals() {
      super.initGoals();
   }

   public EntityGroup getGroup() {
      return EntityGroup.ILLAGER;
   }

   public IllagerEntity.State getState() {
      return IllagerEntity.State.CROSSED;
   }

   public static enum State {
      CROSSED,
      ATTACKING,
      SPELLCASTING,
      BOW_AND_ARROW,
      CROSSBOW_HOLD,
      CROSSBOW_CHARGE,
      CELEBRATING,
      NEUTRAL;
   }

   protected class LongDoorInteractGoal extends net.minecraft.entity.ai.goal.LongDoorInteractGoal {
      public LongDoorInteractGoal(RaiderEntity raider) {
         super(raider, false);
      }

      public boolean canStart() {
         return super.canStart() && IllagerEntity.this.hasActiveRaid();
      }
   }
}
