package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.StateManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class EndRodBlock extends RodBlock {
   protected EndRodBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.UP));
   }

   public BlockState getPlacementState(ItemPlacementContext ctx) {
      Direction direction = ctx.getSide();
      BlockState blockState = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(direction.getOpposite()));
      return blockState.isOf(this) && blockState.get(FACING) == direction ? (BlockState)this.getDefaultState().with(FACING, direction.getOpposite()) : (BlockState)this.getDefaultState().with(FACING, direction);
   }

   public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
      Direction direction = (Direction)state.get(FACING);
      double d = (double)pos.getX() + 0.55D - (double)(random.nextFloat() * 0.1F);
      double e = (double)pos.getY() + 0.55D - (double)(random.nextFloat() * 0.1F);
      double f = (double)pos.getZ() + 0.55D - (double)(random.nextFloat() * 0.1F);
      double g = (double)(0.4F - (random.nextFloat() + random.nextFloat()) * 0.4F);
      if (random.nextInt(5) == 0) {
         world.addParticle(ParticleTypes.END_ROD, d + (double)direction.getOffsetX() * g, e + (double)direction.getOffsetY() * g, f + (double)direction.getOffsetZ() * g, random.nextGaussian() * 0.005D, random.nextGaussian() * 0.005D, random.nextGaussian() * 0.005D);
      }

   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   public PistonBehavior getPistonBehavior(BlockState state) {
      return PistonBehavior.NORMAL;
   }
}
