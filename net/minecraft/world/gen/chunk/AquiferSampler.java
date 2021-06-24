package net.minecraft.world.gen.chunk;

import java.util.Arrays;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.gen.BlockSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.NoiseColumnSampler;

public interface AquiferSampler {
   int field_33571 = 9;
   int field_33572 = 30;

   static AquiferSampler aquifer(ChunkPos pos, DoublePerlinNoiseSampler edgeDensityNoise, DoublePerlinNoiseSampler fluidLevelNoise, DoublePerlinNoiseSampler fluidTypeNoise, ChunkGeneratorSettings settings, NoiseColumnSampler columnSampler, int startY, int deltaY) {
      return new AquiferSampler.Impl(pos, edgeDensityNoise, fluidLevelNoise, fluidTypeNoise, settings, columnSampler, startY, deltaY);
   }

   static AquiferSampler seaLevel(final int seaLevel, final BlockState state) {
      return new AquiferSampler() {
         public BlockState apply(BlockSource source, int x, int y, int z, double weight) {
            if (weight > 0.0D) {
               return source.sample(x, y, z);
            } else {
               return y >= seaLevel ? Blocks.AIR.getDefaultState() : state;
            }
         }

         public boolean needsFluidTick() {
            return false;
         }
      };
   }

   BlockState apply(BlockSource source, int x, int y, int z, double weight);

   boolean needsFluidTick();

   public static class Impl implements AquiferSampler {
      private static final int field_31451 = 10;
      private static final int field_31452 = 9;
      private static final int field_31453 = 10;
      private static final int field_31454 = 6;
      private static final int field_31455 = 3;
      private static final int field_31456 = 6;
      private static final int field_31457 = 16;
      private static final int field_31458 = 12;
      private static final int field_31459 = 16;
      private final DoublePerlinNoiseSampler edgeDensityNoise;
      private final DoublePerlinNoiseSampler fluidLevelNoise;
      private final DoublePerlinNoiseSampler fluidTypeNoise;
      private final ChunkGeneratorSettings settings;
      private final AquiferSampler.Impl.FluidLevel[] waterLevels;
      private final long[] blockPositions;
      private boolean needsFluidTick;
      private final NoiseColumnSampler columnSampler;
      private final int startX;
      private final int startY;
      private final int startZ;
      private final int sizeX;
      private final int sizeZ;

      Impl(ChunkPos pos, DoublePerlinNoiseSampler edgeDensityNoise, DoublePerlinNoiseSampler fluidLevelNoise, DoublePerlinNoiseSampler fluidTypeNoise, ChunkGeneratorSettings settings, NoiseColumnSampler columnSampler, int startY, int deltaY) {
         this.edgeDensityNoise = edgeDensityNoise;
         this.fluidLevelNoise = fluidLevelNoise;
         this.fluidTypeNoise = fluidTypeNoise;
         this.settings = settings;
         this.columnSampler = columnSampler;
         this.startX = this.getLocalX(pos.getStartX()) - 1;
         int i = this.getLocalX(pos.getEndX()) + 1;
         this.sizeX = i - this.startX + 1;
         this.startY = this.getLocalY(startY) - 1;
         int j = this.getLocalY(startY + deltaY) + 1;
         int k = j - this.startY + 1;
         this.startZ = this.getLocalZ(pos.getStartZ()) - 1;
         int l = this.getLocalZ(pos.getEndZ()) + 1;
         this.sizeZ = l - this.startZ + 1;
         int m = this.sizeX * k * this.sizeZ;
         this.waterLevels = new AquiferSampler.Impl.FluidLevel[m];
         this.blockPositions = new long[m];
         Arrays.fill(this.blockPositions, Long.MAX_VALUE);
      }

      private int index(int x, int y, int z) {
         int i = x - this.startX;
         int j = y - this.startY;
         int k = z - this.startZ;
         return (j * this.sizeZ + k) * this.sizeX + i;
      }

      public BlockState apply(BlockSource source, int x, int y, int z, double weight) {
         if (weight <= 0.0D) {
            double ar;
            BlockState blockState2;
            boolean bl2;
            if (this.shouldBeLava(y)) {
               blockState2 = Blocks.LAVA.getDefaultState();
               ar = 0.0D;
               bl2 = false;
            } else {
               int i = Math.floorDiv(x - 5, 16);
               int j = Math.floorDiv(y + 1, 12);
               int k = Math.floorDiv(z - 5, 16);
               int l = Integer.MAX_VALUE;
               int m = Integer.MAX_VALUE;
               int n = Integer.MAX_VALUE;
               long o = 0L;
               long p = 0L;
               long q = 0L;
               int r = 0;

               while(true) {
                  if (r > 1) {
                     AquiferSampler.Impl.FluidLevel fluidLevel = this.getWaterLevel(o);
                     AquiferSampler.Impl.FluidLevel fluidLevel2 = this.getWaterLevel(p);
                     AquiferSampler.Impl.FluidLevel fluidLevel3 = this.getWaterLevel(q);
                     double e = this.maxDistance(l, m);
                     double f = this.maxDistance(l, n);
                     double g = this.maxDistance(m, n);
                     bl2 = e > 0.0D;
                     if (fluidLevel.y >= y && fluidLevel.state.isOf(Blocks.WATER) && this.shouldBeLava(y - 1)) {
                        ar = 1.0D;
                     } else if (e > -1.0D) {
                        double ai = 1.0D + (this.edgeDensityNoise.sample((double)x, (double)y, (double)z) + 0.05D) / 4.0D;
                        double aj = this.calculateDensity(y, ai, fluidLevel, fluidLevel2);
                        double ak = this.calculateDensity(y, ai, fluidLevel, fluidLevel3);
                        double al = this.calculateDensity(y, ai, fluidLevel2, fluidLevel3);
                        double am = Math.max(0.0D, e);
                        double an = Math.max(0.0D, f);
                        double ao = Math.max(0.0D, g);
                        double ap = 2.0D * am * Math.max(aj, Math.max(ak * an, al * ao));
                        ar = Math.max(0.0D, ap);
                     } else {
                        ar = 0.0D;
                     }

                     blockState2 = y >= fluidLevel.y ? Blocks.AIR.getDefaultState() : fluidLevel.state;
                     break;
                  }

                  for(int s = -1; s <= 1; ++s) {
                     for(int t = 0; t <= 1; ++t) {
                        int u = i + r;
                        int v = j + s;
                        int w = k + t;
                        int aa = this.index(u, v, w);
                        long ab = this.blockPositions[aa];
                        long ad;
                        if (ab != Long.MAX_VALUE) {
                           ad = ab;
                        } else {
                           ChunkRandom chunkRandom = new ChunkRandom(MathHelper.hashCode(u, v * 3, w) + 1L);
                           ad = BlockPos.asLong(u * 16 + chunkRandom.nextInt(10), v * 12 + chunkRandom.nextInt(9), w * 16 + chunkRandom.nextInt(10));
                           this.blockPositions[aa] = ad;
                        }

                        int ae = BlockPos.unpackLongX(ad) - x;
                        int af = BlockPos.unpackLongY(ad) - y;
                        int ag = BlockPos.unpackLongZ(ad) - z;
                        int ah = ae * ae + af * af + ag * ag;
                        if (l >= ah) {
                           q = p;
                           p = o;
                           o = ad;
                           n = m;
                           m = l;
                           l = ah;
                        } else if (m >= ah) {
                           q = p;
                           p = ad;
                           n = m;
                           m = ah;
                        } else if (n >= ah) {
                           q = ad;
                           n = ah;
                        }
                     }
                  }

                  ++r;
               }
            }

            if (weight + ar <= 0.0D) {
               this.needsFluidTick = bl2;
               return blockState2;
            }
         }

         this.needsFluidTick = false;
         return source.sample(x, y, z);
      }

      public boolean needsFluidTick() {
         return this.needsFluidTick;
      }

      private boolean shouldBeLava(int y) {
         return y - this.settings.getGenerationShapeConfig().getMinimumY() <= 9;
      }

      private double maxDistance(int a, int b) {
         double d = 25.0D;
         return 1.0D - (double)Math.abs(b - a) / 25.0D;
      }

      private double calculateDensity(int y, double noise, AquiferSampler.Impl.FluidLevel first, AquiferSampler.Impl.FluidLevel second) {
         if (y <= first.y && y <= second.y && first.state != second.state) {
            return 1.0D;
         } else {
            int i = Math.abs(first.y - second.y);
            double d = 0.5D * (double)(first.y + second.y);
            double e = Math.abs(d - (double)y - 0.5D);
            return 0.5D * (double)i * noise - e;
         }
      }

      private int getLocalX(int x) {
         return Math.floorDiv(x, 16);
      }

      private int getLocalY(int y) {
         return Math.floorDiv(y, 12);
      }

      private int getLocalZ(int z) {
         return Math.floorDiv(z, 16);
      }

      private AquiferSampler.Impl.FluidLevel getWaterLevel(long pos) {
         int i = BlockPos.unpackLongX(pos);
         int j = BlockPos.unpackLongY(pos);
         int k = BlockPos.unpackLongZ(pos);
         int l = this.getLocalX(i);
         int m = this.getLocalY(j);
         int n = this.getLocalZ(k);
         int o = this.index(l, m, n);
         AquiferSampler.Impl.FluidLevel fluidLevel = this.waterLevels[o];
         if (fluidLevel != null) {
            return fluidLevel;
         } else {
            AquiferSampler.Impl.FluidLevel fluidLevel2 = this.getFluidLevel(i, j, k);
            this.waterLevels[o] = fluidLevel2;
            return fluidLevel2;
         }
      }

      private AquiferSampler.Impl.FluidLevel getFluidLevel(int x, int y, int z) {
         int i = this.settings.getSeaLevel();
         if (y > 30) {
            return new AquiferSampler.Impl.FluidLevel(i, Blocks.WATER.getDefaultState());
         } else {
            int j = true;
            int k = true;
            int l = true;
            double d = this.fluidLevelNoise.sample((double)Math.floorDiv(x, 64), (double)Math.floorDiv(y, 40) / 1.4D, (double)Math.floorDiv(z, 64)) * 30.0D + -10.0D;
            boolean bl = false;
            if (Math.abs(d) > 8.0D) {
               d *= 4.0D;
            }

            int m = Math.floorDiv(y, 40) * 40 + 20;
            int n = m + MathHelper.floor(d);
            if (m == -20) {
               double e = this.fluidTypeNoise.sample((double)Math.floorDiv(x, 64), (double)Math.floorDiv(y, 40) / 1.4D, (double)Math.floorDiv(z, 64));
               bl = Math.abs(e) > 0.2199999988079071D;
            }

            return new AquiferSampler.Impl.FluidLevel(Math.min(56, n), bl ? Blocks.LAVA.getDefaultState() : Blocks.WATER.getDefaultState());
         }
      }

      private static final class FluidLevel {
         final int y;
         final BlockState state;

         public FluidLevel(int y, BlockState state) {
            this.y = y;
            this.state = state;
         }
      }
   }
}
