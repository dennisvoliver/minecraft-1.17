package net.minecraft.data.server;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.criterion.CriterionConditions;
import net.minecraft.advancement.criterion.EnterBlockCriterion;
import net.minecraft.advancement.criterion.ImpossibleCriterion;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.data.DataCache;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.family.BlockFamilies;
import net.minecraft.data.family.BlockFamily;
import net.minecraft.data.server.recipe.ComplexRecipeJsonFactory;
import net.minecraft.data.server.recipe.CookingRecipeJsonFactory;
import net.minecraft.data.server.recipe.CraftingRecipeJsonFactory;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonFactory;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonFactory;
import net.minecraft.data.server.recipe.SingleItemRecipeJsonFactory;
import net.minecraft.data.server.recipe.SmithingRecipeJsonFactory;
import net.minecraft.item.HoneycombItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.predicate.NumberRange;
import net.minecraft.predicate.StatePredicate;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.item.ItemPredicate;
import net.minecraft.recipe.CookingRecipeSerializer;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.tag.ItemTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class RecipesProvider implements DataProvider {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private static final ImmutableList<ItemConvertible> COAL_ORES;
   private static final ImmutableList<ItemConvertible> IRON_ORES;
   private static final ImmutableList<ItemConvertible> COPPER_ORES;
   private static final ImmutableList<ItemConvertible> GOLD_ORES;
   private static final ImmutableList<ItemConvertible> DIAMOND_ORES;
   private static final ImmutableList<ItemConvertible> LAPIS_ORES;
   private static final ImmutableList<ItemConvertible> REDSTONE_ORES;
   private static final ImmutableList<ItemConvertible> EMERALD_ORES;
   private final DataGenerator root;
   private static final Map<BlockFamily.Variant, BiFunction<ItemConvertible, ItemConvertible, CraftingRecipeJsonFactory>> VARIANT_FACTORIES;

   public RecipesProvider(DataGenerator root) {
      this.root = root;
   }

   public void run(DataCache cache) {
      Path path = this.root.getOutput();
      Set<Identifier> set = Sets.newHashSet();
      generate((recipeJsonProvider) -> {
         if (!set.add(recipeJsonProvider.getRecipeId())) {
            throw new IllegalStateException("Duplicate recipe " + recipeJsonProvider.getRecipeId());
         } else {
            JsonObject var10001 = recipeJsonProvider.toJson();
            String var10003 = recipeJsonProvider.getRecipeId().getNamespace();
            saveRecipe(cache, var10001, path.resolve("data/" + var10003 + "/recipes/" + recipeJsonProvider.getRecipeId().getPath() + ".json"));
            JsonObject jsonObject = recipeJsonProvider.toAdvancementJson();
            if (jsonObject != null) {
               var10003 = recipeJsonProvider.getRecipeId().getNamespace();
               saveRecipeAdvancement(cache, jsonObject, path.resolve("data/" + var10003 + "/advancements/" + recipeJsonProvider.getAdvancementId().getPath() + ".json"));
            }

         }
      });
      saveRecipeAdvancement(cache, Advancement.Task.create().criterion("impossible", (CriterionConditions)(new ImpossibleCriterion.Conditions())).toJson(), path.resolve("data/minecraft/advancements/recipes/root.json"));
   }

   private static void saveRecipe(DataCache cache, JsonObject json, Path path) {
      try {
         String string = GSON.toJson((JsonElement)json);
         String string2 = SHA1.hashUnencodedChars(string).toString();
         if (!Objects.equals(cache.getOldSha1(path), string2) || !Files.exists(path, new LinkOption[0])) {
            Files.createDirectories(path.getParent());
            BufferedWriter bufferedWriter = Files.newBufferedWriter(path);

            try {
               bufferedWriter.write(string);
            } catch (Throwable var9) {
               if (bufferedWriter != null) {
                  try {
                     bufferedWriter.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (bufferedWriter != null) {
               bufferedWriter.close();
            }
         }

         cache.updateSha1(path, string2);
      } catch (IOException var10) {
         LOGGER.error((String)"Couldn't save recipe {}", (Object)path, (Object)var10);
      }

   }

   private static void saveRecipeAdvancement(DataCache cache, JsonObject json, Path path) {
      try {
         String string = GSON.toJson((JsonElement)json);
         String string2 = SHA1.hashUnencodedChars(string).toString();
         if (!Objects.equals(cache.getOldSha1(path), string2) || !Files.exists(path, new LinkOption[0])) {
            Files.createDirectories(path.getParent());
            BufferedWriter bufferedWriter = Files.newBufferedWriter(path);

            try {
               bufferedWriter.write(string);
            } catch (Throwable var9) {
               if (bufferedWriter != null) {
                  try {
                     bufferedWriter.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (bufferedWriter != null) {
               bufferedWriter.close();
            }
         }

         cache.updateSha1(path, string2);
      } catch (IOException var10) {
         LOGGER.error((String)"Couldn't save recipe advancement {}", (Object)path, (Object)var10);
      }

   }

   private static void generate(Consumer<RecipeJsonProvider> exporter) {
      BlockFamilies.getFamilies().filter(BlockFamily::shouldGenerateRecipes).forEach((family) -> {
         generateFamily(exporter, family);
      });
      offerPlanksRecipe2(exporter, Blocks.ACACIA_PLANKS, ItemTags.ACACIA_LOGS);
      offerPlanksRecipe(exporter, Blocks.BIRCH_PLANKS, ItemTags.BIRCH_LOGS);
      offerPlanksRecipe(exporter, Blocks.CRIMSON_PLANKS, ItemTags.CRIMSON_STEMS);
      offerPlanksRecipe2(exporter, Blocks.DARK_OAK_PLANKS, ItemTags.DARK_OAK_LOGS);
      offerPlanksRecipe(exporter, Blocks.JUNGLE_PLANKS, ItemTags.JUNGLE_LOGS);
      offerPlanksRecipe(exporter, Blocks.OAK_PLANKS, ItemTags.OAK_LOGS);
      offerPlanksRecipe(exporter, Blocks.SPRUCE_PLANKS, ItemTags.SPRUCE_LOGS);
      offerPlanksRecipe(exporter, Blocks.WARPED_PLANKS, ItemTags.WARPED_STEMS);
      offerBarkBlockRecipe(exporter, Blocks.ACACIA_WOOD, Blocks.ACACIA_LOG);
      offerBarkBlockRecipe(exporter, Blocks.BIRCH_WOOD, Blocks.BIRCH_LOG);
      offerBarkBlockRecipe(exporter, Blocks.DARK_OAK_WOOD, Blocks.DARK_OAK_LOG);
      offerBarkBlockRecipe(exporter, Blocks.JUNGLE_WOOD, Blocks.JUNGLE_LOG);
      offerBarkBlockRecipe(exporter, Blocks.OAK_WOOD, Blocks.OAK_LOG);
      offerBarkBlockRecipe(exporter, Blocks.SPRUCE_WOOD, Blocks.SPRUCE_LOG);
      offerBarkBlockRecipe(exporter, Blocks.CRIMSON_HYPHAE, Blocks.CRIMSON_STEM);
      offerBarkBlockRecipe(exporter, Blocks.WARPED_HYPHAE, Blocks.WARPED_STEM);
      offerBarkBlockRecipe(exporter, Blocks.STRIPPED_ACACIA_WOOD, Blocks.STRIPPED_ACACIA_LOG);
      offerBarkBlockRecipe(exporter, Blocks.STRIPPED_BIRCH_WOOD, Blocks.STRIPPED_BIRCH_LOG);
      offerBarkBlockRecipe(exporter, Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.STRIPPED_DARK_OAK_LOG);
      offerBarkBlockRecipe(exporter, Blocks.STRIPPED_JUNGLE_WOOD, Blocks.STRIPPED_JUNGLE_LOG);
      offerBarkBlockRecipe(exporter, Blocks.STRIPPED_OAK_WOOD, Blocks.STRIPPED_OAK_LOG);
      offerBarkBlockRecipe(exporter, Blocks.STRIPPED_SPRUCE_WOOD, Blocks.STRIPPED_SPRUCE_LOG);
      offerBarkBlockRecipe(exporter, Blocks.STRIPPED_CRIMSON_HYPHAE, Blocks.STRIPPED_CRIMSON_STEM);
      offerBarkBlockRecipe(exporter, Blocks.STRIPPED_WARPED_HYPHAE, Blocks.STRIPPED_WARPED_STEM);
      offerBoatRecipe(exporter, Items.ACACIA_BOAT, Blocks.ACACIA_PLANKS);
      offerBoatRecipe(exporter, Items.BIRCH_BOAT, Blocks.BIRCH_PLANKS);
      offerBoatRecipe(exporter, Items.DARK_OAK_BOAT, Blocks.DARK_OAK_PLANKS);
      offerBoatRecipe(exporter, Items.JUNGLE_BOAT, Blocks.JUNGLE_PLANKS);
      offerBoatRecipe(exporter, Items.OAK_BOAT, Blocks.OAK_PLANKS);
      offerBoatRecipe(exporter, Items.SPRUCE_BOAT, Blocks.SPRUCE_PLANKS);
      offerWoolDyeingRecipe(exporter, Blocks.BLACK_WOOL, Items.BLACK_DYE);
      offerCarpetRecipe(exporter, Blocks.BLACK_CARPET, Blocks.BLACK_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.BLACK_CARPET, Items.BLACK_DYE);
      offerBedRecipe(exporter, Items.BLACK_BED, Blocks.BLACK_WOOL);
      offerBedDyeingRecipe(exporter, Items.BLACK_BED, Items.BLACK_DYE);
      offerBannerRecipe(exporter, Items.BLACK_BANNER, Blocks.BLACK_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.BLUE_WOOL, Items.BLUE_DYE);
      offerCarpetRecipe(exporter, Blocks.BLUE_CARPET, Blocks.BLUE_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.BLUE_CARPET, Items.BLUE_DYE);
      offerBedRecipe(exporter, Items.BLUE_BED, Blocks.BLUE_WOOL);
      offerBedDyeingRecipe(exporter, Items.BLUE_BED, Items.BLUE_DYE);
      offerBannerRecipe(exporter, Items.BLUE_BANNER, Blocks.BLUE_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.BROWN_WOOL, Items.BROWN_DYE);
      offerCarpetRecipe(exporter, Blocks.BROWN_CARPET, Blocks.BROWN_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.BROWN_CARPET, Items.BROWN_DYE);
      offerBedRecipe(exporter, Items.BROWN_BED, Blocks.BROWN_WOOL);
      offerBedDyeingRecipe(exporter, Items.BROWN_BED, Items.BROWN_DYE);
      offerBannerRecipe(exporter, Items.BROWN_BANNER, Blocks.BROWN_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.CYAN_WOOL, Items.CYAN_DYE);
      offerCarpetRecipe(exporter, Blocks.CYAN_CARPET, Blocks.CYAN_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.CYAN_CARPET, Items.CYAN_DYE);
      offerBedRecipe(exporter, Items.CYAN_BED, Blocks.CYAN_WOOL);
      offerBedDyeingRecipe(exporter, Items.CYAN_BED, Items.CYAN_DYE);
      offerBannerRecipe(exporter, Items.CYAN_BANNER, Blocks.CYAN_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.GRAY_WOOL, Items.GRAY_DYE);
      offerCarpetRecipe(exporter, Blocks.GRAY_CARPET, Blocks.GRAY_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.GRAY_CARPET, Items.GRAY_DYE);
      offerBedRecipe(exporter, Items.GRAY_BED, Blocks.GRAY_WOOL);
      offerBedDyeingRecipe(exporter, Items.GRAY_BED, Items.GRAY_DYE);
      offerBannerRecipe(exporter, Items.GRAY_BANNER, Blocks.GRAY_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.GREEN_WOOL, Items.GREEN_DYE);
      offerCarpetRecipe(exporter, Blocks.GREEN_CARPET, Blocks.GREEN_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.GREEN_CARPET, Items.GREEN_DYE);
      offerBedRecipe(exporter, Items.GREEN_BED, Blocks.GREEN_WOOL);
      offerBedDyeingRecipe(exporter, Items.GREEN_BED, Items.GREEN_DYE);
      offerBannerRecipe(exporter, Items.GREEN_BANNER, Blocks.GREEN_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.LIGHT_BLUE_WOOL, Items.LIGHT_BLUE_DYE);
      offerCarpetRecipe(exporter, Blocks.LIGHT_BLUE_CARPET, Blocks.LIGHT_BLUE_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.LIGHT_BLUE_CARPET, Items.LIGHT_BLUE_DYE);
      offerBedRecipe(exporter, Items.LIGHT_BLUE_BED, Blocks.LIGHT_BLUE_WOOL);
      offerBedDyeingRecipe(exporter, Items.LIGHT_BLUE_BED, Items.LIGHT_BLUE_DYE);
      offerBannerRecipe(exporter, Items.LIGHT_BLUE_BANNER, Blocks.LIGHT_BLUE_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.LIGHT_GRAY_WOOL, Items.LIGHT_GRAY_DYE);
      offerCarpetRecipe(exporter, Blocks.LIGHT_GRAY_CARPET, Blocks.LIGHT_GRAY_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.LIGHT_GRAY_CARPET, Items.LIGHT_GRAY_DYE);
      offerBedRecipe(exporter, Items.LIGHT_GRAY_BED, Blocks.LIGHT_GRAY_WOOL);
      offerBedDyeingRecipe(exporter, Items.LIGHT_GRAY_BED, Items.LIGHT_GRAY_DYE);
      offerBannerRecipe(exporter, Items.LIGHT_GRAY_BANNER, Blocks.LIGHT_GRAY_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.LIME_WOOL, Items.LIME_DYE);
      offerCarpetRecipe(exporter, Blocks.LIME_CARPET, Blocks.LIME_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.LIME_CARPET, Items.LIME_DYE);
      offerBedRecipe(exporter, Items.LIME_BED, Blocks.LIME_WOOL);
      offerBedDyeingRecipe(exporter, Items.LIME_BED, Items.LIME_DYE);
      offerBannerRecipe(exporter, Items.LIME_BANNER, Blocks.LIME_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.MAGENTA_WOOL, Items.MAGENTA_DYE);
      offerCarpetRecipe(exporter, Blocks.MAGENTA_CARPET, Blocks.MAGENTA_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.MAGENTA_CARPET, Items.MAGENTA_DYE);
      offerBedRecipe(exporter, Items.MAGENTA_BED, Blocks.MAGENTA_WOOL);
      offerBedDyeingRecipe(exporter, Items.MAGENTA_BED, Items.MAGENTA_DYE);
      offerBannerRecipe(exporter, Items.MAGENTA_BANNER, Blocks.MAGENTA_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.ORANGE_WOOL, Items.ORANGE_DYE);
      offerCarpetRecipe(exporter, Blocks.ORANGE_CARPET, Blocks.ORANGE_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.ORANGE_CARPET, Items.ORANGE_DYE);
      offerBedRecipe(exporter, Items.ORANGE_BED, Blocks.ORANGE_WOOL);
      offerBedDyeingRecipe(exporter, Items.ORANGE_BED, Items.ORANGE_DYE);
      offerBannerRecipe(exporter, Items.ORANGE_BANNER, Blocks.ORANGE_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.PINK_WOOL, Items.PINK_DYE);
      offerCarpetRecipe(exporter, Blocks.PINK_CARPET, Blocks.PINK_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.PINK_CARPET, Items.PINK_DYE);
      offerBedRecipe(exporter, Items.PINK_BED, Blocks.PINK_WOOL);
      offerBedDyeingRecipe(exporter, Items.PINK_BED, Items.PINK_DYE);
      offerBannerRecipe(exporter, Items.PINK_BANNER, Blocks.PINK_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.PURPLE_WOOL, Items.PURPLE_DYE);
      offerCarpetRecipe(exporter, Blocks.PURPLE_CARPET, Blocks.PURPLE_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.PURPLE_CARPET, Items.PURPLE_DYE);
      offerBedRecipe(exporter, Items.PURPLE_BED, Blocks.PURPLE_WOOL);
      offerBedDyeingRecipe(exporter, Items.PURPLE_BED, Items.PURPLE_DYE);
      offerBannerRecipe(exporter, Items.PURPLE_BANNER, Blocks.PURPLE_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.RED_WOOL, Items.RED_DYE);
      offerCarpetRecipe(exporter, Blocks.RED_CARPET, Blocks.RED_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.RED_CARPET, Items.RED_DYE);
      offerBedRecipe(exporter, Items.RED_BED, Blocks.RED_WOOL);
      offerBedDyeingRecipe(exporter, Items.RED_BED, Items.RED_DYE);
      offerBannerRecipe(exporter, Items.RED_BANNER, Blocks.RED_WOOL);
      offerCarpetRecipe(exporter, Blocks.WHITE_CARPET, Blocks.WHITE_WOOL);
      offerBedRecipe(exporter, Items.WHITE_BED, Blocks.WHITE_WOOL);
      offerBannerRecipe(exporter, Items.WHITE_BANNER, Blocks.WHITE_WOOL);
      offerWoolDyeingRecipe(exporter, Blocks.YELLOW_WOOL, Items.YELLOW_DYE);
      offerCarpetRecipe(exporter, Blocks.YELLOW_CARPET, Blocks.YELLOW_WOOL);
      offerCarpetDyeingRecipe(exporter, Blocks.YELLOW_CARPET, Items.YELLOW_DYE);
      offerBedRecipe(exporter, Items.YELLOW_BED, Blocks.YELLOW_WOOL);
      offerBedDyeingRecipe(exporter, Items.YELLOW_BED, Items.YELLOW_DYE);
      offerBannerRecipe(exporter, Items.YELLOW_BANNER, Blocks.YELLOW_WOOL);
      offerCarpetRecipe(exporter, Blocks.MOSS_CARPET, Blocks.MOSS_BLOCK);
      offerStainedGlassDyeingRecipe(exporter, Blocks.BLACK_STAINED_GLASS, Items.BLACK_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.BLACK_STAINED_GLASS_PANE, Blocks.BLACK_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.BLACK_STAINED_GLASS_PANE, Items.BLACK_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.BLUE_STAINED_GLASS, Items.BLUE_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.BLUE_STAINED_GLASS_PANE, Blocks.BLUE_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.BLUE_STAINED_GLASS_PANE, Items.BLUE_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.BROWN_STAINED_GLASS, Items.BROWN_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.BROWN_STAINED_GLASS_PANE, Blocks.BROWN_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.BROWN_STAINED_GLASS_PANE, Items.BROWN_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.CYAN_STAINED_GLASS, Items.CYAN_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.CYAN_STAINED_GLASS_PANE, Blocks.CYAN_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.CYAN_STAINED_GLASS_PANE, Items.CYAN_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.GRAY_STAINED_GLASS, Items.GRAY_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.GRAY_STAINED_GLASS_PANE, Blocks.GRAY_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.GRAY_STAINED_GLASS_PANE, Items.GRAY_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.GREEN_STAINED_GLASS, Items.GREEN_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.GREEN_STAINED_GLASS_PANE, Blocks.GREEN_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.GREEN_STAINED_GLASS_PANE, Items.GREEN_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.LIGHT_BLUE_STAINED_GLASS, Items.LIGHT_BLUE_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Blocks.LIGHT_BLUE_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.LIGHT_BLUE_STAINED_GLASS_PANE, Items.LIGHT_BLUE_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.LIGHT_GRAY_STAINED_GLASS, Items.LIGHT_GRAY_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Blocks.LIGHT_GRAY_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.LIGHT_GRAY_STAINED_GLASS_PANE, Items.LIGHT_GRAY_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.LIME_STAINED_GLASS, Items.LIME_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.LIME_STAINED_GLASS_PANE, Blocks.LIME_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.LIME_STAINED_GLASS_PANE, Items.LIME_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.MAGENTA_STAINED_GLASS, Items.MAGENTA_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.MAGENTA_STAINED_GLASS_PANE, Blocks.MAGENTA_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.MAGENTA_STAINED_GLASS_PANE, Items.MAGENTA_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.ORANGE_STAINED_GLASS, Items.ORANGE_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.ORANGE_STAINED_GLASS_PANE, Blocks.ORANGE_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.ORANGE_STAINED_GLASS_PANE, Items.ORANGE_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.PINK_STAINED_GLASS, Items.PINK_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.PINK_STAINED_GLASS_PANE, Blocks.PINK_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.PINK_STAINED_GLASS_PANE, Items.PINK_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.PURPLE_STAINED_GLASS, Items.PURPLE_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.PURPLE_STAINED_GLASS_PANE, Blocks.PURPLE_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.PURPLE_STAINED_GLASS_PANE, Items.PURPLE_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.RED_STAINED_GLASS, Items.RED_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.RED_STAINED_GLASS_PANE, Blocks.RED_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.RED_STAINED_GLASS_PANE, Items.RED_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.WHITE_STAINED_GLASS, Items.WHITE_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.WHITE_STAINED_GLASS_PANE, Blocks.WHITE_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.WHITE_STAINED_GLASS_PANE, Items.WHITE_DYE);
      offerStainedGlassDyeingRecipe(exporter, Blocks.YELLOW_STAINED_GLASS, Items.YELLOW_DYE);
      offerStainedGlassPaneRecipe(exporter, Blocks.YELLOW_STAINED_GLASS_PANE, Blocks.YELLOW_STAINED_GLASS);
      offerStainedGlassPaneDyeingRecipe(exporter, Blocks.YELLOW_STAINED_GLASS_PANE, Items.YELLOW_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.BLACK_TERRACOTTA, Items.BLACK_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.BLUE_TERRACOTTA, Items.BLUE_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.BROWN_TERRACOTTA, Items.BROWN_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.CYAN_TERRACOTTA, Items.CYAN_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.GRAY_TERRACOTTA, Items.GRAY_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.GREEN_TERRACOTTA, Items.GREEN_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.LIGHT_BLUE_TERRACOTTA, Items.LIGHT_BLUE_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.LIGHT_GRAY_TERRACOTTA, Items.LIGHT_GRAY_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.LIME_TERRACOTTA, Items.LIME_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.MAGENTA_TERRACOTTA, Items.MAGENTA_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.ORANGE_TERRACOTTA, Items.ORANGE_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.PINK_TERRACOTTA, Items.PINK_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.PURPLE_TERRACOTTA, Items.PURPLE_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.RED_TERRACOTTA, Items.RED_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.WHITE_TERRACOTTA, Items.WHITE_DYE);
      offerTerracottaDyeingRecipe(exporter, Blocks.YELLOW_TERRACOTTA, Items.YELLOW_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.BLACK_CONCRETE_POWDER, Items.BLACK_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.BLUE_CONCRETE_POWDER, Items.BLUE_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.BROWN_CONCRETE_POWDER, Items.BROWN_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.CYAN_CONCRETE_POWDER, Items.CYAN_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.GRAY_CONCRETE_POWDER, Items.GRAY_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.GREEN_CONCRETE_POWDER, Items.GREEN_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.LIGHT_BLUE_CONCRETE_POWDER, Items.LIGHT_BLUE_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.LIGHT_GRAY_CONCRETE_POWDER, Items.LIGHT_GRAY_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.LIME_CONCRETE_POWDER, Items.LIME_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.MAGENTA_CONCRETE_POWDER, Items.MAGENTA_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.ORANGE_CONCRETE_POWDER, Items.ORANGE_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.PINK_CONCRETE_POWDER, Items.PINK_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.PURPLE_CONCRETE_POWDER, Items.PURPLE_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.RED_CONCRETE_POWDER, Items.RED_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.WHITE_CONCRETE_POWDER, Items.WHITE_DYE);
      offerConcretePowderDyeingRecipe(exporter, Blocks.YELLOW_CONCRETE_POWDER, Items.YELLOW_DYE);
      ShapedRecipeJsonFactory.create(Items.CANDLE).input('S', (ItemConvertible)Items.STRING).input('H', (ItemConvertible)Items.HONEYCOMB).pattern("S").pattern("H").criterion("has_string", conditionsFromItem(Items.STRING)).criterion("has_honeycomb", conditionsFromItem(Items.HONEYCOMB)).offerTo(exporter);
      offerCandleDyeingRecipe(exporter, Blocks.BLACK_CANDLE, Items.BLACK_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.BLUE_CANDLE, Items.BLUE_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.BROWN_CANDLE, Items.BROWN_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.CYAN_CANDLE, Items.CYAN_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.GRAY_CANDLE, Items.GRAY_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.GREEN_CANDLE, Items.GREEN_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.LIGHT_BLUE_CANDLE, Items.LIGHT_BLUE_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.LIGHT_GRAY_CANDLE, Items.LIGHT_GRAY_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.LIME_CANDLE, Items.LIME_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.MAGENTA_CANDLE, Items.MAGENTA_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.ORANGE_CANDLE, Items.ORANGE_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.PINK_CANDLE, Items.PINK_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.PURPLE_CANDLE, Items.PURPLE_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.RED_CANDLE, Items.RED_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.WHITE_CANDLE, Items.WHITE_DYE);
      offerCandleDyeingRecipe(exporter, Blocks.YELLOW_CANDLE, Items.YELLOW_DYE);
      ShapedRecipeJsonFactory.create(Blocks.ACTIVATOR_RAIL, 6).input('#', (ItemConvertible)Blocks.REDSTONE_TORCH).input('S', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("XSX").pattern("X#X").pattern("XSX").criterion("has_rail", conditionsFromItem(Blocks.RAIL)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Blocks.ANDESITE, 2).input((ItemConvertible)Blocks.DIORITE).input((ItemConvertible)Blocks.COBBLESTONE).criterion("has_stone", conditionsFromItem(Blocks.DIORITE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.ANVIL).input('I', (ItemConvertible)Blocks.IRON_BLOCK).input('i', (ItemConvertible)Items.IRON_INGOT).pattern("III").pattern(" i ").pattern("iii").criterion("has_iron_block", conditionsFromItem(Blocks.IRON_BLOCK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.ARMOR_STAND).input('/', (ItemConvertible)Items.STICK).input('_', (ItemConvertible)Blocks.SMOOTH_STONE_SLAB).pattern("///").pattern(" / ").pattern("/_/").criterion("has_stone_slab", conditionsFromItem(Blocks.SMOOTH_STONE_SLAB)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.ARROW, 4).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.FLINT).input('Y', (ItemConvertible)Items.FEATHER).pattern("X").pattern("#").pattern("Y").criterion("has_feather", conditionsFromItem(Items.FEATHER)).criterion("has_flint", conditionsFromItem(Items.FLINT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.BARREL, 1).input('P', (Tag)ItemTags.PLANKS).input('S', (Tag)ItemTags.WOODEN_SLABS).pattern("PSP").pattern("P P").pattern("PSP").criterion("has_planks", conditionsFromTag(ItemTags.PLANKS)).criterion("has_wood_slab", conditionsFromTag(ItemTags.WOODEN_SLABS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.BEACON).input('S', (ItemConvertible)Items.NETHER_STAR).input('G', (ItemConvertible)Blocks.GLASS).input('O', (ItemConvertible)Blocks.OBSIDIAN).pattern("GGG").pattern("GSG").pattern("OOO").criterion("has_nether_star", conditionsFromItem(Items.NETHER_STAR)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.BEEHIVE).input('P', (Tag)ItemTags.PLANKS).input('H', (ItemConvertible)Items.HONEYCOMB).pattern("PPP").pattern("HHH").pattern("PPP").criterion("has_honeycomb", conditionsFromItem(Items.HONEYCOMB)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.BEETROOT_SOUP).input((ItemConvertible)Items.BOWL).input((ItemConvertible)Items.BEETROOT, 6).criterion("has_beetroot", conditionsFromItem(Items.BEETROOT)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.BLACK_DYE).input((ItemConvertible)Items.INK_SAC).group("black_dye").criterion("has_ink_sac", conditionsFromItem(Items.INK_SAC)).offerTo(exporter);
      offerSingleOutputShapelessRecipe(exporter, Items.BLACK_DYE, Blocks.WITHER_ROSE, "black_dye");
      ShapelessRecipeJsonFactory.create(Items.BLAZE_POWDER, 2).input((ItemConvertible)Items.BLAZE_ROD).criterion("has_blaze_rod", conditionsFromItem(Items.BLAZE_ROD)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.BLUE_DYE).input((ItemConvertible)Items.LAPIS_LAZULI).group("blue_dye").criterion("has_lapis_lazuli", conditionsFromItem(Items.LAPIS_LAZULI)).offerTo(exporter);
      offerSingleOutputShapelessRecipe(exporter, Items.BLUE_DYE, Blocks.CORNFLOWER, "blue_dye");
      ShapedRecipeJsonFactory.create(Blocks.BLUE_ICE).input('#', (ItemConvertible)Blocks.PACKED_ICE).pattern("###").pattern("###").pattern("###").criterion("has_packed_ice", conditionsFromItem(Blocks.PACKED_ICE)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.BONE_MEAL, 3).input((ItemConvertible)Items.BONE).group("bonemeal").criterion("has_bone", conditionsFromItem(Items.BONE)).offerTo(exporter);
      offerReversibleCompactingRecipesWithInputItemGroup(exporter, Items.BONE_MEAL, Items.BONE_BLOCK, "bone_meal_from_bone_block", "bonemeal");
      ShapelessRecipeJsonFactory.create(Items.BOOK).input((ItemConvertible)Items.PAPER, 3).input((ItemConvertible)Items.LEATHER).criterion("has_paper", conditionsFromItem(Items.PAPER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.BOOKSHELF).input('#', (Tag)ItemTags.PLANKS).input('X', (ItemConvertible)Items.BOOK).pattern("###").pattern("XXX").pattern("###").criterion("has_book", conditionsFromItem(Items.BOOK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.BOW).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.STRING).pattern(" #X").pattern("# X").pattern(" #X").criterion("has_string", conditionsFromItem(Items.STRING)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.BOWL, 4).input('#', (Tag)ItemTags.PLANKS).pattern("# #").pattern(" # ").criterion("has_brown_mushroom", conditionsFromItem(Blocks.BROWN_MUSHROOM)).criterion("has_red_mushroom", conditionsFromItem(Blocks.RED_MUSHROOM)).criterion("has_mushroom_stew", conditionsFromItem(Items.MUSHROOM_STEW)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.BREAD).input('#', (ItemConvertible)Items.WHEAT).pattern("###").criterion("has_wheat", conditionsFromItem(Items.WHEAT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.BREWING_STAND).input('B', (ItemConvertible)Items.BLAZE_ROD).input('#', (Tag)ItemTags.STONE_CRAFTING_MATERIALS).pattern(" B ").pattern("###").criterion("has_blaze_rod", conditionsFromItem(Items.BLAZE_ROD)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.BRICKS).input('#', (ItemConvertible)Items.BRICK).pattern("##").pattern("##").criterion("has_brick", conditionsFromItem(Items.BRICK)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.BROWN_DYE).input((ItemConvertible)Items.COCOA_BEANS).group("brown_dye").criterion("has_cocoa_beans", conditionsFromItem(Items.COCOA_BEANS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.BUCKET).input('#', (ItemConvertible)Items.IRON_INGOT).pattern("# #").pattern(" # ").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.CAKE).input('A', (ItemConvertible)Items.MILK_BUCKET).input('B', (ItemConvertible)Items.SUGAR).input('C', (ItemConvertible)Items.WHEAT).input('E', (ItemConvertible)Items.EGG).pattern("AAA").pattern("BEB").pattern("CCC").criterion("has_egg", conditionsFromItem(Items.EGG)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.CAMPFIRE).input('L', (Tag)ItemTags.LOGS).input('S', (ItemConvertible)Items.STICK).input('C', (Tag)ItemTags.COALS).pattern(" S ").pattern("SCS").pattern("LLL").criterion("has_stick", conditionsFromItem(Items.STICK)).criterion("has_coal", conditionsFromTag(ItemTags.COALS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.CARROT_ON_A_STICK).input('#', (ItemConvertible)Items.FISHING_ROD).input('X', (ItemConvertible)Items.CARROT).pattern("# ").pattern(" X").criterion("has_carrot", conditionsFromItem(Items.CARROT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.WARPED_FUNGUS_ON_A_STICK).input('#', (ItemConvertible)Items.FISHING_ROD).input('X', (ItemConvertible)Items.WARPED_FUNGUS).pattern("# ").pattern(" X").criterion("has_warped_fungus", conditionsFromItem(Items.WARPED_FUNGUS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.CAULDRON).input('#', (ItemConvertible)Items.IRON_INGOT).pattern("# #").pattern("# #").pattern("###").criterion("has_water_bucket", conditionsFromItem(Items.WATER_BUCKET)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.COMPOSTER).input('#', (Tag)ItemTags.WOODEN_SLABS).pattern("# #").pattern("# #").pattern("###").criterion("has_wood_slab", conditionsFromTag(ItemTags.WOODEN_SLABS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.CHEST).input('#', (Tag)ItemTags.PLANKS).pattern("###").pattern("# #").pattern("###").criterion("has_lots_of_items", new InventoryChangedCriterion.Conditions(EntityPredicate.Extended.EMPTY, NumberRange.IntRange.atLeast(10), NumberRange.IntRange.ANY, NumberRange.IntRange.ANY, new ItemPredicate[0])).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.CHEST_MINECART).input('A', (ItemConvertible)Blocks.CHEST).input('B', (ItemConvertible)Items.MINECART).pattern("A").pattern("B").criterion("has_minecart", conditionsFromItem(Items.MINECART)).offerTo(exporter);
      createChiseledBlockRecipe(Blocks.CHISELED_QUARTZ_BLOCK, Ingredient.ofItems(Blocks.QUARTZ_SLAB)).criterion("has_chiseled_quartz_block", conditionsFromItem(Blocks.CHISELED_QUARTZ_BLOCK)).criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK)).criterion("has_quartz_pillar", conditionsFromItem(Blocks.QUARTZ_PILLAR)).offerTo(exporter);
      createChiseledBlockRecipe(Blocks.CHISELED_STONE_BRICKS, Ingredient.ofItems(Blocks.STONE_BRICK_SLAB)).criterion("has_tag", conditionsFromTag(ItemTags.STONE_BRICKS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.CLAY).input('#', (ItemConvertible)Items.CLAY_BALL).pattern("##").pattern("##").criterion("has_clay_ball", conditionsFromItem(Items.CLAY_BALL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.CLOCK).input('#', (ItemConvertible)Items.GOLD_INGOT).input('X', (ItemConvertible)Items.REDSTONE).pattern(" # ").pattern("#X#").pattern(" # ").criterion("has_redstone", conditionsFromItem(Items.REDSTONE)).offerTo(exporter);
      offerReversibleCompactingRecipes(exporter, Items.COAL, Items.COAL_BLOCK);
      ShapedRecipeJsonFactory.create(Blocks.COARSE_DIRT, 4).input('D', (ItemConvertible)Blocks.DIRT).input('G', (ItemConvertible)Blocks.GRAVEL).pattern("DG").pattern("GD").criterion("has_gravel", conditionsFromItem(Blocks.GRAVEL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.COMPARATOR).input('#', (ItemConvertible)Blocks.REDSTONE_TORCH).input('X', (ItemConvertible)Items.QUARTZ).input('I', (ItemConvertible)Blocks.STONE).pattern(" # ").pattern("#X#").pattern("III").criterion("has_quartz", conditionsFromItem(Items.QUARTZ)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.COMPASS).input('#', (ItemConvertible)Items.IRON_INGOT).input('X', (ItemConvertible)Items.REDSTONE).pattern(" # ").pattern("#X#").pattern(" # ").criterion("has_redstone", conditionsFromItem(Items.REDSTONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.COOKIE, 8).input('#', (ItemConvertible)Items.WHEAT).input('X', (ItemConvertible)Items.COCOA_BEANS).pattern("#X#").criterion("has_cocoa", conditionsFromItem(Items.COCOA_BEANS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.CRAFTING_TABLE).input('#', (Tag)ItemTags.PLANKS).pattern("##").pattern("##").criterion("has_planks", conditionsFromTag(ItemTags.PLANKS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.CROSSBOW).input('~', (ItemConvertible)Items.STRING).input('#', (ItemConvertible)Items.STICK).input('&', (ItemConvertible)Items.IRON_INGOT).input('$', (ItemConvertible)Blocks.TRIPWIRE_HOOK).pattern("#&#").pattern("~$~").pattern(" # ").criterion("has_string", conditionsFromItem(Items.STRING)).criterion("has_stick", conditionsFromItem(Items.STICK)).criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).criterion("has_tripwire_hook", conditionsFromItem(Blocks.TRIPWIRE_HOOK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.LOOM).input('#', (Tag)ItemTags.PLANKS).input('@', (ItemConvertible)Items.STRING).pattern("@@").pattern("##").criterion("has_string", conditionsFromItem(Items.STRING)).offerTo(exporter);
      createChiseledBlockRecipe(Blocks.CHISELED_RED_SANDSTONE, Ingredient.ofItems(Blocks.RED_SANDSTONE_SLAB)).criterion("has_red_sandstone", conditionsFromItem(Blocks.RED_SANDSTONE)).criterion("has_chiseled_red_sandstone", conditionsFromItem(Blocks.CHISELED_RED_SANDSTONE)).criterion("has_cut_red_sandstone", conditionsFromItem(Blocks.CUT_RED_SANDSTONE)).offerTo(exporter);
      offerChiseledBlockRecipe(exporter, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE_SLAB);
      offerReversibleCompactingRecipesWithInputItemGroup(exporter, Items.COPPER_INGOT, Items.COPPER_BLOCK, method_36450(Items.COPPER_INGOT), getItemPath(Items.COPPER_INGOT));
      ShapelessRecipeJsonFactory.create(Items.COPPER_INGOT, 9).input((ItemConvertible)Blocks.WAXED_COPPER_BLOCK).group(getItemPath(Items.COPPER_INGOT)).criterion(hasItem(Blocks.WAXED_COPPER_BLOCK), conditionsFromItem(Blocks.WAXED_COPPER_BLOCK)).offerTo(exporter, convertBetween(Items.COPPER_INGOT, Blocks.WAXED_COPPER_BLOCK));
      offerWaxingRecipes(exporter);
      ShapelessRecipeJsonFactory.create(Items.CYAN_DYE, 2).input((ItemConvertible)Items.BLUE_DYE).input((ItemConvertible)Items.GREEN_DYE).criterion("has_green_dye", conditionsFromItem(Items.GREEN_DYE)).criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.DARK_PRISMARINE).input('S', (ItemConvertible)Items.PRISMARINE_SHARD).input('I', (ItemConvertible)Items.BLACK_DYE).pattern("SSS").pattern("SIS").pattern("SSS").criterion("has_prismarine_shard", conditionsFromItem(Items.PRISMARINE_SHARD)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.DAYLIGHT_DETECTOR).input('Q', (ItemConvertible)Items.QUARTZ).input('G', (ItemConvertible)Blocks.GLASS).input('W', Ingredient.fromTag(ItemTags.WOODEN_SLABS)).pattern("GGG").pattern("QQQ").pattern("WWW").criterion("has_quartz", conditionsFromItem(Items.QUARTZ)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.DEEPSLATE_BRICKS, 4).input('S', (ItemConvertible)Blocks.POLISHED_DEEPSLATE).pattern("SS").pattern("SS").criterion("has_polished_deepslate", conditionsFromItem(Blocks.POLISHED_DEEPSLATE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.DEEPSLATE_TILES, 4).input('S', (ItemConvertible)Blocks.DEEPSLATE_BRICKS).pattern("SS").pattern("SS").criterion("has_deepslate_bricks", conditionsFromItem(Blocks.DEEPSLATE_BRICKS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.DETECTOR_RAIL, 6).input('R', (ItemConvertible)Items.REDSTONE).input('#', (ItemConvertible)Blocks.STONE_PRESSURE_PLATE).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("X X").pattern("X#X").pattern("XRX").criterion("has_rail", conditionsFromItem(Blocks.RAIL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.DIAMOND_AXE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.DIAMOND).pattern("XX").pattern("X#").pattern(" #").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      offerReversibleCompactingRecipes(exporter, Items.DIAMOND, Items.DIAMOND_BLOCK);
      ShapedRecipeJsonFactory.create(Items.DIAMOND_BOOTS).input('X', (ItemConvertible)Items.DIAMOND).pattern("X X").pattern("X X").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.DIAMOND_CHESTPLATE).input('X', (ItemConvertible)Items.DIAMOND).pattern("X X").pattern("XXX").pattern("XXX").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.DIAMOND_HELMET).input('X', (ItemConvertible)Items.DIAMOND).pattern("XXX").pattern("X X").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.DIAMOND_HOE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.DIAMOND).pattern("XX").pattern(" #").pattern(" #").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.DIAMOND_LEGGINGS).input('X', (ItemConvertible)Items.DIAMOND).pattern("XXX").pattern("X X").pattern("X X").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.DIAMOND_PICKAXE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.DIAMOND).pattern("XXX").pattern(" # ").pattern(" # ").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.DIAMOND_SHOVEL).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.DIAMOND).pattern("X").pattern("#").pattern("#").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.DIAMOND_SWORD).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.DIAMOND).pattern("X").pattern("X").pattern("#").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.DIORITE, 2).input('Q', (ItemConvertible)Items.QUARTZ).input('C', (ItemConvertible)Blocks.COBBLESTONE).pattern("CQ").pattern("QC").criterion("has_quartz", conditionsFromItem(Items.QUARTZ)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.DISPENSER).input('R', (ItemConvertible)Items.REDSTONE).input('#', (ItemConvertible)Blocks.COBBLESTONE).input('X', (ItemConvertible)Items.BOW).pattern("###").pattern("#X#").pattern("#R#").criterion("has_bow", conditionsFromItem(Items.BOW)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.DRIPSTONE_BLOCK).input('#', (ItemConvertible)Items.POINTED_DRIPSTONE).pattern("##").pattern("##").group("pointed_dripstone").criterion("has_pointed_dripstone", conditionsFromItem(Items.POINTED_DRIPSTONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.DROPPER).input('R', (ItemConvertible)Items.REDSTONE).input('#', (ItemConvertible)Blocks.COBBLESTONE).pattern("###").pattern("# #").pattern("#R#").criterion("has_redstone", conditionsFromItem(Items.REDSTONE)).offerTo(exporter);
      offerReversibleCompactingRecipes(exporter, Items.EMERALD, Items.EMERALD_BLOCK);
      ShapedRecipeJsonFactory.create(Blocks.ENCHANTING_TABLE).input('B', (ItemConvertible)Items.BOOK).input('#', (ItemConvertible)Blocks.OBSIDIAN).input('D', (ItemConvertible)Items.DIAMOND).pattern(" B ").pattern("D#D").pattern("###").criterion("has_obsidian", conditionsFromItem(Blocks.OBSIDIAN)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.ENDER_CHEST).input('#', (ItemConvertible)Blocks.OBSIDIAN).input('E', (ItemConvertible)Items.ENDER_EYE).pattern("###").pattern("#E#").pattern("###").criterion("has_ender_eye", conditionsFromItem(Items.ENDER_EYE)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.ENDER_EYE).input((ItemConvertible)Items.ENDER_PEARL).input((ItemConvertible)Items.BLAZE_POWDER).criterion("has_blaze_powder", conditionsFromItem(Items.BLAZE_POWDER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.END_STONE_BRICKS, 4).input('#', (ItemConvertible)Blocks.END_STONE).pattern("##").pattern("##").criterion("has_end_stone", conditionsFromItem(Blocks.END_STONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.END_CRYSTAL).input('T', (ItemConvertible)Items.GHAST_TEAR).input('E', (ItemConvertible)Items.ENDER_EYE).input('G', (ItemConvertible)Blocks.GLASS).pattern("GGG").pattern("GEG").pattern("GTG").criterion("has_ender_eye", conditionsFromItem(Items.ENDER_EYE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.END_ROD, 4).input('#', (ItemConvertible)Items.POPPED_CHORUS_FRUIT).input('/', (ItemConvertible)Items.BLAZE_ROD).pattern("/").pattern("#").criterion("has_chorus_fruit_popped", conditionsFromItem(Items.POPPED_CHORUS_FRUIT)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.FERMENTED_SPIDER_EYE).input((ItemConvertible)Items.SPIDER_EYE).input((ItemConvertible)Blocks.BROWN_MUSHROOM).input((ItemConvertible)Items.SUGAR).criterion("has_spider_eye", conditionsFromItem(Items.SPIDER_EYE)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.FIRE_CHARGE, 3).input((ItemConvertible)Items.GUNPOWDER).input((ItemConvertible)Items.BLAZE_POWDER).input(Ingredient.ofItems(Items.COAL, Items.CHARCOAL)).criterion("has_blaze_powder", conditionsFromItem(Items.BLAZE_POWDER)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.FIREWORK_ROCKET, 3).input((ItemConvertible)Items.GUNPOWDER).input((ItemConvertible)Items.PAPER).criterion("has_gunpowder", conditionsFromItem(Items.GUNPOWDER)).offerTo(exporter, "firework_rocket_simple");
      ShapedRecipeJsonFactory.create(Items.FISHING_ROD).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.STRING).pattern("  #").pattern(" #X").pattern("# X").criterion("has_string", conditionsFromItem(Items.STRING)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.FLINT_AND_STEEL).input((ItemConvertible)Items.IRON_INGOT).input((ItemConvertible)Items.FLINT).criterion("has_flint", conditionsFromItem(Items.FLINT)).criterion("has_obsidian", conditionsFromItem(Blocks.OBSIDIAN)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.FLOWER_POT).input('#', (ItemConvertible)Items.BRICK).pattern("# #").pattern(" # ").criterion("has_brick", conditionsFromItem(Items.BRICK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.FURNACE).input('#', (Tag)ItemTags.STONE_CRAFTING_MATERIALS).pattern("###").pattern("# #").pattern("###").criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_CRAFTING_MATERIALS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.FURNACE_MINECART).input('A', (ItemConvertible)Blocks.FURNACE).input('B', (ItemConvertible)Items.MINECART).pattern("A").pattern("B").criterion("has_minecart", conditionsFromItem(Items.MINECART)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GLASS_BOTTLE, 3).input('#', (ItemConvertible)Blocks.GLASS).pattern("# #").pattern(" # ").criterion("has_glass", conditionsFromItem(Blocks.GLASS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.GLASS_PANE, 16).input('#', (ItemConvertible)Blocks.GLASS).pattern("###").pattern("###").criterion("has_glass", conditionsFromItem(Blocks.GLASS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.GLOWSTONE).input('#', (ItemConvertible)Items.GLOWSTONE_DUST).pattern("##").pattern("##").criterion("has_glowstone_dust", conditionsFromItem(Items.GLOWSTONE_DUST)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.GLOW_ITEM_FRAME).input((ItemConvertible)Items.ITEM_FRAME).input((ItemConvertible)Items.GLOW_INK_SAC).criterion("has_item_frame", conditionsFromItem(Items.ITEM_FRAME)).criterion("has_glow_ink_sac", conditionsFromItem(Items.GLOW_INK_SAC)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_APPLE).input('#', (ItemConvertible)Items.GOLD_INGOT).input('X', (ItemConvertible)Items.APPLE).pattern("###").pattern("#X#").pattern("###").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_AXE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("XX").pattern("X#").pattern(" #").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_BOOTS).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("X X").pattern("X X").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_CARROT).input('#', (ItemConvertible)Items.GOLD_NUGGET).input('X', (ItemConvertible)Items.CARROT).pattern("###").pattern("#X#").pattern("###").criterion("has_gold_nugget", conditionsFromItem(Items.GOLD_NUGGET)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_CHESTPLATE).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("X X").pattern("XXX").pattern("XXX").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_HELMET).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("XXX").pattern("X X").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_HOE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("XX").pattern(" #").pattern(" #").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_LEGGINGS).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("XXX").pattern("X X").pattern("X X").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_PICKAXE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("XXX").pattern(" # ").pattern(" # ").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.POWERED_RAIL, 6).input('R', (ItemConvertible)Items.REDSTONE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("X X").pattern("X#X").pattern("XRX").criterion("has_rail", conditionsFromItem(Blocks.RAIL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_SHOVEL).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("X").pattern("#").pattern("#").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GOLDEN_SWORD).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.GOLD_INGOT).pattern("X").pattern("X").pattern("#").criterion("has_gold_ingot", conditionsFromItem(Items.GOLD_INGOT)).offerTo(exporter);
      offerReversibleCompactingRecipesWithInputItemGroup(exporter, Items.GOLD_INGOT, Items.GOLD_BLOCK, "gold_ingot_from_gold_block", "gold_ingot");
      offerReversibleCompactingRecipesWithCompactedItemGroup(exporter, Items.GOLD_NUGGET, Items.GOLD_INGOT, "gold_ingot_from_nuggets", "gold_ingot");
      ShapelessRecipeJsonFactory.create(Blocks.GRANITE).input((ItemConvertible)Blocks.DIORITE).input((ItemConvertible)Items.QUARTZ).criterion("has_quartz", conditionsFromItem(Items.QUARTZ)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.GRAY_DYE, 2).input((ItemConvertible)Items.BLACK_DYE).input((ItemConvertible)Items.WHITE_DYE).criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE)).criterion("has_black_dye", conditionsFromItem(Items.BLACK_DYE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.HAY_BLOCK).input('#', (ItemConvertible)Items.WHEAT).pattern("###").pattern("###").pattern("###").criterion("has_wheat", conditionsFromItem(Items.WHEAT)).offerTo(exporter);
      createPressurePlateRecipe(exporter, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Items.IRON_INGOT);
      ShapelessRecipeJsonFactory.create(Items.HONEY_BOTTLE, 4).input((ItemConvertible)Items.HONEY_BLOCK).input((ItemConvertible)Items.GLASS_BOTTLE, 4).criterion("has_honey_block", conditionsFromItem(Blocks.HONEY_BLOCK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.HONEY_BLOCK, 1).input('S', (ItemConvertible)Items.HONEY_BOTTLE).pattern("SS").pattern("SS").criterion("has_honey_bottle", conditionsFromItem(Items.HONEY_BOTTLE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.HONEYCOMB_BLOCK).input('H', (ItemConvertible)Items.HONEYCOMB).pattern("HH").pattern("HH").criterion("has_honeycomb", conditionsFromItem(Items.HONEYCOMB)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.HOPPER).input('C', (ItemConvertible)Blocks.CHEST).input('I', (ItemConvertible)Items.IRON_INGOT).pattern("I I").pattern("ICI").pattern(" I ").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.HOPPER_MINECART).input('A', (ItemConvertible)Blocks.HOPPER).input('B', (ItemConvertible)Items.MINECART).pattern("A").pattern("B").criterion("has_minecart", conditionsFromItem(Items.MINECART)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.IRON_AXE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("XX").pattern("X#").pattern(" #").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.IRON_BARS, 16).input('#', (ItemConvertible)Items.IRON_INGOT).pattern("###").pattern("###").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.IRON_BOOTS).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("X X").pattern("X X").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.IRON_CHESTPLATE).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("X X").pattern("XXX").pattern("XXX").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      createDoorRecipe(Blocks.IRON_DOOR, Ingredient.ofItems(Items.IRON_INGOT)).criterion(hasItem(Items.IRON_INGOT), conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.IRON_HELMET).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("XXX").pattern("X X").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.IRON_HOE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("XX").pattern(" #").pattern(" #").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      offerReversibleCompactingRecipesWithInputItemGroup(exporter, Items.IRON_INGOT, Items.IRON_BLOCK, "iron_ingot_from_iron_block", "iron_ingot");
      offerReversibleCompactingRecipesWithCompactedItemGroup(exporter, Items.IRON_NUGGET, Items.IRON_INGOT, "iron_ingot_from_nuggets", "iron_ingot");
      ShapedRecipeJsonFactory.create(Items.IRON_LEGGINGS).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("XXX").pattern("X X").pattern("X X").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.IRON_PICKAXE).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("XXX").pattern(" # ").pattern(" # ").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.IRON_SHOVEL).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("X").pattern("#").pattern("#").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.IRON_SWORD).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("X").pattern("X").pattern("#").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.IRON_TRAPDOOR).input('#', (ItemConvertible)Items.IRON_INGOT).pattern("##").pattern("##").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.ITEM_FRAME).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.LEATHER).pattern("###").pattern("#X#").pattern("###").criterion("has_leather", conditionsFromItem(Items.LEATHER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.JUKEBOX).input('#', (Tag)ItemTags.PLANKS).input('X', (ItemConvertible)Items.DIAMOND).pattern("###").pattern("#X#").pattern("###").criterion("has_diamond", conditionsFromItem(Items.DIAMOND)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.LADDER, 3).input('#', (ItemConvertible)Items.STICK).pattern("# #").pattern("###").pattern("# #").criterion("has_stick", conditionsFromItem(Items.STICK)).offerTo(exporter);
      offerReversibleCompactingRecipes(exporter, Items.LAPIS_LAZULI, Items.LAPIS_BLOCK);
      ShapedRecipeJsonFactory.create(Items.LEAD, 2).input('~', (ItemConvertible)Items.STRING).input('O', (ItemConvertible)Items.SLIME_BALL).pattern("~~ ").pattern("~O ").pattern("  ~").criterion("has_slime_ball", conditionsFromItem(Items.SLIME_BALL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.LEATHER).input('#', (ItemConvertible)Items.RABBIT_HIDE).pattern("##").pattern("##").criterion("has_rabbit_hide", conditionsFromItem(Items.RABBIT_HIDE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.LEATHER_BOOTS).input('X', (ItemConvertible)Items.LEATHER).pattern("X X").pattern("X X").criterion("has_leather", conditionsFromItem(Items.LEATHER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.LEATHER_CHESTPLATE).input('X', (ItemConvertible)Items.LEATHER).pattern("X X").pattern("XXX").pattern("XXX").criterion("has_leather", conditionsFromItem(Items.LEATHER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.LEATHER_HELMET).input('X', (ItemConvertible)Items.LEATHER).pattern("XXX").pattern("X X").criterion("has_leather", conditionsFromItem(Items.LEATHER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.LEATHER_LEGGINGS).input('X', (ItemConvertible)Items.LEATHER).pattern("XXX").pattern("X X").pattern("X X").criterion("has_leather", conditionsFromItem(Items.LEATHER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.LEATHER_HORSE_ARMOR).input('X', (ItemConvertible)Items.LEATHER).pattern("X X").pattern("XXX").pattern("X X").criterion("has_leather", conditionsFromItem(Items.LEATHER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.LECTERN).input('S', (Tag)ItemTags.WOODEN_SLABS).input('B', (ItemConvertible)Blocks.BOOKSHELF).pattern("SSS").pattern(" B ").pattern(" S ").criterion("has_book", conditionsFromItem(Items.BOOK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.LEVER).input('#', (ItemConvertible)Blocks.COBBLESTONE).input('X', (ItemConvertible)Items.STICK).pattern("X").pattern("#").criterion("has_cobblestone", conditionsFromItem(Blocks.COBBLESTONE)).offerTo(exporter);
      offerSingleOutputShapelessRecipe(exporter, Items.LIGHT_BLUE_DYE, Blocks.BLUE_ORCHID, "light_blue_dye");
      ShapelessRecipeJsonFactory.create(Items.LIGHT_BLUE_DYE, 2).input((ItemConvertible)Items.BLUE_DYE).input((ItemConvertible)Items.WHITE_DYE).group("light_blue_dye").criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE)).criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE)).offerTo(exporter, "light_blue_dye_from_blue_white_dye");
      offerSingleOutputShapelessRecipe(exporter, Items.LIGHT_GRAY_DYE, Blocks.AZURE_BLUET, "light_gray_dye");
      ShapelessRecipeJsonFactory.create(Items.LIGHT_GRAY_DYE, 2).input((ItemConvertible)Items.GRAY_DYE).input((ItemConvertible)Items.WHITE_DYE).group("light_gray_dye").criterion("has_gray_dye", conditionsFromItem(Items.GRAY_DYE)).criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE)).offerTo(exporter, "light_gray_dye_from_gray_white_dye");
      ShapelessRecipeJsonFactory.create(Items.LIGHT_GRAY_DYE, 3).input((ItemConvertible)Items.BLACK_DYE).input((ItemConvertible)Items.WHITE_DYE, 2).group("light_gray_dye").criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE)).criterion("has_black_dye", conditionsFromItem(Items.BLACK_DYE)).offerTo(exporter, "light_gray_dye_from_black_white_dye");
      offerSingleOutputShapelessRecipe(exporter, Items.LIGHT_GRAY_DYE, Blocks.OXEYE_DAISY, "light_gray_dye");
      offerSingleOutputShapelessRecipe(exporter, Items.LIGHT_GRAY_DYE, Blocks.WHITE_TULIP, "light_gray_dye");
      createPressurePlateRecipe(exporter, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, Items.GOLD_INGOT);
      ShapedRecipeJsonFactory.create(Blocks.LIGHTNING_ROD).input('#', (ItemConvertible)Items.COPPER_INGOT).pattern("#").pattern("#").pattern("#").criterion("has_copper_ingot", conditionsFromItem(Items.COPPER_INGOT)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.LIME_DYE, 2).input((ItemConvertible)Items.GREEN_DYE).input((ItemConvertible)Items.WHITE_DYE).criterion("has_green_dye", conditionsFromItem(Items.GREEN_DYE)).criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.JACK_O_LANTERN).input('A', (ItemConvertible)Blocks.CARVED_PUMPKIN).input('B', (ItemConvertible)Blocks.TORCH).pattern("A").pattern("B").criterion("has_carved_pumpkin", conditionsFromItem(Blocks.CARVED_PUMPKIN)).offerTo(exporter);
      offerSingleOutputShapelessRecipe(exporter, Items.MAGENTA_DYE, Blocks.ALLIUM, "magenta_dye");
      ShapelessRecipeJsonFactory.create(Items.MAGENTA_DYE, 4).input((ItemConvertible)Items.BLUE_DYE).input((ItemConvertible)Items.RED_DYE, 2).input((ItemConvertible)Items.WHITE_DYE).group("magenta_dye").criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE)).criterion("has_rose_red", conditionsFromItem(Items.RED_DYE)).criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE)).offerTo(exporter, "magenta_dye_from_blue_red_white_dye");
      ShapelessRecipeJsonFactory.create(Items.MAGENTA_DYE, 3).input((ItemConvertible)Items.BLUE_DYE).input((ItemConvertible)Items.RED_DYE).input((ItemConvertible)Items.PINK_DYE).group("magenta_dye").criterion("has_pink_dye", conditionsFromItem(Items.PINK_DYE)).criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE)).criterion("has_red_dye", conditionsFromItem(Items.RED_DYE)).offerTo(exporter, "magenta_dye_from_blue_red_pink");
      offerShapelessRecipe(exporter, Items.MAGENTA_DYE, Blocks.LILAC, "magenta_dye", 2);
      ShapelessRecipeJsonFactory.create(Items.MAGENTA_DYE, 2).input((ItemConvertible)Items.PURPLE_DYE).input((ItemConvertible)Items.PINK_DYE).group("magenta_dye").criterion("has_pink_dye", conditionsFromItem(Items.PINK_DYE)).criterion("has_purple_dye", conditionsFromItem(Items.PURPLE_DYE)).offerTo(exporter, "magenta_dye_from_purple_and_pink");
      ShapedRecipeJsonFactory.create(Blocks.MAGMA_BLOCK).input('#', (ItemConvertible)Items.MAGMA_CREAM).pattern("##").pattern("##").criterion("has_magma_cream", conditionsFromItem(Items.MAGMA_CREAM)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.MAGMA_CREAM).input((ItemConvertible)Items.BLAZE_POWDER).input((ItemConvertible)Items.SLIME_BALL).criterion("has_blaze_powder", conditionsFromItem(Items.BLAZE_POWDER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.MAP).input('#', (ItemConvertible)Items.PAPER).input('X', (ItemConvertible)Items.COMPASS).pattern("###").pattern("#X#").pattern("###").criterion("has_compass", conditionsFromItem(Items.COMPASS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.MELON).input('M', (ItemConvertible)Items.MELON_SLICE).pattern("MMM").pattern("MMM").pattern("MMM").criterion("has_melon", conditionsFromItem(Items.MELON_SLICE)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.MELON_SEEDS).input((ItemConvertible)Items.MELON_SLICE).criterion("has_melon", conditionsFromItem(Items.MELON_SLICE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.MINECART).input('#', (ItemConvertible)Items.IRON_INGOT).pattern("# #").pattern("###").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Blocks.MOSSY_COBBLESTONE).input((ItemConvertible)Blocks.COBBLESTONE).input((ItemConvertible)Blocks.VINE).group("mossy_cobblestone").criterion("has_vine", conditionsFromItem(Blocks.VINE)).offerTo(exporter, convertBetween(Blocks.MOSSY_COBBLESTONE, Blocks.VINE));
      ShapelessRecipeJsonFactory.create(Blocks.MOSSY_STONE_BRICKS).input((ItemConvertible)Blocks.STONE_BRICKS).input((ItemConvertible)Blocks.VINE).group("mossy_stone_bricks").criterion("has_vine", conditionsFromItem(Blocks.VINE)).offerTo(exporter, convertBetween(Blocks.MOSSY_STONE_BRICKS, Blocks.VINE));
      ShapelessRecipeJsonFactory.create(Blocks.MOSSY_COBBLESTONE).input((ItemConvertible)Blocks.COBBLESTONE).input((ItemConvertible)Blocks.MOSS_BLOCK).group("mossy_cobblestone").criterion("has_moss_block", conditionsFromItem(Blocks.MOSS_BLOCK)).offerTo(exporter, convertBetween(Blocks.MOSSY_COBBLESTONE, Blocks.MOSS_BLOCK));
      ShapelessRecipeJsonFactory.create(Blocks.MOSSY_STONE_BRICKS).input((ItemConvertible)Blocks.STONE_BRICKS).input((ItemConvertible)Blocks.MOSS_BLOCK).group("mossy_stone_bricks").criterion("has_moss_block", conditionsFromItem(Blocks.MOSS_BLOCK)).offerTo(exporter, convertBetween(Blocks.MOSSY_STONE_BRICKS, Blocks.MOSS_BLOCK));
      ShapelessRecipeJsonFactory.create(Items.MUSHROOM_STEW).input((ItemConvertible)Blocks.BROWN_MUSHROOM).input((ItemConvertible)Blocks.RED_MUSHROOM).input((ItemConvertible)Items.BOWL).criterion("has_mushroom_stew", conditionsFromItem(Items.MUSHROOM_STEW)).criterion("has_bowl", conditionsFromItem(Items.BOWL)).criterion("has_brown_mushroom", conditionsFromItem(Blocks.BROWN_MUSHROOM)).criterion("has_red_mushroom", conditionsFromItem(Blocks.RED_MUSHROOM)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.NETHER_BRICKS).input('N', (ItemConvertible)Items.NETHER_BRICK).pattern("NN").pattern("NN").criterion("has_netherbrick", conditionsFromItem(Items.NETHER_BRICK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.NETHER_WART_BLOCK).input('#', (ItemConvertible)Items.NETHER_WART).pattern("###").pattern("###").pattern("###").criterion("has_nether_wart", conditionsFromItem(Items.NETHER_WART)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.NOTE_BLOCK).input('#', (Tag)ItemTags.PLANKS).input('X', (ItemConvertible)Items.REDSTONE).pattern("###").pattern("#X#").pattern("###").criterion("has_redstone", conditionsFromItem(Items.REDSTONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.OBSERVER).input('Q', (ItemConvertible)Items.QUARTZ).input('R', (ItemConvertible)Items.REDSTONE).input('#', (ItemConvertible)Blocks.COBBLESTONE).pattern("###").pattern("RRQ").pattern("###").criterion("has_quartz", conditionsFromItem(Items.QUARTZ)).offerTo(exporter);
      offerSingleOutputShapelessRecipe(exporter, Items.ORANGE_DYE, Blocks.ORANGE_TULIP, "orange_dye");
      ShapelessRecipeJsonFactory.create(Items.ORANGE_DYE, 2).input((ItemConvertible)Items.RED_DYE).input((ItemConvertible)Items.YELLOW_DYE).group("orange_dye").criterion("has_red_dye", conditionsFromItem(Items.RED_DYE)).criterion("has_yellow_dye", conditionsFromItem(Items.YELLOW_DYE)).offerTo(exporter, "orange_dye_from_red_yellow");
      ShapedRecipeJsonFactory.create(Items.PAINTING).input('#', (ItemConvertible)Items.STICK).input('X', Ingredient.fromTag(ItemTags.WOOL)).pattern("###").pattern("#X#").pattern("###").criterion("has_wool", conditionsFromTag(ItemTags.WOOL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.PAPER, 3).input('#', (ItemConvertible)Blocks.SUGAR_CANE).pattern("###").criterion("has_reeds", conditionsFromItem(Blocks.SUGAR_CANE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.QUARTZ_PILLAR, 2).input('#', (ItemConvertible)Blocks.QUARTZ_BLOCK).pattern("#").pattern("#").criterion("has_chiseled_quartz_block", conditionsFromItem(Blocks.CHISELED_QUARTZ_BLOCK)).criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK)).criterion("has_quartz_pillar", conditionsFromItem(Blocks.QUARTZ_PILLAR)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Blocks.PACKED_ICE).input((ItemConvertible)Blocks.ICE, 9).criterion("has_ice", conditionsFromItem(Blocks.ICE)).offerTo(exporter);
      offerShapelessRecipe(exporter, Items.PINK_DYE, Blocks.PEONY, "pink_dye", 2);
      offerSingleOutputShapelessRecipe(exporter, Items.PINK_DYE, Blocks.PINK_TULIP, "pink_dye");
      ShapelessRecipeJsonFactory.create(Items.PINK_DYE, 2).input((ItemConvertible)Items.RED_DYE).input((ItemConvertible)Items.WHITE_DYE).group("pink_dye").criterion("has_white_dye", conditionsFromItem(Items.WHITE_DYE)).criterion("has_red_dye", conditionsFromItem(Items.RED_DYE)).offerTo(exporter, "pink_dye_from_red_white_dye");
      ShapedRecipeJsonFactory.create(Blocks.PISTON).input('R', (ItemConvertible)Items.REDSTONE).input('#', (ItemConvertible)Blocks.COBBLESTONE).input('T', (Tag)ItemTags.PLANKS).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("TTT").pattern("#X#").pattern("#R#").criterion("has_redstone", conditionsFromItem(Items.REDSTONE)).offerTo(exporter);
      offerPolishedStoneRecipe(exporter, Blocks.POLISHED_BASALT, Blocks.BASALT);
      ShapedRecipeJsonFactory.create(Blocks.PRISMARINE).input('S', (ItemConvertible)Items.PRISMARINE_SHARD).pattern("SS").pattern("SS").criterion("has_prismarine_shard", conditionsFromItem(Items.PRISMARINE_SHARD)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.PRISMARINE_BRICKS).input('S', (ItemConvertible)Items.PRISMARINE_SHARD).pattern("SSS").pattern("SSS").pattern("SSS").criterion("has_prismarine_shard", conditionsFromItem(Items.PRISMARINE_SHARD)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.PUMPKIN_PIE).input((ItemConvertible)Blocks.PUMPKIN).input((ItemConvertible)Items.SUGAR).input((ItemConvertible)Items.EGG).criterion("has_carved_pumpkin", conditionsFromItem(Blocks.CARVED_PUMPKIN)).criterion("has_pumpkin", conditionsFromItem(Blocks.PUMPKIN)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.PUMPKIN_SEEDS, 4).input((ItemConvertible)Blocks.PUMPKIN).criterion("has_pumpkin", conditionsFromItem(Blocks.PUMPKIN)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.PURPLE_DYE, 2).input((ItemConvertible)Items.BLUE_DYE).input((ItemConvertible)Items.RED_DYE).criterion("has_blue_dye", conditionsFromItem(Items.BLUE_DYE)).criterion("has_red_dye", conditionsFromItem(Items.RED_DYE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SHULKER_BOX).input('#', (ItemConvertible)Blocks.CHEST).input('-', (ItemConvertible)Items.SHULKER_SHELL).pattern("-").pattern("#").pattern("-").criterion("has_shulker_shell", conditionsFromItem(Items.SHULKER_SHELL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.PURPUR_BLOCK, 4).input('F', (ItemConvertible)Items.POPPED_CHORUS_FRUIT).pattern("FF").pattern("FF").criterion("has_chorus_fruit_popped", conditionsFromItem(Items.POPPED_CHORUS_FRUIT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.PURPUR_PILLAR).input('#', (ItemConvertible)Blocks.PURPUR_SLAB).pattern("#").pattern("#").criterion("has_purpur_block", conditionsFromItem(Blocks.PURPUR_BLOCK)).offerTo(exporter);
      createSlabRecipe(Blocks.PURPUR_SLAB, Ingredient.ofItems(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR)).criterion("has_purpur_block", conditionsFromItem(Blocks.PURPUR_BLOCK)).offerTo(exporter);
      createStairsRecipe(Blocks.PURPUR_STAIRS, Ingredient.ofItems(Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR)).criterion("has_purpur_block", conditionsFromItem(Blocks.PURPUR_BLOCK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.QUARTZ_BLOCK).input('#', (ItemConvertible)Items.QUARTZ).pattern("##").pattern("##").criterion("has_quartz", conditionsFromItem(Items.QUARTZ)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.QUARTZ_BRICKS, 4).input('#', (ItemConvertible)Blocks.QUARTZ_BLOCK).pattern("##").pattern("##").criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK)).offerTo(exporter);
      createSlabRecipe(Blocks.QUARTZ_SLAB, Ingredient.ofItems(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR)).criterion("has_chiseled_quartz_block", conditionsFromItem(Blocks.CHISELED_QUARTZ_BLOCK)).criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK)).criterion("has_quartz_pillar", conditionsFromItem(Blocks.QUARTZ_PILLAR)).offerTo(exporter);
      createStairsRecipe(Blocks.QUARTZ_STAIRS, Ingredient.ofItems(Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK, Blocks.QUARTZ_PILLAR)).criterion("has_chiseled_quartz_block", conditionsFromItem(Blocks.CHISELED_QUARTZ_BLOCK)).criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK)).criterion("has_quartz_pillar", conditionsFromItem(Blocks.QUARTZ_PILLAR)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.RABBIT_STEW).input((ItemConvertible)Items.BAKED_POTATO).input((ItemConvertible)Items.COOKED_RABBIT).input((ItemConvertible)Items.BOWL).input((ItemConvertible)Items.CARROT).input((ItemConvertible)Blocks.BROWN_MUSHROOM).group("rabbit_stew").criterion("has_cooked_rabbit", conditionsFromItem(Items.COOKED_RABBIT)).offerTo(exporter, convertBetween(Items.RABBIT_STEW, Items.BROWN_MUSHROOM));
      ShapelessRecipeJsonFactory.create(Items.RABBIT_STEW).input((ItemConvertible)Items.BAKED_POTATO).input((ItemConvertible)Items.COOKED_RABBIT).input((ItemConvertible)Items.BOWL).input((ItemConvertible)Items.CARROT).input((ItemConvertible)Blocks.RED_MUSHROOM).group("rabbit_stew").criterion("has_cooked_rabbit", conditionsFromItem(Items.COOKED_RABBIT)).offerTo(exporter, convertBetween(Items.RABBIT_STEW, Items.RED_MUSHROOM));
      ShapedRecipeJsonFactory.create(Blocks.RAIL, 16).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.IRON_INGOT).pattern("X X").pattern("X#X").pattern("X X").criterion("has_minecart", conditionsFromItem(Items.MINECART)).offerTo(exporter);
      offerReversibleCompactingRecipes(exporter, Items.REDSTONE, Items.REDSTONE_BLOCK);
      ShapedRecipeJsonFactory.create(Blocks.REDSTONE_LAMP).input('R', (ItemConvertible)Items.REDSTONE).input('G', (ItemConvertible)Blocks.GLOWSTONE).pattern(" R ").pattern("RGR").pattern(" R ").criterion("has_glowstone", conditionsFromItem(Blocks.GLOWSTONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.REDSTONE_TORCH).input('#', (ItemConvertible)Items.STICK).input('X', (ItemConvertible)Items.REDSTONE).pattern("X").pattern("#").criterion("has_redstone", conditionsFromItem(Items.REDSTONE)).offerTo(exporter);
      offerSingleOutputShapelessRecipe(exporter, Items.RED_DYE, Items.BEETROOT, "red_dye");
      offerSingleOutputShapelessRecipe(exporter, Items.RED_DYE, Blocks.POPPY, "red_dye");
      offerShapelessRecipe(exporter, Items.RED_DYE, Blocks.ROSE_BUSH, "red_dye", 2);
      ShapelessRecipeJsonFactory.create(Items.RED_DYE).input((ItemConvertible)Blocks.RED_TULIP).group("red_dye").criterion("has_red_flower", conditionsFromItem(Blocks.RED_TULIP)).offerTo(exporter, "red_dye_from_tulip");
      ShapedRecipeJsonFactory.create(Blocks.RED_NETHER_BRICKS).input('W', (ItemConvertible)Items.NETHER_WART).input('N', (ItemConvertible)Items.NETHER_BRICK).pattern("NW").pattern("WN").criterion("has_nether_wart", conditionsFromItem(Items.NETHER_WART)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.RED_SANDSTONE).input('#', (ItemConvertible)Blocks.RED_SAND).pattern("##").pattern("##").criterion("has_sand", conditionsFromItem(Blocks.RED_SAND)).offerTo(exporter);
      createSlabRecipe(Blocks.RED_SANDSTONE_SLAB, Ingredient.ofItems(Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE)).criterion("has_red_sandstone", conditionsFromItem(Blocks.RED_SANDSTONE)).criterion("has_chiseled_red_sandstone", conditionsFromItem(Blocks.CHISELED_RED_SANDSTONE)).offerTo(exporter);
      createStairsRecipe(Blocks.RED_SANDSTONE_STAIRS, Ingredient.ofItems(Blocks.RED_SANDSTONE, Blocks.CHISELED_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE)).criterion("has_red_sandstone", conditionsFromItem(Blocks.RED_SANDSTONE)).criterion("has_chiseled_red_sandstone", conditionsFromItem(Blocks.CHISELED_RED_SANDSTONE)).criterion("has_cut_red_sandstone", conditionsFromItem(Blocks.CUT_RED_SANDSTONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.REPEATER).input('#', (ItemConvertible)Blocks.REDSTONE_TORCH).input('X', (ItemConvertible)Items.REDSTONE).input('I', (ItemConvertible)Blocks.STONE).pattern("#X#").pattern("III").criterion("has_redstone_torch", conditionsFromItem(Blocks.REDSTONE_TORCH)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SANDSTONE).input('#', (ItemConvertible)Blocks.SAND).pattern("##").pattern("##").criterion("has_sand", conditionsFromItem(Blocks.SAND)).offerTo(exporter);
      createSlabRecipe(Blocks.SANDSTONE_SLAB, Ingredient.ofItems(Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE)).criterion("has_sandstone", conditionsFromItem(Blocks.SANDSTONE)).criterion("has_chiseled_sandstone", conditionsFromItem(Blocks.CHISELED_SANDSTONE)).offerTo(exporter);
      createStairsRecipe(Blocks.SANDSTONE_STAIRS, Ingredient.ofItems(Blocks.SANDSTONE, Blocks.CHISELED_SANDSTONE, Blocks.CUT_SANDSTONE)).criterion("has_sandstone", conditionsFromItem(Blocks.SANDSTONE)).criterion("has_chiseled_sandstone", conditionsFromItem(Blocks.CHISELED_SANDSTONE)).criterion("has_cut_sandstone", conditionsFromItem(Blocks.CUT_SANDSTONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SEA_LANTERN).input('S', (ItemConvertible)Items.PRISMARINE_SHARD).input('C', (ItemConvertible)Items.PRISMARINE_CRYSTALS).pattern("SCS").pattern("CCC").pattern("SCS").criterion("has_prismarine_crystals", conditionsFromItem(Items.PRISMARINE_CRYSTALS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.SHEARS).input('#', (ItemConvertible)Items.IRON_INGOT).pattern(" #").pattern("# ").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.SHIELD).input('W', (Tag)ItemTags.PLANKS).input('o', (ItemConvertible)Items.IRON_INGOT).pattern("WoW").pattern("WWW").pattern(" W ").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      offerReversibleCompactingRecipes(exporter, Items.SLIME_BALL, Items.SLIME_BLOCK);
      offerCutCopperRecipe(exporter, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE);
      offerCutCopperRecipe(exporter, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE);
      ShapedRecipeJsonFactory.create(Blocks.SNOW_BLOCK).input('#', (ItemConvertible)Items.SNOWBALL).pattern("##").pattern("##").criterion("has_snowball", conditionsFromItem(Items.SNOWBALL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SNOW, 6).input('#', (ItemConvertible)Blocks.SNOW_BLOCK).pattern("###").criterion("has_snowball", conditionsFromItem(Items.SNOWBALL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SOUL_CAMPFIRE).input('L', (Tag)ItemTags.LOGS).input('S', (ItemConvertible)Items.STICK).input('#', (Tag)ItemTags.SOUL_FIRE_BASE_BLOCKS).pattern(" S ").pattern("S#S").pattern("LLL").criterion("has_stick", conditionsFromItem(Items.STICK)).criterion("has_soul_sand", conditionsFromTag(ItemTags.SOUL_FIRE_BASE_BLOCKS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.GLISTERING_MELON_SLICE).input('#', (ItemConvertible)Items.GOLD_NUGGET).input('X', (ItemConvertible)Items.MELON_SLICE).pattern("###").pattern("#X#").pattern("###").criterion("has_melon", conditionsFromItem(Items.MELON_SLICE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.SPECTRAL_ARROW, 2).input('#', (ItemConvertible)Items.GLOWSTONE_DUST).input('X', (ItemConvertible)Items.ARROW).pattern(" # ").pattern("#X#").pattern(" # ").criterion("has_glowstone_dust", conditionsFromItem(Items.GLOWSTONE_DUST)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.SPYGLASS).input('#', (ItemConvertible)Items.AMETHYST_SHARD).input('X', (ItemConvertible)Items.COPPER_INGOT).pattern(" # ").pattern(" X ").pattern(" X ").criterion("has_amethyst_shard", conditionsFromItem(Items.AMETHYST_SHARD)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.STICK, 4).input('#', (Tag)ItemTags.PLANKS).pattern("#").pattern("#").group("sticks").criterion("has_planks", conditionsFromTag(ItemTags.PLANKS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.STICK, 1).input('#', (ItemConvertible)Blocks.BAMBOO).pattern("#").pattern("#").group("sticks").criterion("has_bamboo", conditionsFromItem(Blocks.BAMBOO)).offerTo(exporter, "stick_from_bamboo_item");
      ShapedRecipeJsonFactory.create(Blocks.STICKY_PISTON).input('P', (ItemConvertible)Blocks.PISTON).input('S', (ItemConvertible)Items.SLIME_BALL).pattern("S").pattern("P").criterion("has_slime_ball", conditionsFromItem(Items.SLIME_BALL)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.STONE_BRICKS, 4).input('#', (ItemConvertible)Blocks.STONE).pattern("##").pattern("##").criterion("has_stone", conditionsFromItem(Blocks.STONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.STONE_AXE).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.STONE_TOOL_MATERIALS).pattern("XX").pattern("X#").pattern(" #").criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS)).offerTo(exporter);
      createSlabRecipe(Blocks.STONE_BRICK_SLAB, Ingredient.ofItems(Blocks.STONE_BRICKS)).criterion("has_stone_bricks", conditionsFromTag(ItemTags.STONE_BRICKS)).offerTo(exporter);
      createStairsRecipe(Blocks.STONE_BRICK_STAIRS, Ingredient.ofItems(Blocks.STONE_BRICKS)).criterion("has_stone_bricks", conditionsFromTag(ItemTags.STONE_BRICKS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.STONE_HOE).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.STONE_TOOL_MATERIALS).pattern("XX").pattern(" #").pattern(" #").criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.STONE_PICKAXE).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.STONE_TOOL_MATERIALS).pattern("XXX").pattern(" # ").pattern(" # ").criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.STONE_SHOVEL).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.STONE_TOOL_MATERIALS).pattern("X").pattern("#").pattern("#").criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS)).offerTo(exporter);
      offerSlabRecipe(exporter, Blocks.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE);
      ShapedRecipeJsonFactory.create(Items.STONE_SWORD).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.STONE_TOOL_MATERIALS).pattern("X").pattern("X").pattern("#").criterion("has_cobblestone", conditionsFromTag(ItemTags.STONE_TOOL_MATERIALS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.WHITE_WOOL).input('#', (ItemConvertible)Items.STRING).pattern("##").pattern("##").criterion("has_string", conditionsFromItem(Items.STRING)).offerTo(exporter, convertBetween(Blocks.WHITE_WOOL, Items.STRING));
      offerSingleOutputShapelessRecipe(exporter, Items.SUGAR, Blocks.SUGAR_CANE, "sugar");
      ShapelessRecipeJsonFactory.create(Items.SUGAR, 3).input((ItemConvertible)Items.HONEY_BOTTLE).group("sugar").criterion("has_honey_bottle", conditionsFromItem(Items.HONEY_BOTTLE)).offerTo(exporter, convertBetween(Items.SUGAR, Items.HONEY_BOTTLE));
      ShapedRecipeJsonFactory.create(Blocks.TARGET).input('H', (ItemConvertible)Items.HAY_BLOCK).input('R', (ItemConvertible)Items.REDSTONE).pattern(" R ").pattern("RHR").pattern(" R ").criterion("has_redstone", conditionsFromItem(Items.REDSTONE)).criterion("has_hay_block", conditionsFromItem(Blocks.HAY_BLOCK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.TNT).input('#', Ingredient.ofItems(Blocks.SAND, Blocks.RED_SAND)).input('X', (ItemConvertible)Items.GUNPOWDER).pattern("X#X").pattern("#X#").pattern("X#X").criterion("has_gunpowder", conditionsFromItem(Items.GUNPOWDER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.TNT_MINECART).input('A', (ItemConvertible)Blocks.TNT).input('B', (ItemConvertible)Items.MINECART).pattern("A").pattern("B").criterion("has_minecart", conditionsFromItem(Items.MINECART)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.TORCH, 4).input('#', (ItemConvertible)Items.STICK).input('X', Ingredient.ofItems(Items.COAL, Items.CHARCOAL)).pattern("X").pattern("#").criterion("has_stone_pickaxe", conditionsFromItem(Items.STONE_PICKAXE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SOUL_TORCH, 4).input('X', Ingredient.ofItems(Items.COAL, Items.CHARCOAL)).input('#', (ItemConvertible)Items.STICK).input('S', (Tag)ItemTags.SOUL_FIRE_BASE_BLOCKS).pattern("X").pattern("#").pattern("S").criterion("has_soul_sand", conditionsFromTag(ItemTags.SOUL_FIRE_BASE_BLOCKS)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.LANTERN).input('#', (ItemConvertible)Items.TORCH).input('X', (ItemConvertible)Items.IRON_NUGGET).pattern("XXX").pattern("X#X").pattern("XXX").criterion("has_iron_nugget", conditionsFromItem(Items.IRON_NUGGET)).criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SOUL_LANTERN).input('#', (ItemConvertible)Items.SOUL_TORCH).input('X', (ItemConvertible)Items.IRON_NUGGET).pattern("XXX").pattern("X#X").pattern("XXX").criterion("has_soul_torch", conditionsFromItem(Items.SOUL_TORCH)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Blocks.TRAPPED_CHEST).input((ItemConvertible)Blocks.CHEST).input((ItemConvertible)Blocks.TRIPWIRE_HOOK).criterion("has_tripwire_hook", conditionsFromItem(Blocks.TRIPWIRE_HOOK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.TRIPWIRE_HOOK, 2).input('#', (Tag)ItemTags.PLANKS).input('S', (ItemConvertible)Items.STICK).input('I', (ItemConvertible)Items.IRON_INGOT).pattern("I").pattern("S").pattern("#").criterion("has_string", conditionsFromItem(Items.STRING)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.TURTLE_HELMET).input('X', (ItemConvertible)Items.SCUTE).pattern("XXX").pattern("X X").criterion("has_scute", conditionsFromItem(Items.SCUTE)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.WHEAT, 9).input((ItemConvertible)Blocks.HAY_BLOCK).criterion("has_hay_block", conditionsFromItem(Blocks.HAY_BLOCK)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.WHITE_DYE).input((ItemConvertible)Items.BONE_MEAL).group("white_dye").criterion("has_bone_meal", conditionsFromItem(Items.BONE_MEAL)).offerTo(exporter);
      offerSingleOutputShapelessRecipe(exporter, Items.WHITE_DYE, Blocks.LILY_OF_THE_VALLEY, "white_dye");
      ShapedRecipeJsonFactory.create(Items.WOODEN_AXE).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.PLANKS).pattern("XX").pattern("X#").pattern(" #").criterion("has_stick", conditionsFromItem(Items.STICK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.WOODEN_HOE).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.PLANKS).pattern("XX").pattern(" #").pattern(" #").criterion("has_stick", conditionsFromItem(Items.STICK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.WOODEN_PICKAXE).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.PLANKS).pattern("XXX").pattern(" # ").pattern(" # ").criterion("has_stick", conditionsFromItem(Items.STICK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.WOODEN_SHOVEL).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.PLANKS).pattern("X").pattern("#").pattern("#").criterion("has_stick", conditionsFromItem(Items.STICK)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Items.WOODEN_SWORD).input('#', (ItemConvertible)Items.STICK).input('X', (Tag)ItemTags.PLANKS).pattern("X").pattern("X").pattern("#").criterion("has_stick", conditionsFromItem(Items.STICK)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.WRITABLE_BOOK).input((ItemConvertible)Items.BOOK).input((ItemConvertible)Items.INK_SAC).input((ItemConvertible)Items.FEATHER).criterion("has_book", conditionsFromItem(Items.BOOK)).offerTo(exporter);
      offerSingleOutputShapelessRecipe(exporter, Items.YELLOW_DYE, Blocks.DANDELION, "yellow_dye");
      offerShapelessRecipe(exporter, Items.YELLOW_DYE, Blocks.SUNFLOWER, "yellow_dye", 2);
      offerReversibleCompactingRecipes(exporter, Items.DRIED_KELP, Items.DRIED_KELP_BLOCK);
      ShapedRecipeJsonFactory.create(Blocks.CONDUIT).input('#', (ItemConvertible)Items.NAUTILUS_SHELL).input('X', (ItemConvertible)Items.HEART_OF_THE_SEA).pattern("###").pattern("#X#").pattern("###").criterion("has_nautilus_core", conditionsFromItem(Items.HEART_OF_THE_SEA)).criterion("has_nautilus_shell", conditionsFromItem(Items.NAUTILUS_SHELL)).offerTo(exporter);
      offerWallRecipe(exporter, Blocks.RED_SANDSTONE_WALL, Blocks.RED_SANDSTONE);
      offerWallRecipe(exporter, Blocks.STONE_BRICK_WALL, Blocks.STONE_BRICKS);
      offerWallRecipe(exporter, Blocks.SANDSTONE_WALL, Blocks.SANDSTONE);
      ShapelessRecipeJsonFactory.create(Items.CREEPER_BANNER_PATTERN).input((ItemConvertible)Items.PAPER).input((ItemConvertible)Items.CREEPER_HEAD).criterion("has_creeper_head", conditionsFromItem(Items.CREEPER_HEAD)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.SKULL_BANNER_PATTERN).input((ItemConvertible)Items.PAPER).input((ItemConvertible)Items.WITHER_SKELETON_SKULL).criterion("has_wither_skeleton_skull", conditionsFromItem(Items.WITHER_SKELETON_SKULL)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.FLOWER_BANNER_PATTERN).input((ItemConvertible)Items.PAPER).input((ItemConvertible)Blocks.OXEYE_DAISY).criterion("has_oxeye_daisy", conditionsFromItem(Blocks.OXEYE_DAISY)).offerTo(exporter);
      ShapelessRecipeJsonFactory.create(Items.MOJANG_BANNER_PATTERN).input((ItemConvertible)Items.PAPER).input((ItemConvertible)Items.ENCHANTED_GOLDEN_APPLE).criterion("has_enchanted_golden_apple", conditionsFromItem(Items.ENCHANTED_GOLDEN_APPLE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SCAFFOLDING, 6).input('~', (ItemConvertible)Items.STRING).input('I', (ItemConvertible)Blocks.BAMBOO).pattern("I~I").pattern("I I").pattern("I I").criterion("has_bamboo", conditionsFromItem(Blocks.BAMBOO)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.GRINDSTONE).input('I', (ItemConvertible)Items.STICK).input('-', (ItemConvertible)Blocks.STONE_SLAB).input('#', (Tag)ItemTags.PLANKS).pattern("I-I").pattern("# #").criterion("has_stone_slab", conditionsFromItem(Blocks.STONE_SLAB)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.BLAST_FURNACE).input('#', (ItemConvertible)Blocks.SMOOTH_STONE).input('X', (ItemConvertible)Blocks.FURNACE).input('I', (ItemConvertible)Items.IRON_INGOT).pattern("III").pattern("IXI").pattern("###").criterion("has_smooth_stone", conditionsFromItem(Blocks.SMOOTH_STONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SMOKER).input('#', (Tag)ItemTags.LOGS).input('X', (ItemConvertible)Blocks.FURNACE).pattern(" # ").pattern("#X#").pattern(" # ").criterion("has_furnace", conditionsFromItem(Blocks.FURNACE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.CARTOGRAPHY_TABLE).input('#', (Tag)ItemTags.PLANKS).input('@', (ItemConvertible)Items.PAPER).pattern("@@").pattern("##").pattern("##").criterion("has_paper", conditionsFromItem(Items.PAPER)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.SMITHING_TABLE).input('#', (Tag)ItemTags.PLANKS).input('@', (ItemConvertible)Items.IRON_INGOT).pattern("@@").pattern("##").pattern("##").criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.FLETCHING_TABLE).input('#', (Tag)ItemTags.PLANKS).input('@', (ItemConvertible)Items.FLINT).pattern("@@").pattern("##").pattern("##").criterion("has_flint", conditionsFromItem(Items.FLINT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.STONECUTTER).input('I', (ItemConvertible)Items.IRON_INGOT).input('#', (ItemConvertible)Blocks.STONE).pattern(" I ").pattern("###").criterion("has_stone", conditionsFromItem(Blocks.STONE)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.LODESTONE).input('S', (ItemConvertible)Items.CHISELED_STONE_BRICKS).input('#', (ItemConvertible)Items.NETHERITE_INGOT).pattern("SSS").pattern("S#S").pattern("SSS").criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT)).offerTo(exporter);
      offerReversibleCompactingRecipesWithInputItemGroup(exporter, Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK, "netherite_ingot_from_netherite_block", "netherite_ingot");
      ShapelessRecipeJsonFactory.create(Items.NETHERITE_INGOT).input((ItemConvertible)Items.NETHERITE_SCRAP, 4).input((ItemConvertible)Items.GOLD_INGOT, 4).group("netherite_ingot").criterion("has_netherite_scrap", conditionsFromItem(Items.NETHERITE_SCRAP)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.RESPAWN_ANCHOR).input('O', (ItemConvertible)Blocks.CRYING_OBSIDIAN).input('G', (ItemConvertible)Blocks.GLOWSTONE).pattern("OOO").pattern("GGG").pattern("OOO").criterion("has_obsidian", conditionsFromItem(Blocks.CRYING_OBSIDIAN)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.CHAIN).input('I', (ItemConvertible)Items.IRON_INGOT).input('N', (ItemConvertible)Items.IRON_NUGGET).pattern("N").pattern("I").pattern("N").criterion("has_iron_nugget", conditionsFromItem(Items.IRON_NUGGET)).criterion("has_iron_ingot", conditionsFromItem(Items.IRON_INGOT)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.TINTED_GLASS, 2).input('G', (ItemConvertible)Blocks.GLASS).input('S', (ItemConvertible)Items.AMETHYST_SHARD).pattern(" S ").pattern("SGS").pattern(" S ").criterion("has_amethyst_shard", conditionsFromItem(Items.AMETHYST_SHARD)).offerTo(exporter);
      ShapedRecipeJsonFactory.create(Blocks.AMETHYST_BLOCK).input('S', (ItemConvertible)Items.AMETHYST_SHARD).pattern("SS").pattern("SS").criterion("has_amethyst_shard", conditionsFromItem(Items.AMETHYST_SHARD)).offerTo(exporter);
      ComplexRecipeJsonFactory.create(RecipeSerializer.ARMOR_DYE).offerTo(exporter, "armor_dye");
      ComplexRecipeJsonFactory.create(RecipeSerializer.BANNER_DUPLICATE).offerTo(exporter, "banner_duplicate");
      ComplexRecipeJsonFactory.create(RecipeSerializer.BOOK_CLONING).offerTo(exporter, "book_cloning");
      ComplexRecipeJsonFactory.create(RecipeSerializer.FIREWORK_ROCKET).offerTo(exporter, "firework_rocket");
      ComplexRecipeJsonFactory.create(RecipeSerializer.FIREWORK_STAR).offerTo(exporter, "firework_star");
      ComplexRecipeJsonFactory.create(RecipeSerializer.FIREWORK_STAR_FADE).offerTo(exporter, "firework_star_fade");
      ComplexRecipeJsonFactory.create(RecipeSerializer.MAP_CLONING).offerTo(exporter, "map_cloning");
      ComplexRecipeJsonFactory.create(RecipeSerializer.MAP_EXTENDING).offerTo(exporter, "map_extending");
      ComplexRecipeJsonFactory.create(RecipeSerializer.REPAIR_ITEM).offerTo(exporter, "repair_item");
      ComplexRecipeJsonFactory.create(RecipeSerializer.SHIELD_DECORATION).offerTo(exporter, "shield_decoration");
      ComplexRecipeJsonFactory.create(RecipeSerializer.SHULKER_BOX).offerTo(exporter, "shulker_box_coloring");
      ComplexRecipeJsonFactory.create(RecipeSerializer.TIPPED_ARROW).offerTo(exporter, "tipped_arrow");
      ComplexRecipeJsonFactory.create(RecipeSerializer.SUSPICIOUS_STEW).offerTo(exporter, "suspicious_stew");
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.POTATO), Items.BAKED_POTATO, 0.35F, 200).criterion("has_potato", conditionsFromItem(Items.POTATO)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.CLAY_BALL), Items.BRICK, 0.3F, 200).criterion("has_clay_ball", conditionsFromItem(Items.CLAY_BALL)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.fromTag(ItemTags.LOGS_THAT_BURN), Items.CHARCOAL, 0.15F, 200).criterion("has_log", conditionsFromTag(ItemTags.LOGS_THAT_BURN)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.CHORUS_FRUIT), Items.POPPED_CHORUS_FRUIT, 0.1F, 200).criterion("has_chorus_fruit", conditionsFromItem(Items.CHORUS_FRUIT)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.BEEF), Items.COOKED_BEEF, 0.35F, 200).criterion("has_beef", conditionsFromItem(Items.BEEF)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.CHICKEN), Items.COOKED_CHICKEN, 0.35F, 200).criterion("has_chicken", conditionsFromItem(Items.CHICKEN)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.COD), Items.COOKED_COD, 0.35F, 200).criterion("has_cod", conditionsFromItem(Items.COD)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.KELP), Items.DRIED_KELP, 0.1F, 200).criterion("has_kelp", conditionsFromItem(Blocks.KELP)).offerTo(exporter, getSmeltingItemPath(Items.DRIED_KELP));
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.SALMON), Items.COOKED_SALMON, 0.35F, 200).criterion("has_salmon", conditionsFromItem(Items.SALMON)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.MUTTON), Items.COOKED_MUTTON, 0.35F, 200).criterion("has_mutton", conditionsFromItem(Items.MUTTON)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.PORKCHOP), Items.COOKED_PORKCHOP, 0.35F, 200).criterion("has_porkchop", conditionsFromItem(Items.PORKCHOP)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.RABBIT), Items.COOKED_RABBIT, 0.35F, 200).criterion("has_rabbit", conditionsFromItem(Items.RABBIT)).offerTo(exporter);
      offerSmelting(exporter, COAL_ORES, Items.COAL, 0.1F, 200, "coal");
      offerSmelting(exporter, IRON_ORES, Items.IRON_INGOT, 0.7F, 200, "iron_ingot");
      offerSmelting(exporter, COPPER_ORES, Items.COPPER_INGOT, 0.7F, 200, "copper_ingot");
      offerSmelting(exporter, GOLD_ORES, Items.GOLD_INGOT, 1.0F, 200, "gold_ingot");
      offerSmelting(exporter, DIAMOND_ORES, Items.DIAMOND, 1.0F, 200, "diamond");
      offerSmelting(exporter, LAPIS_ORES, Items.LAPIS_LAZULI, 0.2F, 200, "lapus_lazuli");
      offerSmelting(exporter, REDSTONE_ORES, Items.REDSTONE, 0.7F, 200, "redstone");
      offerSmelting(exporter, EMERALD_ORES, Items.EMERALD, 1.0F, 200, "emerald");
      offerReversibleCompactingRecipes(exporter, Items.RAW_IRON, Items.RAW_IRON_BLOCK);
      offerReversibleCompactingRecipes(exporter, Items.RAW_COPPER, Items.RAW_COPPER_BLOCK);
      offerReversibleCompactingRecipes(exporter, Items.RAW_GOLD, Items.RAW_GOLD_BLOCK);
      CookingRecipeJsonFactory.createSmelting(Ingredient.fromTag(ItemTags.SAND), Blocks.GLASS.asItem(), 0.1F, 200).criterion("has_sand", conditionsFromTag(ItemTags.SAND)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.SEA_PICKLE), Items.LIME_DYE, 0.1F, 200).criterion("has_sea_pickle", conditionsFromItem(Blocks.SEA_PICKLE)).offerTo(exporter, getSmeltingItemPath(Items.LIME_DYE));
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.CACTUS.asItem()), Items.GREEN_DYE, 1.0F, 200).criterion("has_cactus", conditionsFromItem(Blocks.CACTUS)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.GOLDEN_SWORD, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.GOLDEN_HORSE_ARMOR), Items.GOLD_NUGGET, 0.1F, 200).criterion("has_golden_pickaxe", conditionsFromItem(Items.GOLDEN_PICKAXE)).criterion("has_golden_shovel", conditionsFromItem(Items.GOLDEN_SHOVEL)).criterion("has_golden_axe", conditionsFromItem(Items.GOLDEN_AXE)).criterion("has_golden_hoe", conditionsFromItem(Items.GOLDEN_HOE)).criterion("has_golden_sword", conditionsFromItem(Items.GOLDEN_SWORD)).criterion("has_golden_helmet", conditionsFromItem(Items.GOLDEN_HELMET)).criterion("has_golden_chestplate", conditionsFromItem(Items.GOLDEN_CHESTPLATE)).criterion("has_golden_leggings", conditionsFromItem(Items.GOLDEN_LEGGINGS)).criterion("has_golden_boots", conditionsFromItem(Items.GOLDEN_BOOTS)).criterion("has_golden_horse_armor", conditionsFromItem(Items.GOLDEN_HORSE_ARMOR)).offerTo(exporter, getSmeltingItemPath(Items.GOLD_NUGGET));
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_AXE, Items.IRON_HOE, Items.IRON_SWORD, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_HORSE_ARMOR, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS), Items.IRON_NUGGET, 0.1F, 200).criterion("has_iron_pickaxe", conditionsFromItem(Items.IRON_PICKAXE)).criterion("has_iron_shovel", conditionsFromItem(Items.IRON_SHOVEL)).criterion("has_iron_axe", conditionsFromItem(Items.IRON_AXE)).criterion("has_iron_hoe", conditionsFromItem(Items.IRON_HOE)).criterion("has_iron_sword", conditionsFromItem(Items.IRON_SWORD)).criterion("has_iron_helmet", conditionsFromItem(Items.IRON_HELMET)).criterion("has_iron_chestplate", conditionsFromItem(Items.IRON_CHESTPLATE)).criterion("has_iron_leggings", conditionsFromItem(Items.IRON_LEGGINGS)).criterion("has_iron_boots", conditionsFromItem(Items.IRON_BOOTS)).criterion("has_iron_horse_armor", conditionsFromItem(Items.IRON_HORSE_ARMOR)).criterion("has_chainmail_helmet", conditionsFromItem(Items.CHAINMAIL_HELMET)).criterion("has_chainmail_chestplate", conditionsFromItem(Items.CHAINMAIL_CHESTPLATE)).criterion("has_chainmail_leggings", conditionsFromItem(Items.CHAINMAIL_LEGGINGS)).criterion("has_chainmail_boots", conditionsFromItem(Items.CHAINMAIL_BOOTS)).offerTo(exporter, getSmeltingItemPath(Items.IRON_NUGGET));
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.CLAY), Blocks.TERRACOTTA.asItem(), 0.35F, 200).criterion("has_clay_block", conditionsFromItem(Blocks.CLAY)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.NETHERRACK), Items.NETHER_BRICK, 0.1F, 200).criterion("has_netherrack", conditionsFromItem(Blocks.NETHERRACK)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 0.2F, 200).criterion("has_nether_quartz_ore", conditionsFromItem(Blocks.NETHER_QUARTZ_ORE)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.WET_SPONGE), Blocks.SPONGE.asItem(), 0.15F, 200).criterion("has_wet_sponge", conditionsFromItem(Blocks.WET_SPONGE)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.COBBLESTONE), Blocks.STONE.asItem(), 0.1F, 200).criterion("has_cobblestone", conditionsFromItem(Blocks.COBBLESTONE)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.STONE), Blocks.SMOOTH_STONE.asItem(), 0.1F, 200).criterion("has_stone", conditionsFromItem(Blocks.STONE)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.SANDSTONE), Blocks.SMOOTH_SANDSTONE.asItem(), 0.1F, 200).criterion("has_sandstone", conditionsFromItem(Blocks.SANDSTONE)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.RED_SANDSTONE), Blocks.SMOOTH_RED_SANDSTONE.asItem(), 0.1F, 200).criterion("has_red_sandstone", conditionsFromItem(Blocks.RED_SANDSTONE)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.QUARTZ_BLOCK), Blocks.SMOOTH_QUARTZ.asItem(), 0.1F, 200).criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.STONE_BRICKS), Blocks.CRACKED_STONE_BRICKS.asItem(), 0.1F, 200).criterion("has_stone_bricks", conditionsFromItem(Blocks.STONE_BRICKS)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.BLACK_TERRACOTTA), Blocks.BLACK_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_black_terracotta", conditionsFromItem(Blocks.BLACK_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.BLUE_TERRACOTTA), Blocks.BLUE_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_blue_terracotta", conditionsFromItem(Blocks.BLUE_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.BROWN_TERRACOTTA), Blocks.BROWN_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_brown_terracotta", conditionsFromItem(Blocks.BROWN_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.CYAN_TERRACOTTA), Blocks.CYAN_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_cyan_terracotta", conditionsFromItem(Blocks.CYAN_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.GRAY_TERRACOTTA), Blocks.GRAY_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_gray_terracotta", conditionsFromItem(Blocks.GRAY_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.GREEN_TERRACOTTA), Blocks.GREEN_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_green_terracotta", conditionsFromItem(Blocks.GREEN_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.LIGHT_BLUE_TERRACOTTA), Blocks.LIGHT_BLUE_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_light_blue_terracotta", conditionsFromItem(Blocks.LIGHT_BLUE_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.LIGHT_GRAY_TERRACOTTA), Blocks.LIGHT_GRAY_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_light_gray_terracotta", conditionsFromItem(Blocks.LIGHT_GRAY_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.LIME_TERRACOTTA), Blocks.LIME_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_lime_terracotta", conditionsFromItem(Blocks.LIME_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.MAGENTA_TERRACOTTA), Blocks.MAGENTA_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_magenta_terracotta", conditionsFromItem(Blocks.MAGENTA_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.ORANGE_TERRACOTTA), Blocks.ORANGE_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_orange_terracotta", conditionsFromItem(Blocks.ORANGE_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.PINK_TERRACOTTA), Blocks.PINK_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_pink_terracotta", conditionsFromItem(Blocks.PINK_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.PURPLE_TERRACOTTA), Blocks.PURPLE_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_purple_terracotta", conditionsFromItem(Blocks.PURPLE_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.RED_TERRACOTTA), Blocks.RED_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_red_terracotta", conditionsFromItem(Blocks.RED_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.WHITE_TERRACOTTA), Blocks.WHITE_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_white_terracotta", conditionsFromItem(Blocks.WHITE_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.YELLOW_TERRACOTTA), Blocks.YELLOW_GLAZED_TERRACOTTA.asItem(), 0.1F, 200).criterion("has_yellow_terracotta", conditionsFromItem(Blocks.YELLOW_TERRACOTTA)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.ANCIENT_DEBRIS), Items.NETHERITE_SCRAP, 2.0F, 200).criterion("has_ancient_debris", conditionsFromItem(Blocks.ANCIENT_DEBRIS)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.BASALT), Blocks.SMOOTH_BASALT, 0.1F, 200).criterion("has_basalt", conditionsFromItem(Blocks.BASALT)).offerTo(exporter);
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(Blocks.COBBLED_DEEPSLATE), Blocks.DEEPSLATE, 0.1F, 200).criterion("has_cobbled_deepslate", conditionsFromItem(Blocks.COBBLED_DEEPSLATE)).offerTo(exporter);
      offerBlasting(exporter, COAL_ORES, Items.COAL, 0.1F, 100, "coal");
      offerBlasting(exporter, IRON_ORES, Items.IRON_INGOT, 0.7F, 100, "iron_ingot");
      offerBlasting(exporter, COPPER_ORES, Items.COPPER_INGOT, 0.7F, 100, "copper_ingot");
      offerBlasting(exporter, GOLD_ORES, Items.GOLD_INGOT, 1.0F, 100, "gold_ingot");
      offerBlasting(exporter, DIAMOND_ORES, Items.DIAMOND, 1.0F, 100, "diamond");
      offerBlasting(exporter, LAPIS_ORES, Items.LAPIS_LAZULI, 0.2F, 100, "lapus_lazuli");
      offerBlasting(exporter, REDSTONE_ORES, Items.REDSTONE, 0.7F, 100, "redstone");
      offerBlasting(exporter, EMERALD_ORES, Items.EMERALD, 1.0F, 100, "emerald");
      CookingRecipeJsonFactory.createBlasting(Ingredient.ofItems(Blocks.NETHER_QUARTZ_ORE), Items.QUARTZ, 0.2F, 100).criterion("has_nether_quartz_ore", conditionsFromItem(Blocks.NETHER_QUARTZ_ORE)).offerTo(exporter, getBlastingItemPath(Items.QUARTZ));
      CookingRecipeJsonFactory.createBlasting(Ingredient.ofItems(Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.GOLDEN_SWORD, Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS, Items.GOLDEN_HORSE_ARMOR), Items.GOLD_NUGGET, 0.1F, 100).criterion("has_golden_pickaxe", conditionsFromItem(Items.GOLDEN_PICKAXE)).criterion("has_golden_shovel", conditionsFromItem(Items.GOLDEN_SHOVEL)).criterion("has_golden_axe", conditionsFromItem(Items.GOLDEN_AXE)).criterion("has_golden_hoe", conditionsFromItem(Items.GOLDEN_HOE)).criterion("has_golden_sword", conditionsFromItem(Items.GOLDEN_SWORD)).criterion("has_golden_helmet", conditionsFromItem(Items.GOLDEN_HELMET)).criterion("has_golden_chestplate", conditionsFromItem(Items.GOLDEN_CHESTPLATE)).criterion("has_golden_leggings", conditionsFromItem(Items.GOLDEN_LEGGINGS)).criterion("has_golden_boots", conditionsFromItem(Items.GOLDEN_BOOTS)).criterion("has_golden_horse_armor", conditionsFromItem(Items.GOLDEN_HORSE_ARMOR)).offerTo(exporter, getBlastingItemPath(Items.GOLD_NUGGET));
      CookingRecipeJsonFactory.createBlasting(Ingredient.ofItems(Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_AXE, Items.IRON_HOE, Items.IRON_SWORD, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_HORSE_ARMOR, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS), Items.IRON_NUGGET, 0.1F, 100).criterion("has_iron_pickaxe", conditionsFromItem(Items.IRON_PICKAXE)).criterion("has_iron_shovel", conditionsFromItem(Items.IRON_SHOVEL)).criterion("has_iron_axe", conditionsFromItem(Items.IRON_AXE)).criterion("has_iron_hoe", conditionsFromItem(Items.IRON_HOE)).criterion("has_iron_sword", conditionsFromItem(Items.IRON_SWORD)).criterion("has_iron_helmet", conditionsFromItem(Items.IRON_HELMET)).criterion("has_iron_chestplate", conditionsFromItem(Items.IRON_CHESTPLATE)).criterion("has_iron_leggings", conditionsFromItem(Items.IRON_LEGGINGS)).criterion("has_iron_boots", conditionsFromItem(Items.IRON_BOOTS)).criterion("has_iron_horse_armor", conditionsFromItem(Items.IRON_HORSE_ARMOR)).criterion("has_chainmail_helmet", conditionsFromItem(Items.CHAINMAIL_HELMET)).criterion("has_chainmail_chestplate", conditionsFromItem(Items.CHAINMAIL_CHESTPLATE)).criterion("has_chainmail_leggings", conditionsFromItem(Items.CHAINMAIL_LEGGINGS)).criterion("has_chainmail_boots", conditionsFromItem(Items.CHAINMAIL_BOOTS)).offerTo(exporter, getBlastingItemPath(Items.IRON_NUGGET));
      CookingRecipeJsonFactory.createBlasting(Ingredient.ofItems(Blocks.ANCIENT_DEBRIS), Items.NETHERITE_SCRAP, 2.0F, 100).criterion("has_ancient_debris", conditionsFromItem(Blocks.ANCIENT_DEBRIS)).offerTo(exporter, getBlastingItemPath(Items.NETHERITE_SCRAP));
      generateCookingRecipes(exporter, "smoking", RecipeSerializer.SMOKING, 100);
      generateCookingRecipes(exporter, "campfire_cooking", RecipeSerializer.CAMPFIRE_COOKING, 600);
      offerRecipe(exporter, Blocks.STONE_SLAB, Blocks.STONE, 2);
      offerRecipe(exporter, Blocks.STONE_STAIRS, Blocks.STONE);
      offerRecipe(exporter, Blocks.STONE_BRICKS, Blocks.STONE);
      offerRecipe(exporter, Blocks.STONE_BRICK_SLAB, Blocks.STONE, 2);
      offerRecipe(exporter, Blocks.STONE_BRICK_STAIRS, Blocks.STONE);
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.STONE), Blocks.CHISELED_STONE_BRICKS).criterion("has_stone", conditionsFromItem(Blocks.STONE)).offerTo(exporter, "chiseled_stone_bricks_stone_from_stonecutting");
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.STONE), Blocks.STONE_BRICK_WALL).criterion("has_stone", conditionsFromItem(Blocks.STONE)).offerTo(exporter, "stone_brick_walls_from_stone_stonecutting");
      offerRecipe(exporter, Blocks.CUT_SANDSTONE, Blocks.SANDSTONE);
      offerRecipe(exporter, Blocks.SANDSTONE_SLAB, Blocks.SANDSTONE, 2);
      offerRecipe(exporter, Blocks.CUT_SANDSTONE_SLAB, Blocks.SANDSTONE, 2);
      offerRecipe(exporter, Blocks.CUT_SANDSTONE_SLAB, Blocks.CUT_SANDSTONE, 2);
      offerRecipe(exporter, Blocks.SANDSTONE_STAIRS, Blocks.SANDSTONE);
      offerRecipe(exporter, Blocks.SANDSTONE_WALL, Blocks.SANDSTONE);
      offerRecipe(exporter, Blocks.CHISELED_SANDSTONE, Blocks.SANDSTONE);
      offerRecipe(exporter, Blocks.CUT_RED_SANDSTONE, Blocks.RED_SANDSTONE);
      offerRecipe(exporter, Blocks.RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE, 2);
      offerRecipe(exporter, Blocks.CUT_RED_SANDSTONE_SLAB, Blocks.RED_SANDSTONE, 2);
      offerRecipe(exporter, Blocks.CUT_RED_SANDSTONE_SLAB, Blocks.CUT_RED_SANDSTONE, 2);
      offerRecipe(exporter, Blocks.RED_SANDSTONE_STAIRS, Blocks.RED_SANDSTONE);
      offerRecipe(exporter, Blocks.RED_SANDSTONE_WALL, Blocks.RED_SANDSTONE);
      offerRecipe(exporter, Blocks.CHISELED_RED_SANDSTONE, Blocks.RED_SANDSTONE);
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.QUARTZ_BLOCK), Blocks.QUARTZ_SLAB, 2).criterion("has_quartz_block", conditionsFromItem(Blocks.QUARTZ_BLOCK)).offerTo(exporter, "quartz_slab_from_stonecutting");
      offerRecipe(exporter, Blocks.QUARTZ_STAIRS, Blocks.QUARTZ_BLOCK);
      offerRecipe(exporter, Blocks.QUARTZ_PILLAR, Blocks.QUARTZ_BLOCK);
      offerRecipe(exporter, Blocks.CHISELED_QUARTZ_BLOCK, Blocks.QUARTZ_BLOCK);
      offerRecipe(exporter, Blocks.QUARTZ_BRICKS, Blocks.QUARTZ_BLOCK);
      offerRecipe(exporter, Blocks.COBBLESTONE_STAIRS, Blocks.COBBLESTONE);
      offerRecipe(exporter, Blocks.COBBLESTONE_SLAB, Blocks.COBBLESTONE, 2);
      offerRecipe(exporter, Blocks.COBBLESTONE_WALL, Blocks.COBBLESTONE);
      offerRecipe(exporter, Blocks.STONE_BRICK_SLAB, Blocks.STONE_BRICKS, 2);
      offerRecipe(exporter, Blocks.STONE_BRICK_STAIRS, Blocks.STONE_BRICKS);
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.STONE_BRICKS), Blocks.STONE_BRICK_WALL).criterion("has_stone_bricks", conditionsFromItem(Blocks.STONE_BRICKS)).offerTo(exporter, "stone_brick_wall_from_stone_bricks_stonecutting");
      offerRecipe(exporter, Blocks.CHISELED_STONE_BRICKS, Blocks.STONE_BRICKS);
      offerRecipe(exporter, Blocks.BRICK_SLAB, Blocks.BRICKS, 2);
      offerRecipe(exporter, Blocks.BRICK_STAIRS, Blocks.BRICKS);
      offerRecipe(exporter, Blocks.BRICK_WALL, Blocks.BRICKS);
      offerRecipe(exporter, Blocks.NETHER_BRICK_SLAB, Blocks.NETHER_BRICKS, 2);
      offerRecipe(exporter, Blocks.NETHER_BRICK_STAIRS, Blocks.NETHER_BRICKS);
      offerRecipe(exporter, Blocks.NETHER_BRICK_WALL, Blocks.NETHER_BRICKS);
      offerRecipe(exporter, Blocks.CHISELED_NETHER_BRICKS, Blocks.NETHER_BRICKS);
      offerRecipe(exporter, Blocks.RED_NETHER_BRICK_SLAB, Blocks.RED_NETHER_BRICKS, 2);
      offerRecipe(exporter, Blocks.RED_NETHER_BRICK_STAIRS, Blocks.RED_NETHER_BRICKS);
      offerRecipe(exporter, Blocks.RED_NETHER_BRICK_WALL, Blocks.RED_NETHER_BRICKS);
      offerRecipe(exporter, Blocks.PURPUR_SLAB, Blocks.PURPUR_BLOCK, 2);
      offerRecipe(exporter, Blocks.PURPUR_STAIRS, Blocks.PURPUR_BLOCK);
      offerRecipe(exporter, Blocks.PURPUR_PILLAR, Blocks.PURPUR_BLOCK);
      offerRecipe(exporter, Blocks.PRISMARINE_SLAB, Blocks.PRISMARINE, 2);
      offerRecipe(exporter, Blocks.PRISMARINE_STAIRS, Blocks.PRISMARINE);
      offerRecipe(exporter, Blocks.PRISMARINE_WALL, Blocks.PRISMARINE);
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.PRISMARINE_BRICKS), Blocks.PRISMARINE_BRICK_SLAB, 2).criterion("has_prismarine_brick", conditionsFromItem(Blocks.PRISMARINE_BRICKS)).offerTo(exporter, "prismarine_brick_slab_from_prismarine_stonecutting");
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.PRISMARINE_BRICKS), Blocks.PRISMARINE_BRICK_STAIRS).criterion("has_prismarine_brick", conditionsFromItem(Blocks.PRISMARINE_BRICKS)).offerTo(exporter, "prismarine_brick_stairs_from_prismarine_stonecutting");
      offerRecipe(exporter, Blocks.DARK_PRISMARINE_SLAB, Blocks.DARK_PRISMARINE, 2);
      offerRecipe(exporter, Blocks.DARK_PRISMARINE_STAIRS, Blocks.DARK_PRISMARINE);
      offerRecipe(exporter, Blocks.ANDESITE_SLAB, Blocks.ANDESITE, 2);
      offerRecipe(exporter, Blocks.ANDESITE_STAIRS, Blocks.ANDESITE);
      offerRecipe(exporter, Blocks.ANDESITE_WALL, Blocks.ANDESITE);
      offerRecipe(exporter, Blocks.POLISHED_ANDESITE, Blocks.ANDESITE);
      offerRecipe(exporter, Blocks.POLISHED_ANDESITE_SLAB, Blocks.ANDESITE, 2);
      offerRecipe(exporter, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.ANDESITE);
      offerRecipe(exporter, Blocks.POLISHED_ANDESITE_SLAB, Blocks.POLISHED_ANDESITE, 2);
      offerRecipe(exporter, Blocks.POLISHED_ANDESITE_STAIRS, Blocks.POLISHED_ANDESITE);
      offerRecipe(exporter, Blocks.POLISHED_BASALT, Blocks.BASALT);
      offerRecipe(exporter, Blocks.GRANITE_SLAB, Blocks.GRANITE, 2);
      offerRecipe(exporter, Blocks.GRANITE_STAIRS, Blocks.GRANITE);
      offerRecipe(exporter, Blocks.GRANITE_WALL, Blocks.GRANITE);
      offerRecipe(exporter, Blocks.POLISHED_GRANITE, Blocks.GRANITE);
      offerRecipe(exporter, Blocks.POLISHED_GRANITE_SLAB, Blocks.GRANITE, 2);
      offerRecipe(exporter, Blocks.POLISHED_GRANITE_STAIRS, Blocks.GRANITE);
      offerRecipe(exporter, Blocks.POLISHED_GRANITE_SLAB, Blocks.POLISHED_GRANITE, 2);
      offerRecipe(exporter, Blocks.POLISHED_GRANITE_STAIRS, Blocks.POLISHED_GRANITE);
      offerRecipe(exporter, Blocks.DIORITE_SLAB, Blocks.DIORITE, 2);
      offerRecipe(exporter, Blocks.DIORITE_STAIRS, Blocks.DIORITE);
      offerRecipe(exporter, Blocks.DIORITE_WALL, Blocks.DIORITE);
      offerRecipe(exporter, Blocks.POLISHED_DIORITE, Blocks.DIORITE);
      offerRecipe(exporter, Blocks.POLISHED_DIORITE_SLAB, Blocks.DIORITE, 2);
      offerRecipe(exporter, Blocks.POLISHED_DIORITE_STAIRS, Blocks.DIORITE);
      offerRecipe(exporter, Blocks.POLISHED_DIORITE_SLAB, Blocks.POLISHED_DIORITE, 2);
      offerRecipe(exporter, Blocks.POLISHED_DIORITE_STAIRS, Blocks.POLISHED_DIORITE);
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.MOSSY_STONE_BRICKS), Blocks.MOSSY_STONE_BRICK_SLAB, 2).criterion("has_mossy_stone_bricks", conditionsFromItem(Blocks.MOSSY_STONE_BRICKS)).offerTo(exporter, "mossy_stone_brick_slab_from_mossy_stone_brick_stonecutting");
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.MOSSY_STONE_BRICKS), Blocks.MOSSY_STONE_BRICK_STAIRS).criterion("has_mossy_stone_bricks", conditionsFromItem(Blocks.MOSSY_STONE_BRICKS)).offerTo(exporter, "mossy_stone_brick_stairs_from_mossy_stone_brick_stonecutting");
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.MOSSY_STONE_BRICKS), Blocks.MOSSY_STONE_BRICK_WALL).criterion("has_mossy_stone_bricks", conditionsFromItem(Blocks.MOSSY_STONE_BRICKS)).offerTo(exporter, "mossy_stone_brick_wall_from_mossy_stone_brick_stonecutting");
      offerRecipe(exporter, Blocks.MOSSY_COBBLESTONE_SLAB, Blocks.MOSSY_COBBLESTONE, 2);
      offerRecipe(exporter, Blocks.MOSSY_COBBLESTONE_STAIRS, Blocks.MOSSY_COBBLESTONE);
      offerRecipe(exporter, Blocks.MOSSY_COBBLESTONE_WALL, Blocks.MOSSY_COBBLESTONE);
      offerRecipe(exporter, Blocks.SMOOTH_SANDSTONE_SLAB, Blocks.SMOOTH_SANDSTONE, 2);
      offerRecipe(exporter, Blocks.SMOOTH_SANDSTONE_STAIRS, Blocks.SMOOTH_SANDSTONE);
      offerRecipe(exporter, Blocks.SMOOTH_RED_SANDSTONE_SLAB, Blocks.SMOOTH_RED_SANDSTONE, 2);
      offerRecipe(exporter, Blocks.SMOOTH_RED_SANDSTONE_STAIRS, Blocks.SMOOTH_RED_SANDSTONE);
      offerRecipe(exporter, Blocks.SMOOTH_QUARTZ_SLAB, Blocks.SMOOTH_QUARTZ, 2);
      offerRecipe(exporter, Blocks.SMOOTH_QUARTZ_STAIRS, Blocks.SMOOTH_QUARTZ);
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.END_STONE_BRICKS), Blocks.END_STONE_BRICK_SLAB, 2).criterion("has_end_stone_brick", conditionsFromItem(Blocks.END_STONE_BRICKS)).offerTo(exporter, "end_stone_brick_slab_from_end_stone_brick_stonecutting");
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.END_STONE_BRICKS), Blocks.END_STONE_BRICK_STAIRS).criterion("has_end_stone_brick", conditionsFromItem(Blocks.END_STONE_BRICKS)).offerTo(exporter, "end_stone_brick_stairs_from_end_stone_brick_stonecutting");
      SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(Blocks.END_STONE_BRICKS), Blocks.END_STONE_BRICK_WALL).criterion("has_end_stone_brick", conditionsFromItem(Blocks.END_STONE_BRICKS)).offerTo(exporter, "end_stone_brick_wall_from_end_stone_brick_stonecutting");
      offerRecipe(exporter, Blocks.END_STONE_BRICKS, Blocks.END_STONE);
      offerRecipe(exporter, Blocks.END_STONE_BRICK_SLAB, Blocks.END_STONE, 2);
      offerRecipe(exporter, Blocks.END_STONE_BRICK_STAIRS, Blocks.END_STONE);
      offerRecipe(exporter, Blocks.END_STONE_BRICK_WALL, Blocks.END_STONE);
      offerRecipe(exporter, Blocks.SMOOTH_STONE_SLAB, Blocks.SMOOTH_STONE, 2);
      offerRecipe(exporter, Blocks.BLACKSTONE_SLAB, Blocks.BLACKSTONE, 2);
      offerRecipe(exporter, Blocks.BLACKSTONE_STAIRS, Blocks.BLACKSTONE);
      offerRecipe(exporter, Blocks.BLACKSTONE_WALL, Blocks.BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE, Blocks.BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_WALL, Blocks.BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.BLACKSTONE, 2);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.BLACKSTONE);
      offerRecipe(exporter, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.BLACKSTONE, 2);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_SLAB, Blocks.POLISHED_BLACKSTONE, 2);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_STAIRS, Blocks.POLISHED_BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.POLISHED_BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_WALL, Blocks.POLISHED_BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE, 2);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE);
      offerRecipe(exporter, Blocks.CHISELED_POLISHED_BLACKSTONE, Blocks.POLISHED_BLACKSTONE);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_SLAB, Blocks.POLISHED_BLACKSTONE_BRICKS, 2);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.POLISHED_BLACKSTONE_BRICKS);
      offerRecipe(exporter, Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.POLISHED_BLACKSTONE_BRICKS);
      offerRecipe(exporter, Blocks.CUT_COPPER_SLAB, Blocks.CUT_COPPER, 2);
      offerRecipe(exporter, Blocks.CUT_COPPER_STAIRS, Blocks.CUT_COPPER);
      offerRecipe(exporter, Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.EXPOSED_CUT_COPPER, 2);
      offerRecipe(exporter, Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.EXPOSED_CUT_COPPER);
      offerRecipe(exporter, Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.WEATHERED_CUT_COPPER, 2);
      offerRecipe(exporter, Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.WEATHERED_CUT_COPPER);
      offerRecipe(exporter, Blocks.OXIDIZED_CUT_COPPER_SLAB, Blocks.OXIDIZED_CUT_COPPER, 2);
      offerRecipe(exporter, Blocks.OXIDIZED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_CUT_COPPER);
      offerRecipe(exporter, Blocks.WAXED_CUT_COPPER_SLAB, Blocks.WAXED_CUT_COPPER, 2);
      offerRecipe(exporter, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_CUT_COPPER);
      offerRecipe(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_CUT_COPPER, 2);
      offerRecipe(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_CUT_COPPER);
      offerRecipe(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_CUT_COPPER, 2);
      offerRecipe(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_CUT_COPPER);
      offerRecipe(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, Blocks.WAXED_OXIDIZED_CUT_COPPER, 2);
      offerRecipe(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS, Blocks.WAXED_OXIDIZED_CUT_COPPER);
      offerRecipe(exporter, Blocks.CUT_COPPER, Blocks.COPPER_BLOCK);
      offerRecipe(exporter, Blocks.CUT_COPPER_STAIRS, Blocks.COPPER_BLOCK);
      offerRecipe(exporter, Blocks.CUT_COPPER_SLAB, Blocks.COPPER_BLOCK, 2);
      offerRecipe(exporter, Blocks.EXPOSED_CUT_COPPER, Blocks.EXPOSED_COPPER);
      offerRecipe(exporter, Blocks.EXPOSED_CUT_COPPER_STAIRS, Blocks.EXPOSED_COPPER);
      offerRecipe(exporter, Blocks.EXPOSED_CUT_COPPER_SLAB, Blocks.EXPOSED_COPPER, 2);
      offerRecipe(exporter, Blocks.WEATHERED_CUT_COPPER, Blocks.WEATHERED_COPPER);
      offerRecipe(exporter, Blocks.WEATHERED_CUT_COPPER_STAIRS, Blocks.WEATHERED_COPPER);
      offerRecipe(exporter, Blocks.WEATHERED_CUT_COPPER_SLAB, Blocks.WEATHERED_COPPER, 2);
      offerRecipe(exporter, Blocks.OXIDIZED_CUT_COPPER, Blocks.OXIDIZED_COPPER);
      offerRecipe(exporter, Blocks.OXIDIZED_CUT_COPPER_STAIRS, Blocks.OXIDIZED_COPPER);
      offerRecipe(exporter, Blocks.OXIDIZED_CUT_COPPER_SLAB, Blocks.OXIDIZED_COPPER, 2);
      offerRecipe(exporter, Blocks.WAXED_CUT_COPPER, Blocks.WAXED_COPPER_BLOCK);
      offerRecipe(exporter, Blocks.WAXED_CUT_COPPER_STAIRS, Blocks.WAXED_COPPER_BLOCK);
      offerRecipe(exporter, Blocks.WAXED_CUT_COPPER_SLAB, Blocks.WAXED_COPPER_BLOCK, 2);
      offerRecipe(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER, Blocks.WAXED_EXPOSED_COPPER);
      offerRecipe(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER_STAIRS, Blocks.WAXED_EXPOSED_COPPER);
      offerRecipe(exporter, Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB, Blocks.WAXED_EXPOSED_COPPER, 2);
      offerRecipe(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER, Blocks.WAXED_WEATHERED_COPPER);
      offerRecipe(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER_STAIRS, Blocks.WAXED_WEATHERED_COPPER);
      offerRecipe(exporter, Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB, Blocks.WAXED_WEATHERED_COPPER, 2);
      offerRecipe(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER, Blocks.WAXED_OXIDIZED_COPPER);
      offerRecipe(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER_STAIRS, Blocks.WAXED_OXIDIZED_COPPER);
      offerRecipe(exporter, Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB, Blocks.WAXED_OXIDIZED_COPPER, 2);
      offerRecipe(exporter, Blocks.COBBLED_DEEPSLATE_SLAB, Blocks.COBBLED_DEEPSLATE, 2);
      offerRecipe(exporter, Blocks.COBBLED_DEEPSLATE_STAIRS, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.COBBLED_DEEPSLATE_WALL, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.CHISELED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.POLISHED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.COBBLED_DEEPSLATE, 2);
      offerRecipe(exporter, Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.POLISHED_DEEPSLATE_WALL, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICKS, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.COBBLED_DEEPSLATE, 2);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICK_WALL, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILES, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_SLAB, Blocks.COBBLED_DEEPSLATE, 2);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_WALL, Blocks.COBBLED_DEEPSLATE);
      offerRecipe(exporter, Blocks.POLISHED_DEEPSLATE_SLAB, Blocks.POLISHED_DEEPSLATE, 2);
      offerRecipe(exporter, Blocks.POLISHED_DEEPSLATE_STAIRS, Blocks.POLISHED_DEEPSLATE);
      offerRecipe(exporter, Blocks.POLISHED_DEEPSLATE_WALL, Blocks.POLISHED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICKS, Blocks.POLISHED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.POLISHED_DEEPSLATE, 2);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.POLISHED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICK_WALL, Blocks.POLISHED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_SLAB, Blocks.POLISHED_DEEPSLATE, 2);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.POLISHED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_WALL, Blocks.POLISHED_DEEPSLATE);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICK_SLAB, Blocks.DEEPSLATE_BRICKS, 2);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.DEEPSLATE_BRICKS);
      offerRecipe(exporter, Blocks.DEEPSLATE_BRICK_WALL, Blocks.DEEPSLATE_BRICKS);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_BRICKS, 2);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_BRICKS);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_WALL, Blocks.DEEPSLATE_BRICKS);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_SLAB, Blocks.DEEPSLATE_TILES, 2);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_STAIRS, Blocks.DEEPSLATE_TILES);
      offerRecipe(exporter, Blocks.DEEPSLATE_TILE_WALL, Blocks.DEEPSLATE_TILES);
      offerNetheriteUpgradeRecipe(exporter, Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE);
      offerNetheriteUpgradeRecipe(exporter, Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS);
      offerNetheriteUpgradeRecipe(exporter, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET);
      offerNetheriteUpgradeRecipe(exporter, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS);
      offerNetheriteUpgradeRecipe(exporter, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);
      offerNetheriteUpgradeRecipe(exporter, Items.DIAMOND_AXE, Items.NETHERITE_AXE);
      offerNetheriteUpgradeRecipe(exporter, Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE);
      offerNetheriteUpgradeRecipe(exporter, Items.DIAMOND_HOE, Items.NETHERITE_HOE);
      offerNetheriteUpgradeRecipe(exporter, Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL);
   }

   private static void offerSingleOutputShapelessRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input, @Nullable String group) {
      offerShapelessRecipe(exporter, output, input, group, 1);
   }

   private static void offerShapelessRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input, @Nullable String group, int outputCount) {
      ShapelessRecipeJsonFactory.create(output, outputCount).input(input).group(group).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter, convertBetween(output, input));
   }

   private static void offerSmelting(Consumer<RecipeJsonProvider> exporter, List<ItemConvertible> inputs, ItemConvertible output, float experience, int cookingTime, String group) {
      offerMultipleOptions(exporter, RecipeSerializer.SMELTING, inputs, output, experience, cookingTime, group, "_from_smelting");
   }

   private static void offerBlasting(Consumer<RecipeJsonProvider> exporter, List<ItemConvertible> inputs, ItemConvertible output, float experience, int cookingTime, String group) {
      offerMultipleOptions(exporter, RecipeSerializer.BLASTING, inputs, output, experience, cookingTime, group, "_from_blasting");
   }

   private static void offerMultipleOptions(Consumer<RecipeJsonProvider> exporter, CookingRecipeSerializer<?> serializer, List<ItemConvertible> inputs, ItemConvertible output, float experience, int cookingTime, String group, String baseIdString) {
      Iterator var8 = inputs.iterator();

      while(var8.hasNext()) {
         ItemConvertible itemConvertible = (ItemConvertible)var8.next();
         CookingRecipeJsonFactory.create(Ingredient.ofItems(itemConvertible), output, experience, cookingTime, serializer).group(group).criterion(hasItem(itemConvertible), conditionsFromItem(itemConvertible)).offerTo(exporter, getItemPath(output) + baseIdString + "_" + getItemPath(itemConvertible));
      }

   }

   private static void offerNetheriteUpgradeRecipe(Consumer<RecipeJsonProvider> exporter, Item output, Item input) {
      SmithingRecipeJsonFactory.create(Ingredient.ofItems(output), Ingredient.ofItems(Items.NETHERITE_INGOT), input).criterion("has_netherite_ingot", conditionsFromItem(Items.NETHERITE_INGOT)).offerTo(exporter, getItemPath(input) + "_smithing");
   }

   private static void offerPlanksRecipe2(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, Tag<Item> input) {
      ShapelessRecipeJsonFactory.create(output, 4).input(input).group("planks").criterion("has_log", conditionsFromTag(input)).offerTo(exporter);
   }

   private static void offerPlanksRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, Tag<Item> input) {
      ShapelessRecipeJsonFactory.create(output, 4).input(input).group("planks").criterion("has_logs", conditionsFromTag(input)).offerTo(exporter);
   }

   private static void offerBarkBlockRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output, 3).input('#', input).pattern("##").pattern("##").group("bark").criterion("has_log", conditionsFromItem(input)).offerTo(exporter);
   }

   private static void offerBoatRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output).input('#', input).pattern("# #").pattern("###").group("boat").criterion("in_water", requireEnteringFluid(Blocks.WATER)).offerTo(exporter);
   }

   private static CraftingRecipeJsonFactory createTransmutationRecipe(ItemConvertible output, Ingredient input) {
      return ShapelessRecipeJsonFactory.create(output).input(input);
   }

   private static CraftingRecipeJsonFactory createDoorRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output, 3).input('#', input).pattern("##").pattern("##").pattern("##");
   }

   private static CraftingRecipeJsonFactory createFenceRecipe(ItemConvertible output, Ingredient input) {
      int i = output == Blocks.NETHER_BRICK_FENCE ? 6 : 3;
      Item item = output == Blocks.NETHER_BRICK_FENCE ? Items.NETHER_BRICK : Items.STICK;
      return ShapedRecipeJsonFactory.create(output, i).input('W', input).input('#', (ItemConvertible)item).pattern("W#W").pattern("W#W");
   }

   private static CraftingRecipeJsonFactory createFenceGateRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output).input('#', (ItemConvertible)Items.STICK).input('W', input).pattern("#W#").pattern("#W#");
   }

   private static void createPressurePlateRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      createPressurePlateRecipe(output, Ingredient.ofItems(input)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   private static CraftingRecipeJsonFactory createPressurePlateRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output).input('#', input).pattern("##");
   }

   private static void offerSlabRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      createSlabRecipe(output, Ingredient.ofItems(input)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   private static CraftingRecipeJsonFactory createSlabRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output, 6).input('#', input).pattern("###");
   }

   private static CraftingRecipeJsonFactory createStairsRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output, 4).input('#', input).pattern("#  ").pattern("## ").pattern("###");
   }

   private static CraftingRecipeJsonFactory createTrapdoorRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output, 2).input('#', input).pattern("###").pattern("###");
   }

   private static CraftingRecipeJsonFactory createSignRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output, 3).group("sign").input('#', input).input('X', (ItemConvertible)Items.STICK).pattern("###").pattern("###").pattern(" X ");
   }

   private static void offerWoolDyeingRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapelessRecipeJsonFactory.create(output).input(input).input((ItemConvertible)Blocks.WHITE_WOOL).group("wool").criterion("has_white_wool", conditionsFromItem(Blocks.WHITE_WOOL)).offerTo(exporter);
   }

   private static void offerCarpetRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output, 3).input('#', input).pattern("##").group("carpet").criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   private static void offerCarpetDyeingRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output, 8).input('#', (ItemConvertible)Blocks.WHITE_CARPET).input('$', input).pattern("###").pattern("#$#").pattern("###").group("carpet").criterion("has_white_carpet", conditionsFromItem(Blocks.WHITE_CARPET)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter, convertBetween(output, Blocks.WHITE_CARPET));
   }

   private static void offerBedRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output).input('#', input).input('X', (Tag)ItemTags.PLANKS).pattern("###").pattern("XXX").group("bed").criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   private static void offerBedDyeingRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapelessRecipeJsonFactory.create(output).input((ItemConvertible)Items.WHITE_BED).input(input).group("dyed_bed").criterion("has_bed", conditionsFromItem(Items.WHITE_BED)).offerTo(exporter, convertBetween(output, Items.WHITE_BED));
   }

   private static void offerBannerRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output).input('#', input).input('|', (ItemConvertible)Items.STICK).pattern("###").pattern("###").pattern(" | ").group("banner").criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   private static void offerStainedGlassDyeingRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output, 8).input('#', (ItemConvertible)Blocks.GLASS).input('X', input).pattern("###").pattern("#X#").pattern("###").group("stained_glass").criterion("has_glass", conditionsFromItem(Blocks.GLASS)).offerTo(exporter);
   }

   private static void offerStainedGlassPaneRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output, 16).input('#', input).pattern("###").pattern("###").group("stained_glass_pane").criterion("has_glass", conditionsFromItem(input)).offerTo(exporter);
   }

   private static void offerStainedGlassPaneDyeingRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output, 8).input('#', (ItemConvertible)Blocks.GLASS_PANE).input('$', input).pattern("###").pattern("#$#").pattern("###").group("stained_glass_pane").criterion("has_glass_pane", conditionsFromItem(Blocks.GLASS_PANE)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter, convertBetween(output, Blocks.GLASS_PANE));
   }

   private static void offerTerracottaDyeingRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapedRecipeJsonFactory.create(output, 8).input('#', (ItemConvertible)Blocks.TERRACOTTA).input('X', input).pattern("###").pattern("#X#").pattern("###").group("stained_terracotta").criterion("has_terracotta", conditionsFromItem(Blocks.TERRACOTTA)).offerTo(exporter);
   }

   private static void offerConcretePowderDyeingRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapelessRecipeJsonFactory.create(output, 8).input(input).input((ItemConvertible)Blocks.SAND, 4).input((ItemConvertible)Blocks.GRAVEL, 4).group("concrete_powder").criterion("has_sand", conditionsFromItem(Blocks.SAND)).criterion("has_gravel", conditionsFromItem(Blocks.GRAVEL)).offerTo(exporter);
   }

   public static void offerCandleDyeingRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      ShapelessRecipeJsonFactory.create(output).input((ItemConvertible)Blocks.CANDLE).input(input).group("dyed_candle").criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   public static void offerWallRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      getWallRecipe(output, Ingredient.ofItems(input)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   public static CraftingRecipeJsonFactory getWallRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output, 6).input('#', input).pattern("###").pattern("###");
   }

   public static void offerPolishedStoneRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      createCondensingRecipe(output, Ingredient.ofItems(input)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   public static CraftingRecipeJsonFactory createCondensingRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output, 4).input('S', input).pattern("SS").pattern("SS");
   }

   public static void offerCutCopperRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      createCutCopperRecipe(output, Ingredient.ofItems(input)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   public static ShapedRecipeJsonFactory createCutCopperRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output, 4).input('#', input).pattern("##").pattern("##");
   }

   public static void offerChiseledBlockRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      createChiseledBlockRecipe(output, Ingredient.ofItems(input)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   public static ShapedRecipeJsonFactory createChiseledBlockRecipe(ItemConvertible output, Ingredient input) {
      return ShapedRecipeJsonFactory.create(output).input('#', input).pattern("#").pattern("#");
   }

   private static void offerRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      offerRecipe(exporter, output, input, 1);
   }

   private static void offerRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input, int count) {
      SingleItemRecipeJsonFactory var10000 = SingleItemRecipeJsonFactory.createStonecutting(Ingredient.ofItems(input), output, count).criterion(hasItem(input), conditionsFromItem(input));
      String var10002 = convertBetween(output, input);
      var10000.offerTo(exporter, var10002 + "_stonecutting");
   }

   /**
    * Offers a smelting recipe to the exporter that is used to convert the main block of a block family to its cracked variant.
    */
   private static void offerCrackingRecipe(Consumer<RecipeJsonProvider> exporter, ItemConvertible output, ItemConvertible input) {
      CookingRecipeJsonFactory.createSmelting(Ingredient.ofItems(input), output, 0.1F, 200).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter);
   }

   /**
    * Offers two recipes to convert between a normal and compacted form of an item.
    * 
    * <p>The shaped recipe converts 9 items in a square to a compacted form of the item.
    * <p>The shapeless recipe converts the compacted form to 9 of the normal form.
    * 
    * @param compacted compacted output item, e.g. block of copper
    * @param input input item used to craft compacted item, e.g. copper ingot
    */
   private static void offerReversibleCompactingRecipes(Consumer<RecipeJsonProvider> exporter, ItemConvertible compacted, ItemConvertible input) {
      offerReversibleCompactingRecipes(exporter, compacted, input, method_36450(input), (String)null, method_36450(compacted), (String)null);
   }

   private static void offerReversibleCompactingRecipesWithCompactedItemGroup(Consumer<RecipeJsonProvider> exporter, ItemConvertible compacted, ItemConvertible input, String compactedItemId, String compactedItemGroup) {
      offerReversibleCompactingRecipes(exporter, compacted, input, compactedItemId, compactedItemGroup, method_36450(compacted), (String)null);
   }

   private static void offerReversibleCompactingRecipesWithInputItemGroup(Consumer<RecipeJsonProvider> exporter, ItemConvertible input, ItemConvertible compacted, String inputItemId, String inputItemGroup) {
      offerReversibleCompactingRecipes(exporter, input, compacted, method_36450(compacted), (String)null, inputItemId, inputItemGroup);
   }

   private static void offerReversibleCompactingRecipes(Consumer<RecipeJsonProvider> exporter, ItemConvertible input, ItemConvertible compacted, String compactedItemId, @Nullable String compactedItemGroup, String inputItemId, @Nullable String inputItemGroup) {
      ShapelessRecipeJsonFactory.create(input, 9).input(compacted).group(inputItemGroup).criterion(hasItem(compacted), conditionsFromItem(compacted)).offerTo(exporter, new Identifier(inputItemId));
      ShapedRecipeJsonFactory.create(compacted).input('#', input).pattern("###").pattern("###").pattern("###").group(compactedItemGroup).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter, new Identifier(compactedItemId));
   }

   private static void generateCookingRecipes(Consumer<RecipeJsonProvider> exporter, String cooker, CookingRecipeSerializer<?> serializer, int cookingTime) {
      offerCookingRecipe(exporter, cooker, serializer, cookingTime, Items.BEEF, Items.COOKED_BEEF, 0.35F);
      offerCookingRecipe(exporter, cooker, serializer, cookingTime, Items.CHICKEN, Items.COOKED_CHICKEN, 0.35F);
      offerCookingRecipe(exporter, cooker, serializer, cookingTime, Items.COD, Items.COOKED_COD, 0.35F);
      offerCookingRecipe(exporter, cooker, serializer, cookingTime, Items.KELP, Items.DRIED_KELP, 0.1F);
      offerCookingRecipe(exporter, cooker, serializer, cookingTime, Items.SALMON, Items.COOKED_SALMON, 0.35F);
      offerCookingRecipe(exporter, cooker, serializer, cookingTime, Items.MUTTON, Items.COOKED_MUTTON, 0.35F);
      offerCookingRecipe(exporter, cooker, serializer, cookingTime, Items.PORKCHOP, Items.COOKED_PORKCHOP, 0.35F);
      offerCookingRecipe(exporter, cooker, serializer, cookingTime, Items.POTATO, Items.BAKED_POTATO, 0.35F);
      offerCookingRecipe(exporter, cooker, serializer, cookingTime, Items.RABBIT, Items.COOKED_RABBIT, 0.35F);
   }

   private static void offerCookingRecipe(Consumer<RecipeJsonProvider> exporter, String cooker, CookingRecipeSerializer<?> serializer, int cookingTime, ItemConvertible input, ItemConvertible output, float experience) {
      CookingRecipeJsonFactory var10000 = CookingRecipeJsonFactory.create(Ingredient.ofItems(input), output, experience, cookingTime, serializer).criterion(hasItem(input), conditionsFromItem(input));
      String var10002 = getItemPath(output);
      var10000.offerTo(exporter, var10002 + "_from_" + cooker);
   }

   private static void offerWaxingRecipes(Consumer<RecipeJsonProvider> exporter) {
      ((BiMap)HoneycombItem.UNWAXED_TO_WAXED_BLOCKS.get()).forEach((input, output) -> {
         ShapelessRecipeJsonFactory.create(output).input((ItemConvertible)input).input((ItemConvertible)Items.HONEYCOMB).group(getItemPath(output)).criterion(hasItem(input), conditionsFromItem(input)).offerTo(exporter, convertBetween(output, Items.HONEYCOMB));
      });
   }

   private static void generateFamily(Consumer<RecipeJsonProvider> exporter, BlockFamily family) {
      family.getVariants().forEach((variant, block) -> {
         BiFunction<ItemConvertible, ItemConvertible, CraftingRecipeJsonFactory> biFunction = (BiFunction)VARIANT_FACTORIES.get(variant);
         ItemConvertible itemConvertible = getVariantRecipeInput(family, variant);
         if (biFunction != null) {
            CraftingRecipeJsonFactory craftingRecipeJsonFactory = (CraftingRecipeJsonFactory)biFunction.apply(block, itemConvertible);
            family.getGroup().ifPresent((group) -> {
               craftingRecipeJsonFactory.group(group + (variant == BlockFamily.Variant.CUT ? "" : "_" + variant.getName()));
            });
            craftingRecipeJsonFactory.criterion((String)family.getUnlockCriterionName().orElseGet(() -> {
               return hasItem(itemConvertible);
            }), conditionsFromItem(itemConvertible));
            craftingRecipeJsonFactory.offerTo(exporter);
         }

         if (variant == BlockFamily.Variant.CRACKED) {
            offerCrackingRecipe(exporter, block, itemConvertible);
         }

      });
   }

   /**
    * Gets the block used to craft a certain {@linkplain net.minecraft.data.family.BlockFamily.Variant variant} of a base block.
    * 
    * <p>Normally, the block used to craft a variant is the base block.
    * For chiseled variants, this is the slab variant of that block.
    * 
    * <p>Purpur is handled separately because both purpur and purpur pillars can be used to craft purpur slabs and stairs.
    */
   private static Block getVariantRecipeInput(BlockFamily family, BlockFamily.Variant variant) {
      if (variant == BlockFamily.Variant.CHISELED) {
         if (!family.getVariants().containsKey(BlockFamily.Variant.SLAB)) {
            throw new IllegalStateException("Slab is not defined for the family.");
         } else {
            return family.getVariant(BlockFamily.Variant.SLAB);
         }
      } else {
         return family.getBaseBlock();
      }
   }

   private static EnterBlockCriterion.Conditions requireEnteringFluid(Block block) {
      return new EnterBlockCriterion.Conditions(EntityPredicate.Extended.EMPTY, block, StatePredicate.ANY);
   }

   private static InventoryChangedCriterion.Conditions conditionsFromItem(NumberRange.IntRange count, ItemConvertible item) {
      return conditionsFromItemPredicates(ItemPredicate.Builder.create().items(item).count(count).build());
   }

   private static InventoryChangedCriterion.Conditions conditionsFromItem(ItemConvertible item) {
      return conditionsFromItemPredicates(ItemPredicate.Builder.create().items(item).build());
   }

   private static InventoryChangedCriterion.Conditions conditionsFromTag(Tag<Item> tag) {
      return conditionsFromItemPredicates(ItemPredicate.Builder.create().tag(tag).build());
   }

   private static InventoryChangedCriterion.Conditions conditionsFromItemPredicates(ItemPredicate... items) {
      return new InventoryChangedCriterion.Conditions(EntityPredicate.Extended.EMPTY, NumberRange.IntRange.ANY, NumberRange.IntRange.ANY, NumberRange.IntRange.ANY, items);
   }

   private static String hasItem(ItemConvertible item) {
      return "has_" + getItemPath(item);
   }

   private static String getItemPath(ItemConvertible item) {
      return Registry.ITEM.getId(item.asItem()).getPath();
   }

   private static String method_36450(ItemConvertible item) {
      return getItemPath(item);
   }

   private static String convertBetween(ItemConvertible from, ItemConvertible to) {
      String var10000 = getItemPath(from);
      return var10000 + "_from_" + getItemPath(to);
   }

   private static String getSmeltingItemPath(ItemConvertible item) {
      return getItemPath(item) + "_from_smelting";
   }

   private static String getBlastingItemPath(ItemConvertible item) {
      return getItemPath(item) + "_from_blasting";
   }

   public String getName() {
      return "Recipes";
   }

   static {
      COAL_ORES = ImmutableList.of(Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE);
      IRON_ORES = ImmutableList.of(Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.RAW_IRON);
      COPPER_ORES = ImmutableList.of(Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.RAW_COPPER);
      GOLD_ORES = ImmutableList.of(Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.NETHER_GOLD_ORE, Items.RAW_GOLD);
      DIAMOND_ORES = ImmutableList.of(Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE);
      LAPIS_ORES = ImmutableList.of(Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE);
      REDSTONE_ORES = ImmutableList.of(Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE);
      EMERALD_ORES = ImmutableList.of(Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE);
      VARIANT_FACTORIES = ImmutableMap.builder().put(BlockFamily.Variant.BUTTON, (output, input) -> {
         return createTransmutationRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.CHISELED, (output, input) -> {
         return createChiseledBlockRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.CUT, (output, input) -> {
         return createCutCopperRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.DOOR, (output, input) -> {
         return createDoorRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.FENCE, (output, input) -> {
         return createFenceRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.FENCE_GATE, (output, input) -> {
         return createFenceGateRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.SIGN, (output, input) -> {
         return createSignRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.SLAB, (output, input) -> {
         return createSlabRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.STAIRS, (output, input) -> {
         return createStairsRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.PRESSURE_PLATE, (output, input) -> {
         return createPressurePlateRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.POLISHED, (output, input) -> {
         return createCondensingRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.TRAPDOOR, (output, input) -> {
         return createTrapdoorRecipe(output, Ingredient.ofItems(input));
      }).put(BlockFamily.Variant.WALL, (output, input) -> {
         return getWallRecipe(output, Ingredient.ofItems(input));
      }).build();
   }
}
