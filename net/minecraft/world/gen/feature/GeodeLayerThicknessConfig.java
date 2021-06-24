package net.minecraft.world.gen.feature;

import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class GeodeLayerThicknessConfig {
   private static final Codec<Double> RANGE = Codec.doubleRange(0.01D, 50.0D);
   public static final Codec<GeodeLayerThicknessConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(RANGE.fieldOf("filling").orElse(1.7D).forGetter((geodeLayerThicknessConfig) -> {
         return geodeLayerThicknessConfig.filling;
      }), RANGE.fieldOf("inner_layer").orElse(2.2D).forGetter((geodeLayerThicknessConfig) -> {
         return geodeLayerThicknessConfig.innerLayer;
      }), RANGE.fieldOf("middle_layer").orElse(3.2D).forGetter((geodeLayerThicknessConfig) -> {
         return geodeLayerThicknessConfig.middleLayer;
      }), RANGE.fieldOf("outer_layer").orElse(4.2D).forGetter((geodeLayerThicknessConfig) -> {
         return geodeLayerThicknessConfig.outerLayer;
      })).apply(instance, (Function4)(GeodeLayerThicknessConfig::new));
   });
   public final double filling;
   public final double innerLayer;
   public final double middleLayer;
   public final double outerLayer;

   public GeodeLayerThicknessConfig(double filling, double innerLayer, double middleLayer, double outerLayer) {
      this.filling = filling;
      this.innerLayer = innerLayer;
      this.middleLayer = middleLayer;
      this.outerLayer = outerLayer;
   }
}
