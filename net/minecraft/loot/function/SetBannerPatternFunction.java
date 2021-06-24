package net.minecraft.loot.function;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Objects;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.DyeColor;
import net.minecraft.util.JsonHelper;

public class SetBannerPatternFunction extends ConditionalLootFunction {
   final List<Pair<BannerPattern, DyeColor>> patterns;
   final boolean append;

   SetBannerPatternFunction(LootCondition[] lootConditions, List<Pair<BannerPattern, DyeColor>> list, boolean bl) {
      super(lootConditions);
      this.patterns = list;
      this.append = bl;
   }

   protected ItemStack process(ItemStack stack, LootContext context) {
      NbtCompound nbtCompound = stack.getOrCreateSubTag("BlockEntityTag");
      BannerPattern.Patterns patterns = new BannerPattern.Patterns();
      List var10000 = this.patterns;
      Objects.requireNonNull(patterns);
      var10000.forEach(patterns::add);
      NbtList nbtList = patterns.toNbt();
      NbtList nbtList3;
      if (this.append) {
         nbtList3 = nbtCompound.getList("Patterns", 10).copy();
         nbtList3.addAll(nbtList);
      } else {
         nbtList3 = nbtList;
      }

      nbtCompound.put("Patterns", nbtList3);
      return stack;
   }

   public LootFunctionType getType() {
      return LootFunctionTypes.SET_BANNER_PATTERN;
   }

   public static SetBannerPatternFunction.Builder method_35531(boolean bl) {
      return new SetBannerPatternFunction.Builder(bl);
   }

   public static class Builder extends ConditionalLootFunction.Builder<SetBannerPatternFunction.Builder> {
      private final com.google.common.collect.ImmutableList.Builder<Pair<BannerPattern, DyeColor>> patterns = ImmutableList.builder();
      private final boolean append;

      Builder(boolean bl) {
         this.append = bl;
      }

      protected SetBannerPatternFunction.Builder getThisBuilder() {
         return this;
      }

      public LootFunction build() {
         return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
      }

      public SetBannerPatternFunction.Builder pattern(BannerPattern pattern, DyeColor color) {
         this.patterns.add((Object)Pair.of(pattern, color));
         return this;
      }
   }

   public static class Serializer extends ConditionalLootFunction.Serializer<SetBannerPatternFunction> {
      public void toJson(JsonObject jsonObject, SetBannerPatternFunction setBannerPatternFunction, JsonSerializationContext jsonSerializationContext) {
         super.toJson(jsonObject, (ConditionalLootFunction)setBannerPatternFunction, jsonSerializationContext);
         JsonArray jsonArray = new JsonArray();
         setBannerPatternFunction.patterns.forEach((pair) -> {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("pattern", ((BannerPattern)pair.getFirst()).getName());
            jsonObject.addProperty("color", ((DyeColor)pair.getSecond()).getName());
            jsonArray.add((JsonElement)jsonObject);
         });
         jsonObject.add("patterns", jsonArray);
         jsonObject.addProperty("append", setBannerPatternFunction.append);
      }

      public SetBannerPatternFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
         com.google.common.collect.ImmutableList.Builder<Pair<BannerPattern, DyeColor>> builder = ImmutableList.builder();
         JsonArray jsonArray = JsonHelper.getArray(jsonObject, "patterns");

         for(int i = 0; i < jsonArray.size(); ++i) {
            JsonObject jsonObject2 = JsonHelper.asObject(jsonArray.get(i), "pattern[" + i + "]");
            String string = JsonHelper.getString(jsonObject2, "pattern");
            BannerPattern bannerPattern = BannerPattern.byName(string);
            if (bannerPattern == null) {
               throw new JsonSyntaxException("Unknown pattern: " + string);
            }

            String string2 = JsonHelper.getString(jsonObject2, "color");
            DyeColor dyeColor = DyeColor.byName(string2, (DyeColor)null);
            if (dyeColor == null) {
               throw new JsonSyntaxException("Unknown color: " + string2);
            }

            builder.add((Object)Pair.of(bannerPattern, dyeColor));
         }

         boolean bl = JsonHelper.getBoolean(jsonObject, "append");
         return new SetBannerPatternFunction(lootConditions, builder.build(), bl);
      }
   }
}
