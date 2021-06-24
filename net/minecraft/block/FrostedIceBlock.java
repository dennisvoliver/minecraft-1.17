package net.minecraft.block;

import java.util.Random;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class FrostedIceBlock extends IceBlock {
   public static final int MAX_AGE = 3;
   public static final IntProperty AGE;
   private static final int field_31097 = 4;
   private static final int field_31098 = 2;

   public FrostedIceBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(AGE, 0));
   }

   public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      this.scheduledTick(state, world, pos, random);
   }

   public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      if ((random.nextInt(3) == 0 || this.canMelt(world, pos, 4)) && world.getLightLevel(pos) > 11 - (Integer)state.get(AGE) - state.getOpacity(world, pos) && this.increaseAge(state, world, pos)) {
         BlockPos.Mutable mutable = new BlockPos.Mutable();
         Direction[] var6 = Direction.values();
         int var7 = var6.length;

         for(int var8 = 0; var8 < var7; ++var8) {
            Direction direction = var6[var8];
            mutable.set(pos, (Direction)direction);
            BlockState blockState = world.getBlockState(mutable);
            if (blockState.isOf(this) && !this.increaseAge(blockState, world, mutable)) {
               world.getBlockTickScheduler().schedule(mutable, this, MathHelper.nextInt(random, 20, 40));
            }
         }

      } else {
         world.getBlockTickScheduler().schedule(pos, this, MathHelper.nextInt(random, 20, 40));
      }
   }

   private boolean increaseAge(BlockState state, World world, BlockPos pos) {
      int i = (Integer)state.get(AGE);
      if (i < 3) {
         world.setBlockState(pos, (BlockState)state.with(AGE, i + 1), Block.NOTIFY_LISTENERS);
         return false;
      } else {
         this.melt(state, world, pos);
         return true;
      }
   }

   public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
      if (block.getDefaultState().isOf(this) && this.canMelt(world, pos, 2)) {
         this.melt(state, world, pos);
      }

      super.neighborUpdate(state, world, pos, block, fromPos, notify);
   }

   private boolean canMelt(BlockView world, BlockPos pos, int maxNeighbors) {
      int i = 0;
      BlockPos.Mutable mutable = new BlockPos.Mutable();
      Direction[] var6 = Direction.values();
      int var7 = var6.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         Direction direction = var6[var8];
         mutable.set(pos, (Direction)direction);
         if (world.getBlockState(mutable).isOf(this)) {
            ++i;
            if (i >= maxNeighbors) {
               return false;
            }
         }
      }

      return true;
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(AGE);
   }

   public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
      return ItemStack.EMPTY;
   }

   static {
      AGE = Properties.AGE_3;
   }
}
