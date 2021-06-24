package net.minecraft.data.server.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Iterator;
import java.util.List;
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
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public class ShapelessRecipeJsonFactory implements CraftingRecipeJsonFactory {
   private final Item output;
   private final int outputCount;
   private final List<Ingredient> inputs = Lists.newArrayList();
   private final Advancement.Task builder = Advancement.Task.create();
   @Nullable
   private String group;

   public ShapelessRecipeJsonFactory(ItemConvertible output, int outputCount) {
      this.output = output.asItem();
      this.outputCount = outputCount;
   }

   public static ShapelessRecipeJsonFactory create(ItemConvertible output) {
      return new ShapelessRecipeJsonFactory(output, 1);
   }

   public static ShapelessRecipeJsonFactory create(ItemConvertible output, int outputCount) {
      return new ShapelessRecipeJsonFactory(output, outputCount);
   }

   public ShapelessRecipeJsonFactory input(Tag<Item> tag) {
      return this.input(Ingredient.fromTag(tag));
   }

   public ShapelessRecipeJsonFactory input(ItemConvertible itemProvider) {
      return this.input((ItemConvertible)itemProvider, 1);
   }

   public ShapelessRecipeJsonFactory input(ItemConvertible itemProvider, int size) {
      for(int i = 0; i < size; ++i) {
         this.input(Ingredient.ofItems(itemProvider));
      }

      return this;
   }

   public ShapelessRecipeJsonFactory input(Ingredient ingredient) {
      return this.input((Ingredient)ingredient, 1);
   }

   public ShapelessRecipeJsonFactory input(Ingredient ingredient, int size) {
      for(int i = 0; i < size; ++i) {
         this.inputs.add(ingredient);
      }

      return this;
   }

   public ShapelessRecipeJsonFactory criterion(String string, CriterionConditions criterionConditions) {
      this.builder.criterion(string, criterionConditions);
      return this;
   }

   public ShapelessRecipeJsonFactory group(@Nullable String string) {
      this.group = string;
      return this;
   }

   public Item getOutputItem() {
      return this.output;
   }

   public void offerTo(Consumer<RecipeJsonProvider> exporter, Identifier recipeId) {
      this.validate(recipeId);
      this.builder.parent(new Identifier("recipes/root")).criterion("has_the_recipe", (CriterionConditions)RecipeUnlockedCriterion.create(recipeId)).rewards(AdvancementRewards.Builder.recipe(recipeId)).criteriaMerger(CriterionMerger.OR);
      Item var10004 = this.output;
      int var10005 = this.outputCount;
      String var10006 = this.group == null ? "" : this.group;
      List var10007 = this.inputs;
      Advancement.Task var10008 = this.builder;
      String var10011 = recipeId.getNamespace();
      String var10012 = this.output.getGroup().getName();
      exporter.accept(new ShapelessRecipeJsonFactory.ShapelessRecipeJsonProvider(recipeId, var10004, var10005, var10006, var10007, var10008, new Identifier(var10011, "recipes/" + var10012 + "/" + recipeId.getPath())));
   }

   private void validate(Identifier recipeId) {
      if (this.builder.getCriteria().isEmpty()) {
         throw new IllegalStateException("No way of obtaining recipe " + recipeId);
      }
   }

   public static class ShapelessRecipeJsonProvider implements RecipeJsonProvider {
      private final Identifier recipeId;
      private final Item output;
      private final int count;
      private final String group;
      private final List<Ingredient> inputs;
      private final Advancement.Task builder;
      private final Identifier advancementId;

      public ShapelessRecipeJsonProvider(Identifier recipeId, Item output, int outputCount, String group, List<Ingredient> inputs, Advancement.Task builder, Identifier advancementId) {
         this.recipeId = recipeId;
         this.output = output;
         this.count = outputCount;
         this.group = group;
         this.inputs = inputs;
         this.builder = builder;
         this.advancementId = advancementId;
      }

      public void serialize(JsonObject json) {
         if (!this.group.isEmpty()) {
            json.addProperty("group", this.group);
         }

         JsonArray jsonArray = new JsonArray();
         Iterator var3 = this.inputs.iterator();

         while(var3.hasNext()) {
            Ingredient ingredient = (Ingredient)var3.next();
            jsonArray.add(ingredient.toJson());
         }

         json.add("ingredients", jsonArray);
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("item", Registry.ITEM.getId(this.output).toString());
         if (this.count > 1) {
            jsonObject.addProperty("count", (Number)this.count);
         }

         json.add("result", jsonObject);
      }

      public RecipeSerializer<?> getSerializer() {
         return RecipeSerializer.SHAPELESS;
      }

      public Identifier getRecipeId() {
         return this.recipeId;
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
