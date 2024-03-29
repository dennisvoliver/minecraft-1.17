package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class SpringFeature extends Feature<SpringFeatureConfig> {
   public SpringFeature(Codec<SpringFeatureConfig> codec) {
      super(codec);
   }

   public boolean generate(FeatureContext<SpringFeatureConfig> context) {
      SpringFeatureConfig springFeatureConfig = (SpringFeatureConfig)context.getConfig();
      StructureWorldAccess structureWorldAccess = context.getWorld();
      BlockPos blockPos = context.getOrigin();
      if (!springFeatureConfig.validBlocks.contains(structureWorldAccess.getBlockState(blockPos.up()).getBlock())) {
         return false;
      } else if (springFeatureConfig.requiresBlockBelow && !springFeatureConfig.validBlocks.contains(structureWorldAccess.getBlockState(blockPos.down()).getBlock())) {
         return false;
      } else {
         BlockState blockState = structureWorldAccess.getBlockState(blockPos);
         if (!blockState.isAir() && !springFeatureConfig.validBlocks.contains(blockState.getBlock())) {
            return false;
         } else {
            int i = 0;
            int j = 0;
            if (springFeatureConfig.validBlocks.contains(structureWorldAccess.getBlockState(blockPos.west()).getBlock())) {
               ++j;
            }

            if (springFeatureConfig.validBlocks.contains(structureWorldAccess.getBlockState(blockPos.east()).getBlock())) {
               ++j;
            }

            if (springFeatureConfig.validBlocks.contains(structureWorldAccess.getBlockState(blockPos.north()).getBlock())) {
               ++j;
            }

            if (springFeatureConfig.validBlocks.contains(structureWorldAccess.getBlockState(blockPos.south()).getBlock())) {
               ++j;
            }

            if (springFeatureConfig.validBlocks.contains(structureWorldAccess.getBlockState(blockPos.down()).getBlock())) {
               ++j;
            }

            int k = 0;
            if (structureWorldAccess.isAir(blockPos.west())) {
               ++k;
            }

            if (structureWorldAccess.isAir(blockPos.east())) {
               ++k;
            }

            if (structureWorldAccess.isAir(blockPos.north())) {
               ++k;
            }

            if (structureWorldAccess.isAir(blockPos.south())) {
               ++k;
            }

            if (structureWorldAccess.isAir(blockPos.down())) {
               ++k;
            }

            if (j == springFeatureConfig.rockCount && k == springFeatureConfig.holeCount) {
               structureWorldAccess.setBlockState(blockPos, springFeatureConfig.state.getBlockState(), Block.NOTIFY_LISTENERS);
               structureWorldAccess.getFluidTickScheduler().schedule(blockPos, springFeatureConfig.state.getFluid(), 0);
               ++i;
            }

            return i > 0;
         }
      }
   }
}
