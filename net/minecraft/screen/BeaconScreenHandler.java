package net.minecraft.screen;

import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.tag.ItemTags;
import org.jetbrains.annotations.Nullable;

public class BeaconScreenHandler extends ScreenHandler {
   private static final int field_30756 = 0;
   private static final int field_30757 = 1;
   private static final int field_30758 = 3;
   private static final int field_30759 = 1;
   private static final int field_30760 = 28;
   private static final int field_30761 = 28;
   private static final int field_30762 = 37;
   private final Inventory payment;
   private final BeaconScreenHandler.PaymentSlot paymentSlot;
   private final ScreenHandlerContext context;
   private final PropertyDelegate propertyDelegate;

   public BeaconScreenHandler(int syncId, Inventory inventory) {
      this(syncId, inventory, new ArrayPropertyDelegate(3), ScreenHandlerContext.EMPTY);
   }

   public BeaconScreenHandler(int syncId, Inventory inventory, PropertyDelegate propertyDelegate, ScreenHandlerContext context) {
      super(ScreenHandlerType.BEACON, syncId);
      this.payment = new SimpleInventory(1) {
         public boolean isValid(int slot, ItemStack stack) {
            return stack.isIn(ItemTags.BEACON_PAYMENT_ITEMS);
         }

         public int getMaxCountPerStack() {
            return 1;
         }
      };
      checkDataCount(propertyDelegate, 3);
      this.propertyDelegate = propertyDelegate;
      this.context = context;
      this.paymentSlot = new BeaconScreenHandler.PaymentSlot(this.payment, 0, 136, 110);
      this.addSlot(this.paymentSlot);
      this.addProperties(propertyDelegate);
      int i = true;
      int j = true;

      int m;
      for(m = 0; m < 3; ++m) {
         for(int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(inventory, l + m * 9 + 9, 36 + l * 18, 137 + m * 18));
         }
      }

      for(m = 0; m < 9; ++m) {
         this.addSlot(new Slot(inventory, m, 36 + m * 18, 195));
      }

   }

   public void close(PlayerEntity playerEntity) {
      super.close(playerEntity);
      if (!playerEntity.world.isClient) {
         ItemStack itemStack = this.paymentSlot.takeStack(this.paymentSlot.getMaxItemCount());
         if (!itemStack.isEmpty()) {
            playerEntity.dropItem(itemStack, false);
         }

      }
   }

   public boolean canUse(PlayerEntity player) {
      return canUse(this.context, player, Blocks.BEACON);
   }

   public void setProperty(int id, int value) {
      super.setProperty(id, value);
      this.sendContentUpdates();
   }

   public ItemStack transferSlot(PlayerEntity player, int index) {
      ItemStack itemStack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasStack()) {
         ItemStack itemStack2 = slot.getStack();
         itemStack = itemStack2.copy();
         if (index == 0) {
            if (!this.insertItem(itemStack2, 1, 37, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickTransfer(itemStack2, itemStack);
         } else if (!this.paymentSlot.hasStack() && this.paymentSlot.canInsert(itemStack2) && itemStack2.getCount() == 1) {
            if (!this.insertItem(itemStack2, 0, 1, false)) {
               return ItemStack.EMPTY;
            }
         } else if (index >= 1 && index < 28) {
            if (!this.insertItem(itemStack2, 28, 37, false)) {
               return ItemStack.EMPTY;
            }
         } else if (index >= 28 && index < 37) {
            if (!this.insertItem(itemStack2, 1, 28, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.insertItem(itemStack2, 1, 37, false)) {
            return ItemStack.EMPTY;
         }

         if (itemStack2.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
         } else {
            slot.markDirty();
         }

         if (itemStack2.getCount() == itemStack.getCount()) {
            return ItemStack.EMPTY;
         }

         slot.onTakeItem(player, itemStack2);
      }

      return itemStack;
   }

   public int getProperties() {
      return this.propertyDelegate.get(0);
   }

   @Nullable
   public StatusEffect getPrimaryEffect() {
      return StatusEffect.byRawId(this.propertyDelegate.get(1));
   }

   @Nullable
   public StatusEffect getSecondaryEffect() {
      return StatusEffect.byRawId(this.propertyDelegate.get(2));
   }

   public void setEffects(int primaryEffectId, int secondaryEffectId) {
      if (this.paymentSlot.hasStack()) {
         this.propertyDelegate.set(1, primaryEffectId);
         this.propertyDelegate.set(2, secondaryEffectId);
         this.paymentSlot.takeStack(1);
      }

   }

   public boolean hasPayment() {
      return !this.payment.getStack(0).isEmpty();
   }

   private class PaymentSlot extends Slot {
      public PaymentSlot(Inventory inventory, int index, int x, int y) {
         super(inventory, index, x, y);
      }

      public boolean canInsert(ItemStack stack) {
         return stack.isIn(ItemTags.BEACON_PAYMENT_ITEMS);
      }

      public int getMaxItemCount() {
         return 1;
      }
   }
}
