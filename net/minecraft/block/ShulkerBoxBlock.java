package net.minecraft.block;

import java.util.Iterator;
import java.util.List;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PiglinBrain;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.stat.Stats;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class ShulkerBoxBlock extends BlockWithEntity {
   public static final EnumProperty<Direction> FACING;
   public static final Identifier CONTENTS;
   @Nullable
   private final DyeColor color;

   public ShulkerBoxBlock(@Nullable DyeColor color, AbstractBlock.Settings settings) {
      super(settings);
      this.color = color;
      this.setDefaultState((BlockState)((BlockState)this.stateManager.getDefaultState()).with(FACING, Direction.UP));
   }

   public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
      return new ShulkerBoxBlockEntity(this.color, pos, state);
   }

   @Nullable
   public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
      return checkType(type, BlockEntityType.SHULKER_BOX, ShulkerBoxBlockEntity::tick);
   }

   public BlockRenderType getRenderType(BlockState state) {
      return BlockRenderType.ENTITYBLOCK_ANIMATED;
   }

   public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
      if (world.isClient) {
         return ActionResult.SUCCESS;
      } else if (player.isSpectator()) {
         return ActionResult.CONSUME;
      } else {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (blockEntity instanceof ShulkerBoxBlockEntity) {
            ShulkerBoxBlockEntity shulkerBoxBlockEntity = (ShulkerBoxBlockEntity)blockEntity;
            if (canOpen(state, world, pos, shulkerBoxBlockEntity)) {
               player.openHandledScreen(shulkerBoxBlockEntity);
               player.incrementStat(Stats.OPEN_SHULKER_BOX);
               PiglinBrain.onGuardedBlockInteracted(player, true);
            }

            return ActionResult.CONSUME;
         } else {
            return ActionResult.PASS;
         }
      }
   }

   private static boolean canOpen(BlockState state, World world, BlockPos pos, ShulkerBoxBlockEntity entity) {
      if (entity.getAnimationStage() != ShulkerBoxBlockEntity.AnimationStage.CLOSED) {
         return true;
      } else {
         Box box = ShulkerEntity.method_33347((Direction)state.get(FACING), 0.0F, 0.5F).offset(pos).contract(1.0E-6D);
         return world.isSpaceEmpty(box);
      }
   }

   public BlockState getPlacementState(ItemPlacementContext ctx) {
      return (BlockState)this.getDefaultState().with(FACING, ctx.getSide());
   }

   protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
      builder.add(FACING);
   }

   public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      if (blockEntity instanceof ShulkerBoxBlockEntity) {
         ShulkerBoxBlockEntity shulkerBoxBlockEntity = (ShulkerBoxBlockEntity)blockEntity;
         if (!world.isClient && player.isCreative() && !shulkerBoxBlockEntity.isEmpty()) {
            ItemStack itemStack = getItemStack(this.getColor());
            NbtCompound nbtCompound = shulkerBoxBlockEntity.writeInventoryNbt(new NbtCompound());
            if (!nbtCompound.isEmpty()) {
               itemStack.putSubTag("BlockEntityTag", nbtCompound);
            }

            if (shulkerBoxBlockEntity.hasCustomName()) {
               itemStack.setCustomName(shulkerBoxBlockEntity.getCustomName());
            }

            ItemEntity itemEntity = new ItemEntity(world, (double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, itemStack);
            itemEntity.setToDefaultPickupDelay();
            world.spawnEntity(itemEntity);
         } else {
            shulkerBoxBlockEntity.checkLootInteraction(player);
         }
      }

      super.onBreak(world, pos, state, player);
   }

   public List<ItemStack> getDroppedStacks(BlockState state, LootContext.Builder builder) {
      BlockEntity blockEntity = (BlockEntity)builder.getNullable(LootContextParameters.BLOCK_ENTITY);
      if (blockEntity instanceof ShulkerBoxBlockEntity) {
         ShulkerBoxBlockEntity shulkerBoxBlockEntity = (ShulkerBoxBlockEntity)blockEntity;
         builder = builder.putDrop(CONTENTS, (lootContext, consumer) -> {
            for(int i = 0; i < shulkerBoxBlockEntity.size(); ++i) {
               consumer.accept(shulkerBoxBlockEntity.getStack(i));
            }

         });
      }

      return super.getDroppedStacks(state, builder);
   }

   public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack itemStack) {
      if (itemStack.hasCustomName()) {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (blockEntity instanceof ShulkerBoxBlockEntity) {
            ((ShulkerBoxBlockEntity)blockEntity).setCustomName(itemStack.getName());
         }
      }

   }

   public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
      if (!state.isOf(newState.getBlock())) {
         BlockEntity blockEntity = world.getBlockEntity(pos);
         if (blockEntity instanceof ShulkerBoxBlockEntity) {
            world.updateComparators(pos, state.getBlock());
         }

         super.onStateReplaced(state, world, pos, newState, moved);
      }
   }

   public void appendTooltip(ItemStack stack, @Nullable BlockView world, List<Text> tooltip, TooltipContext options) {
      super.appendTooltip(stack, world, tooltip, options);
      NbtCompound nbtCompound = stack.getSubTag("BlockEntityTag");
      if (nbtCompound != null) {
         if (nbtCompound.contains("LootTable", 8)) {
            tooltip.add(new LiteralText("???????"));
         }

         if (nbtCompound.contains("Items", 9)) {
            DefaultedList<ItemStack> defaultedList = DefaultedList.ofSize(27, ItemStack.EMPTY);
            Inventories.readNbt(nbtCompound, defaultedList);
            int i = 0;
            int j = 0;
            Iterator var9 = defaultedList.iterator();

            while(var9.hasNext()) {
               ItemStack itemStack = (ItemStack)var9.next();
               if (!itemStack.isEmpty()) {
                  ++j;
                  if (i <= 4) {
                     ++i;
                     MutableText mutableText = itemStack.getName().shallowCopy();
                     mutableText.append(" x").append(String.valueOf(itemStack.getCount()));
                     tooltip.add(mutableText);
                  }
               }
            }

            if (j - i > 0) {
               tooltip.add((new TranslatableText("container.shulkerBox.more", new Object[]{j - i})).formatted(Formatting.ITALIC));
            }
         }
      }

   }

   public PistonBehavior getPistonBehavior(BlockState state) {
      return PistonBehavior.DESTROY;
   }

   public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
      BlockEntity blockEntity = world.getBlockEntity(pos);
      return blockEntity instanceof ShulkerBoxBlockEntity ? VoxelShapes.cuboid(((ShulkerBoxBlockEntity)blockEntity).getBoundingBox(state)) : VoxelShapes.fullCube();
   }

   public boolean hasComparatorOutput(BlockState state) {
      return true;
   }

   public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
      return ScreenHandler.calculateComparatorOutput((Inventory)world.getBlockEntity(pos));
   }

   public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
      ItemStack itemStack = super.getPickStack(world, pos, state);
      ShulkerBoxBlockEntity shulkerBoxBlockEntity = (ShulkerBoxBlockEntity)world.getBlockEntity(pos);
      NbtCompound nbtCompound = shulkerBoxBlockEntity.writeInventoryNbt(new NbtCompound());
      if (!nbtCompound.isEmpty()) {
         itemStack.putSubTag("BlockEntityTag", nbtCompound);
      }

      return itemStack;
   }

   @Nullable
   public static DyeColor getColor(Item item) {
      return getColor(Block.getBlockFromItem(item));
   }

   @Nullable
   public static DyeColor getColor(Block block) {
      return block instanceof ShulkerBoxBlock ? ((ShulkerBoxBlock)block).getColor() : null;
   }

   public static Block get(@Nullable DyeColor dyeColor) {
      if (dyeColor == null) {
         return Blocks.SHULKER_BOX;
      } else {
         switch(dyeColor) {
         case WHITE:
            return Blocks.WHITE_SHULKER_BOX;
         case ORANGE:
            return Blocks.ORANGE_SHULKER_BOX;
         case MAGENTA:
            return Blocks.MAGENTA_SHULKER_BOX;
         case LIGHT_BLUE:
            return Blocks.LIGHT_BLUE_SHULKER_BOX;
         case YELLOW:
            return Blocks.YELLOW_SHULKER_BOX;
         case LIME:
            return Blocks.LIME_SHULKER_BOX;
         case PINK:
            return Blocks.PINK_SHULKER_BOX;
         case GRAY:
            return Blocks.GRAY_SHULKER_BOX;
         case LIGHT_GRAY:
            return Blocks.LIGHT_GRAY_SHULKER_BOX;
         case CYAN:
            return Blocks.CYAN_SHULKER_BOX;
         case PURPLE:
         default:
            return Blocks.PURPLE_SHULKER_BOX;
         case BLUE:
            return Blocks.BLUE_SHULKER_BOX;
         case BROWN:
            return Blocks.BROWN_SHULKER_BOX;
         case GREEN:
            return Blocks.GREEN_SHULKER_BOX;
         case RED:
            return Blocks.RED_SHULKER_BOX;
         case BLACK:
            return Blocks.BLACK_SHULKER_BOX;
         }
      }
   }

   @Nullable
   public DyeColor getColor() {
      return this.color;
   }

   public static ItemStack getItemStack(@Nullable DyeColor color) {
      return new ItemStack(get(color));
   }

   public BlockState rotate(BlockState state, BlockRotation rotation) {
      return (BlockState)state.with(FACING, rotation.rotate((Direction)state.get(FACING)));
   }

   public BlockState mirror(BlockState state, BlockMirror mirror) {
      return state.rotate(mirror.getRotation((Direction)state.get(FACING)));
   }

   static {
      FACING = FacingBlock.FACING;
      CONTENTS = new Identifier("contents");
   }
}
