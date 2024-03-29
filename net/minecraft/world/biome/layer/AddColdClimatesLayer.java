package net.minecraft.world.biome.layer;

import net.minecraft.world.biome.BiomeIds;
import net.minecraft.world.biome.layer.type.SouthEastSamplingLayer;
import net.minecraft.world.biome.layer.util.LayerRandomnessSource;

public enum AddColdClimatesLayer implements SouthEastSamplingLayer {
   INSTANCE;

   public int sample(LayerRandomnessSource context, int se) {
      if (BiomeLayers.isShallowOcean(se)) {
         return se;
      } else {
         int i = context.nextInt(6);
         if (i == 0) {
            return BiomeIds.FOREST;
         } else {
            return i == 1 ? BiomeIds.MOUNTAINS : BiomeIds.PLAINS;
         }
      }
   }
}
