package net.minecraft.world.gen.carver;

import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.floatprovider.FloatProvider;
import net.minecraft.world.gen.YOffset;
import net.minecraft.world.gen.heightprovider.HeightProvider;

public class CaveCarverConfig extends CarverConfig {
   public static final Codec<CaveCarverConfig> CAVE_CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(CarverConfig.CONFIG_CODEC.forGetter((caveCarverConfig) -> {
         return caveCarverConfig;
      }), FloatProvider.VALUE_CODEC.fieldOf("horizontal_radius_multiplier").forGetter((caveCarverConfig) -> {
         return caveCarverConfig.horizontalRadiusMultiplier;
      }), FloatProvider.VALUE_CODEC.fieldOf("vertical_radius_multiplier").forGetter((caveCarverConfig) -> {
         return caveCarverConfig.verticalRadiusMultiplier;
      }), FloatProvider.createValidatedCodec(-1.0F, 1.0F).fieldOf("floor_level").forGetter((caveCarverConfig) -> {
         return caveCarverConfig.floorLevel;
      })).apply(instance, (Function4)(CaveCarverConfig::new));
   });
   public final FloatProvider horizontalRadiusMultiplier;
   public final FloatProvider verticalRadiusMultiplier;
   final FloatProvider floorLevel;

   public CaveCarverConfig(float probability, HeightProvider y, FloatProvider yScale, YOffset lavaLevel, boolean aquifers, CarverDebugConfig debugConfig, FloatProvider horizontalRadiusMultiplier, FloatProvider verticalRadiusMultiplier, FloatProvider floorLevel) {
      super(probability, y, yScale, lavaLevel, aquifers, debugConfig);
      this.horizontalRadiusMultiplier = horizontalRadiusMultiplier;
      this.verticalRadiusMultiplier = verticalRadiusMultiplier;
      this.floorLevel = floorLevel;
   }

   public CaveCarverConfig(float probability, HeightProvider y, FloatProvider yScale, YOffset lavaLevel, boolean aquifers, FloatProvider horizontalRadiusMultiplier, FloatProvider verticalRadiusMultiplier, FloatProvider floorLevel) {
      this(probability, y, yScale, lavaLevel, aquifers, CarverDebugConfig.DEFAULT, horizontalRadiusMultiplier, verticalRadiusMultiplier, floorLevel);
   }

   public CaveCarverConfig(CarverConfig config, FloatProvider horizontalRadiusMultiplier, FloatProvider verticalRadiusMultiplier, FloatProvider floorLevel) {
      this(config.probability, config.y, config.yScale, config.lavaLevel, config.aquifers, config.debugConfig, horizontalRadiusMultiplier, verticalRadiusMultiplier, floorLevel);
   }
}
