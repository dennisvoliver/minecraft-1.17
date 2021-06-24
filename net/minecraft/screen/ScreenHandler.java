package net.minecraft.screen;

import com.google.common.base.Suppliers;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ClickType;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public abstract class ScreenHandler {
   /**
    * A special slot index value ({@value}) indicating that the player has clicked outside the main panel
    * of a screen. Used for dropping the cursor stack.
    */
   public static final int EMPTY_SPACE_SLOT_INDEX = -999;
   public static final int field_30731 = 0;
   public static final int field_30732 = 1;
   public static final int field_30733 = 2;
   public static final int field_30734 = 0;
   public static final int field_30735 = 1;
   public static final int field_30736 = 2;
   public static final int field_30737 = Integer.MAX_VALUE;
   /**
    * A list of item stacks that is used for tracking changes in {@link #sendContentUpdates()}.
    */
   private final DefaultedList<ItemStack> trackedStacks = DefaultedList.of();
   public final DefaultedList<Slot> slots = DefaultedList.of();
   private final List<Property> properties = Lists.newArrayList();
   private ItemStack cursorStack;
   private final DefaultedList<ItemStack> previousTrackedStacks;
   private final IntList trackedPropertyValues;
   private ItemStack previousCursorStack;
   @Nullable
   private final ScreenHandlerType<?> type;
   public final int syncId;
   private int quickCraftButton;
   private int quickCraftStage;
   private final Set<Slot> quickCraftSlots;
   private final List<ScreenHandlerListener> listeners;
   @Nullable
   private ScreenHandlerSyncHandler syncHandler;
   private boolean disableSync;

   protected ScreenHandler(@Nullable ScreenHandlerType<?> type, int syncId) {
      this.cursorStack = ItemStack.EMPTY;
      this.previousTrackedStacks = DefaultedList.of();
      this.trackedPropertyValues = new IntArrayList();
      this.previousCursorStack = ItemStack.EMPTY;
      this.quickCraftButton = -1;
      this.quickCraftSlots = Sets.newHashSet();
      this.listeners = Lists.newArrayList();
      this.type = type;
      this.syncId = syncId;
   }

   protected static boolean canUse(ScreenHandlerContext context, PlayerEntity player, Block block) {
      return (Boolean)context.get((world, pos) -> {
         return !world.getBlockState(pos).isOf(block) ? false : player.squaredDistanceTo((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D) <= 64.0D;
      }, true);
   }

   public ScreenHandlerType<?> getType() {
      if (this.type == null) {
         throw new UnsupportedOperationException("Unable to construct this menu by type");
      } else {
         return this.type;
      }
   }

   /**
    * Checks that the size of the provided inventory is at least as large as the {@code expectedSize}.
    * 
    * @throws IllegalArgumentException if the inventory size is smaller than {@code expectedSize}
    */
   protected static void checkSize(Inventory inventory, int expectedSize) {
      int i = inventory.size();
      if (i < expectedSize) {
         throw new IllegalArgumentException("Container size " + i + " is smaller than expected " + expectedSize);
      }
   }

   /**
    * Checks that the size of the {@code data} is at least as large as the {@code expectedCount}.
    * 
    * @throws IllegalArgumentException if the {@code data} has a smaller size than {@code expectedCount}
    */
   protected static void checkDataCount(PropertyDelegate data, int expectedCount) {
      int i = data.size();
      if (i < expectedCount) {
         throw new IllegalArgumentException("Container data count " + i + " is smaller than expected " + expectedCount);
      }
   }

   protected Slot addSlot(Slot slot) {
      slot.id = this.slots.size();
      this.slots.add(slot);
      this.trackedStacks.add(ItemStack.EMPTY);
      this.previousTrackedStacks.add(ItemStack.EMPTY);
      return slot;
   }

   protected Property addProperty(Property property) {
      this.properties.add(property);
      this.trackedPropertyValues.add(0);
      return property;
   }

   protected void addProperties(PropertyDelegate propertyDelegate) {
      for(int i = 0; i < propertyDelegate.size(); ++i) {
         this.addProperty(Property.create(propertyDelegate, i));
      }

   }

   public void addListener(ScreenHandlerListener listener) {
      if (!this.listeners.contains(listener)) {
         this.listeners.add(listener);
         this.sendContentUpdates();
      }
   }

   public void updateSyncHandler(ScreenHandlerSyncHandler handler) {
      this.syncHandler = handler;
      this.syncState();
   }

   public void syncState() {
      int k = 0;

      int l;
      for(l = this.slots.size(); k < l; ++k) {
         this.previousTrackedStacks.set(k, ((Slot)this.slots.get(k)).getStack().copy());
      }

      this.previousCursorStack = this.getCursorStack().copy();
      k = 0;

      for(l = this.properties.size(); k < l; ++k) {
         this.trackedPropertyValues.set(k, ((Property)this.properties.get(k)).get());
      }

      if (this.syncHandler != null) {
         this.syncHandler.updateState(this, this.previousTrackedStacks, this.previousCursorStack, this.trackedPropertyValues.toIntArray());
      }

   }

   public void removeListener(ScreenHandlerListener listener) {
      this.listeners.remove(listener);
   }

   public DefaultedList<ItemStack> getStacks() {
      DefaultedList<ItemStack> defaultedList = DefaultedList.of();
      Iterator var2 = this.slots.iterator();

      while(var2.hasNext()) {
         Slot slot = (Slot)var2.next();
         defaultedList.add(slot.getStack());
      }

      return defaultedList;
   }

   /**
    * Sends updates to listeners if any properties or slot stacks have changed.
    */
   public void sendContentUpdates() {
      int j;
      for(j = 0; j < this.slots.size(); ++j) {
         ItemStack itemStack = ((Slot)this.slots.get(j)).getStack();
         Objects.requireNonNull(itemStack);
         Supplier<ItemStack> supplier = Suppliers.memoize(itemStack::copy);
         this.updateTrackedSlot(j, itemStack, supplier);
         this.checkSlotUpdates(j, itemStack, supplier);
      }

      this.checkCursorStackUpdates();

      for(j = 0; j < this.properties.size(); ++j) {
         Property property = (Property)this.properties.get(j);
         int k = property.get();
         if (property.hasChanged()) {
            Iterator var4 = this.listeners.iterator();

            while(var4.hasNext()) {
               ScreenHandlerListener screenHandlerListener = (ScreenHandlerListener)var4.next();
               screenHandlerListener.onPropertyUpdate(this, j, k);
            }
         }

         this.checkPropertyUpdates(j, k);
      }

   }

   private void updateTrackedSlot(int slot, ItemStack stack, Supplier<ItemStack> copySupplier) {
      ItemStack itemStack = (ItemStack)this.trackedStacks.get(slot);
      if (!ItemStack.areEqual(itemStack, stack)) {
         ItemStack itemStack2 = (ItemStack)copySupplier.get();
         this.trackedStacks.set(slot, itemStack2);
         Iterator var6 = this.listeners.iterator();

         while(var6.hasNext()) {
            ScreenHandlerListener screenHandlerListener = (ScreenHandlerListener)var6.next();
            screenHandlerListener.onSlotUpdate(this, slot, itemStack2);
         }
      }

   }

   private void checkSlotUpdates(int slot, ItemStack stack, Supplier<ItemStack> copySupplier) {
      if (!this.disableSync) {
         ItemStack itemStack = (ItemStack)this.previousTrackedStacks.get(slot);
         if (!ItemStack.areEqual(itemStack, stack)) {
            ItemStack itemStack2 = (ItemStack)copySupplier.get();
            this.previousTrackedStacks.set(slot, itemStack2);
            if (this.syncHandler != null) {
               this.syncHandler.updateSlot(this, slot, itemStack2);
            }
         }

      }
   }

   private void checkPropertyUpdates(int id, int value) {
      if (!this.disableSync) {
         int i = this.trackedPropertyValues.getInt(id);
         if (i != value) {
            this.trackedPropertyValues.set(id, value);
            if (this.syncHandler != null) {
               this.syncHandler.updateProperty(this, id, value);
            }
         }

      }
   }

   private void checkCursorStackUpdates() {
      if (!this.disableSync) {
         if (!ItemStack.areEqual(this.getCursorStack(), this.previousCursorStack)) {
            this.previousCursorStack = this.getCursorStack().copy();
            if (this.syncHandler != null) {
               this.syncHandler.updateCursorStack(this, this.previousCursorStack);
            }
         }

      }
   }

   public void setPreviousTrackedSlot(int slot, ItemStack stack) {
      this.previousTrackedStacks.set(slot, stack);
   }

   public void setPreviousCursorStack(ItemStack stack) {
      this.previousCursorStack = stack.copy();
   }

   public boolean onButtonClick(PlayerEntity player, int id) {
      return false;
   }

   public Slot getSlot(int index) {
      return (Slot)this.slots.get(index);
   }

   public ItemStack transferSlot(PlayerEntity player, int index) {
      return ((Slot)this.slots.get(index)).getStack();
   }

   /**
    * Performs a slot click. This can behave in many different ways depending mainly on the action type.
    * 
    * @param actionType the type of slot click, check the docs for each {@link SlotActionType} value for details
    */
   public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      try {
         this.internalOnSlotClick(slotIndex, button, actionType, player);
      } catch (Exception var8) {
         CrashReport crashReport = CrashReport.create(var8, "Container click");
         CrashReportSection crashReportSection = crashReport.addElement("Click info");
         crashReportSection.add("Menu Type", () -> {
            return this.type != null ? Registry.SCREEN_HANDLER.getId(this.type).toString() : "<no type>";
         });
         crashReportSection.add("Menu Class", () -> {
            return this.getClass().getCanonicalName();
         });
         crashReportSection.add("Slot Count", (Object)this.slots.size());
         crashReportSection.add("Slot", (Object)slotIndex);
         crashReportSection.add("Button", (Object)button);
         crashReportSection.add("Type", (Object)actionType);
         throw new CrashException(crashReport);
      }
   }

   /**
    * The actual logic that handles a slot click. Called by {@link #onSlotClick
    * (int, int, SlotActionType, PlayerEntity)} in a try-catch block that wraps
    * exceptions from this method into a crash report.
    */
   private void internalOnSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
      PlayerInventory playerInventory = player.getInventory();
      Slot slot4;
      ItemStack itemStack6;
      ItemStack itemStack2;
      int j;
      int k;
      if (actionType == SlotActionType.QUICK_CRAFT) {
         int i = this.quickCraftStage;
         this.quickCraftStage = unpackQuickCraftStage(button);
         if ((i != 1 || this.quickCraftStage != 2) && i != this.quickCraftStage) {
            this.endQuickCraft();
         } else if (this.getCursorStack().isEmpty()) {
            this.endQuickCraft();
         } else if (this.quickCraftStage == 0) {
            this.quickCraftButton = unpackQuickCraftButton(button);
            if (shouldQuickCraftContinue(this.quickCraftButton, player)) {
               this.quickCraftStage = 1;
               this.quickCraftSlots.clear();
            } else {
               this.endQuickCraft();
            }
         } else if (this.quickCraftStage == 1) {
            slot4 = (Slot)this.slots.get(slotIndex);
            itemStack6 = this.getCursorStack();
            if (canInsertItemIntoSlot(slot4, itemStack6, true) && slot4.canInsert(itemStack6) && (this.quickCraftButton == 2 || itemStack6.getCount() > this.quickCraftSlots.size()) && this.canInsertIntoSlot(slot4)) {
               this.quickCraftSlots.add(slot4);
            }
         } else if (this.quickCraftStage == 2) {
            if (!this.quickCraftSlots.isEmpty()) {
               if (this.quickCraftSlots.size() == 1) {
                  j = ((Slot)this.quickCraftSlots.iterator().next()).id;
                  this.endQuickCraft();
                  this.internalOnSlotClick(j, this.quickCraftButton, SlotActionType.PICKUP, player);
                  return;
               }

               itemStack2 = this.getCursorStack().copy();
               k = this.getCursorStack().getCount();
               Iterator var9 = this.quickCraftSlots.iterator();

               label305:
               while(true) {
                  Slot slot2;
                  ItemStack itemStack3;
                  do {
                     do {
                        do {
                           do {
                              if (!var9.hasNext()) {
                                 itemStack2.setCount(k);
                                 this.setCursorStack(itemStack2);
                                 break label305;
                              }

                              slot2 = (Slot)var9.next();
                              itemStack3 = this.getCursorStack();
                           } while(slot2 == null);
                        } while(!canInsertItemIntoSlot(slot2, itemStack3, true));
                     } while(!slot2.canInsert(itemStack3));
                  } while(this.quickCraftButton != 2 && itemStack3.getCount() < this.quickCraftSlots.size());

                  if (this.canInsertIntoSlot(slot2)) {
                     ItemStack itemStack4 = itemStack2.copy();
                     int l = slot2.hasStack() ? slot2.getStack().getCount() : 0;
                     calculateStackSize(this.quickCraftSlots, this.quickCraftButton, itemStack4, l);
                     int m = Math.min(itemStack4.getMaxCount(), slot2.getMaxItemCount(itemStack4));
                     if (itemStack4.getCount() > m) {
                        itemStack4.setCount(m);
                     }

                     k -= itemStack4.getCount() - l;
                     slot2.setStack(itemStack4);
                  }
               }
            }

            this.endQuickCraft();
         } else {
            this.endQuickCraft();
         }
      } else if (this.quickCraftStage != 0) {
         this.endQuickCraft();
      } else {
         int v;
         if ((actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) && (button == 0 || button == 1)) {
            ClickType clickType = button == 0 ? ClickType.LEFT : ClickType.RIGHT;
            if (slotIndex == EMPTY_SPACE_SLOT_INDEX) {
               if (!this.getCursorStack().isEmpty()) {
                  if (clickType == ClickType.LEFT) {
                     player.dropItem(this.getCursorStack(), true);
                     this.setCursorStack(ItemStack.EMPTY);
                  } else {
                     player.dropItem(this.getCursorStack().split(1), true);
                  }
               }
            } else if (actionType == SlotActionType.QUICK_MOVE) {
               if (slotIndex < 0) {
                  return;
               }

               slot4 = (Slot)this.slots.get(slotIndex);
               if (!slot4.canTakeItems(player)) {
                  return;
               }

               for(itemStack6 = this.transferSlot(player, slotIndex); !itemStack6.isEmpty() && ItemStack.areItemsEqualIgnoreDamage(slot4.getStack(), itemStack6); itemStack6 = this.transferSlot(player, slotIndex)) {
               }
            } else {
               if (slotIndex < 0) {
                  return;
               }

               slot4 = (Slot)this.slots.get(slotIndex);
               itemStack6 = slot4.getStack();
               ItemStack itemStack7 = this.getCursorStack();
               player.onPickupSlotClick(itemStack7, slot4.getStack(), clickType);
               if (!itemStack7.onStackClicked(slot4, clickType, player) && !itemStack6.onClicked(itemStack7, slot4, clickType, player, this.getCursorStackReference())) {
                  if (itemStack6.isEmpty()) {
                     if (!itemStack7.isEmpty()) {
                        v = clickType == ClickType.LEFT ? itemStack7.getCount() : 1;
                        this.setCursorStack(slot4.insertStack(itemStack7, v));
                     }
                  } else if (slot4.canTakeItems(player)) {
                     if (itemStack7.isEmpty()) {
                        v = clickType == ClickType.LEFT ? itemStack6.getCount() : (itemStack6.getCount() + 1) / 2;
                        Optional<ItemStack> optional = slot4.tryTakeStackRange(v, Integer.MAX_VALUE, player);
                        optional.ifPresent((itemStack) -> {
                           this.setCursorStack(itemStack);
                           slot4.onTakeItem(player, itemStack);
                        });
                     } else if (slot4.canInsert(itemStack7)) {
                        if (ItemStack.canCombine(itemStack6, itemStack7)) {
                           v = clickType == ClickType.LEFT ? itemStack7.getCount() : 1;
                           this.setCursorStack(slot4.insertStack(itemStack7, v));
                        } else if (itemStack7.getCount() <= slot4.getMaxItemCount(itemStack7)) {
                           slot4.setStack(itemStack7);
                           this.setCursorStack(itemStack6);
                        }
                     } else if (ItemStack.canCombine(itemStack6, itemStack7)) {
                        Optional<ItemStack> optional2 = slot4.tryTakeStackRange(itemStack6.getCount(), itemStack7.getMaxCount() - itemStack7.getCount(), player);
                        optional2.ifPresent((itemStack2x) -> {
                           itemStack7.increment(itemStack2x.getCount());
                           slot4.onTakeItem(player, itemStack2x);
                        });
                     }
                  }
               }

               slot4.markDirty();
            }
         } else {
            Slot slot5;
            int u;
            if (actionType == SlotActionType.SWAP) {
               slot5 = (Slot)this.slots.get(slotIndex);
               itemStack2 = playerInventory.getStack(button);
               itemStack6 = slot5.getStack();
               if (!itemStack2.isEmpty() || !itemStack6.isEmpty()) {
                  if (itemStack2.isEmpty()) {
                     if (slot5.canTakeItems(player)) {
                        playerInventory.setStack(button, itemStack6);
                        slot5.onTake(itemStack6.getCount());
                        slot5.setStack(ItemStack.EMPTY);
                        slot5.onTakeItem(player, itemStack6);
                     }
                  } else if (itemStack6.isEmpty()) {
                     if (slot5.canInsert(itemStack2)) {
                        u = slot5.getMaxItemCount(itemStack2);
                        if (itemStack2.getCount() > u) {
                           slot5.setStack(itemStack2.split(u));
                        } else {
                           slot5.setStack(itemStack2);
                           playerInventory.setStack(button, ItemStack.EMPTY);
                        }
                     }
                  } else if (slot5.canTakeItems(player) && slot5.canInsert(itemStack2)) {
                     u = slot5.getMaxItemCount(itemStack2);
                     if (itemStack2.getCount() > u) {
                        slot5.setStack(itemStack2.split(u));
                        slot5.onTakeItem(player, itemStack6);
                        if (!playerInventory.insertStack(itemStack6)) {
                           player.dropItem(itemStack6, true);
                        }
                     } else {
                        slot5.setStack(itemStack2);
                        playerInventory.setStack(button, itemStack6);
                        slot5.onTakeItem(player, itemStack6);
                     }
                  }
               }
            } else if (actionType == SlotActionType.CLONE && player.getAbilities().creativeMode && this.getCursorStack().isEmpty() && slotIndex >= 0) {
               slot5 = (Slot)this.slots.get(slotIndex);
               if (slot5.hasStack()) {
                  itemStack2 = slot5.getStack().copy();
                  itemStack2.setCount(itemStack2.getMaxCount());
                  this.setCursorStack(itemStack2);
               }
            } else if (actionType == SlotActionType.THROW && this.getCursorStack().isEmpty() && slotIndex >= 0) {
               slot5 = (Slot)this.slots.get(slotIndex);
               j = button == 0 ? 1 : slot5.getStack().getCount();
               itemStack6 = slot5.takeStackRange(j, Integer.MAX_VALUE, player);
               player.dropItem(itemStack6, true);
            } else if (actionType == SlotActionType.PICKUP_ALL && slotIndex >= 0) {
               slot5 = (Slot)this.slots.get(slotIndex);
               itemStack2 = this.getCursorStack();
               if (!itemStack2.isEmpty() && (!slot5.hasStack() || !slot5.canTakeItems(player))) {
                  k = button == 0 ? 0 : this.slots.size() - 1;
                  u = button == 0 ? 1 : -1;

                  for(v = 0; v < 2; ++v) {
                     for(int w = k; w >= 0 && w < this.slots.size() && itemStack2.getCount() < itemStack2.getMaxCount(); w += u) {
                        Slot slot9 = (Slot)this.slots.get(w);
                        if (slot9.hasStack() && canInsertItemIntoSlot(slot9, itemStack2, true) && slot9.canTakeItems(player) && this.canInsertIntoSlot(itemStack2, slot9)) {
                           ItemStack itemStack13 = slot9.getStack();
                           if (v != 0 || itemStack13.getCount() != itemStack13.getMaxCount()) {
                              ItemStack itemStack14 = slot9.takeStackRange(itemStack13.getCount(), itemStack2.getMaxCount() - itemStack2.getCount(), player);
                              itemStack2.increment(itemStack14.getCount());
                           }
                        }
                     }
                  }
               }
            }
         }
      }

   }

   private StackReference getCursorStackReference() {
      return new StackReference() {
         public ItemStack get() {
            return ScreenHandler.this.getCursorStack();
         }

         public boolean set(ItemStack stack) {
            ScreenHandler.this.setCursorStack(stack);
            return true;
         }
      };
   }

   public boolean canInsertIntoSlot(ItemStack stack, Slot slot) {
      return true;
   }

   public void close(PlayerEntity playerEntity) {
      if (!this.getCursorStack().isEmpty()) {
         playerEntity.dropItem(this.getCursorStack(), false);
         this.setCursorStack(ItemStack.EMPTY);
      }

   }

   protected void dropInventory(PlayerEntity player, Inventory inventory) {
      int j;
      if (!player.isAlive() || player instanceof ServerPlayerEntity && ((ServerPlayerEntity)player).isDisconnected()) {
         for(j = 0; j < inventory.size(); ++j) {
            player.dropItem(inventory.removeStack(j), false);
         }

      } else {
         for(j = 0; j < inventory.size(); ++j) {
            PlayerInventory playerInventory = player.getInventory();
            if (playerInventory.player instanceof ServerPlayerEntity) {
               playerInventory.offerOrDrop(inventory.removeStack(j));
            }
         }

      }
   }

   public void onContentChanged(Inventory inventory) {
      this.sendContentUpdates();
   }

   public void setStackInSlot(int slot, ItemStack stack) {
      this.getSlot(slot).setStack(stack);
   }

   public void updateSlotStacks(List<ItemStack> stacks) {
      for(int i = 0; i < stacks.size(); ++i) {
         this.getSlot(i).setStack((ItemStack)stacks.get(i));
      }

   }

   public void setProperty(int id, int value) {
      ((Property)this.properties.get(id)).set(value);
   }

   public abstract boolean canUse(PlayerEntity player);

   protected boolean insertItem(ItemStack stack, int startIndex, int endIndex, boolean fromLast) {
      boolean bl = false;
      int i = startIndex;
      if (fromLast) {
         i = endIndex - 1;
      }

      Slot slot2;
      ItemStack itemStack;
      if (stack.isStackable()) {
         while(!stack.isEmpty()) {
            if (fromLast) {
               if (i < startIndex) {
                  break;
               }
            } else if (i >= endIndex) {
               break;
            }

            slot2 = (Slot)this.slots.get(i);
            itemStack = slot2.getStack();
            if (!itemStack.isEmpty() && ItemStack.canCombine(stack, itemStack)) {
               int j = itemStack.getCount() + stack.getCount();
               if (j <= stack.getMaxCount()) {
                  stack.setCount(0);
                  itemStack.setCount(j);
                  slot2.markDirty();
                  bl = true;
               } else if (itemStack.getCount() < stack.getMaxCount()) {
                  stack.decrement(stack.getMaxCount() - itemStack.getCount());
                  itemStack.setCount(stack.getMaxCount());
                  slot2.markDirty();
                  bl = true;
               }
            }

            if (fromLast) {
               --i;
            } else {
               ++i;
            }
         }
      }

      if (!stack.isEmpty()) {
         if (fromLast) {
            i = endIndex - 1;
         } else {
            i = startIndex;
         }

         while(true) {
            if (fromLast) {
               if (i < startIndex) {
                  break;
               }
            } else if (i >= endIndex) {
               break;
            }

            slot2 = (Slot)this.slots.get(i);
            itemStack = slot2.getStack();
            if (itemStack.isEmpty() && slot2.canInsert(stack)) {
               if (stack.getCount() > slot2.getMaxItemCount()) {
                  slot2.setStack(stack.split(slot2.getMaxItemCount()));
               } else {
                  slot2.setStack(stack.split(stack.getCount()));
               }

               slot2.markDirty();
               bl = true;
               break;
            }

            if (fromLast) {
               --i;
            } else {
               ++i;
            }
         }
      }

      return bl;
   }

   public static int unpackQuickCraftButton(int quickCraftData) {
      return quickCraftData >> 2 & 3;
   }

   public static int unpackQuickCraftStage(int quickCraftData) {
      return quickCraftData & 3;
   }

   public static int packQuickCraftData(int quickCraftStage, int buttonId) {
      return quickCraftStage & 3 | (buttonId & 3) << 2;
   }

   public static boolean shouldQuickCraftContinue(int stage, PlayerEntity player) {
      if (stage == 0) {
         return true;
      } else if (stage == 1) {
         return true;
      } else {
         return stage == 2 && player.getAbilities().creativeMode;
      }
   }

   protected void endQuickCraft() {
      this.quickCraftStage = 0;
      this.quickCraftSlots.clear();
   }

   public static boolean canInsertItemIntoSlot(@Nullable Slot slot, ItemStack stack, boolean allowOverflow) {
      boolean bl = slot == null || !slot.hasStack();
      if (!bl && ItemStack.canCombine(stack, slot.getStack())) {
         return slot.getStack().getCount() + (allowOverflow ? 0 : stack.getCount()) <= stack.getMaxCount();
      } else {
         return bl;
      }
   }

   public static void calculateStackSize(Set<Slot> slots, int mode, ItemStack stack, int stackSize) {
      switch(mode) {
      case 0:
         stack.setCount(MathHelper.floor((float)stack.getCount() / (float)slots.size()));
         break;
      case 1:
         stack.setCount(1);
         break;
      case 2:
         stack.setCount(stack.getItem().getMaxCount());
      }

      stack.increment(stackSize);
   }

   public boolean canInsertIntoSlot(Slot slot) {
      return true;
   }

   public static int calculateComparatorOutput(@Nullable BlockEntity entity) {
      return entity instanceof Inventory ? calculateComparatorOutput((Inventory)entity) : 0;
   }

   public static int calculateComparatorOutput(@Nullable Inventory inventory) {
      if (inventory == null) {
         return 0;
      } else {
         int i = 0;
         float f = 0.0F;

         for(int j = 0; j < inventory.size(); ++j) {
            ItemStack itemStack = inventory.getStack(j);
            if (!itemStack.isEmpty()) {
               f += (float)itemStack.getCount() / (float)Math.min(inventory.getMaxCountPerStack(), itemStack.getMaxCount());
               ++i;
            }
         }

         f /= (float)inventory.size();
         return MathHelper.floor(f * 14.0F) + (i > 0 ? 1 : 0);
      }
   }

   public void setCursorStack(ItemStack stack) {
      this.cursorStack = stack;
   }

   public ItemStack getCursorStack() {
      return this.cursorStack;
   }

   public void disableSyncing() {
      this.disableSync = true;
   }

   public void enableSyncing() {
      this.disableSync = false;
   }

   public void copySharedSlots(ScreenHandler handler) {
      Table<Inventory, Integer, Integer> table = HashBasedTable.create();

      int j;
      Slot slot2;
      for(j = 0; j < handler.slots.size(); ++j) {
         slot2 = (Slot)handler.slots.get(j);
         table.put(slot2.inventory, slot2.getIndex(), j);
      }

      for(j = 0; j < this.slots.size(); ++j) {
         slot2 = (Slot)this.slots.get(j);
         Integer integer = (Integer)table.get(slot2.inventory, slot2.getIndex());
         if (integer != null) {
            this.trackedStacks.set(j, (ItemStack)handler.trackedStacks.get(integer));
            this.previousTrackedStacks.set(j, (ItemStack)handler.previousTrackedStacks.get(integer));
         }
      }

   }
}
