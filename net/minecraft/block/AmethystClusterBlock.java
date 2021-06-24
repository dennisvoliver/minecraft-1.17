package net.minecraft.block;

import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class AmethystClusterBlock extends AmethystBlock implements Waterloggable {
   public static final BooleanProperty WATERLOGGED;
   public static final DirectionProperty FACING;
   protected final VoxelShape NORTH_SHAPE;
   protected final VoxelShape SOUTH_SHAPE;
   protected final VoxelShape EAST_SHAPE;
   protected final VoxelShape WEST_SHAPE;
   protected final VoxelShape UP_SHAPE;
   protected final VoxelShape DOWN_SHAPE;

   public AmethystClusterBlock(int height, int xzOffset, AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)((BlockState)this.getDefaultState().with(WATERLOGGED, false)).with(FACING, Direction.UP));
      this.UP_SHAPE = Block.createCuboidShape((double)xzOffset, 0.0D, (double)xzOffset, (double)(16 - xzOffset), (double)height, (double)(16 - xzOffset));
      this.DOWN_SHAPE = Block.createCuboidShape((double)xzOffset, (double)(16 - height), (double)xzOffset, (double)(16 - xzOffset), 16.0D, (double)(16 - xzOffset));
      this.NORTH_SHAPE = Block.createCuboidShape((double)xzOffset, (double)xzOffset, (double)(16 - height), (double)(16 - xzOffset), (double)(16 - xzOffset), 16.0D);
      this.SOUTH_SHAPE = Block.createCuboidShape((double)xzOffset, (double)xzOffset, 0.0D, (double)(16 - xzOffset), (double)(16 - xzOffset), (double)height);
      this.EAST_SHAPE = Block.createCuboidShape(0.0D, (double)xzOffset, (double)xzOffset, (double)height, (double)(16 - xzOffset), (double)(16 - xzOffset));
      this.WEST_SHAPE = Block.createCuboidShape((double)(16 - height), (double)xzOffset, (double)xzOffset, 16.0D, (double)(16 - xzOffset), (double)(16 - xzOffset));
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      Direction direction = (Direction)state.get(FACING);
      switch(direction) {
      case NORTH:
         return this.NORTH_SHAPE;
      case SOUTH:
         return this.SOUTH_SHAPE;
      case EAST:
         return this.EAST_SHAPE;
      case WEST:
         return this.WEST_SHAPE;
      case DOWN:
         return this.DOWN_SHAPE;
      case UP:
      default:
         return this.UP_SHAPE;
      }
   }

   public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
      Direction direction = (Direction)state.get(FACING);
      BlockPos blockPos = pos.offset(direction.getOpposite());
      return world.getBlockState(blockPos).isSideSolidFullSquare(world, blockPos, direction);
   }

   public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
      if ((Boolean)state.get(WATERLOGGED)) {
         world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
      }

      return direction == ((Direction)state.get(FACING)).getOpposite() && !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
   }

   @Nullable
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      WorldAccess worldAccess = ctx.getWorld();
      BlockPos blockPos = ctx.getBlockPos();
      return (BlockState)((BlockState)this.getDefaultState().with(WATERLOGGED, worldAccess.getFluidState(blockPos).getFluid() == Fluids.WATER)).with(FACING, ctx.getSide());
   }

   public BlockState rotate(BlockState state, BlockRotation rotation) {
      return (BlockState)state.with(FACING, rotation.rotate((Direction)state.get(FACING)));
   }

   public BlockState mirror(BlockState state, BlockMirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.get(FACING)));
   }

   public FluidState getFluidState(BlockState state) {
      return (Boolean)state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(WATERLOGGED, FACING);
   }

   public PistonBehavior getPistonBehavior(BlockState state) {
      return PistonBehavior.DESTROY;
   }

   static {
      WATERLOGGED = Properties.WATERLOGGED;
      FACING = Properties.FACING;
   }
}
