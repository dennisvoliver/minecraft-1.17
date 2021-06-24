package net.minecraft.world.gen.decorator;

import com.mojang.serialization.Codec;
import java.util.Random;
import java.util.stream.Stream;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.CountConfig;

public abstract class Decorator<DC extends DecoratorConfig> {
   public static final Decorator<NopeDecoratorConfig> NOPE;
   public static final Decorator<DecoratedDecoratorConfig> DECORATED;
   public static final Decorator<CarvingMaskDecoratorConfig> CARVING_MASK;
   public static final Decorator<CountConfig> COUNT_MULTILAYER;
   public static final Decorator<NopeDecoratorConfig> SQUARE;
   public static final Decorator<NopeDecoratorConfig> DARK_OAK_TREE;
   public static final Decorator<NopeDecoratorConfig> ICEBERG;
   public static final Decorator<ChanceDecoratorConfig> CHANCE;
   public static final Decorator<CountConfig> COUNT;
   public static final Decorator<CountNoiseDecoratorConfig> COUNT_NOISE;
   public static final Decorator<CountNoiseBiasedDecoratorConfig> COUNT_NOISE_BIASED;
   public static final Decorator<CountExtraDecoratorConfig> COUNT_EXTRA;
   public static final Decorator<ChanceDecoratorConfig> LAVA_LAKE;
   public static final Decorator<HeightmapDecoratorConfig> HEIGHTMAP;
   public static final Decorator<HeightmapDecoratorConfig> HEIGHTMAP_SPREAD_DOUBLE;
   public static final Decorator<WaterDepthThresholdDecoratorConfig> WATER_DEPTH_THRESHOLD;
   public static final Decorator<CaveSurfaceDecoratorConfig> CAVE_SURFACE;
   public static final Decorator<RangeDecoratorConfig> RANGE;
   public static final Decorator<NopeDecoratorConfig> SPREAD_32_ABOVE;
   public static final Decorator<NopeDecoratorConfig> END_GATEWAY;
   private final Codec<ConfiguredDecorator<DC>> codec;

   private static <T extends DecoratorConfig, G extends Decorator<T>> G register(String registryName, G decorator) {
      return (Decorator)Registry.register(Registry.DECORATOR, (String)registryName, decorator);
   }

   public Decorator(Codec<DC> configCodec) {
      this.codec = configCodec.fieldOf("config").xmap((decoratorConfig) -> {
         return new ConfiguredDecorator(this, decoratorConfig);
      }, ConfiguredDecorator::getConfig).codec();
   }

   public ConfiguredDecorator<DC> configure(DC config) {
      return new ConfiguredDecorator(this, config);
   }

   public Codec<ConfiguredDecorator<DC>> getCodec() {
      return this.codec;
   }

   public abstract Stream<BlockPos> getPositions(DecoratorContext context, Random random, DC config, BlockPos pos);

   public String toString() {
      String var10000 = this.getClass().getSimpleName();
      return var10000 + "@" + Integer.toHexString(this.hashCode());
   }

   static {
      NOPE = register("nope", new NopeDecorator(NopeDecoratorConfig.CODEC));
      DECORATED = register("decorated", new DecoratedDecorator(DecoratedDecoratorConfig.CODEC));
      CARVING_MASK = register("carving_mask", new CarvingMaskDecorator(CarvingMaskDecoratorConfig.CODEC));
      COUNT_MULTILAYER = register("count_multilayer", new CountMultilayerDecorator(CountConfig.CODEC));
      SQUARE = register("square", new SquareDecorator(NopeDecoratorConfig.CODEC));
      DARK_OAK_TREE = register("dark_oak_tree", new DarkOakTreeDecorator(NopeDecoratorConfig.CODEC));
      ICEBERG = register("iceberg", new IcebergDecorator(NopeDecoratorConfig.CODEC));
      CHANCE = register("chance", new ChanceDecorator(ChanceDecoratorConfig.CODEC));
      COUNT = register("count", new CountDecorator(CountConfig.CODEC));
      COUNT_NOISE = register("count_noise", new CountNoiseDecorator(CountNoiseDecoratorConfig.CODEC));
      COUNT_NOISE_BIASED = register("count_noise_biased", new CountNoiseBiasedDecorator(CountNoiseBiasedDecoratorConfig.CODEC));
      COUNT_EXTRA = register("count_extra", new CountExtraDecorator(CountExtraDecoratorConfig.CODEC));
      LAVA_LAKE = register("lava_lake", new LavaLakeDecorator(ChanceDecoratorConfig.CODEC));
      HEIGHTMAP = register("heightmap", new HeightmapDecorator(HeightmapDecoratorConfig.CODEC));
      HEIGHTMAP_SPREAD_DOUBLE = register("heightmap_spread_double", new SpreadDoubleHeightmapDecorator(HeightmapDecoratorConfig.CODEC));
      WATER_DEPTH_THRESHOLD = register("water_depth_threshold", new WaterDepthThresholdDecorator(WaterDepthThresholdDecoratorConfig.CODEC));
      CAVE_SURFACE = register("cave_surface", new CaveSurfaceDecorator(CaveSurfaceDecoratorConfig.CODEC));
      RANGE = register("range", new RangeDecorator(RangeDecoratorConfig.CODEC));
      SPREAD_32_ABOVE = register("spread_32_above", new Spread32AboveDecorator(NopeDecoratorConfig.CODEC));
      END_GATEWAY = register("end_gateway", new EndGatewayDecorator(NopeDecoratorConfig.CODEC));
   }
}
