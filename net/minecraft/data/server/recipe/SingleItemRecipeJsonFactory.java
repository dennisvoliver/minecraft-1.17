package net.minecraft.data.server.recipe;

import com.google.gson.JsonObject;
import java.util.function.Consumer;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementRewards;
import net.minecraft.advancement.CriterionMerger;
import net.minecraft.advancement.criterion.CriterionConditions;
import net.minecraft.advancement.criterion.RecipeUnlockedCriterion;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public class SingleItemRecipeJsonFactory implements CraftingRecipeJsonFactory {
   private final Item output;
   private final Ingredient input;
   private final int count;
   private final Advancement.Task builder = Advancement.Task.create();
   @Nullable
   private String group;
   private final RecipeSerializer<?> serializer;

   public SingleItemRecipeJsonFactory(RecipeSerializer<?> serializer, Ingredient input, ItemConvertible output, int outputCount) {
      this.serializer = serializer;
      this.output = output.asItem();
      this.input = input;
      this.count = outputCount;
   }

   public static SingleItemRecipeJsonFactory createStonecutting(Ingredient input, ItemConvertible output) {
      return new SingleItemRecipeJsonFactory(RecipeSerializer.STONECUTTING, input, output, 1);
   }

   public static SingleItemRecipeJsonFactory createStonecutting(Ingredient input, ItemConvertible output, int outputCount) {
      return new SingleItemRecipeJsonFactory(RecipeSerializer.STONECUTTING, input, output, outputCount);
   }

   public SingleItemRecipeJsonFactory criterion(String string, CriterionConditions criterionConditions) {
      this.builder.criterion(string, criterionConditions);
      return this;
   }

   public SingleItemRecipeJsonFactory group(@Nullable String string) {
      this.group = string;
      return this;
   }

   public Item getOutputItem() {
      return this.output;
   }

   public void offerTo(Consumer<RecipeJsonProvider> exporter, Identifier recipeId) {
      this.validate(recipeId);
      this.builder.parent(new Identifier("recipes/root")).criterion("has_the_recipe", (CriterionConditions)RecipeUnlockedCriterion.create(recipeId)).rewards(AdvancementRewards.Builder.recipe(recipeId)).criteriaMerger(CriterionMerger.OR);
      RecipeSerializer var10004 = this.serializer;
      String var10005 = this.group == null ? "" : this.group;
      Ingredient var10006 = this.input;
      Item var10007 = this.output;
      int var10008 = this.count;
      Advancement.Task var10009 = this.builder;
      String var10012 = recipeId.getNamespace();
      String var10013 = this.output.getGroup().getName();
      exporter.accept(new SingleItemRecipeJsonFactory.SingleItemRecipeJsonProvider(recipeId, var10004, var10005, var10006, var10007, var10008, var10009, new Identifier(var10012, "recipes/" + var10013 + "/" + recipeId.getPath())));
   }

   private void validate(Identifier recipeId) {
      if (this.builder.getCriteria().isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + recipeId);
      }
   }

   public static class SingleItemRecipeJsonProvider implements RecipeJsonProvider {
      private final Identifier recipeId;
      private final String group;
      private final Ingredient input;
      private final Item output;
      private final int count;
      private final Advancement.Task builder;
      private final Identifier advancementId;
      private final RecipeSerializer<?> serializer;

      public SingleItemRecipeJsonProvider(Identifier recipeId, RecipeSerializer<?> serializer, String group, Ingredient input, Item output, int outputCount, Advancement.Task builder, Identifier advancementId) {
         this.recipeId = recipeId;
         this.serializer = serializer;
         this.group = group;
         this.input = input;
         this.output = output;
         this.count = outputCount;
         this.builder = builder;
         this.advancementId = advancementId;
      }

      public void serialize(JsonObject json) {
         if (!this.group.isEmpty()) {
            json.addProperty("group", this.group);
         }

         json.add("ingredient", this.input.toJson());
         json.addProperty("result", Registry.ITEM.getId(this.output).toString());
         json.addProperty("count", (Number)this.count);
      }

      public Identifier getRecipeId() {
         return this.recipeId;
      }

      public RecipeSerializer<?> getSerializer() {
         return this.serializer;
      }

      @Nullable
      public JsonObject toAdvancementJson() {
         return this.builder.toJson();
      }

      @Nullable
      public Identifier getAdvancementId() {
         return this.advancementId;
      }
   }
}
