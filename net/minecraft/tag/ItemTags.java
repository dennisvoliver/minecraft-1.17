package net.minecraft.tag;

import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;

public final class ItemTags {
   protected static final RequiredTagList<Item> REQUIRED_TAGS;
   public static final Tag.Identified<Item> WOOL;
   public static final Tag.Identified<Item> PLANKS;
   public static final Tag.Identified<Item> STONE_BRICKS;
   public static final Tag.Identified<Item> WOODEN_BUTTONS;
   public static final Tag.Identified<Item> BUTTONS;
   public static final Tag.Identified<Item> CARPETS;
   public static final Tag.Identified<Item> WOODEN_DOORS;
   public static final Tag.Identified<Item> WOODEN_STAIRS;
   public static final Tag.Identified<Item> WOODEN_SLABS;
   public static final Tag.Identified<Item> WOODEN_FENCES;
   public static final Tag.Identified<Item> WOODEN_PRESSURE_PLATES;
   public static final Tag.Identified<Item> WOODEN_TRAPDOORS;
   public static final Tag.Identified<Item> DOORS;
   public static final Tag.Identified<Item> SAPLINGS;
   public static final Tag.Identified<Item> LOGS_THAT_BURN;
   public static final Tag.Identified<Item> LOGS;
   public static final Tag.Identified<Item> DARK_OAK_LOGS;
   public static final Tag.Identified<Item> OAK_LOGS;
   public static final Tag.Identified<Item> BIRCH_LOGS;
   public static final Tag.Identified<Item> ACACIA_LOGS;
   public static final Tag.Identified<Item> JUNGLE_LOGS;
   public static final Tag.Identified<Item> SPRUCE_LOGS;
   public static final Tag.Identified<Item> CRIMSON_STEMS;
   public static final Tag.Identified<Item> WARPED_STEMS;
   public static final Tag.Identified<Item> BANNERS;
   public static final Tag.Identified<Item> SAND;
   public static final Tag.Identified<Item> STAIRS;
   public static final Tag.Identified<Item> SLABS;
   public static final Tag.Identified<Item> WALLS;
   public static final Tag.Identified<Item> ANVIL;
   public static final Tag.Identified<Item> RAILS;
   public static final Tag.Identified<Item> LEAVES;
   public static final Tag.Identified<Item> TRAPDOORS;
   public static final Tag.Identified<Item> SMALL_FLOWERS;
   public static final Tag.Identified<Item> BEDS;
   public static final Tag.Identified<Item> FENCES;
   public static final Tag.Identified<Item> TALL_FLOWERS;
   public static final Tag.Identified<Item> FLOWERS;
   public static final Tag.Identified<Item> PIGLIN_REPELLENTS;
   public static final Tag.Identified<Item> PIGLIN_LOVED;
   public static final Tag.Identified<Item> IGNORED_BY_PIGLIN_BABIES;
   public static final Tag.Identified<Item> PIGLIN_FOOD;
   public static final Tag.Identified<Item> FOX_FOOD;
   public static final Tag.Identified<Item> GOLD_ORES;
   public static final Tag.Identified<Item> IRON_ORES;
   public static final Tag.Identified<Item> DIAMOND_ORES;
   public static final Tag.Identified<Item> REDSTONE_ORES;
   public static final Tag.Identified<Item> LAPIS_ORES;
   public static final Tag.Identified<Item> COAL_ORES;
   public static final Tag.Identified<Item> EMERALD_ORES;
   public static final Tag.Identified<Item> COPPER_ORES;
   public static final Tag.Identified<Item> NON_FLAMMABLE_WOOD;
   public static final Tag.Identified<Item> SOUL_FIRE_BASE_BLOCKS;
   public static final Tag.Identified<Item> CANDLES;
   public static final Tag.Identified<Item> BOATS;
   public static final Tag.Identified<Item> FISHES;
   public static final Tag.Identified<Item> SIGNS;
   public static final Tag.Identified<Item> MUSIC_DISCS;
   public static final Tag.Identified<Item> CREEPER_DROP_MUSIC_DISCS;
   public static final Tag.Identified<Item> COALS;
   public static final Tag.Identified<Item> ARROWS;
   public static final Tag.Identified<Item> LECTERN_BOOKS;
   public static final Tag.Identified<Item> BEACON_PAYMENT_ITEMS;
   public static final Tag.Identified<Item> STONE_TOOL_MATERIALS;
   public static final Tag.Identified<Item> STONE_CRAFTING_MATERIALS;
   public static final Tag.Identified<Item> FREEZE_IMMUNE_WEARABLES;
   public static final Tag.Identified<Item> AXOLOTL_TEMPT_ITEMS;
   public static final Tag.Identified<Item> OCCLUDES_VIBRATION_SIGNALS;
   public static final Tag.Identified<Item> CLUSTER_MAX_HARVESTABLES;

   private ItemTags() {
   }

   private static Tag.Identified<Item> register(String id) {
      return REQUIRED_TAGS.add(id);
   }

   public static TagGroup<Item> getTagGroup() {
      return REQUIRED_TAGS.getGroup();
   }

   static {
      REQUIRED_TAGS = RequiredTagListRegistry.register(Registry.ITEM_KEY, "tags/items");
      WOOL = register("wool");
      PLANKS = register("planks");
      STONE_BRICKS = register("stone_bricks");
      WOODEN_BUTTONS = register("wooden_buttons");
      BUTTONS = register("buttons");
      CARPETS = register("carpets");
      WOODEN_DOORS = register("wooden_doors");
      WOODEN_STAIRS = register("wooden_stairs");
      WOODEN_SLABS = register("wooden_slabs");
      WOODEN_FENCES = register("wooden_fences");
      WOODEN_PRESSURE_PLATES = register("wooden_pressure_plates");
      WOODEN_TRAPDOORS = register("wooden_trapdoors");
      DOORS = register("doors");
      SAPLINGS = register("saplings");
      LOGS_THAT_BURN = register("logs_that_burn");
      LOGS = register("logs");
      DARK_OAK_LOGS = register("dark_oak_logs");
      OAK_LOGS = register("oak_logs");
      BIRCH_LOGS = register("birch_logs");
      ACACIA_LOGS = register("acacia_logs");
      JUNGLE_LOGS = register("jungle_logs");
      SPRUCE_LOGS = register("spruce_logs");
      CRIMSON_STEMS = register("crimson_stems");
      WARPED_STEMS = register("warped_stems");
      BANNERS = register("banners");
      SAND = register("sand");
      STAIRS = register("stairs");
      SLABS = register("slabs");
      WALLS = register("walls");
      ANVIL = register("anvil");
      RAILS = register("rails");
      LEAVES = register("leaves");
      TRAPDOORS = register("trapdoors");
      SMALL_FLOWERS = register("small_flowers");
      BEDS = register("beds");
      FENCES = register("fences");
      TALL_FLOWERS = register("tall_flowers");
      FLOWERS = register("flowers");
      PIGLIN_REPELLENTS = register("piglin_repellents");
      PIGLIN_LOVED = register("piglin_loved");
      IGNORED_BY_PIGLIN_BABIES = register("ignored_by_piglin_babies");
      PIGLIN_FOOD = register("piglin_food");
      FOX_FOOD = register("fox_food");
      GOLD_ORES = register("gold_ores");
      IRON_ORES = register("iron_ores");
      DIAMOND_ORES = register("diamond_ores");
      REDSTONE_ORES = register("redstone_ores");
      LAPIS_ORES = register("lapis_ores");
      COAL_ORES = register("coal_ores");
      EMERALD_ORES = register("emerald_ores");
      COPPER_ORES = register("copper_ores");
      NON_FLAMMABLE_WOOD = register("non_flammable_wood");
      SOUL_FIRE_BASE_BLOCKS = register("soul_fire_base_blocks");
      CANDLES = register("candles");
      BOATS = register("boats");
      FISHES = register("fishes");
      SIGNS = register("signs");
      MUSIC_DISCS = register("music_discs");
      CREEPER_DROP_MUSIC_DISCS = register("creeper_drop_music_discs");
      COALS = register("coals");
      ARROWS = register("arrows");
      LECTERN_BOOKS = register("lectern_books");
      BEACON_PAYMENT_ITEMS = register("beacon_payment_items");
      STONE_TOOL_MATERIALS = register("stone_tool_materials");
      STONE_CRAFTING_MATERIALS = register("stone_crafting_materials");
      FREEZE_IMMUNE_WEARABLES = register("freeze_immune_wearables");
      AXOLOTL_TEMPT_ITEMS = register("axolotl_tempt_items");
      OCCLUDES_VIBRATION_SIGNALS = register("occludes_vibration_signals");
      CLUSTER_MAX_HARVESTABLES = register("cluster_max_harvestables");
   }
}
