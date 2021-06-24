package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ModifiableWorld;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.CountConfig;
import net.minecraft.world.gen.ProbabilityConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;

public abstract class Feature<FC extends FeatureConfig> {
   public static final Feature<DefaultFeatureConfig> NO_OP;
   public static final Feature<TreeFeatureConfig> TREE;
   public static final FlowerFeature<RandomPatchFeatureConfig> FLOWER;
   public static final FlowerFeature<RandomPatchFeatureConfig> NO_BONEMEAL_FLOWER;
   public static final Feature<RandomPatchFeatureConfig> RANDOM_PATCH;
   public static final Feature<BlockPileFeatureConfig> BLOCK_PILE;
   public static final Feature<SpringFeatureConfig> SPRING_FEATURE;
   public static final Feature<DefaultFeatureConfig> CHORUS_PLANT;
   public static final Feature<EmeraldOreFeatureConfig> REPLACE_SINGLE_BLOCK;
   public static final Feature<DefaultFeatureConfig> VOID_START_PLATFORM;
   public static final Feature<DefaultFeatureConfig> DESERT_WELL;
   public static final Feature<FossilFeatureConfig> FOSSIL;
   public static final Feature<HugeMushroomFeatureConfig> HUGE_RED_MUSHROOM;
   public static final Feature<HugeMushroomFeatureConfig> HUGE_BROWN_MUSHROOM;
   public static final Feature<DefaultFeatureConfig> ICE_SPIKE;
   public static final Feature<DefaultFeatureConfig> GLOWSTONE_BLOB;
   public static final Feature<DefaultFeatureConfig> FREEZE_TOP_LAYER;
   public static final Feature<DefaultFeatureConfig> VINES;
   public static final Feature<GrowingPlantFeatureConfig> GROWING_PLANT;
   public static final Feature<VegetationPatchFeatureConfig> VEGETATION_PATCH;
   public static final Feature<VegetationPatchFeatureConfig> WATERLOGGED_VEGETATION_PATCH;
   public static final Feature<RootSystemFeatureConfig> ROOT_SYSTEM;
   public static final Feature<GlowLichenFeatureConfig> GLOW_LICHEN;
   public static final Feature<UnderwaterMagmaFeatureConfig> UNDERWATER_MAGMA;
   public static final Feature<DefaultFeatureConfig> MONSTER_ROOM;
   public static final Feature<DefaultFeatureConfig> BLUE_ICE;
   public static final Feature<SingleStateFeatureConfig> ICEBERG;
   public static final Feature<SingleStateFeatureConfig> FOREST_ROCK;
   public static final Feature<DiskFeatureConfig> DISK;
   public static final Feature<DiskFeatureConfig> ICE_PATCH;
   public static final Feature<SingleStateFeatureConfig> LAKE;
   public static final Feature<OreFeatureConfig> ORE;
   public static final Feature<EndSpikeFeatureConfig> END_SPIKE;
   public static final Feature<DefaultFeatureConfig> END_ISLAND;
   public static final Feature<EndGatewayFeatureConfig> END_GATEWAY;
   public static final SeagrassFeature SEAGRASS;
   public static final Feature<DefaultFeatureConfig> KELP;
   public static final Feature<DefaultFeatureConfig> CORAL_TREE;
   public static final Feature<DefaultFeatureConfig> CORAL_MUSHROOM;
   public static final Feature<DefaultFeatureConfig> CORAL_CLAW;
   public static final Feature<CountConfig> SEA_PICKLE;
   public static final Feature<SimpleBlockFeatureConfig> SIMPLE_BLOCK;
   public static final Feature<ProbabilityConfig> BAMBOO;
   public static final Feature<HugeFungusFeatureConfig> HUGE_FUNGUS;
   public static final Feature<BlockPileFeatureConfig> NETHER_FOREST_VEGETATION;
   public static final Feature<DefaultFeatureConfig> WEEPING_VINES;
   public static final Feature<DefaultFeatureConfig> TWISTING_VINES;
   public static final Feature<BasaltColumnsFeatureConfig> BASALT_COLUMNS;
   public static final Feature<DeltaFeatureConfig> DELTA_FEATURE;
   public static final Feature<ReplaceBlobsFeatureConfig> NETHERRACK_REPLACE_BLOBS;
   public static final Feature<FillLayerFeatureConfig> FILL_LAYER;
   public static final BonusChestFeature BONUS_CHEST;
   public static final Feature<DefaultFeatureConfig> BASALT_PILLAR;
   public static final Feature<OreFeatureConfig> SCATTERED_ORE;
   public static final Feature<RandomFeatureConfig> RANDOM_SELECTOR;
   public static final Feature<SimpleRandomFeatureConfig> SIMPLE_RANDOM_SELECTOR;
   public static final Feature<RandomBooleanFeatureConfig> RANDOM_BOOLEAN_SELECTOR;
   public static final Feature<DecoratedFeatureConfig> DECORATED;
   public static final Feature<GeodeFeatureConfig> GEODE;
   public static final Feature<DripstoneClusterFeatureConfig> DRIPSTONE_CLUSTER;
   public static final Feature<LargeDripstoneFeatureConfig> LARGE_DRIPSTONE;
   public static final Feature<SmallDripstoneFeatureConfig> SMALL_DRIPSTONE;
   private final Codec<ConfiguredFeature<FC, Feature<FC>>> codec;

   private static <C extends FeatureConfig, F extends Feature<C>> F register(String name, F feature) {
      return (Feature)Registry.register(Registry.FEATURE, (String)name, feature);
   }

   public Feature(Codec<FC> configCodec) {
      this.codec = configCodec.fieldOf("config").xmap((config) -> {
         return new ConfiguredFeature(this, config);
      }, (feature) -> {
         return feature.config;
      }).codec();
   }

   public Codec<ConfiguredFeature<FC, Feature<FC>>> getCodec() {
      return this.codec;
   }

   public ConfiguredFeature<FC, ?> configure(FC config) {
      return new ConfiguredFeature(this, config);
   }

   protected void setBlockState(ModifiableWorld world, BlockPos pos, BlockState state) {
      world.setBlockState(pos, state, Block.NOTIFY_ALL);
   }

   public static Predicate<BlockState> notInBlockTagPredicate(Identifier tagId) {
      Tag<Block> tag = BlockTags.getTagGroup().getTag(tagId);
      return tag == null ? (state) -> {
         return true;
      } : (state) -> {
         return !state.isIn(tag);
      };
   }

   protected void setBlockStateIf(StructureWorldAccess world, BlockPos pos, BlockState state, Predicate<BlockState> predicate) {
      if (predicate.test(world.getBlockState(pos))) {
         world.setBlockState(pos, state, Block.NOTIFY_LISTENERS);
      }

   }

   public abstract boolean generate(FeatureContext<FC> context);

   protected static boolean isStone(BlockState state) {
      return state.isIn(BlockTags.BASE_STONE_OVERWORLD);
   }

   public static boolean isSoil(BlockState state) {
      return state.isIn(BlockTags.DIRT);
   }

   public static boolean isSoil(TestableWorld world, BlockPos pos) {
      return world.testBlockState(pos, Feature::isSoil);
   }

   public static boolean isAir(TestableWorld world, BlockPos pos) {
      return world.testBlockState(pos, AbstractBlock.AbstractBlockState::isAir);
   }

   public static boolean testAdjacentStates(Function<BlockPos, BlockState> posToState, BlockPos pos, Predicate<BlockState> predicate) {
      BlockPos.Mutable mutable = new BlockPos.Mutable();
      Direction[] var4 = Direction.values();
      int var5 = var4.length;

      for(int var6 = 0; var6 < var5; ++var6) {
         Direction direction = var4[var6];
         mutable.set(pos, (Direction)direction);
         if (predicate.test((BlockState)posToState.apply(mutable))) {
            return true;
         }
      }

      return false;
   }

   public static boolean isExposedToAir(Function<BlockPos, BlockState> posToState, BlockPos pos) {
      return testAdjacentStates(posToState, pos, AbstractBlock.AbstractBlockState::isAir);
   }

   protected void markBlocksAboveForPostProcessing(StructureWorldAccess world, BlockPos pos) {
      BlockPos.Mutable mutable = pos.mutableCopy();

      for(int i = 0; i < 2; ++i) {
         mutable.move(Direction.UP);
         if (world.getBlockState(mutable).isAir()) {
            return;
         }

         world.getChunk(mutable).markBlockForPostProcessing(mutable);
      }

   }

   static {
      NO_OP = register("no_op", new NoOpFeature(DefaultFeatureConfig.CODEC));
      TREE = register("tree", new TreeFeature(TreeFeatureConfig.CODEC));
      FLOWER = (FlowerFeature)register("flower", new DefaultFlowerFeature(RandomPatchFeatureConfig.CODEC));
      NO_BONEMEAL_FLOWER = (FlowerFeature)register("no_bonemeal_flower", new DefaultFlowerFeature(RandomPatchFeatureConfig.CODEC));
      RANDOM_PATCH = register("random_patch", new RandomPatchFeature(RandomPatchFeatureConfig.CODEC));
      BLOCK_PILE = register("block_pile", new BlockPileFeature(BlockPileFeatureConfig.CODEC));
      SPRING_FEATURE = register("spring_feature", new SpringFeature(SpringFeatureConfig.CODEC));
      CHORUS_PLANT = register("chorus_plant", new ChorusPlantFeature(DefaultFeatureConfig.CODEC));
      REPLACE_SINGLE_BLOCK = register("replace_single_block", new EmeraldOreFeature(EmeraldOreFeatureConfig.CODEC));
      VOID_START_PLATFORM = register("void_start_platform", new VoidStartPlatformFeature(DefaultFeatureConfig.CODEC));
      DESERT_WELL = register("desert_well", new DesertWellFeature(DefaultFeatureConfig.CODEC));
      FOSSIL = register("fossil", new FossilFeature(FossilFeatureConfig.CODEC));
      HUGE_RED_MUSHROOM = register("huge_red_mushroom", new HugeRedMushroomFeature(HugeMushroomFeatureConfig.CODEC));
      HUGE_BROWN_MUSHROOM = register("huge_brown_mushroom", new HugeBrownMushroomFeature(HugeMushroomFeatureConfig.CODEC));
      ICE_SPIKE = register("ice_spike", new IceSpikeFeature(DefaultFeatureConfig.CODEC));
      GLOWSTONE_BLOB = register("glowstone_blob", new GlowstoneBlobFeature(DefaultFeatureConfig.CODEC));
      FREEZE_TOP_LAYER = register("freeze_top_layer", new FreezeTopLayerFeature(DefaultFeatureConfig.CODEC));
      VINES = register("vines", new VinesFeature(DefaultFeatureConfig.CODEC));
      GROWING_PLANT = register("growing_plant", new GrowingPlantFeature(GrowingPlantFeatureConfig.CODEC));
      VEGETATION_PATCH = register("vegetation_patch", new VegetationPatchFeature(VegetationPatchFeatureConfig.CODEC));
      WATERLOGGED_VEGETATION_PATCH = register("waterlogged_vegetation_patch", new WaterloggedVegetationPatchFeature(VegetationPatchFeatureConfig.CODEC));
      ROOT_SYSTEM = register("root_system", new RootSystemFeature(RootSystemFeatureConfig.CODEC));
      GLOW_LICHEN = register("glow_lichen", new GlowLichenFeature(GlowLichenFeatureConfig.CODEC));
      UNDERWATER_MAGMA = register("underwater_magma", new UnderwaterMagmaFeature(UnderwaterMagmaFeatureConfig.CODEC));
      MONSTER_ROOM = register("monster_room", new DungeonFeature(DefaultFeatureConfig.CODEC));
      BLUE_ICE = register("blue_ice", new BlueIceFeature(DefaultFeatureConfig.CODEC));
      ICEBERG = register("iceberg", new IcebergFeature(SingleStateFeatureConfig.CODEC));
      FOREST_ROCK = register("forest_rock", new ForestRockFeature(SingleStateFeatureConfig.CODEC));
      DISK = register("disk", new UnderwaterDiskFeature(DiskFeatureConfig.CODEC));
      ICE_PATCH = register("ice_patch", new IcePatchFeature(DiskFeatureConfig.CODEC));
      LAKE = register("lake", new LakeFeature(SingleStateFeatureConfig.CODEC));
      ORE = register("ore", new OreFeature(OreFeatureConfig.CODEC));
      END_SPIKE = register("end_spike", new EndSpikeFeature(EndSpikeFeatureConfig.CODEC));
      END_ISLAND = register("end_island", new EndIslandFeature(DefaultFeatureConfig.CODEC));
      END_GATEWAY = register("end_gateway", new EndGatewayFeature(EndGatewayFeatureConfig.CODEC));
      SEAGRASS = (SeagrassFeature)register("seagrass", new SeagrassFeature(ProbabilityConfig.CODEC));
      KELP = register("kelp", new KelpFeature(DefaultFeatureConfig.CODEC));
      CORAL_TREE = register("coral_tree", new CoralTreeFeature(DefaultFeatureConfig.CODEC));
      CORAL_MUSHROOM = register("coral_mushroom", new CoralMushroomFeature(DefaultFeatureConfig.CODEC));
      CORAL_CLAW = register("coral_claw", new CoralClawFeature(DefaultFeatureConfig.CODEC));
      SEA_PICKLE = register("sea_pickle", new SeaPickleFeature(CountConfig.CODEC));
      SIMPLE_BLOCK = register("simple_block", new SimpleBlockFeature(SimpleBlockFeatureConfig.CODEC));
      BAMBOO = register("bamboo", new BambooFeature(ProbabilityConfig.CODEC));
      HUGE_FUNGUS = register("huge_fungus", new HugeFungusFeature(HugeFungusFeatureConfig.CODEC));
      NETHER_FOREST_VEGETATION = register("nether_forest_vegetation", new NetherForestVegetationFeature(BlockPileFeatureConfig.CODEC));
      WEEPING_VINES = register("weeping_vines", new WeepingVinesFeature(DefaultFeatureConfig.CODEC));
      TWISTING_VINES = register("twisting_vines", new TwistingVinesFeature(DefaultFeatureConfig.CODEC));
      BASALT_COLUMNS = register("basalt_columns", new BasaltColumnsFeature(BasaltColumnsFeatureConfig.CODEC));
      DELTA_FEATURE = register("delta_feature", new DeltaFeature(DeltaFeatureConfig.CODEC));
      NETHERRACK_REPLACE_BLOBS = register("netherrack_replace_blobs", new ReplaceBlobsFeature(ReplaceBlobsFeatureConfig.CODEC));
      FILL_LAYER = register("fill_layer", new FillLayerFeature(FillLayerFeatureConfig.CODEC));
      BONUS_CHEST = (BonusChestFeature)register("bonus_chest", new BonusChestFeature(DefaultFeatureConfig.CODEC));
      BASALT_PILLAR = register("basalt_pillar", new BasaltPillarFeature(DefaultFeatureConfig.CODEC));
      SCATTERED_ORE = register("scattered_ore", new ScatteredOreFeature(OreFeatureConfig.CODEC));
      RANDOM_SELECTOR = register("random_selector", new RandomFeature(RandomFeatureConfig.CODEC));
      SIMPLE_RANDOM_SELECTOR = register("simple_random_selector", new SimpleRandomFeature(SimpleRandomFeatureConfig.CODEC));
      RANDOM_BOOLEAN_SELECTOR = register("random_boolean_selector", new RandomBooleanFeature(RandomBooleanFeatureConfig.CODEC));
      DECORATED = register("decorated", new DecoratedFeature(DecoratedFeatureConfig.CODEC));
      GEODE = register("geode", new GeodeFeature(GeodeFeatureConfig.CODEC));
      DRIPSTONE_CLUSTER = register("dripstone_cluster", new DripstoneClusterFeature(DripstoneClusterFeatureConfig.CODEC));
      LARGE_DRIPSTONE = register("large_dripstone", new LargeDripstoneFeature(LargeDripstoneFeatureConfig.CODEC));
      SMALL_DRIPSTONE = register("small_dripstone", new SmallDripstoneFeature(SmallDripstoneFeatureConfig.CODEC));
   }
}
