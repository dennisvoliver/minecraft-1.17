package net.minecraft.block;

import java.util.Collections;
import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.PistonBlockEntity;
import net.minecraft.block.enums.PistonType;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public class PistonExtensionBlock extends BlockWithEntity {
   public static final DirectionProperty FACING;
   public static final EnumProperty<PistonType> TYPE;

   public PistonExtensionBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.NORTH)).with(TYPE, PistonType.DEFAULT));
   }

   @Nullable
   public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
      return null;
   }

   public static BlockEntity createBlockEntityPiston(BlockPos pos, BlockState state, BlockState pushedBlock, Direction facing, boolean extending, boolean source) {
      return new PistonBlockEntity(pos, state, pushedBlock, facing, extending, source);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
      return checkType(type, BlockEntityType.PISTON, PistonBlockEntity::tick);
   }

   public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.isOf(newState.getBlock())) {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (blockEntity instanceof PistonBlockEntity) {
            ((PistonBlockEntity)blockEntity).finish();
         }

      }
   }

   public void onBroken(WorldAccess world, BlockPos pos, BlockState state) {
      BlockPos blockPos = pos.offset(((Direction)state.get(FACING)).getOpposite());
      BlockState blockState = world.getBlockState(blockPos);
      if (blockState.getBlock() instanceof PistonBlock && (Boolean)blockState.get(PistonBlock.EXTENDED)) {
         world.removeBlock(blockPos, false);
      }

   }

   public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      if (!world.isClient && world.getBlockEntity(pos) == null) {
         world.removeBlock(pos, false);
         return ActionResult.CONSUME;
      } else {
         return ActionResult.PASS;
      }
   }

   public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
      PistonBlockEntity pistonBlockEntity = this.getPistonBlockEntity(builder.getWorld(), new BlockPos((Vec3d)builder.get(LootContextParameters.ORIGIN)));
      return pistonBlockEntity == null ? Collections.emptyList() : pistonBlockEntity.getPushedBlock().getDroppedStacks(builder);
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return VoxelShapes.empty();
   }

   public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      PistonBlockEntity pistonBlockEntity = this.getPistonBlockEntity(world, pos);
      return pistonBlockEntity != null ? pistonBlockEntity.getCollisionShape(world, pos) : VoxelShapes.empty();
   }

   @Nullable
   private PistonBlockEntity getPistonBlockEntity(BlockView world, BlockPos pos) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      return blockEntity instanceof PistonBlockEntity ? (PistonBlockEntity)blockEntity : null;
   }

   public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
      return ItemStack.EMPTY;
   }

   public BlockState rotate(BlockState state, BlockRotation rotation) {
      return (BlockState)state.with(FACING, rotation.rotate((Direction)state.get(FACING)));
   }

   public BlockState mirror(BlockState state, BlockMirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.get(FACING)));
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(FACING, TYPE);
   }

   public boolean canPathfindThrough(BlockState state, BlockView world, BlockPos pos, NavigationType type) {
      return false;
   }

   static {
      FACING = PistonHeadBlock.FACING;
      TYPE = PistonHeadBlock.TYPE;
   }
}
