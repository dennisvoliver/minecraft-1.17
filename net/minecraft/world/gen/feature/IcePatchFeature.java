package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class IcePatchFeature extends DiskFeature {
   public IcePatchFeature(Codec<DiskFeatureConfig> codec) {
      super(codec);
   }

   public boolean generate(FeatureContext<DiskFeatureConfig> context) {
      StructureWorldAccess structureWorldAccess = context.getWorld();
      ChunkGenerator chunkGenerator = context.getGenerator();
      Random random = context.getRandom();
      DiskFeatureConfig diskFeatureConfig = (DiskFeatureConfig)context.getConfig();

      BlockPos blockPos;
      for(blockPos = context.getOrigin(); structureWorldAccess.isAir(blockPos) && blockPos.getY() > structureWorldAccess.getBottomY() + 2; blockPos = blockPos.down()) {
      }

      return !structureWorldAccess.getBlockState(blockPos).isOf(Blocks.SNOW_BLOCK) ? false : super.generate(new FeatureContext(structureWorldAccess, chunkGenerator, random, blockPos, diskFeatureConfig));
   }
}
