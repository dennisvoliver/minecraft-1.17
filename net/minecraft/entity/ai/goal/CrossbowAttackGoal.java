package net.minecraft.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.entity.CrossbowUser;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.Durations;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.intprovider.UniformIntProvider;

public class CrossbowAttackGoal<T extends HostileEntity & RangedAttackMob & CrossbowUser> extends Goal {
   public static final UniformIntProvider COOLDOWN_RANGE = Durations.betweenSeconds(1, 2);
   private final T actor;
   private CrossbowAttackGoal.Stage stage;
   private final double speed;
   private final float squaredRange;
   private int seeingTargetTicker;
   private int chargedTicksLeft;
   private int cooldown;

   public CrossbowAttackGoal(T actor, double speed, float range) {
      this.stage = CrossbowAttackGoal.Stage.UNCHARGED;
      this.actor = actor;
      this.speed = speed;
      this.squaredRange = range * range;
      this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
   }

   public boolean canStart() {
      return this.hasAliveTarget() && this.isEntityHoldingCrossbow();
   }

   private boolean isEntityHoldingCrossbow() {
      return this.actor.isHolding(Items.CROSSBOW);
   }

   public boolean shouldContinue() {
      return this.hasAliveTarget() && (this.canStart() || !this.actor.getNavigation().isIdle()) && this.isEntityHoldingCrossbow();
   }

   private boolean hasAliveTarget() {
      return this.actor.getTarget() != null && this.actor.getTarget().isAlive();
   }

   public void stop() {
      super.stop();
      this.actor.setAttacking(false);
      this.actor.setTarget((LivingEntity)null);
      this.seeingTargetTicker = 0;
      if (this.actor.isUsingItem()) {
         this.actor.clearActiveItem();
         ((CrossbowUser)this.actor).setCharging(false);
         CrossbowItem.setCharged(this.actor.getActiveItem(), false);
      }

   }

   public void tick() {
      LivingEntity livingEntity = this.actor.getTarget();
      if (livingEntity != null) {
         boolean bl = this.actor.getVisibilityCache().canSee(livingEntity);
         boolean bl2 = this.seeingTargetTicker > 0;
         if (bl != bl2) {
            this.seeingTargetTicker = 0;
         }

         if (bl) {
            ++this.seeingTargetTicker;
         } else {
            --this.seeingTargetTicker;
         }

         double d = this.actor.squaredDistanceTo(livingEntity);
         boolean bl3 = (d > (double)this.squaredRange || this.seeingTargetTicker < 5) && this.chargedTicksLeft == 0;
         if (bl3) {
            --this.cooldown;
            if (this.cooldown <= 0) {
               this.actor.getNavigation().startMovingTo(livingEntity, this.isUncharged() ? this.speed : this.speed * 0.5D);
               this.cooldown = COOLDOWN_RANGE.get(this.actor.getRandom());
            }
         } else {
            this.cooldown = 0;
            this.actor.getNavigation().stop();
         }

         this.actor.getLookControl().lookAt(livingEntity, 30.0F, 30.0F);
         if (this.stage == CrossbowAttackGoal.Stage.UNCHARGED) {
            if (!bl3) {
               this.actor.setCurrentHand(ProjectileUtil.getHandPossiblyHolding(this.actor, Items.CROSSBOW));
               this.stage = CrossbowAttackGoal.Stage.CHARGING;
               ((CrossbowUser)this.actor).setCharging(true);
            }
         } else if (this.stage == CrossbowAttackGoal.Stage.CHARGING) {
            if (!this.actor.isUsingItem()) {
               this.stage = CrossbowAttackGoal.Stage.UNCHARGED;
            }

            int i = this.actor.getItemUseTime();
            ItemStack itemStack = this.actor.getActiveItem();
            if (i >= CrossbowItem.getPullTime(itemStack)) {
               this.actor.stopUsingItem();
               this.stage = CrossbowAttackGoal.Stage.CHARGED;
               this.chargedTicksLeft = 20 + this.actor.getRandom().nextInt(20);
               ((CrossbowUser)this.actor).setCharging(false);
            }
         } else if (this.stage == CrossbowAttackGoal.Stage.CHARGED) {
            --this.chargedTicksLeft;
            if (this.chargedTicksLeft == 0) {
               this.stage = CrossbowAttackGoal.Stage.READY_TO_ATTACK;
            }
         } else if (this.stage == CrossbowAttackGoal.Stage.READY_TO_ATTACK && bl) {
            ((RangedAttackMob)this.actor).attack(livingEntity, 1.0F);
            ItemStack itemStack2 = this.actor.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this.actor, Items.CROSSBOW));
            CrossbowItem.setCharged(itemStack2, false);
            this.stage = CrossbowAttackGoal.Stage.UNCHARGED;
         }

      }
   }

   private boolean isUncharged() {
      return this.stage == CrossbowAttackGoal.Stage.UNCHARGED;
   }

   private static enum Stage {
      UNCHARGED,
      CHARGING,
      CHARGED,
      READY_TO_ATTACK;
   }
}
