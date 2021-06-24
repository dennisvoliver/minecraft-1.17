package net.minecraft.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class AbstractLichenBlock extends Block {
   private static final float field_31194 = 1.0F;
   private static final VoxelShape UP_SHAPE = Block.createCuboidShape(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
   private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
   private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
   private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
   private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
   private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
   private static final Map<Direction, BooleanProperty> FACING_PROPERTIES;
   private static final Map<Direction, VoxelShape> SHAPES_FOR_DIRECTIONS;
   protected static final Direction[] DIRECTIONS;
   private final ImmutableMap<BlockState, VoxelShape> SHAPES;
   private final boolean hasAllHorizontalDirections;
   private final boolean canMirrorX;
   private final boolean canMirrorZ;

   public AbstractLichenBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState(withAllDirections(this.stateManager));
      this.SHAPES = this.getShapesForStates(AbstractLichenBlock::getShapeForState);
      this.hasAllHorizontalDirections = Direction.Type.HORIZONTAL.stream().allMatch(this::canHaveDirection);
      this.canMirrorX = Direction.Type.HORIZONTAL.stream().filter(Direction.Axis.X).filter(this::canHaveDirection).count() % 2L == 0L;
      this.canMirrorZ = Direction.Type.HORIZONTAL.stream().filter(Direction.Axis.Z).filter(this::canHaveDirection).count() % 2L == 0L;
   }

   protected boolean canHaveDirection(Direction direction) {
      return true;
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      Direction[] var2 = DIRECTIONS;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Direction direction = var2[var4];
         if (this.canHaveDirection(direction)) {
            builder.add(getProperty(direction));
         }
      }

   }

   public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
      if (!hasAnyDirection(state)) {
         return Blocks.AIR.getDefaultState();
      } else {
         return hasDirection(state, direction) && !canGrowOn(world, direction, neighborPos, neighborState) ? disableDirection(state, getProperty(direction)) : state;
      }
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return (VoxelShape)this.SHAPES.get(state);
   }

   public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
      boolean bl = false;
      Direction[] var5 = DIRECTIONS;
      int var6 = var5.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         Direction direction = var5[var7];
         if (hasDirection(state, direction)) {
            BlockPos blockPos = pos.offset(direction);
            if (!canGrowOn(world, direction, blockPos, world.getBlockState(blockPos))) {
               return false;
            }

            bl = true;
         }
      }

      return bl;
   }

   public boolean canReplace(BlockState state, ItemPlacementContext context) {
      return isNotFullBlock(state);
   }

   @Nullable
   public BlockState getPlacementState(ItemPlacementContext ctx) {
      World world = ctx.getWorld();
      BlockPos blockPos = ctx.getBlockPos();
      BlockState blockState = world.getBlockState(blockPos);
      return (BlockState)Arrays.stream(ctx.getPlacementDirections()).map((direction) -> {
         return this.withDirection(blockState, world, blockPos, direction);
      }).filter(Objects::nonNull).findFirst().orElse((Object)null);
   }

   @Nullable
   public BlockState withDirection(BlockState state, BlockView world, BlockPos pos, Direction direction) {
      if (!this.canHaveDirection(direction)) {
         return null;
      } else {
         BlockState blockState3;
         if (state.isOf(this)) {
            if (hasDirection(state, direction)) {
               return null;
            }

            blockState3 = state;
         } else if (this.isWaterlogged() && state.getFluidState().isEqualAndStill(Fluids.WATER)) {
            blockState3 = (BlockState)this.getDefaultState().with(Properties.WATERLOGGED, true);
         } else {
            blockState3 = this.getDefaultState();
         }

         BlockPos blockPos = pos.offset(direction);
         return canGrowOn(world, direction, blockPos, world.getBlockState(blockPos)) ? (BlockState)blockState3.with(getProperty(direction), true) : null;
      }
   }

   public BlockState rotate(BlockState state, BlockRotation rotation) {
      if (!this.hasAllHorizontalDirections) {
         return state;
      } else {
         Objects.requireNonNull(rotation);
         return this.mirror(state, rotation::rotate);
      }
   }

   public BlockState mirror(BlockState state, BlockMirror mirror) {
      if (mirror == BlockMirror.FRONT_BACK && !this.canMirrorX) {
         return state;
      } else if (mirror == BlockMirror.LEFT_RIGHT && !this.canMirrorZ) {
         return state;
      } else {
         Objects.requireNonNull(mirror);
         return this.mirror(state, mirror::apply);
      }
   }

   private BlockState mirror(BlockState state, Function<Direction, Direction> mirror) {
      BlockState blockState = state;
      Direction[] var4 = DIRECTIONS;
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         Direction direction = var4[var6];
         if (this.canHaveDirection(direction)) {
            blockState = (BlockState)blockState.with(getProperty((Direction)mirror.apply(direction)), (Boolean)state.get(getProperty(direction)));
         }
      }

      return blockState;
   }

   public boolean trySpreadRandomly(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      List<Direction> list = Lists.newArrayList((Object[])DIRECTIONS);
      Collections.shuffle(list);
      return list.stream().filter((from) -> {
         return hasDirection(state, from);
      }).anyMatch((to) -> {
         return this.trySpreadRandomly(state, world, pos, to, random, false);
      });
   }

   public boolean trySpreadRandomly(BlockState state, WorldAccess world, BlockPos pos, Direction from, Random random, boolean postProcess) {
      List<Direction> list = Arrays.asList(DIRECTIONS);
      Collections.shuffle(list, random);
      return list.stream().anyMatch((direction2) -> {
         return this.trySpreadTo(state, world, pos, from, direction2, postProcess);
      });
   }

   public boolean trySpreadTo(BlockState state, WorldAccess world, BlockPos pos, Direction from, Direction to, boolean postProcess) {
      Optional<Pair<BlockPos, Direction>> optional = this.getSpreadLocation(state, world, pos, from, to);
      if (optional.isPresent()) {
         Pair<BlockPos, Direction> pair = (Pair)optional.get();
         return this.addDirection(world, (BlockPos)pair.getFirst(), (Direction)pair.getSecond(), postProcess);
      } else {
         return false;
      }
   }

   protected boolean canSpread(BlockState state, BlockView world, BlockPos pos, Direction from) {
      return Stream.of(DIRECTIONS).anyMatch((to) -> {
         return this.getSpreadLocation(state, world, pos, from, to).isPresent();
      });
   }

   private Optional<Pair<BlockPos, Direction>> getSpreadLocation(BlockState state, BlockView world, BlockPos pos, Direction from, Direction to) {
      if (to.getAxis() != from.getAxis() && hasDirection(state, from) && !hasDirection(state, to)) {
         if (this.canSpreadTo(world, pos, to)) {
            return Optional.of(Pair.of(pos, to));
         } else {
            BlockPos blockPos = pos.offset(to);
            if (this.canSpreadTo(world, blockPos, from)) {
               return Optional.of(Pair.of(blockPos, from));
            } else {
               BlockPos blockPos2 = blockPos.offset(from);
               Direction direction = to.getOpposite();
               return this.canSpreadTo(world, blockPos2, direction) ? Optional.of(Pair.of(blockPos2, direction)) : Optional.empty();
            }
         }
      } else {
         return Optional.empty();
      }
   }

   private boolean canSpreadTo(BlockView world, BlockPos pos, Direction direction) {
      BlockState blockState = world.getBlockState(pos);
      if (!this.canGrowIn(blockState)) {
         return false;
      } else {
         BlockState blockState2 = this.withDirection(blockState, world, pos, direction);
         return blockState2 != null;
      }
   }

   private boolean addDirection(WorldAccess world, BlockPos pos, Direction direction, boolean postProcess) {
      BlockState blockState = world.getBlockState(pos);
      BlockState blockState2 = this.withDirection(blockState, world, pos, direction);
      if (blockState2 != null) {
         if (postProcess) {
            world.getChunk(pos).markBlockForPostProcessing(pos);
         }

         return world.setBlockState(pos, blockState2, Block.NOTIFY_LISTENERS);
      } else {
         return false;
      }
   }

   private boolean canGrowIn(BlockState state) {
      return state.isAir() || state.isOf(this) || state.isOf(Blocks.WATER) && state.getFluidState().isStill();
   }

   private static boolean hasDirection(BlockState state, Direction direction) {
      BooleanProperty booleanProperty = getProperty(direction);
      return state.contains(booleanProperty) && (Boolean)state.get(booleanProperty);
   }

   private static boolean canGrowOn(BlockView world, Direction direction, BlockPos pos, BlockState state) {
      return Block.isFaceFullSquare(state.getCollisionShape(world, pos), direction.getOpposite());
   }

   private boolean isWaterlogged() {
      return this.stateManager.getProperties().contains(Properties.WATERLOGGED);
   }

   private static BlockState disableDirection(BlockState state, BooleanProperty direction) {
      BlockState blockState = (BlockState)state.with(direction, false);
      return hasAnyDirection(blockState) ? blockState : Blocks.AIR.getDefaultState();
   }

   public static BooleanProperty getProperty(Direction direction) {
      return (BooleanProperty)FACING_PROPERTIES.get(direction);
   }

   private static BlockState withAllDirections(StateManager<Block, BlockState> stateManager) {
      BlockState blockState = (BlockState)stateManager.getDefaultState();
      Iterator var2 = FACING_PROPERTIES.values().iterator();

      while(var2.hasNext()) {
         BooleanProperty booleanProperty = (BooleanProperty)var2.next();
         if (blockState.contains(booleanProperty)) {
            blockState = (BlockState)blockState.with(booleanProperty, false);
         }
      }

      return blockState;
   }

   private static VoxelShape getShapeForState(BlockState state) {
      VoxelShape voxelShape = VoxelShapes.empty();
      Direction[] var2 = DIRECTIONS;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         Direction direction = var2[var4];
         if (hasDirection(state, direction)) {
            voxelShape = VoxelShapes.union(voxelShape, (VoxelShape)SHAPES_FOR_DIRECTIONS.get(direction));
         }
      }

      return voxelShape.isEmpty() ? VoxelShapes.fullCube() : voxelShape;
   }

   protected static boolean hasAnyDirection(BlockState state) {
      return Arrays.stream(DIRECTIONS).anyMatch((direction) -> {
         return hasDirection(state, direction);
      });
   }

   private static boolean isNotFullBlock(BlockState state) {
      return Arrays.stream(DIRECTIONS).anyMatch((direction) -> {
         return !hasDirection(state, direction);
      });
   }

   static {
      FACING_PROPERTIES = ConnectingBlock.FACING_PROPERTIES;
      SHAPES_FOR_DIRECTIONS = (Map)Util.make(Maps.newEnumMap(Direction.class), (shapes) -> {
         shapes.put(Direction.NORTH, SOUTH_SHAPE);
         shapes.put(Direction.EAST, WEST_SHAPE);
         shapes.put(Direction.SOUTH, NORTH_SHAPE);
         shapes.put(Direction.WEST, EAST_SHAPE);
         shapes.put(Direction.UP, UP_SHAPE);
         shapes.put(Direction.DOWN, DOWN_SHAPE);
      });
      DIRECTIONS = Direction.values();
   }
}
