package net.minecraft.world.gen.decorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiFunction;
import net.minecraft.util.math.VerticalSurfaceType;

public class CaveSurfaceDecoratorConfig implements DecoratorConfig {
   public static final Codec<CaveSurfaceDecoratorConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(VerticalSurfaceType.CODEC.fieldOf("surface").forGetter((caveSurfaceDecoratorConfig) -> {
         return caveSurfaceDecoratorConfig.surface;
      }), Codec.INT.fieldOf("floor_to_ceiling_search_range").forGetter((caveSurfaceDecoratorConfig) -> {
         return caveSurfaceDecoratorConfig.searchRange;
      })).apply(instance, (BiFunction)(CaveSurfaceDecoratorConfig::new));
   });
   public final VerticalSurfaceType surface;
   public final int searchRange;

   public CaveSurfaceDecoratorConfig(VerticalSurfaceType surface, int searchRange) {
      this.surface = surface;
      this.searchRange = searchRange;
   }
}
