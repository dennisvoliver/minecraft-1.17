package net.minecraft.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparators;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public final class Ingredient implements Predicate<ItemStack> {
   public static final Ingredient EMPTY = new Ingredient(Stream.empty());
   private final Ingredient.Entry[] entries;
   private ItemStack[] matchingStacks;
   private IntList ids;

   private Ingredient(Stream<? extends Ingredient.Entry> entries) {
      this.entries = (Ingredient.Entry[])entries.toArray((i) -> {
         return new Ingredient.Entry[i];
      });
   }

   public ItemStack[] getMatchingStacksClient() {
      this.cacheMatchingStacks();
      return this.matchingStacks;
   }

   private void cacheMatchingStacks() {
      if (this.matchingStacks == null) {
         this.matchingStacks = (ItemStack[])Arrays.stream(this.entries).flatMap((entry) -> {
            return entry.getStacks().stream();
         }).distinct().toArray((i) -> {
            return new ItemStack[i];
         });
      }

   }

   public boolean test(@Nullable ItemStack itemStack) {
      if (itemStack == null) {
         return false;
      } else {
         this.cacheMatchingStacks();
         if (this.matchingStacks.length == 0) {
            return itemStack.isEmpty();
         } else {
            ItemStack[] var2 = this.matchingStacks;
            int var3 = var2.length;

            for(int var4 = 0; var4 < var3; ++var4) {
               ItemStack itemStack2 = var2[var4];
               if (itemStack2.isOf(itemStack.getItem())) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public IntList getMatchingItemIds() {
      if (this.ids == null) {
         this.cacheMatchingStacks();
         this.ids = new IntArrayList(this.matchingStacks.length);
         ItemStack[] var1 = this.matchingStacks;
         int var2 = var1.length;

         for(int var3 = 0; var3 < var2; ++var3) {
            ItemStack itemStack = var1[var3];
            this.ids.add(RecipeMatcher.getItemId(itemStack));
         }

         this.ids.sort(IntComparators.NATURAL_COMPARATOR);
      }

      return this.ids;
   }

   public void write(PacketByteBuf buf) {
      this.cacheMatchingStacks();
      buf.writeCollection(Arrays.asList(this.matchingStacks), PacketByteBuf::writeItemStack);
   }

   public JsonElement toJson() {
      if (this.entries.length == 1) {
         return this.entries[0].toJson();
      } else {
         JsonArray jsonArray = new JsonArray();
         Ingredient.Entry[] var2 = this.entries;
         int var3 = var2.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            Ingredient.Entry entry = var2[var4];
            jsonArray.add((JsonElement)entry.toJson());
         }

         return jsonArray;
      }
   }

   public boolean isEmpty() {
      return this.entries.length == 0 && (this.matchingStacks == null || this.matchingStacks.length == 0) && (this.ids == null || this.ids.isEmpty());
   }

   private static Ingredient ofEntries(Stream<? extends Ingredient.Entry> entries) {
      Ingredient ingredient = new Ingredient(entries);
      return ingredient.entries.length == 0 ? EMPTY : ingredient;
   }

   public static Ingredient empty() {
      return EMPTY;
   }

   public static Ingredient ofItems(ItemConvertible... items) {
      return ofStacks(Arrays.stream(items).map(ItemStack::new));
   }

   public static Ingredient ofStacks(ItemStack... stacks) {
      return ofStacks(Arrays.stream(stacks));
   }

   public static Ingredient ofStacks(Stream<ItemStack> stacks) {
      return ofEntries(stacks.filter((stack) -> {
         return !stack.isEmpty();
      }).map(Ingredient.StackEntry::new));
   }

   public static Ingredient fromTag(Tag<Item> tag) {
      return ofEntries(Stream.of(new Ingredient.TagEntry(tag)));
   }

   public static Ingredient fromPacket(PacketByteBuf buf) {
      return ofEntries(buf.readList(PacketByteBuf::readItemStack).stream().map(Ingredient.StackEntry::new));
   }

   public static Ingredient fromJson(@Nullable JsonElement json) {
      if (json != null && !json.isJsonNull()) {
         if (json.isJsonObject()) {
            return ofEntries(Stream.of(entryFromJson(json.getAsJsonObject())));
         } else if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            if (jsonArray.size() == 0) {
               throw new JsonSyntaxException("Item array cannot be empty, at least one item must be defined");
            } else {
               return ofEntries(StreamSupport.stream(jsonArray.spliterator(), false).map((jsonElement) -> {
                  return entryFromJson(JsonHelper.asObject(jsonElement, "item"));
               }));
            }
         } else {
            throw new JsonSyntaxException("Expected item to be object or array of objects");
         }
      } else {
         throw new JsonSyntaxException("Item cannot be null");
      }
   }

   private static Ingredient.Entry entryFromJson(JsonObject json) {
      if (json.has("item") && json.has("tag")) {
         throw new JsonParseException("An ingredient entry is either a tag or an item, not both");
      } else if (json.has("item")) {
         Item item = ShapedRecipe.getItem(json);
         return new Ingredient.StackEntry(new ItemStack(item));
      } else if (json.has("tag")) {
         Identifier identifier = new Identifier(JsonHelper.getString(json, "tag"));
         Tag<Item> tag = ServerTagManagerHolder.getTagManager().getTag(Registry.ITEM_KEY, identifier, (identifierx) -> {
            return new JsonSyntaxException("Unknown item tag '" + identifierx + "'");
         });
         return new Ingredient.TagEntry(tag);
      } else {
         throw new JsonParseException("An ingredient entry needs either a tag or an item");
      }
   }

   private interface Entry {
      Collection<ItemStack> getStacks();

      JsonObject toJson();
   }

   private static class TagEntry implements Ingredient.Entry {
      private final Tag<Item> tag;

      TagEntry(Tag<Item> tag) {
         this.tag = tag;
      }

      public Collection<ItemStack> getStacks() {
         List<ItemStack> list = Lists.newArrayList();
         Iterator var2 = this.tag.values().iterator();

         while(var2.hasNext()) {
            Item item = (Item)var2.next();
            list.add(new ItemStack(item));
         }

         return list;
      }

      public JsonObject toJson() {
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("tag", ServerTagManagerHolder.getTagManager().getTagId(Registry.ITEM_KEY, this.tag, () -> {
            return new IllegalStateException("Unknown item tag");
         }).toString());
         return jsonObject;
      }
   }

   static class StackEntry implements Ingredient.Entry {
      private final ItemStack stack;

      StackEntry(ItemStack itemStack) {
         this.stack = itemStack;
      }

      public Collection<ItemStack> getStacks() {
         return Collections.singleton(this.stack);
      }

      public JsonObject toJson() {
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("item", Registry.ITEM.getId(this.stack.getItem()).toString());
         return jsonObject;
      }
   }
}
