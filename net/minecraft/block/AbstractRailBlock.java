package net.minecraft.block;

import net.minecraft.block.enums.RailShape;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;

public abstract class AbstractRailBlock extends Block implements Waterloggable {
   protected static final VoxelShape STRAIGHT_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
   protected static final VoxelShape ASCENDING_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);
   public static final BooleanProperty WATERLOGGED;
   private final boolean allowCurves;

   public static boolean isRail(World world, BlockPos pos) {
      return isRail(world.getBlockState(pos));
   }

   public static boolean isRail(BlockState state) {
      return state.isIn(BlockTags.RAILS) && state.getBlock() instanceof AbstractRailBlock;
   }

   protected AbstractRailBlock(boolean allowCurves, AbstractBlock.Settings settings) {
      super(settings);
      this.allowCurves = allowCurves;
   }

   public boolean canMakeCurves() {
      return this.allowCurves;
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      RailShape railShape = state.isOf(this) ? (RailShape)state.get(this.getShapeProperty()) : null;
      return railShape != null && railShape.isAscending() ? ASCENDING_SHAPE : STRAIGHT_SHAPE;
   }

   public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
      return hasTopRim(world, pos.down());
   }

   public void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
      if (!oldState.isOf(state.getBlock())) {
         this.updateCurves(state, world, pos, notify);
      }
   }

   protected BlockState updateCurves(BlockState state, World world, BlockPos pos, boolean notify) {
      state = this.updateBlockState(world, pos, state, true);
      if (this.allowCurves) {
         state.neighborUpdate(world, pos, this, pos, notify);
      }

      return state;
   }

   public void neighborUpdate(BlockState state, World world, BlockPos pos, Block block, BlockPos fromPos, boolean notify) {
      if (!world.isClient && world.getBlockState(pos).isOf(this)) {
         RailShape railShape = (RailShape)state.get(this.getShapeProperty());
         if (shouldDropRail(pos, world, railShape)) {
            dropStacks(state, world, pos);
            world.removeBlock(pos, notify);
         } else {
            this.updateBlockState(state, world, pos, block);
         }

      }
   }

   /**
    * Checks if this rail should be dropped.
    * 
    * <p>This method will return true if:
    * <ul><li>The rail block is ascending.</li>
    * <li>The block in the direction of ascent does not have a top rim.</li></ul>
    */
   private static boolean shouldDropRail(BlockPos pos, World world, RailShape shape) {
      if (!hasTopRim(world, pos.down())) {
         return true;
      } else {
         switch(shape) {
         case ASCENDING_EAST:
            return !hasTopRim(world, pos.east());
         case ASCENDING_WEST:
            return !hasTopRim(world, pos.west());
         case ASCENDING_NORTH:
            return !hasTopRim(world, pos.north());
         case ASCENDING_SOUTH:
            return !hasTopRim(world, pos.south());
         default:
            return false;
         }
      }
   }

   protected void updateBlockState(BlockState state, World world, BlockPos pos, Block neighbor) {
   }

   protected BlockState updateBlockState(World world, BlockPos pos, BlockState state, boolean forceUpdate) {
      if (world.isClient) {
         return state;
      } else {
         RailShape railShape = (RailShape)state.get(this.getShapeProperty());
         return (new RailPlacementHelper(world, pos, state)).updateBlockState(world.isReceivingRedstonePower(pos), forceUpdate, railShape).getBlockState();
      }
   }

   public PistonBehavior getPistonBehavior(BlockState state) {
      return PistonBehavior.NORMAL;
   }

   public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
      if (!moved) {
         super.onStateReplaced(state, world, pos, newState, moved);
         if (((RailShape)state.get(this.getShapeProperty())).isAscending()) {
            world.updateNeighborsAlways(pos.up(), this);
         }

         if (this.allowCurves) {
            world.updateNeighborsAlways(pos, this);
            world.updateNeighborsAlways(pos.down(), this);
         }

      }
   }

   public BlockState getPlacementState(ItemPlacementContext ctx) {
      FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
      boolean bl = fluidState.getFluid() == Fluids.WATER;
      BlockState blockState = super.getDefaultState();
      Direction direction = ctx.getPlayerFacing();
      boolean bl2 = direction == Direction.EAST || direction == Direction.WEST;
      return (BlockState)((BlockState)blockState.with(this.getShapeProperty(), bl2 ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH)).with(WATERLOGGED, bl);
   }

   public abstract Property<RailShape> getShapeProperty();

   public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
      if ((Boolean)state.get(WATERLOGGED)) {
         world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
      }

      return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
   }

   public FluidState getFluidState(BlockState state) {
      return (Boolean)state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
   }

   static {
      WATERLOGGED = Properties.WATERLOGGED;
   }
}
