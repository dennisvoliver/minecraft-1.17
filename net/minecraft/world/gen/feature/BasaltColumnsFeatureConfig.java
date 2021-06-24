package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiFunction;
import net.minecraft.util.math.intprovider.IntProvider;

public class BasaltColumnsFeatureConfig implements FeatureConfig {
   public static final Codec<BasaltColumnsFeatureConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(IntProvider.createValidatingCodec(0, 3).fieldOf("reach").forGetter((basaltColumnsFeatureConfig) -> {
         return basaltColumnsFeatureConfig.reach;
      }), IntProvider.createValidatingCodec(1, 10).fieldOf("height").forGetter((basaltColumnsFeatureConfig) -> {
         return basaltColumnsFeatureConfig.height;
      })).apply(instance, (BiFunction)(BasaltColumnsFeatureConfig::new));
   });
   private final IntProvider reach;
   private final IntProvider height;

   public BasaltColumnsFeatureConfig(IntProvider reach, IntProvider height) {
      this.reach = reach;
      this.height = height;
   }

   public IntProvider getReach() {
      return this.reach;
   }

   public IntProvider getHeight() {
      return this.height;
   }
}
