package net.minecraft.block.entity;

import java.util.Optional;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.CampfireCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.Clearable;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class CampfireBlockEntity extends BlockEntity implements Clearable {
   private static final int field_31330 = 2;
   private static final int field_31331 = 4;
   private final DefaultedList<ItemStack> itemsBeingCooked;
   private final int[] cookingTimes;
   private final int[] cookingTotalTimes;

   public CampfireBlockEntity(BlockPos pos, BlockState state) {
      super(BlockEntityType.CAMPFIRE, pos, state);
      this.itemsBeingCooked = DefaultedList.ofSize(4, ItemStack.EMPTY);
      this.cookingTimes = new int[4];
      this.cookingTotalTimes = new int[4];
   }

   public static void litServerTick(World world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
      boolean bl = false;

      for(int i = 0; i < campfire.itemsBeingCooked.size(); ++i) {
         ItemStack itemStack = (ItemStack)campfire.itemsBeingCooked.get(i);
         if (!itemStack.isEmpty()) {
            bl = true;
            int var10002 = campfire.cookingTimes[i]++;
            if (campfire.cookingTimes[i] >= campfire.cookingTotalTimes[i]) {
               Inventory inventory = new SimpleInventory(new ItemStack[]{itemStack});
               ItemStack itemStack2 = (ItemStack)world.getRecipeManager().getFirstMatch(RecipeType.CAMPFIRE_COOKING, inventory, world).map((campfireCookingRecipe) -> {
                  return campfireCookingRecipe.craft(inventory);
               }).orElse(itemStack);
               ItemScatterer.spawn(world, (double)pos.getX(), (double)pos.getY(), (double)pos.getZ(), itemStack2);
               campfire.itemsBeingCooked.set(i, ItemStack.EMPTY);
               world.updateListeners(pos, state, state, Block.NOTIFY_ALL);
            }
         }
      }

      if (bl) {
         markDirty(world, pos, state);
      }

   }

   public static void unlitServerTick(World world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
      boolean bl = false;

      for(int i = 0; i < campfire.itemsBeingCooked.size(); ++i) {
         if (campfire.cookingTimes[i] > 0) {
            bl = true;
            campfire.cookingTimes[i] = MathHelper.clamp((int)(campfire.cookingTimes[i] - 2), (int)0, (int)campfire.cookingTotalTimes[i]);
         }
      }

      if (bl) {
         markDirty(world, pos, state);
      }

   }

   public static void clientTick(World world, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
      Random random = world.random;
      int j;
      if (random.nextFloat() < 0.11F) {
         for(j = 0; j < random.nextInt(2) + 2; ++j) {
            CampfireBlock.spawnSmokeParticle(world, pos, (Boolean)state.get(CampfireBlock.SIGNAL_FIRE), false);
         }
      }

      j = ((Direction)state.get(CampfireBlock.FACING)).getHorizontal();

      for(int k = 0; k < campfire.itemsBeingCooked.size(); ++k) {
         if (!((ItemStack)campfire.itemsBeingCooked.get(k)).isEmpty() && random.nextFloat() < 0.2F) {
            Direction direction = Direction.fromHorizontal(Math.floorMod(k + j, 4));
            float f = 0.3125F;
            double d = (double)pos.getX() + 0.5D - (double)((float)direction.getOffsetX() * 0.3125F) + (double)((float)direction.rotateYClockwise().getOffsetX() * 0.3125F);
            double e = (double)pos.getY() + 0.5D;
            double g = (double)pos.getZ() + 0.5D - (double)((float)direction.getOffsetZ() * 0.3125F) + (double)((float)direction.rotateYClockwise().getOffsetZ() * 0.3125F);

            for(int l = 0; l < 4; ++l) {
               world.addParticle(ParticleTypes.SMOKE, d, e, g, 0.0D, 5.0E-4D, 0.0D);
            }
         }
      }

   }

   public DefaultedList<ItemStack> getItemsBeingCooked() {
      return this.itemsBeingCooked;
   }

   public void readNbt(NbtCompound nbt) {
      super.readNbt(nbt);
      this.itemsBeingCooked.clear();
      Inventories.readNbt(nbt, this.itemsBeingCooked);
      int[] js;
      if (nbt.contains("CookingTimes", 11)) {
         js = nbt.getIntArray("CookingTimes");
         System.arraycopy(js, 0, this.cookingTimes, 0, Math.min(this.cookingTotalTimes.length, js.length));
      }

      if (nbt.contains("CookingTotalTimes", 11)) {
         js = nbt.getIntArray("CookingTotalTimes");
         System.arraycopy(js, 0, this.cookingTotalTimes, 0, Math.min(this.cookingTotalTimes.length, js.length));
      }

   }

   public NbtCompound writeNbt(NbtCompound nbt) {
      this.saveInitialChunkData(nbt);
      nbt.putIntArray("CookingTimes", this.cookingTimes);
      nbt.putIntArray("CookingTotalTimes", this.cookingTotalTimes);
      return nbt;
   }

   private NbtCompound saveInitialChunkData(NbtCompound nbt) {
      super.writeNbt(nbt);
      Inventories.writeNbt(nbt, this.itemsBeingCooked, true);
      return nbt;
   }

   @Nullable
   public BlockEntityUpdateS2CPacket toUpdatePacket() {
      return new BlockEntityUpdateS2CPacket(this.pos, BlockEntityUpdateS2CPacket.CAMPFIRE, this.toInitialChunkDataNbt());
   }

   public NbtCompound toInitialChunkDataNbt() {
      return this.saveInitialChunkData(new NbtCompound());
   }

   public Optional<CampfireCookingRecipe> getRecipeFor(ItemStack item) {
      return this.itemsBeingCooked.stream().noneMatch(ItemStack::isEmpty) ? Optional.empty() : this.world.getRecipeManager().getFirstMatch(RecipeType.CAMPFIRE_COOKING, new SimpleInventory(new ItemStack[]{item}), this.world);
   }

   public boolean addItem(ItemStack item, int integer) {
      for(int i = 0; i < this.itemsBeingCooked.size(); ++i) {
         ItemStack itemStack = (ItemStack)this.itemsBeingCooked.get(i);
         if (itemStack.isEmpty()) {
            this.cookingTotalTimes[i] = integer;
            this.cookingTimes[i] = 0;
            this.itemsBeingCooked.set(i, item.split(1));
            this.updateListeners();
            return true;
         }
      }

      return false;
   }

   private void updateListeners() {
      this.markDirty();
      this.getWorld().updateListeners(this.getPos(), this.getCachedState(), this.getCachedState(), Block.NOTIFY_ALL);
   }

   public void clear() {
      this.itemsBeingCooked.clear();
   }

   public void spawnItemsBeingCooked() {
      if (this.world != null) {
         this.updateListeners();
      }

   }
}
