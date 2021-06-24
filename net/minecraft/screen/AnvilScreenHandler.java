package net.minecraft.screen;

import java.util.Iterator;
import java.util.Map;
import net.minecraft.block.AnvilBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.LiteralText;
import net.minecraft.world.WorldEvents;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AnvilScreenHandler extends ForgingScreenHandler {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final boolean field_30752 = false;
   public static final int field_30751 = 50;
   private int repairItemUsage;
   private String newItemName;
   private final Property levelCost;
   private static final int field_30753 = 0;
   private static final int field_30754 = 1;
   private static final int field_30755 = 1;
   private static final int field_30747 = 1;
   private static final int field_30748 = 2;
   private static final int field_30749 = 1;
   private static final int field_30750 = 1;

   public AnvilScreenHandler(int syncId, PlayerInventory inventory) {
      this(syncId, inventory, ScreenHandlerContext.EMPTY);
   }

   public AnvilScreenHandler(int syncId, PlayerInventory inventory, ScreenHandlerContext context) {
      super(ScreenHandlerType.ANVIL, syncId, inventory, context);
      this.levelCost = Property.create();
      this.addProperty(this.levelCost);
   }

   protected boolean canUse(BlockState state) {
      return state.isIn(BlockTags.ANVIL);
   }

   protected boolean canTakeOutput(PlayerEntity player, boolean present) {
      return (player.getAbilities().creativeMode || player.experienceLevel >= this.levelCost.get()) && this.levelCost.get() > 0;
   }

   protected void onTakeOutput(PlayerEntity player, ItemStack stack) {
      if (!player.getAbilities().creativeMode) {
         player.addExperienceLevels(-this.levelCost.get());
      }

      this.input.setStack(0, ItemStack.EMPTY);
      if (this.repairItemUsage > 0) {
         ItemStack itemStack = this.input.getStack(1);
         if (!itemStack.isEmpty() && itemStack.getCount() > this.repairItemUsage) {
            itemStack.decrement(this.repairItemUsage);
            this.input.setStack(1, itemStack);
         } else {
            this.input.setStack(1, ItemStack.EMPTY);
         }
      } else {
         this.input.setStack(1, ItemStack.EMPTY);
      }

      this.levelCost.set(0);
      this.context.run((world, blockPos) -> {
         BlockState blockState = world.getBlockState(blockPos);
         if (!player.getAbilities().creativeMode && blockState.isIn(BlockTags.ANVIL) && player.getRandom().nextFloat() < 0.12F) {
            BlockState blockState2 = AnvilBlock.getLandingState(blockState);
            if (blockState2 == null) {
               world.removeBlock(blockPos, false);
               world.syncWorldEvent(WorldEvents.ANVIL_DESTROYED, blockPos, 0);
               player.getInventory().offerOrDrop(stack);
            } else {
               world.setBlockState(blockPos, blockState2, Block.NOTIFY_LISTENERS);
               world.syncWorldEvent(WorldEvents.ANVIL_USED, blockPos, 0);
            }
         } else {
            world.syncWorldEvent(WorldEvents.ANVIL_USED, blockPos, 0);
         }

      });
   }

   public void updateResult() {
      ItemStack itemStack = this.input.getStack(0);
      this.levelCost.set(1);
      int i = 0;
      int j = 0;
      int k = 0;
      if (itemStack.isEmpty()) {
         this.output.setStack(0, ItemStack.EMPTY);
         this.levelCost.set(0);
      } else {
         ItemStack itemStack2 = itemStack.copy();
         ItemStack itemStack3 = this.input.getStack(1);
         Map<Enchantment, Integer> map = EnchantmentHelper.get(itemStack2);
         int j = j + itemStack.getRepairCost() + (itemStack3.isEmpty() ? 0 : itemStack3.getRepairCost());
         this.repairItemUsage = 0;
         if (!itemStack3.isEmpty()) {
            boolean bl = itemStack3.isOf(Items.ENCHANTED_BOOK) && !EnchantedBookItem.getEnchantmentNbt(itemStack3).isEmpty();
            int o;
            int p;
            int q;
            if (itemStack2.isDamageable() && itemStack2.getItem().canRepair(itemStack, itemStack3)) {
               o = Math.min(itemStack2.getDamage(), itemStack2.getMaxDamage() / 4);
               if (o <= 0) {
                  this.output.setStack(0, ItemStack.EMPTY);
                  this.levelCost.set(0);
                  return;
               }

               for(p = 0; o > 0 && p < itemStack3.getCount(); ++p) {
                  q = itemStack2.getDamage() - o;
                  itemStack2.setDamage(q);
                  ++i;
                  o = Math.min(itemStack2.getDamage(), itemStack2.getMaxDamage() / 4);
               }

               this.repairItemUsage = p;
            } else {
               if (!bl && (!itemStack2.isOf(itemStack3.getItem()) || !itemStack2.isDamageable())) {
                  this.output.setStack(0, ItemStack.EMPTY);
                  this.levelCost.set(0);
                  return;
               }

               if (itemStack2.isDamageable() && !bl) {
                  o = itemStack.getMaxDamage() - itemStack.getDamage();
                  p = itemStack3.getMaxDamage() - itemStack3.getDamage();
                  q = p + itemStack2.getMaxDamage() * 12 / 100;
                  int r = o + q;
                  int s = itemStack2.getMaxDamage() - r;
                  if (s < 0) {
                     s = 0;
                  }

                  if (s < itemStack2.getDamage()) {
                     itemStack2.setDamage(s);
                     i += 2;
                  }
               }

               Map<Enchantment, Integer> map2 = EnchantmentHelper.get(itemStack3);
               boolean bl2 = false;
               boolean bl3 = false;
               Iterator var24 = map2.keySet().iterator();

               label155:
               while(true) {
                  Enchantment enchantment;
                  do {
                     if (!var24.hasNext()) {
                        if (bl3 && !bl2) {
                           this.output.setStack(0, ItemStack.EMPTY);
                           this.levelCost.set(0);
                           return;
                        }
                        break label155;
                     }

                     enchantment = (Enchantment)var24.next();
                  } while(enchantment == null);

                  int t = (Integer)map.getOrDefault(enchantment, 0);
                  int u = (Integer)map2.get(enchantment);
                  u = t == u ? u + 1 : Math.max(u, t);
                  boolean bl4 = enchantment.isAcceptableItem(itemStack);
                  if (this.player.getAbilities().creativeMode || itemStack.isOf(Items.ENCHANTED_BOOK)) {
                     bl4 = true;
                  }

                  Iterator var17 = map.keySet().iterator();

                  while(var17.hasNext()) {
                     Enchantment enchantment2 = (Enchantment)var17.next();
                     if (enchantment2 != enchantment && !enchantment.canCombine(enchantment2)) {
                        bl4 = false;
                        ++i;
                     }
                  }

                  if (!bl4) {
                     bl3 = true;
                  } else {
                     bl2 = true;
                     if (u > enchantment.getMaxLevel()) {
                        u = enchantment.getMaxLevel();
                     }

                     map.put(enchantment, u);
                     int v = 0;
                     switch(enchantment.getRarity()) {
                     case COMMON:
                        v = 1;
                        break;
                     case UNCOMMON:
                        v = 2;
                        break;
                     case RARE:
                        v = 4;
                        break;
                     case VERY_RARE:
                        v = 8;
                     }

                     if (bl) {
                        v = Math.max(1, v / 2);
                     }

                     i += v * u;
                     if (itemStack.getCount() > 1) {
                        i = 40;
                     }
                  }
               }
            }
         }

         if (StringUtils.isBlank(this.newItemName)) {
            if (itemStack.hasCustomName()) {
               k = 1;
               i += k;
               itemStack2.removeCustomName();
            }
         } else if (!this.newItemName.equals(itemStack.getName().getString())) {
            k = 1;
            i += k;
            itemStack2.setCustomName(new LiteralText(this.newItemName));
         }

         this.levelCost.set(j + i);
         if (i <= 0) {
            itemStack2 = ItemStack.EMPTY;
         }

         if (k == i && k > 0 && this.levelCost.get() >= 40) {
            this.levelCost.set(39);
         }

         if (this.levelCost.get() >= 40 && !this.player.getAbilities().creativeMode) {
            itemStack2 = ItemStack.EMPTY;
         }

         if (!itemStack2.isEmpty()) {
            int w = itemStack2.getRepairCost();
            if (!itemStack3.isEmpty() && w < itemStack3.getRepairCost()) {
               w = itemStack3.getRepairCost();
            }

            if (k != i || k == 0) {
               w = getNextCost(w);
            }

            itemStack2.setRepairCost(w);
            EnchantmentHelper.set(map, itemStack2);
         }

         this.output.setStack(0, itemStack2);
         this.sendContentUpdates();
      }
   }

   public static int getNextCost(int cost) {
      return cost * 2 + 1;
   }

   public void setNewItemName(String newItemName) {
      this.newItemName = newItemName;
      if (this.getSlot(2).hasStack()) {
         ItemStack itemStack = this.getSlot(2).getStack();
         if (StringUtils.isBlank(newItemName)) {
            itemStack.removeCustomName();
         } else {
            itemStack.setCustomName(new LiteralText(this.newItemName));
         }
      }

      this.updateResult();
   }

   public int getLevelCost() {
      return this.levelCost.get();
   }
}
