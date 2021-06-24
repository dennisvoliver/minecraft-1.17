package net.minecraft.world.gen.feature;

import com.mojang.datafixers.util.Function5;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.collection.DataPool;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.gen.stateprovider.BlockStateProvider;

public class GrowingPlantFeatureConfig implements FeatureConfig {
   public static final Codec<GrowingPlantFeatureConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(DataPool.createCodec(IntProvider.VALUE_CODEC).fieldOf("height_distribution").forGetter((growingPlantFeatureConfig) -> {
         return growingPlantFeatureConfig.heightDistribution;
      }), Direction.CODEC.fieldOf("direction").forGetter((growingPlantFeatureConfig) -> {
         return growingPlantFeatureConfig.direction;
      }), BlockStateProvider.TYPE_CODEC.fieldOf("body_provider").forGetter((growingPlantFeatureConfig) -> {
         return growingPlantFeatureConfig.bodyProvider;
      }), BlockStateProvider.TYPE_CODEC.fieldOf("head_provider").forGetter((growingPlantFeatureConfig) -> {
         return growingPlantFeatureConfig.headProvider;
      }), Codec.BOOL.fieldOf("allow_water").forGetter((growingPlantFeatureConfig) -> {
         return growingPlantFeatureConfig.allowWater;
      })).apply(instance, (Function5)(GrowingPlantFeatureConfig::new));
   });
   public final DataPool<IntProvider> heightDistribution;
   public final Direction direction;
   public final BlockStateProvider bodyProvider;
   public final BlockStateProvider headProvider;
   public final boolean allowWater;

   public GrowingPlantFeatureConfig(DataPool<IntProvider> heightDistribution, Direction direction, BlockStateProvider bodyProvider, BlockStateProvider headProvider, boolean allowWater) {
      this.heightDistribution = heightDistribution;
      this.direction = direction;
      this.bodyProvider = bodyProvider;
      this.headProvider = headProvider;
      this.allowWater = allowWater;
   }
}
