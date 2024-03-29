package net.minecraft.entity.mob;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ToolItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractPiglinEntity extends HostileEntity {
   protected static final TrackedData<Boolean> IMMUNE_TO_ZOMBIFICATION;
   protected static final int TIME_TO_ZOMBIFY = 300;
   protected int timeInOverworld;

   public AbstractPiglinEntity(EntityType<? extends AbstractPiglinEntity> entityType, World world) {
      super(entityType, world);
      this.setCanPickUpLoot(true);
      this.setCanPathThroughDoors();
      this.setPathfindingPenalty(PathNodeType.DANGER_FIRE, 16.0F);
      this.setPathfindingPenalty(PathNodeType.DAMAGE_FIRE, -1.0F);
   }

   private void setCanPathThroughDoors() {
      if (NavigationConditions.hasMobNavigation(this)) {
         ((MobNavigation)this.getNavigation()).setCanPathThroughDoors(true);
      }

   }

   protected abstract boolean canHunt();

   public void setImmuneToZombification(boolean immuneToZombification) {
      this.getDataTracker().set(IMMUNE_TO_ZOMBIFICATION, immuneToZombification);
   }

   protected boolean isImmuneToZombification() {
      return (Boolean)this.getDataTracker().get(IMMUNE_TO_ZOMBIFICATION);
   }

   protected void initDataTracker() {
      super.initDataTracker();
      this.dataTracker.startTracking(IMMUNE_TO_ZOMBIFICATION, false);
   }

   public void writeCustomDataToNbt(NbtCompound nbt) {
      super.writeCustomDataToNbt(nbt);
      if (this.isImmuneToZombification()) {
         nbt.putBoolean("IsImmuneToZombification", true);
      }

      nbt.putInt("TimeInOverworld", this.timeInOverworld);
   }

   public double getHeightOffset() {
      return this.isBaby() ? -0.05D : -0.45D;
   }

   public void readCustomDataFromNbt(NbtCompound nbt) {
      super.readCustomDataFromNbt(nbt);
      this.setImmuneToZombification(nbt.getBoolean("IsImmuneToZombification"));
      this.timeInOverworld = nbt.getInt("TimeInOverworld");
   }

   protected void mobTick() {
      super.mobTick();
      if (this.shouldZombify()) {
         ++this.timeInOverworld;
      } else {
         this.timeInOverworld = 0;
      }

      if (this.timeInOverworld > 300) {
         this.playZombificationSound();
         this.zombify((ServerWorld)this.world);
      }

   }

   public boolean shouldZombify() {
      return !this.world.getDimension().isPiglinSafe() && !this.isImmuneToZombification() && !this.isAiDisabled();
   }

   protected void zombify(ServerWorld world) {
      ZombifiedPiglinEntity zombifiedPiglinEntity = (ZombifiedPiglinEntity)this.convertTo(EntityType.ZOMBIFIED_PIGLIN, true);
      if (zombifiedPiglinEntity != null) {
         zombifiedPiglinEntity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 0));
      }

   }

   public boolean isAdult() {
      return !this.isBaby();
   }

   public abstract PiglinActivity getActivity();

   @Nullable
   public LivingEntity getTarget() {
      return (LivingEntity)this.brain.getOptionalMemory(MemoryModuleType.ATTACK_TARGET).orElse((Object)null);
   }

   protected boolean isHoldingTool() {
      return this.getMainHandStack().getItem() instanceof ToolItem;
   }

   public void playAmbientSound() {
      if (PiglinBrain.hasIdleActivity(this)) {
         super.playAmbientSound();
      }

   }

   protected void sendAiDebugData() {
      super.sendAiDebugData();
      DebugInfoSender.sendBrainDebugData(this);
   }

   protected abstract void playZombificationSound();

   static {
      IMMUNE_TO_ZOMBIFICATION = DataTracker.registerData(AbstractPiglinEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
   }
}
