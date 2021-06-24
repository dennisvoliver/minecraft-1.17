package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class RandomFeature extends Feature<RandomFeatureConfig> {
   public RandomFeature(Codec<RandomFeatureConfig> codec) {
      super(codec);
   }

   public boolean generate(FeatureContext<RandomFeatureConfig> context) {
      RandomFeatureConfig randomFeatureConfig = (RandomFeatureConfig)context.getConfig();
      Random random = context.getRandom();
      StructureWorldAccess structureWorldAccess = context.getWorld();
      ChunkGenerator chunkGenerator = context.getGenerator();
      BlockPos blockPos = context.getOrigin();
      Iterator var7 = randomFeatureConfig.features.iterator();

      RandomFeatureEntry randomFeatureEntry;
      do {
         if (!var7.hasNext()) {
            return ((ConfiguredFeature)randomFeatureConfig.defaultFeature.get()).generate(structureWorldAccess, chunkGenerator, random, blockPos);
         }

         randomFeatureEntry = (RandomFeatureEntry)var7.next();
      } while(!(random.nextFloat() < randomFeatureEntry.chance));

      return randomFeatureEntry.generate(structureWorldAccess, chunkGenerator, random, blockPos);
   }
}
