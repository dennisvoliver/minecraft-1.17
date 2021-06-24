package net.minecraft.block.sapling;

import java.util.Iterator;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import org.jetbrains.annotations.Nullable;

public abstract class SaplingGenerator {
   @Nullable
   protected abstract ConfiguredFeature<TreeFeatureConfig, ?> createTreeFeature(Random random, boolean bees);

   public boolean generate(ServerWorld world, ChunkGenerator chunkGenerator, BlockPos pos, BlockState state, Random random) {
      ConfiguredFeature<TreeFeatureConfig, ?> configuredFeature = this.createTreeFeature(random, this.areFlowersNearby(world, pos));
      if (configuredFeature == null) {
         return false;
      } else {
         world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NO_REDRAW);
         if (configuredFeature.generate(world, chunkGenerator, random, pos)) {
            return true;
         } else {
            world.setBlockState(pos, state, Block.NO_REDRAW);
            return false;
         }
      }
   }

   private boolean areFlowersNearby(WorldAccess world, BlockPos pos) {
      Iterator var3 = BlockPos.Mutable.iterate(pos.down().north(2).west(2), pos.up().south(2).east(2)).iterator();

      BlockPos blockPos;
      do {
         if (!var3.hasNext()) {
            return false;
         }

         blockPos = (BlockPos)var3.next();
      } while(!world.getBlockState(blockPos).isIn(BlockTags.FLOWERS));

      return true;
   }
}
