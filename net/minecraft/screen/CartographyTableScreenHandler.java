package net.minecraft.screen;

import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

public class CartographyTableScreenHandler extends ScreenHandler {
   public static final int MAP_SLOT_INDEX = 0;
   public static final int MATERIAL_SLOT_INDEX = 1;
   public static final int RESULT_SLOT_INDEX = 2;
   private static final int field_30776 = 3;
   private static final int field_30777 = 30;
   private static final int field_30778 = 30;
   private static final int field_30779 = 39;
   private final ScreenHandlerContext context;
   long lastTakeResultTime;
   public final Inventory inventory;
   private final CraftingResultInventory resultInventory;

   public CartographyTableScreenHandler(int syncId, PlayerInventory inventory) {
      this(syncId, inventory, ScreenHandlerContext.EMPTY);
   }

   public CartographyTableScreenHandler(int syncId, PlayerInventory inventory, final ScreenHandlerContext context) {
      super(ScreenHandlerType.CARTOGRAPHY_TABLE, syncId);
      this.inventory = new SimpleInventory(2) {
         public void markDirty() {
            CartographyTableScreenHandler.this.onContentChanged(this);
            super.markDirty();
         }
      };
      this.resultInventory = new CraftingResultInventory() {
         public void markDirty() {
            CartographyTableScreenHandler.this.onContentChanged(this);
            super.markDirty();
         }
      };
      this.context = context;
      this.addSlot(new Slot(this.inventory, 0, 15, 15) {
         public boolean canInsert(ItemStack stack) {
            return stack.isOf(Items.FILLED_MAP);
         }
      });
      this.addSlot(new Slot(this.inventory, 1, 15, 52) {
         public boolean canInsert(ItemStack stack) {
            return stack.isOf(Items.PAPER) || stack.isOf(Items.MAP) || stack.isOf(Items.GLASS_PANE);
         }
      });
      this.addSlot(new Slot(this.resultInventory, 2, 145, 39) {
         public boolean canInsert(ItemStack stack) {
            return false;
         }

         public void onTakeItem(PlayerEntity player, ItemStack stack) {
            ((Slot)CartographyTableScreenHandler.this.slots.get(0)).takeStack(1);
            ((Slot)CartographyTableScreenHandler.this.slots.get(1)).takeStack(1);
            stack.getItem().onCraft(stack, player.world, player);
            context.run((world, blockPos) -> {
               long l = world.getTime();
               if (CartographyTableScreenHandler.this.lastTakeResultTime != l) {
                  world.playSound((PlayerEntity)null, blockPos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundCategory.BLOCKS, 1.0F, 1.0F);
                  CartographyTableScreenHandler.this.lastTakeResultTime = l;
               }

            });
            super.onTakeItem(player, stack);
         }
      });

      int k;
      for(k = 0; k < 3; ++k) {
         for(int j = 0; j < 9; ++j) {
            this.addSlot(new Slot(inventory, j + k * 9 + 9, 8 + j * 18, 84 + k * 18));
         }
      }

      for(k = 0; k < 9; ++k) {
         this.addSlot(new Slot(inventory, k, 8 + k * 18, 142));
      }

   }

   public boolean canUse(PlayerEntity player) {
      return canUse(this.context, player, Blocks.CARTOGRAPHY_TABLE);
   }

   public void onContentChanged(Inventory inventory) {
      ItemStack itemStack = this.inventory.getStack(0);
      ItemStack itemStack2 = this.inventory.getStack(1);
      ItemStack itemStack3 = this.resultInventory.getStack(2);
      if (itemStack3.isEmpty() || !itemStack.isEmpty() && !itemStack2.isEmpty()) {
         if (!itemStack.isEmpty() && !itemStack2.isEmpty()) {
            this.updateResult(itemStack, itemStack2, itemStack3);
         }
      } else {
         this.resultInventory.removeStack(2);
      }

   }

   private void updateResult(ItemStack map, ItemStack item, ItemStack oldResult) {
      this.context.run((world, blockPos) -> {
         MapState mapState = FilledMapItem.getOrCreateMapState(map, world);
         if (mapState != null) {
            ItemStack itemStack6;
            if (item.isOf(Items.PAPER) && !mapState.locked && mapState.scale < 4) {
               itemStack6 = map.copy();
               itemStack6.setCount(1);
               itemStack6.getOrCreateTag().putInt("map_scale_direction", 1);
               this.sendContentUpdates();
            } else if (item.isOf(Items.GLASS_PANE) && !mapState.locked) {
               itemStack6 = map.copy();
               itemStack6.setCount(1);
               itemStack6.getOrCreateTag().putBoolean("map_to_lock", true);
               this.sendContentUpdates();
            } else {
               if (!item.isOf(Items.MAP)) {
                  this.resultInventory.removeStack(2);
                  this.sendContentUpdates();
                  return;
               }

               itemStack6 = map.copy();
               itemStack6.setCount(2);
               this.sendContentUpdates();
            }

            if (!ItemStack.areEqual(itemStack6, oldResult)) {
               this.resultInventory.setStack(2, itemStack6);
               this.sendContentUpdates();
            }

         }
      });
   }

   public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
      return slot.inventory != this.resultInventory && super.canInsertIntoSlot(stack, slot);
   }

   public ItemStack transferSlot(PlayerEntity player, int index) {
      ItemStack itemStack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasStack()) {
         ItemStack itemStack2 = slot.getStack();
         itemStack = itemStack2.copy();
         if (index == 2) {
            itemStack2.getItem().onCraft(itemStack2, player.world, player);
            if (!this.insertItem(itemStack2, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickTransfer(itemStack2, itemStack);
         } else if (index != 1 && index != 0) {
            if (itemStack2.isOf(Items.FILLED_MAP)) {
               if (!this.insertItem(itemStack2, 0, 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!itemStack2.isOf(Items.PAPER) && !itemStack2.isOf(Items.MAP) && !itemStack2.isOf(Items.GLASS_PANE)) {
               if (index >= 3 && index < 30) {
                  if (!this.insertItem(itemStack2, 30, 39, false)) {
                     return ItemStack.EMPTY;
                  }
               } else if (index >= 30 && index < 39 && !this.insertItem(itemStack2, 3, 30, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (!this.insertItem(itemStack2, 1, 2, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.insertItem(itemStack2, 3, 39, false)) {
            return ItemStack.EMPTY;
         }

         if (itemStack2.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
         }

         slot.markDirty();
         if (itemStack2.getCount() == itemStack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTakeItem(player, itemStack2);
         this.sendContentUpdates();
      }

      return itemStack;
   }

   public void close(PlayerEntity playerEntity) {
      super.close(playerEntity);
      this.resultInventory.removeStack(2);
      this.context.run((world, blockPos) -> {
         this.dropInventory(playerEntity, this.inventory);
      });
   }
}
