package net.minecraft.world.gen.surfacebuilder;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

public class ErodedBadlandsSurfaceBuilder extends BadlandsSurfaceBuilder {
   private static final BlockState WHITE_TERRACOTTA;
   private static final BlockState ORANGE_TERRACOTTA;
   private static final BlockState TERRACOTTA;

   public ErodedBadlandsSurfaceBuilder(Codec<TernarySurfaceConfig> codec) {
      super(codec);
   }

   public void generate(Random random, Chunk chunk, Biome biome, int i, int j, int k, double d, BlockState blockState, BlockState blockState2, int l, int m, long n, TernarySurfaceConfig ternarySurfaceConfig) {
      double e = 0.0D;
      double f = Math.min(Math.abs(d), this.heightCutoffNoise.sample((double)i * 0.25D, (double)j * 0.25D, false) * 15.0D);
      if (f > 0.0D) {
         double g = 0.001953125D;
         double h = Math.abs(this.heightNoise.sample((double)i * 0.001953125D, (double)j * 0.001953125D, false));
         e = f * f * 2.5D;
         double o = Math.ceil(h * 50.0D) + 14.0D;
         if (e > o) {
            e = o;
         }

         e += 64.0D;
      }

      int p = i & 15;
      int q = j & 15;
      BlockState blockState3 = WHITE_TERRACOTTA;
      SurfaceConfig surfaceConfig = biome.getGenerationSettings().getSurfaceConfig();
      BlockState blockState4 = surfaceConfig.getUnderMaterial();
      BlockState blockState5 = surfaceConfig.getTopMaterial();
      BlockState blockState6 = blockState4;
      int r = (int)(d / 3.0D + 3.0D + random.nextDouble() * 0.25D);
      boolean bl = Math.cos(d / 3.0D * 3.141592653589793D) > 0.0D;
      int s = -1;
      boolean bl2 = false;
      BlockPos.Mutable mutable = new BlockPos.Mutable();

      for(int t = Math.max(k, (int)e + 1); t >= m; --t) {
         mutable.set(p, t, q);
         if (chunk.getBlockState(mutable).isAir() && t < (int)e) {
            chunk.setBlockState(mutable, blockState, false);
         }

         BlockState blockState7 = chunk.getBlockState(mutable);
         if (blockState7.isAir()) {
            s = -1;
         } else if (blockState7.isOf(blockState.getBlock())) {
            if (s == -1) {
               bl2 = false;
               if (r <= 0) {
                  blockState3 = Blocks.AIR.getDefaultState();
                  blockState6 = blockState;
               } else if (t >= l - 4 && t <= l + 1) {
                  blockState3 = WHITE_TERRACOTTA;
                  blockState6 = blockState4;
               }

               if (t < l && (blockState3 == null || blockState3.isAir())) {
                  blockState3 = blockState2;
               }

               s = r + Math.max(0, t - l);
               if (t >= l - 1) {
                  if (t <= l + 3 + r) {
                     chunk.setBlockState(mutable, blockState5, false);
                     bl2 = true;
                  } else {
                     BlockState blockState10;
                     if (t >= 64 && t <= 127) {
                        if (bl) {
                           blockState10 = TERRACOTTA;
                        } else {
                           blockState10 = this.calculateLayerBlockState(i, t, j);
                        }
                     } else {
                        blockState10 = ORANGE_TERRACOTTA;
                     }

                     chunk.setBlockState(mutable, blockState10, false);
                  }
               } else {
                  chunk.setBlockState(mutable, blockState6, false);
                  if (blockState6.isOf(Blocks.WHITE_TERRACOTTA) || blockState6.isOf(Blocks.ORANGE_TERRACOTTA) || blockState6.isOf(Blocks.MAGENTA_TERRACOTTA) || blockState6.isOf(Blocks.LIGHT_BLUE_TERRACOTTA) || blockState6.isOf(Blocks.YELLOW_TERRACOTTA) || blockState6.isOf(Blocks.LIME_TERRACOTTA) || blockState6.isOf(Blocks.PINK_TERRACOTTA) || blockState6.isOf(Blocks.GRAY_TERRACOTTA) || blockState6.isOf(Blocks.LIGHT_GRAY_TERRACOTTA) || blockState6.isOf(Blocks.CYAN_TERRACOTTA) || blockState6.isOf(Blocks.PURPLE_TERRACOTTA) || blockState6.isOf(Blocks.BLUE_TERRACOTTA) || blockState6.isOf(Blocks.BROWN_TERRACOTTA) || blockState6.isOf(Blocks.GREEN_TERRACOTTA) || blockState6.isOf(Blocks.RED_TERRACOTTA) || blockState6.isOf(Blocks.BLACK_TERRACOTTA)) {
                     chunk.setBlockState(mutable, ORANGE_TERRACOTTA, false);
                  }
               }
            } else if (s > 0) {
               --s;
               if (bl2) {
                  chunk.setBlockState(mutable, ORANGE_TERRACOTTA, false);
               } else {
                  chunk.setBlockState(mutable, this.calculateLayerBlockState(i, t, j), false);
               }
            }
         }
      }

   }

   static {
      WHITE_TERRACOTTA = Blocks.WHITE_TERRACOTTA.getDefaultState();
      ORANGE_TERRACOTTA = Blocks.ORANGE_TERRACOTTA.getDefaultState();
      TERRACOTTA = Blocks.TERRACOTTA.getDefaultState();
   }
}
