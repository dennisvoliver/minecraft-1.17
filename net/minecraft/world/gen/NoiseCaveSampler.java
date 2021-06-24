package net.minecraft.world.gen;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.gen.chunk.WeightSampler;

public class NoiseCaveSampler implements WeightSampler {
   private final int minY;
   private final DoublePerlinNoiseSampler terrainAdditionNoise;
   private final DoublePerlinNoiseSampler pillarNoise;
   private final DoublePerlinNoiseSampler pillarFalloffNoise;
   private final DoublePerlinNoiseSampler pillarScaleNoise;
   private final DoublePerlinNoiseSampler scaledCaveScaleNoise;
   private final DoublePerlinNoiseSampler horizontalCaveNoise;
   private final DoublePerlinNoiseSampler caveScaleNoise;
   private final DoublePerlinNoiseSampler caveFalloffNoise;
   private final DoublePerlinNoiseSampler tunnelNoise1;
   private final DoublePerlinNoiseSampler tunnelNoise2;
   private final DoublePerlinNoiseSampler tunnelScaleNoise;
   private final DoublePerlinNoiseSampler tunnelFalloffNoise;
   private final DoublePerlinNoiseSampler offsetNoise;
   private final DoublePerlinNoiseSampler offsetScaleNoise;
   private final DoublePerlinNoiseSampler field_28842;
   private final DoublePerlinNoiseSampler caveDensityNoise;
   private static final int field_31463 = 128;
   private static final int field_31464 = 170;

   public NoiseCaveSampler(WorldGenRandom random, int minY) {
      this.minY = minY;
      this.pillarNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -7, (double[])(1.0D, 1.0D));
      this.pillarFalloffNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(1.0D));
      this.pillarScaleNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(1.0D));
      this.scaledCaveScaleNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -7, (double[])(1.0D));
      this.horizontalCaveNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(1.0D));
      this.caveScaleNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -11, (double[])(1.0D));
      this.caveFalloffNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -11, (double[])(1.0D));
      this.tunnelNoise1 = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -7, (double[])(1.0D));
      this.tunnelNoise2 = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -7, (double[])(1.0D));
      this.tunnelScaleNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -11, (double[])(1.0D));
      this.tunnelFalloffNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(1.0D));
      this.offsetNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -5, (double[])(1.0D));
      this.offsetScaleNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(1.0D));
      this.field_28842 = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(1.0D, 1.0D, 1.0D));
      this.terrainAdditionNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(1.0D));
      this.caveDensityNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(0.5D, 1.0D, 2.0D, 1.0D, 2.0D, 1.0D, 0.0D, 2.0D, 0.0D));
   }

   public double sample(double weight, int x, int y, int z) {
      boolean bl = weight < 170.0D;
      double d = this.getTunnelOffsetNoise(z, x, y);
      double e = this.getTunnelNoise(z, x, y);
      if (bl) {
         return Math.min(weight, (e + d) * 128.0D * 5.0D);
      } else {
         double f = this.caveDensityNoise.sample((double)z, (double)x / 1.5D, (double)y);
         double g = MathHelper.clamp(f + 0.25D, -1.0D, 1.0D);
         double h = (double)((float)(30 - x) / 8.0F);
         double i = g + MathHelper.clampedLerp(0.5D, 0.0D, h);
         double j = this.getTerrainAdditionNoise(z, x, y);
         double k = this.getCaveNoise(z, x, y);
         double l = i + j;
         double m = Math.min(l, Math.min(e, k) + d);
         double n = Math.max(m, this.getPillarNoise(z, x, y));
         return 128.0D * MathHelper.clamp(n, -1.0D, 1.0D);
      }
   }

   private double method_35325(double d, int i, int j, int k) {
      double e = this.field_28842.sample((double)(i * 2), (double)j, (double)(k * 2));
      e = NoiseHelper.method_35479(e, 1.0D);
      int l = false;
      double f = (double)(j - 0) / 40.0D;
      e += MathHelper.clampedLerp(0.5D, d, f);
      double g = 3.0D;
      e = 4.0D * e + 3.0D;
      return Math.min(d, e);
   }

   private double getPillarNoise(int x, int y, int z) {
      double d = 0.0D;
      double e = 2.0D;
      double f = NoiseHelper.lerpFromProgress(this.pillarFalloffNoise, (double)x, (double)y, (double)z, 0.0D, 2.0D);
      double g = 0.0D;
      double h = 1.1D;
      double i = NoiseHelper.lerpFromProgress(this.pillarScaleNoise, (double)x, (double)y, (double)z, 0.0D, 1.1D);
      i = Math.pow(i, 3.0D);
      double j = 25.0D;
      double k = 0.3D;
      double l = this.pillarNoise.sample((double)x * 25.0D, (double)y * 0.3D, (double)z * 25.0D);
      l = i * (l * 2.0D - f);
      return l > 0.03D ? l : Double.NEGATIVE_INFINITY;
   }

   private double getTerrainAdditionNoise(int x, int y, int z) {
      double d = this.terrainAdditionNoise.sample((double)x, (double)(y * 8), (double)z);
      return MathHelper.square(d) * 4.0D;
   }

   private double getTunnelNoise(int x, int y, int z) {
      double d = this.tunnelScaleNoise.sample((double)(x * 2), (double)y, (double)(z * 2));
      double e = NoiseCaveSampler.CaveScaler.scaleTunnels(d);
      double f = 0.065D;
      double g = 0.088D;
      double h = NoiseHelper.lerpFromProgress(this.tunnelFalloffNoise, (double)x, (double)y, (double)z, 0.065D, 0.088D);
      double i = sample(this.tunnelNoise1, (double)x, (double)y, (double)z, e);
      double j = Math.abs(e * i) - h;
      double k = sample(this.tunnelNoise2, (double)x, (double)y, (double)z, e);
      double l = Math.abs(e * k) - h;
      return clamp(Math.max(j, l));
   }

   private double getCaveNoise(int x, int y, int z) {
      double d = this.caveScaleNoise.sample((double)(x * 2), (double)y, (double)(z * 2));
      double e = NoiseCaveSampler.CaveScaler.scaleCaves(d);
      double f = 0.6D;
      double g = 1.3D;
      double h = NoiseHelper.lerpFromProgress(this.caveFalloffNoise, (double)(x * 2), (double)y, (double)(z * 2), 0.6D, 1.3D);
      double i = sample(this.scaledCaveScaleNoise, (double)x, (double)y, (double)z, e);
      double j = 0.083D;
      double k = Math.abs(e * i) - 0.083D * h;
      int l = this.minY;
      int m = true;
      double n = NoiseHelper.lerpFromProgress(this.horizontalCaveNoise, (double)x, 0.0D, (double)z, (double)l, 8.0D);
      double o = Math.abs(n - (double)y / 8.0D) - 1.0D * h;
      o = o * o * o;
      return clamp(Math.max(o, k));
   }

   private double getTunnelOffsetNoise(int x, int y, int z) {
      double d = NoiseHelper.lerpFromProgress(this.offsetScaleNoise, (double)x, (double)y, (double)z, 0.0D, 0.1D);
      return (0.4D - Math.abs(this.offsetNoise.sample((double)x, (double)y, (double)z))) * d;
   }

   private static double clamp(double value) {
      return MathHelper.clamp(value, -1.0D, 1.0D);
   }

   private static double sample(DoublePerlinNoiseSampler sampler, double x, double y, double z, double scale) {
      return sampler.sample(x / scale, y / scale, z / scale);
   }

   static final class CaveScaler {
      private CaveScaler() {
      }

      static double scaleCaves(double value) {
         if (value < -0.75D) {
            return 0.5D;
         } else if (value < -0.5D) {
            return 0.75D;
         } else if (value < 0.5D) {
            return 1.0D;
         } else {
            return value < 0.75D ? 2.0D : 3.0D;
         }
      }

      static double scaleTunnels(double value) {
         if (value < -0.5D) {
            return 0.75D;
         } else if (value < 0.0D) {
            return 1.0D;
         } else {
            return value < 0.5D ? 1.5D : 2.0D;
         }
      }
   }
}
