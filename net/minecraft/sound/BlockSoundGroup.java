package net.minecraft.sound;

public class BlockSoundGroup {
   public static final BlockSoundGroup WOOD;
   public static final BlockSoundGroup GRAVEL;
   public static final BlockSoundGroup GRASS;
   public static final BlockSoundGroup LILY_PAD;
   public static final BlockSoundGroup STONE;
   public static final BlockSoundGroup METAL;
   public static final BlockSoundGroup GLASS;
   public static final BlockSoundGroup WOOL;
   public static final BlockSoundGroup SAND;
   public static final BlockSoundGroup SNOW;
   public static final BlockSoundGroup POWDER_SNOW;
   public static final BlockSoundGroup LADDER;
   public static final BlockSoundGroup ANVIL;
   public static final BlockSoundGroup SLIME;
   public static final BlockSoundGroup HONEY;
   public static final BlockSoundGroup WET_GRASS;
   public static final BlockSoundGroup CORAL;
   public static final BlockSoundGroup BAMBOO;
   public static final BlockSoundGroup BAMBOO_SAPLING;
   public static final BlockSoundGroup SCAFFOLDING;
   public static final BlockSoundGroup SWEET_BERRY_BUSH;
   public static final BlockSoundGroup CROP;
   public static final BlockSoundGroup STEM;
   public static final BlockSoundGroup VINE;
   public static final BlockSoundGroup NETHER_WART;
   public static final BlockSoundGroup LANTERN;
   public static final BlockSoundGroup NETHER_STEM;
   public static final BlockSoundGroup NYLIUM;
   public static final BlockSoundGroup FUNGUS;
   public static final BlockSoundGroup ROOTS;
   public static final BlockSoundGroup SHROOMLIGHT;
   public static final BlockSoundGroup WEEPING_VINES;
   public static final BlockSoundGroup WEEPING_VINES_LOW_PITCH;
   public static final BlockSoundGroup SOUL_SAND;
   public static final BlockSoundGroup SOUL_SOIL;
   public static final BlockSoundGroup BASALT;
   public static final BlockSoundGroup WART_BLOCK;
   public static final BlockSoundGroup NETHERRACK;
   public static final BlockSoundGroup NETHER_BRICKS;
   public static final BlockSoundGroup NETHER_SPROUTS;
   public static final BlockSoundGroup NETHER_ORE;
   public static final BlockSoundGroup BONE;
   public static final BlockSoundGroup NETHERITE;
   public static final BlockSoundGroup ANCIENT_DEBRIS;
   public static final BlockSoundGroup LODESTONE;
   public static final BlockSoundGroup CHAIN;
   public static final BlockSoundGroup NETHER_GOLD_ORE;
   public static final BlockSoundGroup GILDED_BLACKSTONE;
   public static final BlockSoundGroup CANDLE;
   public static final BlockSoundGroup AMETHYST_BLOCK;
   public static final BlockSoundGroup AMETHYST_CLUSTER;
   public static final BlockSoundGroup SMALL_AMETHYST_BUD;
   public static final BlockSoundGroup MEDIUM_AMETHYST_BUD;
   public static final BlockSoundGroup LARGE_AMETHYST_BUD;
   public static final BlockSoundGroup TUFF;
   public static final BlockSoundGroup CALCITE;
   public static final BlockSoundGroup DRIPSTONE_BLOCK;
   public static final BlockSoundGroup POINTED_DRIPSTONE;
   public static final BlockSoundGroup COPPER;
   public static final BlockSoundGroup CAVE_VINES;
   public static final BlockSoundGroup SPORE_BLOSSOM;
   public static final BlockSoundGroup AZALEA;
   public static final BlockSoundGroup FLOWERING_AZALEA;
   public static final BlockSoundGroup MOSS_CARPET;
   public static final BlockSoundGroup MOSS_BLOCK;
   public static final BlockSoundGroup BIG_DRIPLEAF;
   public static final BlockSoundGroup SMALL_DRIPLEAF;
   public static final BlockSoundGroup ROOTED_DIRT;
   public static final BlockSoundGroup HANGING_ROOTS;
   public static final BlockSoundGroup AZALEA_LEAVES;
   public static final BlockSoundGroup SCULK_SENSOR;
   public static final BlockSoundGroup GLOW_LICHEN;
   public static final BlockSoundGroup DEEPSLATE;
   public static final BlockSoundGroup DEEPSLATE_BRICKS;
   public static final BlockSoundGroup DEEPSLATE_TILES;
   public static final BlockSoundGroup POLISHED_DEEPSLATE;
   public final float volume;
   public final float pitch;
   private final SoundEvent breakSound;
   private final SoundEvent stepSound;
   private final SoundEvent placeSound;
   private final SoundEvent hitSound;
   private final SoundEvent fallSound;

   public BlockSoundGroup(float volume, float pitch, SoundEvent breakSound, SoundEvent stepSound, SoundEvent placeSound, SoundEvent hitSound, SoundEvent fallSound) {
      this.volume = volume;
      this.pitch = pitch;
      this.breakSound = breakSound;
      this.stepSound = stepSound;
      this.placeSound = placeSound;
      this.hitSound = hitSound;
      this.fallSound = fallSound;
   }

   public float getVolume() {
      return this.volume;
   }

   public float getPitch() {
      return this.pitch;
   }

   public SoundEvent getBreakSound() {
      return this.breakSound;
   }

   public SoundEvent getStepSound() {
      return this.stepSound;
   }

   public SoundEvent getPlaceSound() {
      return this.placeSound;
   }

   public SoundEvent getHitSound() {
      return this.hitSound;
   }

   public SoundEvent getFallSound() {
      return this.fallSound;
   }

   static {
      WOOD = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_WOOD_BREAK, SoundEvents.BLOCK_WOOD_STEP, SoundEvents.BLOCK_WOOD_PLACE, SoundEvents.BLOCK_WOOD_HIT, SoundEvents.BLOCK_WOOD_FALL);
      GRAVEL = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_GRAVEL_BREAK, SoundEvents.BLOCK_GRAVEL_STEP, SoundEvents.BLOCK_GRAVEL_PLACE, SoundEvents.BLOCK_GRAVEL_HIT, SoundEvents.BLOCK_GRAVEL_FALL);
      GRASS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_GRASS_BREAK, SoundEvents.BLOCK_GRASS_STEP, SoundEvents.BLOCK_GRASS_PLACE, SoundEvents.BLOCK_GRASS_HIT, SoundEvents.BLOCK_GRASS_FALL);
      LILY_PAD = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_GRASS_BREAK, SoundEvents.BLOCK_GRASS_STEP, SoundEvents.BLOCK_LILY_PAD_PLACE, SoundEvents.BLOCK_GRASS_HIT, SoundEvents.BLOCK_GRASS_FALL);
      STONE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_STONE_BREAK, SoundEvents.BLOCK_STONE_STEP, SoundEvents.BLOCK_STONE_PLACE, SoundEvents.BLOCK_STONE_HIT, SoundEvents.BLOCK_STONE_FALL);
      METAL = new BlockSoundGroup(1.0F, 1.5F, SoundEvents.BLOCK_METAL_BREAK, SoundEvents.BLOCK_METAL_STEP, SoundEvents.BLOCK_METAL_PLACE, SoundEvents.BLOCK_METAL_HIT, SoundEvents.BLOCK_METAL_FALL);
      GLASS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_GLASS_BREAK, SoundEvents.BLOCK_GLASS_STEP, SoundEvents.BLOCK_GLASS_PLACE, SoundEvents.BLOCK_GLASS_HIT, SoundEvents.BLOCK_GLASS_FALL);
      WOOL = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_WOOL_BREAK, SoundEvents.BLOCK_WOOL_STEP, SoundEvents.BLOCK_WOOL_PLACE, SoundEvents.BLOCK_WOOL_HIT, SoundEvents.BLOCK_WOOL_FALL);
      SAND = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SAND_BREAK, SoundEvents.BLOCK_SAND_STEP, SoundEvents.BLOCK_SAND_PLACE, SoundEvents.BLOCK_SAND_HIT, SoundEvents.BLOCK_SAND_FALL);
      SNOW = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SNOW_BREAK, SoundEvents.BLOCK_SNOW_STEP, SoundEvents.BLOCK_SNOW_PLACE, SoundEvents.BLOCK_SNOW_HIT, SoundEvents.BLOCK_SNOW_FALL);
      POWDER_SNOW = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_POWDER_SNOW_BREAK, SoundEvents.BLOCK_POWDER_SNOW_STEP, SoundEvents.BLOCK_POWDER_SNOW_PLACE, SoundEvents.BLOCK_POWDER_SNOW_HIT, SoundEvents.BLOCK_POWDER_SNOW_FALL);
      LADDER = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_LADDER_BREAK, SoundEvents.BLOCK_LADDER_STEP, SoundEvents.BLOCK_LADDER_PLACE, SoundEvents.BLOCK_LADDER_HIT, SoundEvents.BLOCK_LADDER_FALL);
      ANVIL = new BlockSoundGroup(0.3F, 1.0F, SoundEvents.BLOCK_ANVIL_BREAK, SoundEvents.BLOCK_ANVIL_STEP, SoundEvents.BLOCK_ANVIL_PLACE, SoundEvents.BLOCK_ANVIL_HIT, SoundEvents.BLOCK_ANVIL_FALL);
      SLIME = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SLIME_BLOCK_BREAK, SoundEvents.BLOCK_SLIME_BLOCK_STEP, SoundEvents.BLOCK_SLIME_BLOCK_PLACE, SoundEvents.BLOCK_SLIME_BLOCK_HIT, SoundEvents.BLOCK_SLIME_BLOCK_FALL);
      HONEY = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_HONEY_BLOCK_BREAK, SoundEvents.BLOCK_HONEY_BLOCK_STEP, SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundEvents.BLOCK_HONEY_BLOCK_HIT, SoundEvents.BLOCK_HONEY_BLOCK_FALL);
      WET_GRASS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_WET_GRASS_BREAK, SoundEvents.BLOCK_WET_GRASS_STEP, SoundEvents.BLOCK_WET_GRASS_PLACE, SoundEvents.BLOCK_WET_GRASS_HIT, SoundEvents.BLOCK_WET_GRASS_FALL);
      CORAL = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_CORAL_BLOCK_BREAK, SoundEvents.BLOCK_CORAL_BLOCK_STEP, SoundEvents.BLOCK_CORAL_BLOCK_PLACE, SoundEvents.BLOCK_CORAL_BLOCK_HIT, SoundEvents.BLOCK_CORAL_BLOCK_FALL);
      BAMBOO = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_BAMBOO_BREAK, SoundEvents.BLOCK_BAMBOO_STEP, SoundEvents.BLOCK_BAMBOO_PLACE, SoundEvents.BLOCK_BAMBOO_HIT, SoundEvents.BLOCK_BAMBOO_FALL);
      BAMBOO_SAPLING = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_BAMBOO_SAPLING_BREAK, SoundEvents.BLOCK_BAMBOO_STEP, SoundEvents.BLOCK_BAMBOO_SAPLING_PLACE, SoundEvents.BLOCK_BAMBOO_SAPLING_HIT, SoundEvents.BLOCK_BAMBOO_FALL);
      SCAFFOLDING = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SCAFFOLDING_BREAK, SoundEvents.BLOCK_SCAFFOLDING_STEP, SoundEvents.BLOCK_SCAFFOLDING_PLACE, SoundEvents.BLOCK_SCAFFOLDING_HIT, SoundEvents.BLOCK_SCAFFOLDING_FALL);
      SWEET_BERRY_BUSH = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SWEET_BERRY_BUSH_BREAK, SoundEvents.BLOCK_GRASS_STEP, SoundEvents.BLOCK_SWEET_BERRY_BUSH_PLACE, SoundEvents.BLOCK_GRASS_HIT, SoundEvents.BLOCK_GRASS_FALL);
      CROP = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_CROP_BREAK, SoundEvents.BLOCK_GRASS_STEP, SoundEvents.ITEM_CROP_PLANT, SoundEvents.BLOCK_GRASS_HIT, SoundEvents.BLOCK_GRASS_FALL);
      STEM = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_WOOD_BREAK, SoundEvents.BLOCK_WOOD_STEP, SoundEvents.ITEM_CROP_PLANT, SoundEvents.BLOCK_WOOD_HIT, SoundEvents.BLOCK_WOOD_FALL);
      VINE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_VINE_BREAK, SoundEvents.BLOCK_VINE_STEP, SoundEvents.BLOCK_VINE_PLACE, SoundEvents.BLOCK_VINE_HIT, SoundEvents.BLOCK_VINE_FALL);
      NETHER_WART = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_NETHER_WART_BREAK, SoundEvents.BLOCK_STONE_STEP, SoundEvents.ITEM_NETHER_WART_PLANT, SoundEvents.BLOCK_STONE_HIT, SoundEvents.BLOCK_STONE_FALL);
      LANTERN = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_LANTERN_BREAK, SoundEvents.BLOCK_LANTERN_STEP, SoundEvents.BLOCK_LANTERN_PLACE, SoundEvents.BLOCK_LANTERN_HIT, SoundEvents.BLOCK_LANTERN_FALL);
      NETHER_STEM = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_STEM_BREAK, SoundEvents.BLOCK_STEM_STEP, SoundEvents.BLOCK_STEM_PLACE, SoundEvents.BLOCK_STEM_HIT, SoundEvents.BLOCK_STEM_FALL);
      NYLIUM = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_NYLIUM_BREAK, SoundEvents.BLOCK_NYLIUM_STEP, SoundEvents.BLOCK_NYLIUM_PLACE, SoundEvents.BLOCK_NYLIUM_HIT, SoundEvents.BLOCK_NYLIUM_FALL);
      FUNGUS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_FUNGUS_BREAK, SoundEvents.BLOCK_FUNGUS_STEP, SoundEvents.BLOCK_FUNGUS_PLACE, SoundEvents.BLOCK_FUNGUS_HIT, SoundEvents.BLOCK_FUNGUS_FALL);
      ROOTS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_ROOTS_BREAK, SoundEvents.BLOCK_ROOTS_STEP, SoundEvents.BLOCK_ROOTS_PLACE, SoundEvents.BLOCK_ROOTS_HIT, SoundEvents.BLOCK_ROOTS_FALL);
      SHROOMLIGHT = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SHROOMLIGHT_BREAK, SoundEvents.BLOCK_SHROOMLIGHT_STEP, SoundEvents.BLOCK_SHROOMLIGHT_PLACE, SoundEvents.BLOCK_SHROOMLIGHT_HIT, SoundEvents.BLOCK_SHROOMLIGHT_FALL);
      WEEPING_VINES = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_WEEPING_VINES_BREAK, SoundEvents.BLOCK_WEEPING_VINES_STEP, SoundEvents.BLOCK_WEEPING_VINES_PLACE, SoundEvents.BLOCK_WEEPING_VINES_HIT, SoundEvents.BLOCK_WEEPING_VINES_FALL);
      WEEPING_VINES_LOW_PITCH = new BlockSoundGroup(1.0F, 0.5F, SoundEvents.BLOCK_WEEPING_VINES_BREAK, SoundEvents.BLOCK_WEEPING_VINES_STEP, SoundEvents.BLOCK_WEEPING_VINES_PLACE, SoundEvents.BLOCK_WEEPING_VINES_HIT, SoundEvents.BLOCK_WEEPING_VINES_FALL);
      SOUL_SAND = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SOUL_SAND_BREAK, SoundEvents.BLOCK_SOUL_SAND_STEP, SoundEvents.BLOCK_SOUL_SAND_PLACE, SoundEvents.BLOCK_SOUL_SAND_HIT, SoundEvents.BLOCK_SOUL_SAND_FALL);
      SOUL_SOIL = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SOUL_SOIL_BREAK, SoundEvents.BLOCK_SOUL_SOIL_STEP, SoundEvents.BLOCK_SOUL_SOIL_PLACE, SoundEvents.BLOCK_SOUL_SOIL_HIT, SoundEvents.BLOCK_SOUL_SOIL_FALL);
      BASALT = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_BASALT_BREAK, SoundEvents.BLOCK_BASALT_STEP, SoundEvents.BLOCK_BASALT_PLACE, SoundEvents.BLOCK_BASALT_HIT, SoundEvents.BLOCK_BASALT_FALL);
      WART_BLOCK = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_WART_BLOCK_BREAK, SoundEvents.BLOCK_WART_BLOCK_STEP, SoundEvents.BLOCK_WART_BLOCK_PLACE, SoundEvents.BLOCK_WART_BLOCK_HIT, SoundEvents.BLOCK_WART_BLOCK_FALL);
      NETHERRACK = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_NETHERRACK_BREAK, SoundEvents.BLOCK_NETHERRACK_STEP, SoundEvents.BLOCK_NETHERRACK_PLACE, SoundEvents.BLOCK_NETHERRACK_HIT, SoundEvents.BLOCK_NETHERRACK_FALL);
      NETHER_BRICKS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_NETHER_BRICKS_BREAK, SoundEvents.BLOCK_NETHER_BRICKS_STEP, SoundEvents.BLOCK_NETHER_BRICKS_PLACE, SoundEvents.BLOCK_NETHER_BRICKS_HIT, SoundEvents.BLOCK_NETHER_BRICKS_FALL);
      NETHER_SPROUTS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_NETHER_SPROUTS_BREAK, SoundEvents.BLOCK_NETHER_SPROUTS_STEP, SoundEvents.BLOCK_NETHER_SPROUTS_PLACE, SoundEvents.BLOCK_NETHER_SPROUTS_HIT, SoundEvents.BLOCK_NETHER_SPROUTS_FALL);
      NETHER_ORE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_NETHER_ORE_BREAK, SoundEvents.BLOCK_NETHER_ORE_STEP, SoundEvents.BLOCK_NETHER_ORE_PLACE, SoundEvents.BLOCK_NETHER_ORE_HIT, SoundEvents.BLOCK_NETHER_ORE_FALL);
      BONE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_BONE_BLOCK_BREAK, SoundEvents.BLOCK_BONE_BLOCK_STEP, SoundEvents.BLOCK_BONE_BLOCK_PLACE, SoundEvents.BLOCK_BONE_BLOCK_HIT, SoundEvents.BLOCK_BONE_BLOCK_FALL);
      NETHERITE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_NETHERITE_BLOCK_BREAK, SoundEvents.BLOCK_NETHERITE_BLOCK_STEP, SoundEvents.BLOCK_NETHERITE_BLOCK_PLACE, SoundEvents.BLOCK_NETHERITE_BLOCK_HIT, SoundEvents.BLOCK_NETHERITE_BLOCK_FALL);
      ANCIENT_DEBRIS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_ANCIENT_DEBRIS_BREAK, SoundEvents.BLOCK_ANCIENT_DEBRIS_STEP, SoundEvents.BLOCK_ANCIENT_DEBRIS_PLACE, SoundEvents.BLOCK_ANCIENT_DEBRIS_HIT, SoundEvents.BLOCK_ANCIENT_DEBRIS_FALL);
      LODESTONE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_LODESTONE_BREAK, SoundEvents.BLOCK_LODESTONE_STEP, SoundEvents.BLOCK_LODESTONE_PLACE, SoundEvents.BLOCK_LODESTONE_HIT, SoundEvents.BLOCK_LODESTONE_FALL);
      CHAIN = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_CHAIN_BREAK, SoundEvents.BLOCK_CHAIN_STEP, SoundEvents.BLOCK_CHAIN_PLACE, SoundEvents.BLOCK_CHAIN_HIT, SoundEvents.BLOCK_CHAIN_FALL);
      NETHER_GOLD_ORE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_NETHER_GOLD_ORE_BREAK, SoundEvents.BLOCK_NETHER_GOLD_ORE_STEP, SoundEvents.BLOCK_NETHER_GOLD_ORE_PLACE, SoundEvents.BLOCK_NETHER_GOLD_ORE_HIT, SoundEvents.BLOCK_NETHER_GOLD_ORE_FALL);
      GILDED_BLACKSTONE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_GILDED_BLACKSTONE_BREAK, SoundEvents.BLOCK_GILDED_BLACKSTONE_STEP, SoundEvents.BLOCK_GILDED_BLACKSTONE_PLACE, SoundEvents.BLOCK_GILDED_BLACKSTONE_HIT, SoundEvents.BLOCK_GILDED_BLACKSTONE_FALL);
      CANDLE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_CANDLE_BREAK, SoundEvents.BLOCK_CANDLE_STEP, SoundEvents.BLOCK_CANDLE_PLACE, SoundEvents.BLOCK_CANDLE_HIT, SoundEvents.BLOCK_CANDLE_FALL);
      AMETHYST_BLOCK = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, SoundEvents.BLOCK_AMETHYST_BLOCK_STEP, SoundEvents.BLOCK_AMETHYST_BLOCK_PLACE, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundEvents.BLOCK_AMETHYST_BLOCK_FALL);
      AMETHYST_CLUSTER = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundEvents.BLOCK_AMETHYST_CLUSTER_PLACE, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundEvents.BLOCK_AMETHYST_CLUSTER_FALL);
      SMALL_AMETHYST_BUD = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SMALL_AMETHYST_BUD_BREAK, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundEvents.BLOCK_SMALL_AMETHYST_BUD_PLACE, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundEvents.BLOCK_AMETHYST_CLUSTER_FALL);
      MEDIUM_AMETHYST_BUD = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_MEDIUM_AMETHYST_BUD_BREAK, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundEvents.BLOCK_MEDIUM_AMETHYST_BUD_PLACE, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundEvents.BLOCK_AMETHYST_CLUSTER_FALL);
      LARGE_AMETHYST_BUD = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_LARGE_AMETHYST_BUD_BREAK, SoundEvents.BLOCK_AMETHYST_CLUSTER_STEP, SoundEvents.BLOCK_LARGE_AMETHYST_BUD_PLACE, SoundEvents.BLOCK_AMETHYST_CLUSTER_HIT, SoundEvents.BLOCK_AMETHYST_CLUSTER_FALL);
      TUFF = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_TUFF_BREAK, SoundEvents.BLOCK_TUFF_STEP, SoundEvents.BLOCK_TUFF_PLACE, SoundEvents.BLOCK_TUFF_HIT, SoundEvents.BLOCK_TUFF_FALL);
      CALCITE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_CALCITE_BREAK, SoundEvents.BLOCK_CALCITE_STEP, SoundEvents.BLOCK_CALCITE_PLACE, SoundEvents.BLOCK_CALCITE_HIT, SoundEvents.BLOCK_CALCITE_FALL);
      DRIPSTONE_BLOCK = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_DRIPSTONE_BLOCK_BREAK, SoundEvents.BLOCK_DRIPSTONE_BLOCK_STEP, SoundEvents.BLOCK_DRIPSTONE_BLOCK_PLACE, SoundEvents.BLOCK_DRIPSTONE_BLOCK_HIT, SoundEvents.BLOCK_DRIPSTONE_BLOCK_FALL);
      POINTED_DRIPSTONE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_POINTED_DRIPSTONE_BREAK, SoundEvents.BLOCK_POINTED_DRIPSTONE_STEP, SoundEvents.BLOCK_POINTED_DRIPSTONE_PLACE, SoundEvents.BLOCK_POINTED_DRIPSTONE_HIT, SoundEvents.BLOCK_POINTED_DRIPSTONE_FALL);
      COPPER = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_COPPER_BREAK, SoundEvents.BLOCK_COPPER_STEP, SoundEvents.BLOCK_COPPER_PLACE, SoundEvents.BLOCK_COPPER_HIT, SoundEvents.BLOCK_COPPER_FALL);
      CAVE_VINES = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_CAVE_VINES_BREAK, SoundEvents.BLOCK_CAVE_VINES_STEP, SoundEvents.BLOCK_CAVE_VINES_PLACE, SoundEvents.BLOCK_CAVE_VINES_HIT, SoundEvents.BLOCK_CAVE_VINES_FALL);
      SPORE_BLOSSOM = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SPORE_BLOSSOM_BREAK, SoundEvents.BLOCK_SPORE_BLOSSOM_STEP, SoundEvents.BLOCK_SPORE_BLOSSOM_PLACE, SoundEvents.BLOCK_SPORE_BLOSSOM_HIT, SoundEvents.BLOCK_SPORE_BLOSSOM_FALL);
      AZALEA = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_AZALEA_BREAK, SoundEvents.BLOCK_AZALEA_STEP, SoundEvents.BLOCK_AZALEA_PLACE, SoundEvents.BLOCK_AZALEA_HIT, SoundEvents.BLOCK_AZALEA_FALL);
      FLOWERING_AZALEA = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_FLOWERING_AZALEA_BREAK, SoundEvents.BLOCK_FLOWERING_AZALEA_STEP, SoundEvents.BLOCK_FLOWERING_AZALEA_PLACE, SoundEvents.BLOCK_FLOWERING_AZALEA_HIT, SoundEvents.BLOCK_FLOWERING_AZALEA_FALL);
      MOSS_CARPET = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_MOSS_CARPET_BREAK, SoundEvents.BLOCK_MOSS_CARPET_STEP, SoundEvents.BLOCK_MOSS_CARPET_PLACE, SoundEvents.BLOCK_MOSS_CARPET_HIT, SoundEvents.BLOCK_MOSS_CARPET_FALL);
      MOSS_BLOCK = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_MOSS_BREAK, SoundEvents.BLOCK_MOSS_STEP, SoundEvents.BLOCK_MOSS_PLACE, SoundEvents.BLOCK_MOSS_HIT, SoundEvents.BLOCK_MOSS_FALL);
      BIG_DRIPLEAF = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_BIG_DRIPLEAF_BREAK, SoundEvents.BLOCK_BIG_DRIPLEAF_STEP, SoundEvents.BLOCK_BIG_DRIPLEAF_PLACE, SoundEvents.BLOCK_BIG_DRIPLEAF_HIT, SoundEvents.BLOCK_BIG_DRIPLEAF_FALL);
      SMALL_DRIPLEAF = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SMALL_DRIPLEAF_BREAK, SoundEvents.BLOCK_SMALL_DRIPLEAF_STEP, SoundEvents.BLOCK_SMALL_DRIPLEAF_PLACE, SoundEvents.BLOCK_SMALL_DRIPLEAF_HIT, SoundEvents.BLOCK_SMALL_DRIPLEAF_FALL);
      ROOTED_DIRT = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundEvents.BLOCK_ROOTED_DIRT_STEP, SoundEvents.BLOCK_ROOTED_DIRT_PLACE, SoundEvents.BLOCK_ROOTED_DIRT_HIT, SoundEvents.BLOCK_ROOTED_DIRT_FALL);
      HANGING_ROOTS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_HANGING_ROOTS_BREAK, SoundEvents.BLOCK_HANGING_ROOTS_STEP, SoundEvents.BLOCK_HANGING_ROOTS_PLACE, SoundEvents.BLOCK_HANGING_ROOTS_HIT, SoundEvents.BLOCK_HANGING_ROOTS_FALL);
      AZALEA_LEAVES = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, SoundEvents.BLOCK_AZALEA_LEAVES_STEP, SoundEvents.BLOCK_AZALEA_LEAVES_PLACE, SoundEvents.BLOCK_AZALEA_LEAVES_HIT, SoundEvents.BLOCK_AZALEA_LEAVES_FALL);
      SCULK_SENSOR = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_SCULK_SENSOR_BREAK, SoundEvents.BLOCK_SCULK_SENSOR_STEP, SoundEvents.BLOCK_SCULK_SENSOR_PLACE, SoundEvents.BLOCK_SCULK_SENSOR_HIT, SoundEvents.BLOCK_SCULK_SENSOR_FALL);
      GLOW_LICHEN = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_GRASS_BREAK, SoundEvents.BLOCK_VINE_STEP, SoundEvents.BLOCK_GRASS_PLACE, SoundEvents.BLOCK_GRASS_HIT, SoundEvents.BLOCK_GRASS_FALL);
      DEEPSLATE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_DEEPSLATE_BREAK, SoundEvents.BLOCK_DEEPSLATE_STEP, SoundEvents.BLOCK_DEEPSLATE_PLACE, SoundEvents.BLOCK_DEEPSLATE_HIT, SoundEvents.BLOCK_DEEPSLATE_FALL);
      DEEPSLATE_BRICKS = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_DEEPSLATE_BRICKS_BREAK, SoundEvents.BLOCK_DEEPSLATE_BRICKS_STEP, SoundEvents.BLOCK_DEEPSLATE_BRICKS_PLACE, SoundEvents.BLOCK_DEEPSLATE_BRICKS_HIT, SoundEvents.BLOCK_DEEPSLATE_BRICKS_FALL);
      DEEPSLATE_TILES = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_DEEPSLATE_TILES_BREAK, SoundEvents.BLOCK_DEEPSLATE_TILES_STEP, SoundEvents.BLOCK_DEEPSLATE_TILES_PLACE, SoundEvents.BLOCK_DEEPSLATE_TILES_HIT, SoundEvents.BLOCK_DEEPSLATE_TILES_FALL);
      POLISHED_DEEPSLATE = new BlockSoundGroup(1.0F, 1.0F, SoundEvents.BLOCK_POLISHED_DEEPSLATE_BREAK, SoundEvents.BLOCK_POLISHED_DEEPSLATE_STEP, SoundEvents.BLOCK_POLISHED_DEEPSLATE_PLACE, SoundEvents.BLOCK_POLISHED_DEEPSLATE_HIT, SoundEvents.BLOCK_POLISHED_DEEPSLATE_FALL);
   }
}
