package net.minecraft.block;

import java.util.Random;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class CaveVinesBodyBlock extends AbstractPlantBlock implements Fertilizable, CaveVines {
   public CaveVinesBodyBlock(AbstractBlock.Settings settings) {
      super(settings, Direction.DOWN, SHAPE, false);
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(BERRIES, false));
   }

   protected AbstractPlantStemBlock getStem() {
      return (AbstractPlantStemBlock)Blocks.CAVE_VINES;
   }

   protected BlockState copyState(BlockState from, BlockState to) {
      return (BlockState)to.with(BERRIES, (Boolean)from.get(BERRIES));
   }

   public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
      return new ItemStack(Items.GLOW_BERRIES);
   }

   public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      return CaveVines.pickBerries(state, world, pos);
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(BERRIES);
   }

   public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
      return !(Boolean)state.get(BERRIES);
   }

   public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
      return true;
   }

   public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
      world.setBlockState(pos, (BlockState)state.with(BERRIES, true), Block.NOTIFY_LISTENERS);
   }
}
