package net.minecraft.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.PortalUtil;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public class BigDripleafStemBlock extends HorizontalFacingBlock implements Fertilizable, Waterloggable {
   private static final BooleanProperty WATERLOGGED;
   private static final int field_31021 = 6;
   protected static final VoxelShape NORTH_SHAPE;
   protected static final VoxelShape SOUTH_SHAPE;
   protected static final VoxelShape EAST_SHAPE;
   protected static final VoxelShape WEST_SHAPE;

   protected BigDripleafStemBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(WATERLOGGED, false)).with(FACING, Direction.NORTH));
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      switch((Direction)state.get(FACING)) {
      case SOUTH:
         return SOUTH_SHAPE;
      case NORTH:
      default:
         return NORTH_SHAPE;
      case WEST:
         return WEST_SHAPE;
      case EAST:
         return EAST_SHAPE;
      }
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(WATERLOGGED, FACING);
   }

   public FluidState getFluidState(BlockState state) {
      return (Boolean)state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
   }

   public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
      BlockPos blockPos = pos.down();
      BlockState blockState = world.getBlockState(blockPos);
      BlockState blockState2 = world.getBlockState(pos.up());
      return (blockState.isOf(this) || blockState.isSideSolidFullSquare(world, blockPos, Direction.UP)) && (blockState2.isOf(this) || blockState2.isOf(Blocks.BIG_DRIPLEAF));
   }

   protected static boolean placeStemAt(WorldAccess world, BlockPos pos, FluidState fluidState, Direction direction) {
      BlockState blockState = (BlockState)((BlockState)Blocks.BIG_DRIPLEAF_STEM.getDefaultState().with(WATERLOGGED, fluidState.isEqualAndStill(Fluids.WATER))).with(FACING, direction);
      return world.setBlockState(pos, blockState, Block.NOTIFY_ALL);
   }

   public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
      if ((direction == Direction.DOWN || direction == Direction.UP) && !state.canPlaceAt(world, pos)) {
         world.getBlockTickScheduler().schedule(pos, this, 1);
      }

      if ((Boolean)state.get(WATERLOGGED)) {
         world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
      }

      return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
   }

   public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      if (!state.canPlaceAt(world, pos)) {
         world.breakBlock(pos, true);
      }

   }

   public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
      Optional<BlockPos> optional = PortalUtil.method_34851(world, pos, state.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);
      if (!optional.isPresent()) {
         return false;
      } else {
         BlockPos blockPos = ((BlockPos)optional.get()).up();
         BlockState blockState = world.getBlockState(blockPos);
         return BigDripleafBlock.canGrowInto(world, blockPos, blockState);
      }
   }

   public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
      return true;
   }

   public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
      Optional<BlockPos> optional = PortalUtil.method_34851(world, pos, state.getBlock(), Direction.UP, Blocks.BIG_DRIPLEAF);
      if (optional.isPresent()) {
         BlockPos blockPos = (BlockPos)optional.get();
         BlockPos blockPos2 = blockPos.up();
         Direction direction = (Direction)state.get(FACING);
         placeStemAt(world, blockPos, world.getFluidState(blockPos), direction);
         BigDripleafBlock.placeDripleafAt(world, blockPos2, world.getFluidState(blockPos2), direction);
      }
   }

   public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
      return new ItemStack(Blocks.BIG_DRIPLEAF);
   }

   static {
      WATERLOGGED = Properties.WATERLOGGED;
      NORTH_SHAPE = Block.createCuboidShape(5.0D, 0.0D, 9.0D, 11.0D, 16.0D, 15.0D);
      SOUTH_SHAPE = Block.createCuboidShape(5.0D, 0.0D, 1.0D, 11.0D, 16.0D, 7.0D);
      EAST_SHAPE = Block.createCuboidShape(1.0D, 0.0D, 5.0D, 7.0D, 16.0D, 11.0D);
      WEST_SHAPE = Block.createCuboidShape(9.0D, 0.0D, 5.0D, 15.0D, 16.0D, 11.0D);
   }
}
