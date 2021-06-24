package net.minecraft.block;

import net.minecraft.block.cauldron.CauldronBehavior;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LavaCauldronBlock extends AbstractCauldronBlock {
   public LavaCauldronBlock(AbstractBlock.Settings settings) {
      super(settings, CauldronBehavior.LAVA_CAULDRON_BEHAVIOR);
   }

   protected double getFluidHeight(BlockState state) {
      return 0.9375D;
   }

   public boolean isFull(BlockState state) {
      return true;
   }

   public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
      if (this.isEntityTouchingFluid(state, pos, entity)) {
         entity.setOnFireFromLava();
      }

   }

   public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
      return 3;
   }
}
