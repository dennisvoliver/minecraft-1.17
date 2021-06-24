package net.minecraft.block;

import java.util.Random;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class CocoaBlock extends HorizontalFacingBlock implements Fertilizable {
   public static final int MAX_AGE = 2;
   public static final IntProperty AGE;
   protected static final int field_31062 = 4;
   protected static final int field_31063 = 5;
   protected static final int field_31064 = 2;
   protected static final int field_31065 = 6;
   protected static final int field_31066 = 7;
   protected static final int field_31067 = 3;
   protected static final int field_31068 = 8;
   protected static final int field_31069 = 9;
   protected static final int field_31070 = 4;
   protected static final VoxelShape[] AGE_TO_EAST_SHAPE;
   protected static final VoxelShape[] AGE_TO_WEST_SHAPE;
   protected static final VoxelShape[] AGE_TO_NORTH_SHAPE;
   protected static final VoxelShape[] AGE_TO_SOUTH_SHAPE;

   public CocoaBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.NORTH)).with(AGE, 0));
   }

   public boolean hasRandomTicks(BlockState state) {
      return (Integer)state.get(AGE) < 2;
   }

   public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      if (world.random.nextInt(5) == 0) {
         int i = (Integer)state.get(AGE);
         if (i < 2) {
            world.setBlockState(pos, (BlockState)state.with(AGE, i + 1), Block.NOTIFY_LISTENERS);
         }
      }

   }

   public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
      BlockState blockState = world.getBlockState(pos.offset((Direction)state.get(FACING)));
      return blockState.isIn(BlockTags.JUNGLE_LOGS);
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      int i = (Integer)state.get(AGE);
      switch((Direction)state.get(FACING)) {
      case SOUTH:
         return AGE_TO_SOUTH_SHAPE[i];
      case NORTH:
      default:
         return AGE_TO_NORTH_SHAPE[i];
      case WEST:
         return AGE_TO_WEST_SHAPE[i];
      case EAST:
         return AGE_TO_EAST_SHAPE[i];
      }
   }

   @Nullable
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      BlockState blockState = this.getDefaultState();
      WorldView worldView = ctx.getWorld();
      BlockPos blockPos = ctx.getBlockPos();
      Direction[] var5 = ctx.getPlacementDirections();
      int var6 = var5.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         Direction direction = var5[var7];
         if (direction.getAxis().isHorizontal()) {
            blockState = (BlockState)blockState.with(FACING, direction);
            if (blockState.canPlaceAt(worldView, blockPos)) {
               return blockState;
            }
         }
      }

      return null;
   }

   public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
      return direction == state.get(FACING) && !state.canPlaceAt(world, pos) ? Blocks.AIR.getDefaultState() : super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
   }

   public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
      return (Integer)state.get(AGE) < 2;
   }

   public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
      return true;
   }

   public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
      world.setBlockState(pos, (BlockState)state.with(AGE, (Integer)state.get(AGE) + 1), Block.NOTIFY_LISTENERS);
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(FACING, AGE);
   }

   public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
      return false;
   }

   static {
      AGE = Properties.AGE_2;
      AGE_TO_EAST_SHAPE = new VoxelShape[]{Block.createCuboidShape(11.0D, 7.0D, 6.0D, 15.0D, 12.0D, 10.0D), Block.createCuboidShape(9.0D, 5.0D, 5.0D, 15.0D, 12.0D, 11.0D), Block.createCuboidShape(7.0D, 3.0D, 4.0D, 15.0D, 12.0D, 12.0D)};
      AGE_TO_WEST_SHAPE = new VoxelShape[]{Block.createCuboidShape(1.0D, 7.0D, 6.0D, 5.0D, 12.0D, 10.0D), Block.createCuboidShape(1.0D, 5.0D, 5.0D, 7.0D, 12.0D, 11.0D), Block.createCuboidShape(1.0D, 3.0D, 4.0D, 9.0D, 12.0D, 12.0D)};
      AGE_TO_NORTH_SHAPE = new VoxelShape[]{Block.createCuboidShape(6.0D, 7.0D, 1.0D, 10.0D, 12.0D, 5.0D), Block.createCuboidShape(5.0D, 5.0D, 1.0D, 11.0D, 12.0D, 7.0D), Block.createCuboidShape(4.0D, 3.0D, 1.0D, 12.0D, 12.0D, 9.0D)};
      AGE_TO_SOUTH_SHAPE = new VoxelShape[]{Block.createCuboidShape(6.0D, 7.0D, 11.0D, 10.0D, 12.0D, 15.0D), Block.createCuboidShape(5.0D, 5.0D, 9.0D, 11.0D, 12.0D, 15.0D), Block.createCuboidShape(4.0D, 3.0D, 7.0D, 12.0D, 12.0D, 15.0D)};
   }
}
