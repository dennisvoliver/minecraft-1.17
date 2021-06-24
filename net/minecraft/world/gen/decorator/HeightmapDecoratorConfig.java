package net.minecraft.world.gen.decorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Function;
import net.minecraft.world.Heightmap;

public class HeightmapDecoratorConfig implements DecoratorConfig {
   public static final Codec<HeightmapDecoratorConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(Heightmap.Type.CODEC.fieldOf("heightmap").forGetter((heightmapDecoratorConfig) -> {
         return heightmapDecoratorConfig.heightmap;
      })).apply(instance, (Function)(HeightmapDecoratorConfig::new));
   });
   public final Heightmap.Type heightmap;

   public HeightmapDecoratorConfig(Heightmap.Type heightmap) {
      this.heightmap = heightmap;
   }
}
