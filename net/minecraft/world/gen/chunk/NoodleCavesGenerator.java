package net.minecraft.world.gen.chunk;

import java.util.Random;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.world.gen.NoiseHelper;
import net.minecraft.world.gen.SimpleRandom;

public class NoodleCavesGenerator {
   private static final int field_33654 = 30;
   private static final double field_33655 = 1.5D;
   private static final double field_33656 = 2.6666666666666665D;
   private static final double field_33657 = 2.6666666666666665D;
   private final DoublePerlinNoiseSampler field_33658;
   private final DoublePerlinNoiseSampler field_33659;
   private final DoublePerlinNoiseSampler field_33660;
   private final DoublePerlinNoiseSampler field_33661;

   public NoodleCavesGenerator(long seed) {
      Random random = new Random(seed);
      this.field_33658 = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(1.0D));
      this.field_33659 = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -8, (double[])(1.0D));
      this.field_33660 = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -7, (double[])(1.0D));
      this.field_33661 = DoublePerlinNoiseSampler.create(new SimpleRandom(random.nextLong()), -7, (double[])(1.0D));
   }

   public void method_36471(double[] buffer, int x, int z, int minY, int noiseSizeY) {
      this.sample(buffer, x, z, minY, noiseSizeY, this.field_33658, 1.0D);
   }

   public void method_36474(double[] buffer, int x, int z, int minY, int noiseSizeY) {
      this.sample(buffer, x, z, minY, noiseSizeY, this.field_33659, 1.0D);
   }

   public void method_36475(double[] buffer, int x, int z, int minY, int noiseSizeY) {
      this.sample(buffer, x, z, minY, noiseSizeY, this.field_33660, 2.6666666666666665D, 2.6666666666666665D);
   }

   public void method_36476(double[] buffer, int x, int z, int minY, int noiseSizeY) {
      this.sample(buffer, x, z, minY, noiseSizeY, this.field_33661, 2.6666666666666665D, 2.6666666666666665D);
   }

   public void sample(double[] buffer, int x, int z, int minY, int noiseSizeY, DoublePerlinNoiseSampler sampler, double scale) {
      this.sample(buffer, x, z, minY, noiseSizeY, sampler, scale, scale);
   }

   public void sample(double[] buffer, int x, int z, int minY, int noiseSizeY, DoublePerlinNoiseSampler sampler, double horizontalScale, double verticalScale) {
      int i = true;
      int j = true;

      for(int k = 0; k < noiseSizeY; ++k) {
         int l = k + minY;
         int m = x * 4;
         int n = l * 8;
         int o = z * 4;
         double e;
         if (n < 38) {
            e = NoiseHelper.lerpFromProgress(sampler, (double)m * horizontalScale, (double)n * verticalScale, (double)o * horizontalScale, -1.0D, 1.0D);
         } else {
            e = 1.0D;
         }

         buffer[k] = e;
      }

   }

   public double method_36470(double weight, int x, int y, int z, double d, double e, double f, double g, int minY) {
      if (y <= 30 && y >= minY + 4) {
         if (weight < 0.0D) {
            return weight;
         } else if (d < 0.0D) {
            return weight;
         } else {
            double h = 0.05D;
            double i = 0.1D;
            double j = MathHelper.clampedLerpFromProgress(e, -1.0D, 1.0D, 0.05D, 0.1D);
            double k = Math.abs(1.5D * f) - j;
            double l = Math.abs(1.5D * g) - j;
            double m = Math.max(k, l);
            return Math.min(weight, m);
         }
      } else {
         return weight;
      }
   }
}
