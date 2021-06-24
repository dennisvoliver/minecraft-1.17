package net.minecraft.world.gen.feature;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class GeodeCrackConfig {
   public static final Codec<GeodeCrackConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(GeodeFeatureConfig.RANGE.fieldOf("generate_crack_chance").orElse(1.0D).forGetter((geodeCrackConfig) -> {
         return geodeCrackConfig.generateCrackChance;
      }), Codec.doubleRange(0.0D, 5.0D).fieldOf("base_crack_size").orElse(2.0D).forGetter((geodeCrackConfig) -> {
         return geodeCrackConfig.baseCrackSize;
      }), Codec.intRange(0, 10).fieldOf("crack_point_offset").orElse(2).forGetter((geodeCrackConfig) -> {
         return geodeCrackConfig.crackPointOffset;
      })).apply(instance, (Function3)(GeodeCrackConfig::new));
   });
   public final double generateCrackChance;
   public final double baseCrackSize;
   public final int crackPointOffset;

   public GeodeCrackConfig(double generateCrackChance, double baseCrackSize, int crackPointOffset) {
      this.generateCrackChance = generateCrackChance;
      this.baseCrackSize = baseCrackSize;
      this.crackPointOffset = crackPointOffset;
   }
}
