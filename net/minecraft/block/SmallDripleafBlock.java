package net.minecraft.block;

import java.util.Random;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class SmallDripleafBlock extends TallPlantBlock implements Fertilizable, Waterloggable {
   private static final BooleanProperty WATERLOGGED;
   public static final DirectionProperty FACING;
   protected static final float field_31246 = 6.0F;
   protected static final VoxelShape SHAPE;

   public SmallDripleafBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(HALF, DoubleBlockHalf.LOWER)).with(WATERLOGGED, false)).with(FACING, Direction.NORTH));
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPE;
   }

   protected boolean canPlantOnTop(BlockState floor, BlockView world, BlockPos pos) {
      return floor.isIn(BlockTags.SMALL_DRIPLEAF_PLACEABLE) || world.getFluidState(pos.up()).isEqualAndStill(Fluids.WATER) && super.canPlantOnTop(floor, world, pos);
   }

   @Nullable
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      BlockState blockState = super.getPlacementState(ctx);
      if (blockState != null) {
         FluidState fluidState = ctx.getWorld().getFluidState(ctx.getBlockPos());
         return (BlockState)((BlockState)blockState.with(WATERLOGGED, fluidState.getFluid() == Fluids.WATER)).with(FACING, ctx.getPlayerFacing().getOpposite());
      } else {
         return null;
      }
   }

   public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
      if (!world.isClient()) {
         Direction direction = (Direction)state.get(FACING);
         world.setBlockState(pos.up(), (BlockState)((BlockState)((BlockState)this.getDefaultState().with(HALF, DoubleBlockHalf.UPPER)).with(WATERLOGGED, world.isWater(pos.up()))).with(FACING, direction), Block.NOTIFY_ALL);
      }

   }

   public FluidState getFluidState(BlockState state) {
      return (Boolean)state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
   }

   public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
      if (state.get(HALF) == DoubleBlockHalf.UPPER) {
         return super.canPlaceAt(state, world, pos);
      } else {
         BlockPos blockPos = pos.down();
         BlockState blockState = world.getBlockState(blockPos);
         return this.canPlantOnTop(blockState, world, blockPos);
      }
   }

   public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
      if ((Boolean)state.get(WATERLOGGED)) {
         world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
      }

      return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(HALF, WATERLOGGED, FACING);
   }

   public boolean isFertilizable(BlockView world, BlockPos pos, BlockState state, boolean isClient) {
      return true;
   }

   public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
      return true;
   }

   public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
      BlockPos blockPos;
      if (state.get(TallPlantBlock.HALF) == DoubleBlockHalf.LOWER) {
         blockPos = pos.up();
         world.setBlockState(blockPos, world.getFluidState(blockPos).getBlockState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
         BigDripleafBlock.grow((WorldAccess)world, random, pos, (Direction)((Direction)state.get(FACING)));
      } else {
         blockPos = pos.down();
         this.grow(world, random, blockPos, world.getBlockState(blockPos));
      }

   }

   public BlockState rotate(BlockState state, BlockRotation rotation) {
      return (BlockState)state.with(FACING, rotation.rotate((Direction)state.get(FACING)));
   }

   public BlockState mirror(BlockState state, BlockMirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.get(FACING)));
   }

   public AbstractBlock.OffsetType getOffsetType() {
      return AbstractBlock.OffsetType.XYZ;
   }

   public float method_37247() {
      return 0.1F;
   }

   static {
      WATERLOGGED = Properties.WATERLOGGED;
      FACING = Properties.HORIZONTAL_FACING;
      SHAPE = Block.createCuboidShape(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);
   }
}
