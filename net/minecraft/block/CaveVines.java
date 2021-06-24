package net.minecraft.block;

import java.util.function.ToIntFunction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public interface CaveVines {
   VoxelShape SHAPE = Block.createCuboidShape(1.0D, 0.0D, 1.0D, 15.0D, 16.0D, 15.0D);
   BooleanProperty BERRIES = Properties.BERRIES;

   static ActionResult pickBerries(BlockState state, World world, BlockPos pos) {
      if ((Boolean)state.get(BERRIES)) {
         Block.dropStack(world, pos, new ItemStack(Items.GLOW_BERRIES, 1));
         float f = MathHelper.nextBetween(world.random, 0.8F, 1.2F);
         world.playSound((PlayerEntity)null, pos, SoundEvents.BLOCK_CAVE_VINES_PICK_BERRIES, SoundCategory.BLOCKS, 1.0F, f);
         world.setBlockState(pos, (BlockState)state.with(BERRIES, false), Block.NOTIFY_LISTENERS);
         return ActionResult.success(world.isClient);
      } else {
         return ActionResult.PASS;
      }
   }

   static boolean hasBerries(BlockState state) {
      return state.contains(BERRIES) && (Boolean)state.get(BERRIES);
   }

   /**
    * {@return a function that receives a {@link BlockState} and returns the luminance for the state}
    * If there are no berries, it supplies the value 0.
    * 
    * @apiNote The return value is meant to be passed to
    * {@link AbstractBlock.Settings#luminance} builder method.
    * 
    * @param luminance luminance supplied when the block has berries
    */
   static ToIntFunction<BlockState> getLuminanceSupplier(int luminance) {
      return (state) -> {
         return (Boolean)state.get(Properties.BERRIES) ? luminance : 0;
      };
   }
}
