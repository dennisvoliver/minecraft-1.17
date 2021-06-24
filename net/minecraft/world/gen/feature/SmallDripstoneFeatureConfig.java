package net.minecraft.world.gen.feature;

import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class SmallDripstoneFeatureConfig implements FeatureConfig {
   public static final Codec<SmallDripstoneFeatureConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(Codec.intRange(0, 100).fieldOf("max_placements").orElse(5).forGetter((smallDripstoneFeatureConfig) -> {
         return smallDripstoneFeatureConfig.maxPlacements;
      }), Codec.intRange(0, 20).fieldOf("empty_space_search_radius").orElse(10).forGetter((smallDripstoneFeatureConfig) -> {
         return smallDripstoneFeatureConfig.emptySpaceSearchRadius;
      }), Codec.intRange(0, 20).fieldOf("max_offset_from_origin").orElse(2).forGetter((smallDripstoneFeatureConfig) -> {
         return smallDripstoneFeatureConfig.maxOffsetFromOrigin;
      }), Codec.floatRange(0.0F, 1.0F).fieldOf("chance_of_taller_dripstone").orElse(0.2F).forGetter((smallDripstoneFeatureConfig) -> {
         return smallDripstoneFeatureConfig.chanceOfTallerDripstone;
      })).apply(instance, (Function4)(SmallDripstoneFeatureConfig::new));
   });
   public final int maxPlacements;
   public final int emptySpaceSearchRadius;
   public final int maxOffsetFromOrigin;
   public final float chanceOfTallerDripstone;

   public SmallDripstoneFeatureConfig(int maxPlacements, int emptySpaceSearchRadius, int maxOffsetFromOrigin, float chanceOfTallerDripstone) {
      this.maxPlacements = maxPlacements;
      this.emptySpaceSearchRadius = emptySpaceSearchRadius;
      this.maxOffsetFromOrigin = maxOffsetFromOrigin;
      this.chanceOfTallerDripstone = chanceOfTallerDripstone;
   }
}
