package net.minecraft.screen;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeInputProvider;
import net.minecraft.recipe.RecipeMatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.book.RecipeBookCategory;
import net.minecraft.screen.slot.FurnaceFuelSlot;
import net.minecraft.screen.slot.FurnaceOutputSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.world.World;

public abstract class AbstractFurnaceScreenHandler extends AbstractRecipeScreenHandler<Inventory> {
   public static final int field_30738 = 0;
   public static final int field_30739 = 1;
   public static final int field_30740 = 2;
   public static final int field_30741 = 3;
   public static final int field_30742 = 4;
   private static final int field_30743 = 3;
   private static final int field_30744 = 30;
   private static final int field_30745 = 30;
   private static final int field_30746 = 39;
   private final Inventory inventory;
   private final PropertyDelegate propertyDelegate;
   protected final World world;
   private final RecipeType<? extends AbstractCookingRecipe> recipeType;
   private final RecipeBookCategory category;

   protected AbstractFurnaceScreenHandler(ScreenHandlerType<?> type, RecipeType<? extends AbstractCookingRecipe> recipeType, RecipeBookCategory recipeBookCategory, int i, PlayerInventory playerInventory) {
      this(type, recipeType, recipeBookCategory, i, playerInventory, new SimpleInventory(3), new ArrayPropertyDelegate(4));
   }

   protected AbstractFurnaceScreenHandler(ScreenHandlerType<?> type, RecipeType<? extends AbstractCookingRecipe> recipeType, RecipeBookCategory recipeBookCategory, int i, PlayerInventory playerInventory, Inventory inventory, PropertyDelegate propertyDelegate) {
      super(type, i);
      this.recipeType = recipeType;
      this.category = recipeBookCategory;
      checkSize(inventory, 3);
      checkDataCount(propertyDelegate, 4);
      this.inventory = inventory;
      this.propertyDelegate = propertyDelegate;
      this.world = playerInventory.player.world;
      this.addSlot(new Slot(inventory, 0, 56, 17));
      this.addSlot(new FurnaceFuelSlot(this, inventory, 1, 56, 53));
      this.addSlot(new FurnaceOutputSlot(playerInventory.player, inventory, 2, 116, 35));

      int l;
      for(l = 0; l < 3; ++l) {
         for(int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, 84 + l * 18));
         }
      }

      for(l = 0; l < 9; ++l) {
         this.addSlot(new Slot(playerInventory, l, 8 + l * 18, 142));
      }

      this.addProperties(propertyDelegate);
   }

   public void populateRecipeFinder(RecipeMatcher finder) {
      if (this.inventory instanceof RecipeInputProvider) {
         ((RecipeInputProvider)this.inventory).provideRecipeInputs(finder);
      }

   }

   public void clearCraftingSlots() {
      this.getSlot(0).setStack(ItemStack.EMPTY);
      this.getSlot(2).setStack(ItemStack.EMPTY);
   }

   public boolean matches(Recipe<? super Inventory> recipe) {
      return recipe.matches(this.inventory, this.world);
   }

   public int getCraftingResultSlotIndex() {
      return 2;
   }

   public int getCraftingWidth() {
      return 1;
   }

   public int getCraftingHeight() {
      return 1;
   }

   public int getCraftingSlotCount() {
      return 3;
   }

   public boolean canUse(PlayerEntity player) {
      return this.inventory.canPlayerUse(player);
   }

   public ItemStack transferSlot(PlayerEntity player, int index) {
      ItemStack itemStack = ItemStack.EMPTY;
      Slot slot = (Slot)this.slots.get(index);
      if (slot != null && slot.hasStack()) {
         ItemStack itemStack2 = slot.getStack();
         itemStack = itemStack2.copy();
         if (index == 2) {
            if (!this.insertItem(itemStack2, 3, 39, true)) {
               return ItemStack.EMPTY;
            }

            slot.onQuickTransfer(itemStack2, itemStack);
         } else if (index != 1 && index != 0) {
            if (this.isSmeltable(itemStack2)) {
               if (!this.insertItem(itemStack2, 0, 1, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (this.isFuel(itemStack2)) {
               if (!this.insertItem(itemStack2, 1, 2, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (index >= 3 && index < 30) {
               if (!this.insertItem(itemStack2, 30, 39, false)) {
                  return ItemStack.EMPTY;
               }
            } else if (index >= 30 && index < 39 && !this.insertItem(itemStack2, 3, 30, false)) {
               return ItemStack.EMPTY;
            }
         } else if (!this.insertItem(itemStack2, 3, 39, false)) {
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

   protected boolean isSmeltable(ItemStack itemStack) {
      return this.world.getRecipeManager().getFirstMatch(this.recipeType, new SimpleInventory(new ItemStack[]{itemStack}), this.world).isPresent();
   }

   protected boolean isFuel(ItemStack itemStack) {
      return AbstractFurnaceBlockEntity.canUseAsFuel(itemStack);
   }

   public int getCookProgress() {
      int i = this.propertyDelegate.get(2);
      int j = this.propertyDelegate.get(3);
      return j != 0 && i != 0 ? i * 24 / j : 0;
   }

   public int getFuelProgress() {
      int i = this.propertyDelegate.get(1);
      if (i == 0) {
         i = 200;
      }

      return this.propertyDelegate.get(0) * 13 / i;
   }

   public boolean isBurning() {
      return this.propertyDelegate.get(0) > 0;
   }

   public RecipeBookCategory getCategory() {
      return this.category;
   }

   public boolean canInsertIntoSlot(int index) {
      return index != 1;
   }
}
