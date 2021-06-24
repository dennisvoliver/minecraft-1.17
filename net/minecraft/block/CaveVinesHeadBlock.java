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

public class CaveVinesHeadBlock extends AbstractPlantStemBlock implements Fertilizable, CaveVines {
   private static final float GROW_CHANCE = 0.11F;

   public CaveVinesHeadBlock(AbstractBlock.Settings settings) {
      super(settings, Direction.DOWN, SHAPE, false, 0.1D);
      this.setDefaultState((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(AGE, 0)).with(BERRIES, false));
   }

   protected int getGrowthLength(Random random) {
      return 1;
   }

   protected boolean chooseStemState(BlockState state) {
      return state.isAir();
   }

   protected Block getPlant() {
      return Blocks.CAVE_VINES_PLANT;
   }

   protected BlockState copyState(BlockState from, BlockState to) {
      return (BlockState)to.with(BERRIES, (Boolean)from.get(BERRIES));
   }

   protected BlockState age(BlockState state, Random random) {
      return (BlockState)super.age(state, random).with(BERRIES, random.nextFloat() < 0.11F);
   }

   public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
      return new ItemStack(Items.GLOW_BERRIES);
   }

   public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      return CaveVines.pickBerries(state, world, pos);
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      super.appendProperties(builder);
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
