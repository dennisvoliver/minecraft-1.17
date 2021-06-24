package net.minecraft.entity.player;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.Tag;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Nameable;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;

public class PlayerInventory implements Inventory, Nameable {
   /**
    * The maximum cooldown ({@value} ticks) applied to timed use items such as the Eye of Ender.
    */
   public static final int ITEM_USAGE_COOLDOWN = 5;
   /**
    * The number of slots ({@value}) in the main (non-hotbar) section of the inventory.
    */
   public static final int MAIN_SIZE = 36;
   /**
    * The number of columns ({@value}) in the inventory.
    * 
    * <p>The same value dictates the size of the player's hotbar, excluding the offhand slot.</p>
    */
   private static final int HOTBAR_SIZE = 9;
   /**
    * Zero-based index of the offhand slot.
    * 
    * <p>This value is the result of the sum {@code MAIN_SIZE (36) + ARMOR_SIZE (4)}.</p>
    */
   public static final int OFF_HAND_SLOT = 40;
   /**
    * The slot index ({@value}) used to indicate no result
    * (item not present / no available space) when querying the inventory's contents
    * or to indicate no preference when inserting an item into the inventory.
    */
   public static final int NOT_FOUND = -1;
   public static final int[] ARMOR_SLOTS = new int[]{0, 1, 2, 3};
   public static final int[] HELMET_SLOTS = new int[]{3};
   public final DefaultedList<ItemStack> main;
   public final DefaultedList<ItemStack> armor;
   public final DefaultedList<ItemStack> offHand;
   private final List<DefaultedList<ItemStack>> combinedInventory;
   public int selectedSlot;
   public final PlayerEntity player;
   private int changeCount;

   public PlayerInventory(PlayerEntity player) {
      this.main = DefaultedList.ofSize(36, ItemStack.EMPTY);
      this.armor = DefaultedList.ofSize(4, ItemStack.EMPTY);
      this.offHand = DefaultedList.ofSize(1, ItemStack.EMPTY);
      this.combinedInventory = ImmutableList.of(this.main, this.armor, this.offHand);
      this.player = player;
   }

   public ItemStack getMainHandStack() {
      return isValidHotbarIndex(this.selectedSlot) ? (ItemStack)this.main.get(this.selectedSlot) : ItemStack.EMPTY;
   }

   public static int getHotbarSize() {
      return 9;
   }

   private boolean canStackAddMore(ItemStack existingStack, ItemStack stack) {
      return !existingStack.isEmpty() && ItemStack.canCombine(existingStack, stack) && existingStack.isStackable() && existingStack.getCount() < existingStack.getMaxCount() && existingStack.getCount() < this.getMaxCountPerStack();
   }

   public int getEmptySlot() {
      for(int i = 0; i < this.main.size(); ++i) {
         if (((ItemStack)this.main.get(i)).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   public void addPickBlock(ItemStack stack) {
      int i = this.getSlotWithStack(stack);
      if (isValidHotbarIndex(i)) {
         this.selectedSlot = i;
      } else {
         if (i == -1) {
            this.selectedSlot = this.getSwappableHotbarSlot();
            if (!((ItemStack)this.main.get(this.selectedSlot)).isEmpty()) {
               int j = this.getEmptySlot();
               if (j != -1) {
                  this.main.set(j, (ItemStack)this.main.get(this.selectedSlot));
               }
            }

            this.main.set(this.selectedSlot, stack);
         } else {
            this.swapSlotWithHotbar(i);
         }

      }
   }

   public void swapSlotWithHotbar(int slot) {
      this.selectedSlot = this.getSwappableHotbarSlot();
      ItemStack itemStack = (ItemStack)this.main.get(this.selectedSlot);
      this.main.set(this.selectedSlot, (ItemStack)this.main.get(slot));
      this.main.set(slot, itemStack);
   }

   public static boolean isValidHotbarIndex(int slot) {
      return slot >= 0 && slot < 9;
   }

   public int getSlotWithStack(ItemStack stack) {
      for(int i = 0; i < this.main.size(); ++i) {
         if (!((ItemStack)this.main.get(i)).isEmpty() && ItemStack.canCombine(stack, (ItemStack)this.main.get(i))) {
            return i;
         }
      }

      return -1;
   }

   /**
    * Given the item stack to search for, returns the equivalent slot index with a matching stack that is all of:
    * not damaged, not enchanted, and not renamed.
    * 
    * @return the index where a matching stack was found, or {@value #NOT_FOUND}
    */
   public int indexOf(ItemStack stack) {
      for(int i = 0; i < this.main.size(); ++i) {
         ItemStack itemStack = (ItemStack)this.main.get(i);
         if (!((ItemStack)this.main.get(i)).isEmpty() && ItemStack.canCombine(stack, (ItemStack)this.main.get(i)) && !((ItemStack)this.main.get(i)).isDamaged() && !itemStack.hasEnchantments() && !itemStack.hasCustomName()) {
            return i;
         }
      }

      return -1;
   }

   public int getSwappableHotbarSlot() {
      int k;
      int l;
      for(k = 0; k < 9; ++k) {
         l = (this.selectedSlot + k) % 9;
         if (((ItemStack)this.main.get(l)).isEmpty()) {
            return l;
         }
      }

      for(k = 0; k < 9; ++k) {
         l = (this.selectedSlot + k) % 9;
         if (!((ItemStack)this.main.get(l)).hasEnchantments()) {
            return l;
         }
      }

      return this.selectedSlot;
   }

   public void scrollInHotbar(double scrollAmount) {
      if (scrollAmount > 0.0D) {
         scrollAmount = 1.0D;
      }

      if (scrollAmount < 0.0D) {
         scrollAmount = -1.0D;
      }

      for(this.selectedSlot = (int)((double)this.selectedSlot - scrollAmount); this.selectedSlot < 0; this.selectedSlot += 9) {
      }

      while(this.selectedSlot >= 9) {
         this.selectedSlot -= 9;
      }

   }

   public int remove(Predicate<ItemStack> shouldRemove, int maxCount, Inventory craftingInventory) {
      int i = 0;
      boolean bl = maxCount == 0;
      int i = i + Inventories.remove((Inventory)this, shouldRemove, maxCount - i, bl);
      i += Inventories.remove(craftingInventory, shouldRemove, maxCount - i, bl);
      ItemStack itemStack = this.player.currentScreenHandler.getCursorStack();
      i += Inventories.remove(itemStack, shouldRemove, maxCount - i, bl);
      if (itemStack.isEmpty()) {
         this.player.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
      }

      return i;
   }

   private int addStack(ItemStack stack) {
      int i = this.getOccupiedSlotWithRoomForStack(stack);
      if (i == -1) {
         i = this.getEmptySlot();
      }

      return i == -1 ? stack.getCount() : this.addStack(i, stack);
   }

   private int addStack(int slot, ItemStack stack) {
      Item item = stack.getItem();
      int i = stack.getCount();
      ItemStack itemStack = this.getStack(slot);
      if (itemStack.isEmpty()) {
         itemStack = new ItemStack(item, 0);
         if (stack.hasTag()) {
            itemStack.setTag(stack.getTag().copy());
         }

         this.setStack(slot, itemStack);
      }

      int j = i;
      if (i > itemStack.getMaxCount() - itemStack.getCount()) {
         j = itemStack.getMaxCount() - itemStack.getCount();
      }

      if (j > this.getMaxCountPerStack() - itemStack.getCount()) {
         j = this.getMaxCountPerStack() - itemStack.getCount();
      }

      if (j == 0) {
         return i;
      } else {
         i -= j;
         itemStack.increment(j);
         itemStack.setCooldown(5);
         return i;
      }
   }

   public int getOccupiedSlotWithRoomForStack(ItemStack stack) {
      if (this.canStackAddMore(this.getStack(this.selectedSlot), stack)) {
         return this.selectedSlot;
      } else if (this.canStackAddMore(this.getStack(40), stack)) {
         return 40;
      } else {
         for(int i = 0; i < this.main.size(); ++i) {
            if (this.canStackAddMore((ItemStack)this.main.get(i), stack)) {
               return i;
            }
         }

         return -1;
      }
   }

   public void updateItems() {
      Iterator var1 = this.combinedInventory.iterator();

      while(var1.hasNext()) {
         DefaultedList<ItemStack> defaultedList = (DefaultedList)var1.next();

         for(int i = 0; i < defaultedList.size(); ++i) {
            if (!((ItemStack)defaultedList.get(i)).isEmpty()) {
               ((ItemStack)defaultedList.get(i)).inventoryTick(this.player.world, this.player, i, this.selectedSlot == i);
            }
         }
      }

   }

   public boolean insertStack(ItemStack stack) {
      return this.insertStack(-1, stack);
   }

   public boolean insertStack(int slot, ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      } else {
         try {
            if (stack.isDamaged()) {
               if (slot == -1) {
                  slot = this.getEmptySlot();
               }

               if (slot >= 0) {
                  this.main.set(slot, stack.copy());
                  ((ItemStack)this.main.get(slot)).setCooldown(5);
                  stack.setCount(0);
                  return true;
               } else if (this.player.getAbilities().creativeMode) {
                  stack.setCount(0);
                  return true;
               } else {
                  return false;
               }
            } else {
               int i;
               do {
                  i = stack.getCount();
                  if (slot == -1) {
                     stack.setCount(this.addStack(stack));
                  } else {
                     stack.setCount(this.addStack(slot, stack));
                  }
               } while(!stack.isEmpty() && stack.getCount() < i);

               if (stack.getCount() == i && this.player.getAbilities().creativeMode) {
                  stack.setCount(0);
                  return true;
               } else {
                  return stack.getCount() < i;
               }
            }
         } catch (Throwable var6) {
            CrashReport crashReport = CrashReport.create(var6, "Adding item to inventory");
            CrashReportSection crashReportSection = crashReport.addElement("Item being added");
            crashReportSection.add("Item ID", (Object)Item.getRawId(stack.getItem()));
            crashReportSection.add("Item data", (Object)stack.getDamage());
            crashReportSection.add("Item name", () -> {
               return stack.getName().getString();
            });
            throw new CrashException(crashReport);
         }
      }
   }

   public void offerOrDrop(ItemStack stack) {
      this.offer(stack, true);
   }

   public void offer(ItemStack stack, boolean notifiesClient) {
      while(true) {
         if (!stack.isEmpty()) {
            int i = this.getOccupiedSlotWithRoomForStack(stack);
            if (i == -1) {
               i = this.getEmptySlot();
            }

            if (i != -1) {
               int j = stack.getMaxCount() - this.getStack(i).getCount();
               if (this.insertStack(i, stack.split(j)) && notifiesClient && this.player instanceof ServerPlayerEntity) {
                  ((ServerPlayerEntity)this.player).networkHandler.sendPacket(new ScreenHandlerSlotUpdateS2CPacket(ScreenHandlerSlotUpdateS2CPacket.UPDATE_PLAYER_INVENTORY_SYNC_ID, i, this.getStack(i)));
               }
               continue;
            }

            this.player.dropItem(stack, false);
         }

         return;
      }
   }

   public ItemStack removeStack(int slot, int amount) {
      List<ItemStack> list = null;

      DefaultedList defaultedList;
      for(Iterator var4 = this.combinedInventory.iterator(); var4.hasNext(); slot -= defaultedList.size()) {
         defaultedList = (DefaultedList)var4.next();
         if (slot < defaultedList.size()) {
            list = defaultedList;
            break;
         }
      }

      return list != null && !((ItemStack)list.get(slot)).isEmpty() ? Inventories.splitStack(list, slot, amount) : ItemStack.EMPTY;
   }

   public void removeOne(ItemStack stack) {
      Iterator var2 = this.combinedInventory.iterator();

      while(true) {
         while(var2.hasNext()) {
            DefaultedList<ItemStack> defaultedList = (DefaultedList)var2.next();

            for(int i = 0; i < defaultedList.size(); ++i) {
               if (defaultedList.get(i) == stack) {
                  defaultedList.set(i, ItemStack.EMPTY);
                  break;
               }
            }
         }

         return;
      }
   }

   public ItemStack removeStack(int slot) {
      DefaultedList<ItemStack> defaultedList = null;

      DefaultedList defaultedList2;
      for(Iterator var3 = this.combinedInventory.iterator(); var3.hasNext(); slot -= defaultedList2.size()) {
         defaultedList2 = (DefaultedList)var3.next();
         if (slot < defaultedList2.size()) {
            defaultedList = defaultedList2;
            break;
         }
      }

      if (defaultedList != null && !((ItemStack)defaultedList.get(slot)).isEmpty()) {
         ItemStack itemStack = (ItemStack)defaultedList.get(slot);
         defaultedList.set(slot, ItemStack.EMPTY);
         return itemStack;
      } else {
         return ItemStack.EMPTY;
      }
   }

   public void setStack(int slot, ItemStack stack) {
      DefaultedList<ItemStack> defaultedList = null;

      DefaultedList defaultedList2;
      for(Iterator var4 = this.combinedInventory.iterator(); var4.hasNext(); slot -= defaultedList2.size()) {
         defaultedList2 = (DefaultedList)var4.next();
         if (slot < defaultedList2.size()) {
            defaultedList = defaultedList2;
            break;
         }
      }

      if (defaultedList != null) {
         defaultedList.set(slot, stack);
      }

   }

   public float getBlockBreakingSpeed(BlockState block) {
      return ((ItemStack)this.main.get(this.selectedSlot)).getMiningSpeedMultiplier(block);
   }

   public NbtList writeNbt(NbtList nbtList) {
      int k;
      NbtCompound nbtCompound3;
      for(k = 0; k < this.main.size(); ++k) {
         if (!((ItemStack)this.main.get(k)).isEmpty()) {
            nbtCompound3 = new NbtCompound();
            nbtCompound3.putByte("Slot", (byte)k);
            ((ItemStack)this.main.get(k)).writeNbt(nbtCompound3);
            nbtList.add(nbtCompound3);
         }
      }

      for(k = 0; k < this.armor.size(); ++k) {
         if (!((ItemStack)this.armor.get(k)).isEmpty()) {
            nbtCompound3 = new NbtCompound();
            nbtCompound3.putByte("Slot", (byte)(k + 100));
            ((ItemStack)this.armor.get(k)).writeNbt(nbtCompound3);
            nbtList.add(nbtCompound3);
         }
      }

      for(k = 0; k < this.offHand.size(); ++k) {
         if (!((ItemStack)this.offHand.get(k)).isEmpty()) {
            nbtCompound3 = new NbtCompound();
            nbtCompound3.putByte("Slot", (byte)(k + 150));
            ((ItemStack)this.offHand.get(k)).writeNbt(nbtCompound3);
            nbtList.add(nbtCompound3);
         }
      }

      return nbtList;
   }

   public void readNbt(NbtList nbtList) {
      this.main.clear();
      this.armor.clear();
      this.offHand.clear();

      for(int i = 0; i < nbtList.size(); ++i) {
         NbtCompound nbtCompound = nbtList.getCompound(i);
         int j = nbtCompound.getByte("Slot") & 255;
         ItemStack itemStack = ItemStack.fromNbt(nbtCompound);
         if (!itemStack.isEmpty()) {
            if (j >= 0 && j < this.main.size()) {
               this.main.set(j, itemStack);
            } else if (j >= 100 && j < this.armor.size() + 100) {
               this.armor.set(j - 100, itemStack);
            } else if (j >= 150 && j < this.offHand.size() + 150) {
               this.offHand.set(j - 150, itemStack);
            }
         }
      }

   }

   public int size() {
      return this.main.size() + this.armor.size() + this.offHand.size();
   }

   public boolean isEmpty() {
      Iterator var1 = this.main.iterator();

      ItemStack itemStack3;
      do {
         if (!var1.hasNext()) {
            var1 = this.armor.iterator();

            do {
               if (!var1.hasNext()) {
                  var1 = this.offHand.iterator();

                  do {
                     if (!var1.hasNext()) {
                        return true;
                     }

                     itemStack3 = (ItemStack)var1.next();
                  } while(itemStack3.isEmpty());

                  return false;
               }

               itemStack3 = (ItemStack)var1.next();
            } while(itemStack3.isEmpty());

            return false;
         }

         itemStack3 = (ItemStack)var1.next();
      } while(itemStack3.isEmpty());

      return false;
   }

   public ItemStack getStack(int slot) {
      List<ItemStack> list = null;

      DefaultedList defaultedList;
      for(Iterator var3 = this.combinedInventory.iterator(); var3.hasNext(); slot -= defaultedList.size()) {
         defaultedList = (DefaultedList)var3.next();
         if (slot < defaultedList.size()) {
            list = defaultedList;
            break;
         }
      }

      return list == null ? ItemStack.EMPTY : (ItemStack)list.get(slot);
   }

   public Text getName() {
      return new TranslatableText("container.inventory");
   }

   public ItemStack getArmorStack(int slot) {
      return (ItemStack)this.armor.get(slot);
   }

   public void damageArmor(DamageSource damageSource, float amount, int[] slots) {
      if (!(amount <= 0.0F)) {
         amount /= 4.0F;
         if (amount < 1.0F) {
            amount = 1.0F;
         }

         int[] var4 = slots;
         int var5 = slots.length;

         for(int var6 = 0; var6 < var5; ++var6) {
            int i = var4[var6];
            ItemStack itemStack = (ItemStack)this.armor.get(i);
            if ((!damageSource.isFire() || !itemStack.getItem().isFireproof()) && itemStack.getItem() instanceof ArmorItem) {
               itemStack.damage((int)amount, (LivingEntity)this.player, (Consumer)((player) -> {
                  player.sendEquipmentBreakStatus(EquipmentSlot.fromTypeIndex(EquipmentSlot.Type.ARMOR, i));
               }));
            }
         }

      }
   }

   public void dropAll() {
      Iterator var1 = this.combinedInventory.iterator();

      while(var1.hasNext()) {
         List<ItemStack> list = (List)var1.next();

         for(int i = 0; i < list.size(); ++i) {
            ItemStack itemStack = (ItemStack)list.get(i);
            if (!itemStack.isEmpty()) {
               this.player.dropItem(itemStack, true, false);
               list.set(i, ItemStack.EMPTY);
            }
         }
      }

   }

   public void markDirty() {
      ++this.changeCount;
   }

   public int getChangeCount() {
      return this.changeCount;
   }

   public boolean canPlayerUse(PlayerEntity player) {
      if (this.player.isRemoved()) {
         return false;
      } else {
         return !(player.squaredDistanceTo(this.player) > 64.0D);
      }
   }

   public boolean contains(ItemStack stack) {
      Iterator var2 = this.combinedInventory.iterator();

      while(var2.hasNext()) {
         List<ItemStack> list = (List)var2.next();
         Iterator var4 = list.iterator();

         while(var4.hasNext()) {
            ItemStack itemStack = (ItemStack)var4.next();
            if (!itemStack.isEmpty() && itemStack.isItemEqualIgnoreDamage(stack)) {
               return true;
            }
         }
      }

      return false;
   }

   public boolean contains(Tag<Item> tag) {
      Iterator var2 = this.combinedInventory.iterator();

      while(var2.hasNext()) {
         List<ItemStack> list = (List)var2.next();
         Iterator var4 = list.iterator();

         while(var4.hasNext()) {
            ItemStack itemStack = (ItemStack)var4.next();
            if (!itemStack.isEmpty() && itemStack.isIn(tag)) {
               return true;
            }
         }
      }

      return false;
   }

   public void clone(PlayerInventory other) {
      for(int i = 0; i < this.size(); ++i) {
         this.setStack(i, other.getStack(i));
      }

      this.selectedSlot = other.selectedSlot;
   }

   public void clear() {
      Iterator var1 = this.combinedInventory.iterator();

      while(var1.hasNext()) {
         List<ItemStack> list = (List)var1.next();
         list.clear();
      }

   }

   public void populateRecipeFinder(RecipeMatcher finder) {
      Iterator var2 = this.main.iterator();

      while(var2.hasNext()) {
         ItemStack itemStack = (ItemStack)var2.next();
         finder.addUnenchantedInput(itemStack);
      }

   }
}
