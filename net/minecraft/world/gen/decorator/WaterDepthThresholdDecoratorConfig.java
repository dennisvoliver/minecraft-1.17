package net.minecraft.world.gen.decorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;

public class WaterDepthThresholdDecoratorConfig implements DecoratorConfig {
   public static final Codec<WaterDepthThresholdDecoratorConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(Codec.INT.fieldOf("max_water_depth").forGetter((waterDepthThresholdDecoratorConfig) -> {
         return waterDepthThresholdDecoratorConfig.maxWaterDepth;
      })).apply(instance, (Function)(WaterDepthThresholdDecoratorConfig::new));
   });
   public final int maxWaterDepth;

   public WaterDepthThresholdDecoratorConfig(int maxWaterDepth) {
      this.maxWaterDepth = maxWaterDepth;
   }
}
