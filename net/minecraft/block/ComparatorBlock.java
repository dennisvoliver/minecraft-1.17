package net.minecraft.block;

import java.util.List;
import java.util.Random;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ComparatorBlockEntity;
import net.minecraft.block.enums.ComparatorMode;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.TickPriority;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ComparatorBlock extends AbstractRedstoneGateBlock implements BlockEntityProvider {
   public static final EnumProperty<ComparatorMode> MODE;

   public ComparatorBlock(AbstractBlock.Settings settings) {
      super(settings);
      this.setDefaultState((BlockState)((BlockState)((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.NORTH)).with(POWERED, false)).with(MODE, ComparatorMode.COMPARE));
   }

   protected int getUpdateDelayInternal(BlockState state) {
      return 2;
   }

   protected int getOutputLevel(BlockView world, BlockPos pos, BlockState state) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      return blockEntity instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)blockEntity).getOutputSignal() : 0;
   }

   private int calculateOutputSignal(World world, BlockPos pos, BlockState state) {
      int i = this.getPower(world, pos, state);
      if (i == 0) {
         return 0;
      } else {
         int j = this.getMaxInputLevelSides(world, pos, state);
         if (j > i) {
            return 0;
         } else {
            return state.get(MODE) == ComparatorMode.SUBTRACT ? i - j : i;
         }
      }
   }

   protected boolean hasPower(World world, BlockPos pos, BlockState state) {
      int i = this.getPower(world, pos, state);
      if (i == 0) {
         return false;
      } else {
         int j = this.getMaxInputLevelSides(world, pos, state);
         if (i > j) {
            return true;
         } else {
            return i == j && state.get(MODE) == ComparatorMode.COMPARE;
         }
      }
   }

   protected int getPower(World world, BlockPos pos, BlockState state) {
      int i = super.getPower(world, pos, state);
      Direction direction = (Direction)state.get(FACING);
      BlockPos blockPos = pos.offset(direction);
      BlockState blockState = world.getBlockState(blockPos);
      if (blockState.hasComparatorOutput()) {
         i = blockState.getComparatorOutput(world, blockPos);
      } else if (i < 15 && blockState.isSolidBlock(world, blockPos)) {
         blockPos = blockPos.offset(direction);
         blockState = world.getBlockState(blockPos);
         ItemFrameEntity itemFrameEntity = this.getAttachedItemFrame(world, direction, blockPos);
         int j = Math.max(itemFrameEntity == null ? Integer.MIN_VALUE : itemFrameEntity.getComparatorPower(), blockState.hasComparatorOutput() ? blockState.getComparatorOutput(world, blockPos) : Integer.MIN_VALUE);
         if (j != Integer.MIN_VALUE) {
            i = j;
         }
      }

      return i;
   }

   @Nullable
   private ItemFrameEntity getAttachedItemFrame(World world, Direction facing, BlockPos pos) {
      List<ItemFrameEntity> list = world.getEntitiesByClass(ItemFrameEntity.class, new Box((double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), (double)(pos.getX() + 1), (double)(pos.getY() + 1), (double)(pos.getZ() + 1)), (itemFrameEntity) -> {
         return itemFrameEntity != null && itemFrameEntity.getHorizontalFacing() == facing;
      });
      return list.size() == 1 ? (ItemFrameEntity)list.get(0) : null;
   }

   public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      if (!player.getAbilities().allowModifyWorld) {
         return ActionResult.PASS;
      } else {
         state = (BlockState)state.cycle(MODE);
         float f = state.get(MODE) == ComparatorMode.SUBTRACT ? 0.55F : 0.5F;
         world.playSound(player, pos, SoundEvents.BLOCK_COMPARATOR_CLICK, SoundCategory.BLOCKS, 0.3F, f);
         world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
         this.update(world, pos, state);
         return ActionResult.success(world.isClient);
      }
   }

   protected void updatePowered(World world, BlockPos pos, BlockState state) {
      if (!world.getBlockTickScheduler().isTicking(pos, this)) {
         int i = this.calculateOutputSignal(world, pos, state);
         BlockEntity blockEntity = world.getBlockEntity(pos);
         int j = blockEntity instanceof ComparatorBlockEntity ? ((ComparatorBlockEntity)blockEntity).getOutputSignal() : 0;
         if (i != j || (Boolean)state.get(POWERED) != this.hasPower(world, pos, state)) {
            TickPriority tickPriority = this.isTargetNotAligned(world, pos, state) ? TickPriority.HIGH : TickPriority.NORMAL;
            world.getBlockTickScheduler().schedule(pos, this, 2, tickPriority);
         }

      }
   }

   private void update(World world, BlockPos pos, BlockState state) {
      int i = this.calculateOutputSignal(world, pos, state);
      BlockEntity blockEntity = world.getBlockEntity(pos);
      int j = 0;
      if (blockEntity instanceof ComparatorBlockEntity) {
         ComparatorBlockEntity comparatorBlockEntity = (ComparatorBlockEntity)blockEntity;
         j = comparatorBlockEntity.getOutputSignal();
         comparatorBlockEntity.setOutputSignal(i);
      }

      if (j != i || state.get(MODE) == ComparatorMode.COMPARE) {
         boolean bl = this.hasPower(world, pos, state);
         boolean bl2 = (Boolean)state.get(POWERED);
         if (bl2 && !bl) {
            world.setBlockState(pos, (BlockState)state.with(POWERED, false), Block.NOTIFY_LISTENERS);
         } else if (!bl2 && bl) {
            world.setBlockState(pos, (BlockState)state.with(POWERED, true), Block.NOTIFY_LISTENERS);
         }

         this.updateTarget(world, pos, state);
      }

   }

   public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
      this.update(world, pos, state);
   }

   public boolean onSyncedBlockEvent(BlockState state, World world, BlockPos pos, int type, int data) {
      super.onSyncedBlockEvent(state, world, pos, type, data);
      BlockEntity blockEntity = world.getBlockEntity(pos);
      return blockEntity != null && blockEntity.onSyncedBlockEvent(type, data);
   }

   public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
      return new ComparatorBlockEntity(pos, state);
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(FACING, MODE, POWERED);
   }

   static {
      MODE = Properties.COMPARATOR_MODE;
   }
}
