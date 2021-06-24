package net.minecraft.block;

import java.util.Random;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public class GlowLichenBlock extends AbstractLichenBlock implements Fertilizable, Waterloggable {
   private static final BooleanProperty WATERLOGGED;

   public GlowLichenBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)this.getDefaultState().with(WATERLOGGED, false));
   }

   /**
    * {@return a function that receives a {@link BlockState} and returns the luminance for the state}
    * If the lichen has no visible sides, it supplies 0.
    * 
    * @apiNote The return value is meant to be passed to
    * {@link AbstractBlock.Settings#luminance} builder method.
    * 
    * @param luminance luminance supplied when the lichen has at least one visible side
    */
   public static ToIntFunction<BlockState> getLuminanceSupplier(int luminance) {
      return (state) -> {
         return AbstractLichenBlock.hasAnyDirection(state) ? luminance : 0;
      };
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      super.appendProperties(builder);
      builder.add(WATERLOGGED);
   }

   public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
      if ((Boolean)state.get(WATERLOGGED)) {
         world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
      }

      return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
   }

   public boolean canReplace(BlockState state, ItemPlacementContext context) {
      return !context.getStack().isOf(Items.GLOW_LICHEN) || super.canReplace(state, context);
   }

   public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
      return Stream.of(DIRECTIONS).anyMatch((direction) -> {
         return this.canSpread(state, world, pos, direction.getOpposite());
      });
   }

   public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
      return true;
   }

   public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
      this.trySpreadRandomly(state, world, pos, random);
   }

   public FluidState getFluidState(BlockState state) {
      return (Boolean)state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
   }

   public boolean isTranslucent(BlockState state, BlockView world, BlockPos pos) {
      return state.getFluidState().isEmpty();
   }

   static {
      WATERLOGGED = Properties.WATERLOGGED;
   }
}
