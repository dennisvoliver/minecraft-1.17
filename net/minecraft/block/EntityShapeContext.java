package net.minecraft.block;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

public class EntityShapeContext implements ShapeContext {
   protected static final ShapeContext ABSENT;
   private final boolean descending;
   private final double minY;
   private final ItemStack heldItem;
   private final ItemStack boots;
   private final Predicate<Fluid> walkOnFluidPredicate;
   private final Optional<Entity> entity;

   protected EntityShapeContext(boolean descending, double minY, ItemStack boots, ItemStack heldItem, Predicate<Fluid> walkOnFluidPredicate, Optional<Entity> entity) {
      this.descending = descending;
      this.minY = minY;
      this.boots = boots;
      this.heldItem = heldItem;
      this.walkOnFluidPredicate = walkOnFluidPredicate;
      this.entity = entity;
   }

   @Deprecated
   protected EntityShapeContext(Entity entity) {
      boolean var10001 = entity.isDescending();
      double var10002 = entity.getY();
      ItemStack var10003 = entity instanceof LivingEntity ? ((LivingEntity)entity).getEquippedStack(EquipmentSlot.FEET) : ItemStack.EMPTY;
      ItemStack var10004 = entity instanceof LivingEntity ? ((LivingEntity)entity).getMainHandStack() : ItemStack.EMPTY;
      Predicate var2;
      if (entity instanceof LivingEntity) {
         LivingEntity var10005 = (LivingEntity)entity;
         Objects.requireNonNull((LivingEntity)entity);
         var2 = var10005::canWalkOnFluid;
      } else {
         var2 = (fluid) -> {
            return false;
         };
      }

      this(var10001, var10002, var10003, var10004, var2, Optional.of(entity));
   }

   public boolean isWearingOnFeet(Item item) {
      return this.boots.isOf(item);
   }

   public boolean isHolding(Item item) {
      return this.heldItem.isOf(item);
   }

   public boolean canWalkOnFluid(FluidState state, FlowableFluid fluid) {
      return this.walkOnFluidPredicate.test(fluid) && !state.getFluid().matchesType(fluid);
   }

   public boolean isDescending() {
      return this.descending;
   }

   public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
      return this.minY > (double)pos.getY() + shape.getMax(Direction.Axis.Y) - 9.999999747378752E-6D;
   }

   public Optional<Entity> getEntity() {
      return this.entity;
   }

   static {
      ABSENT = new EntityShapeContext(false, -1.7976931348623157E308D, ItemStack.EMPTY, ItemStack.EMPTY, (fluid) -> {
         return false;
      }, Optional.empty()) {
         public boolean isAbove(VoxelShape shape, BlockPos pos, boolean defaultValue) {
            return defaultValue;
         }
      };
   }
}
