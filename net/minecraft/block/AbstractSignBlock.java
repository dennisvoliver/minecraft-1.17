package net.minecraft.block;

import net.minecraft.advancement.criterion.Criteria;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.DyeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.stat.Stats;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.SignType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;

public abstract class AbstractSignBlock extends BlockWithEntity implements Waterloggable {
   public static final BooleanProperty WATERLOGGED;
   protected static final float field_31243 = 4.0F;
   protected static final VoxelShape SHAPE;
   private final SignType type;

   protected AbstractSignBlock(AbstractBlock.Settings settings, SignType type) {
      super(settings);
      this.type = type;
   }

   public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
      if ((Boolean)state.get(WATERLOGGED)) {
         world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
      }

      return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      return SHAPE;
   }

   public boolean canMobSpawnInside() {
      return true;
   }

   public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
      return new SignBlockEntity(pos, state);
   }

   public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      ItemStack itemStack = player.getStackInHand(hand);
      Item item = itemStack.getItem();
      boolean bl = item instanceof DyeItem;
      boolean bl2 = itemStack.isOf(Items.GLOW_INK_SAC);
      boolean bl3 = itemStack.isOf(Items.INK_SAC);
      boolean bl4 = (bl2 || bl || bl3) && player.getAbilities().allowModifyWorld;
      if (world.isClient) {
         return bl4 ? ActionResult.SUCCESS : ActionResult.CONSUME;
      } else {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (!(blockEntity instanceof SignBlockEntity)) {
            return ActionResult.PASS;
         } else {
            SignBlockEntity signBlockEntity = (SignBlockEntity)blockEntity;
            boolean bl5 = signBlockEntity.isGlowingText();
            if ((!bl2 || !bl5) && (!bl3 || bl5)) {
               if (bl4) {
                  boolean bl8;
                  if (bl2) {
                     world.playSound((PlayerEntity)null, pos, SoundEvents.ITEM_GLOW_INK_SAC_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                     bl8 = signBlockEntity.setGlowingText(true);
                     if (player instanceof ServerPlayerEntity) {
                        Criteria.ITEM_USED_ON_BLOCK.test((ServerPlayerEntity)player, pos, itemStack);
                     }
                  } else if (bl3) {
                     world.playSound((PlayerEntity)null, pos, SoundEvents.ITEM_INK_SAC_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                     bl8 = signBlockEntity.setGlowingText(false);
                  } else {
                     world.playSound((PlayerEntity)null, pos, SoundEvents.ITEM_DYE_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);
                     bl8 = signBlockEntity.setTextColor(((DyeItem)item).getColor());
                  }

                  if (bl8) {
                     if (!player.isCreative()) {
                        itemStack.decrement(1);
                     }

                     player.incrementStat(Stats.USED.getOrCreateStat(item));
                  }
               }

               return signBlockEntity.onActivate((ServerPlayerEntity)player) ? ActionResult.SUCCESS : ActionResult.PASS;
            } else {
               return ActionResult.PASS;
            }
         }
      }
   }

   public FluidState getFluidState(BlockState state) {
      return (Boolean)state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
   }

   public SignType getSignType() {
      return this.type;
   }

   static {
      WATERLOGGED = Properties.WATERLOGGED;
      SHAPE = Block.createCuboidShape(4.0D, 0.0D, 4.0D, 12.0D, 16.0D, 12.0D);
   }
}
