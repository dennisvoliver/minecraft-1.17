package net.minecraft.item;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;

public class TallBlockItem extends BlockItem {
   public TallBlockItem(Block block, Item.Settings settings) {
      super(block, settings);
   }

   protected boolean place(ItemPlacementContext context, BlockState state) {
      context.getWorld().setBlockState(context.getBlockPos().up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.REDRAW_ON_MAIN_THREAD | Block.FORCE_STATE);
      return super.place(context, state);
   }
}
