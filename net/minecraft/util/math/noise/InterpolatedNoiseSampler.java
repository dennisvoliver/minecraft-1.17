package net.minecraft.util.math.noise;

import java.util.stream.IntStream;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.WorldGenRandom;

public class InterpolatedNoiseSampler {
   private final OctavePerlinNoiseSampler lowerInterpolatedNoise;
   private final OctavePerlinNoiseSampler upperInterpolatedNoise;
   private final OctavePerlinNoiseSampler interpolationNoise;

   public InterpolatedNoiseSampler(OctavePerlinNoiseSampler lowerInterpolatedNoise, OctavePerlinNoiseSampler upperInterpolatedNoise, OctavePerlinNoiseSampler interpolationNoise) {
      this.lowerInterpolatedNoise = lowerInterpolatedNoise;
      this.upperInterpolatedNoise = upperInterpolatedNoise;
      this.interpolationNoise = interpolationNoise;
   }

   public InterpolatedNoiseSampler(WorldGenRandom random) {
      this(new OctavePerlinNoiseSampler(random, IntStream.rangeClosed(-15, 0)), new OctavePerlinNoiseSampler(random, IntStream.rangeClosed(-15, 0)), new OctavePerlinNoiseSampler(random, IntStream.rangeClosed(-7, 0)));
   }

   public double sample(int i, int j, int k, double horizontalScale, double verticalScale, double horizontalStretch, double verticalStretch) {
      double d = 0.0D;
      double e = 0.0D;
      double f = 0.0D;
      boolean bl = true;
      double g = 1.0D;

      for(int l = 0; l < 8; ++l) {
         PerlinNoiseSampler perlinNoiseSampler = this.interpolationNoise.getOctave(l);
         if (perlinNoiseSampler != null) {
            f += perlinNoiseSampler.sample(OctavePerlinNoiseSampler.maintainPrecision((double)i * horizontalStretch * g), OctavePerlinNoiseSampler.maintainPrecision((double)j * verticalStretch * g), OctavePerlinNoiseSampler.maintainPrecision((double)k * horizontalStretch * g), verticalStretch * g, (double)j * verticalStretch * g) / g;
         }

         g /= 2.0D;
      }

      double h = (f / 10.0D + 1.0D) / 2.0D;
      boolean bl2 = h >= 1.0D;
      boolean bl3 = h <= 0.0D;
      g = 1.0D;

      for(int m = 0; m < 16; ++m) {
         double n = OctavePerlinNoiseSampler.maintainPrecision((double)i * horizontalScale * g);
         double o = OctavePerlinNoiseSampler.maintainPrecision((double)j * verticalScale * g);
         double p = OctavePerlinNoiseSampler.maintainPrecision((double)k * horizontalScale * g);
         double q = verticalScale * g;
         PerlinNoiseSampler perlinNoiseSampler3;
         if (!bl2) {
            perlinNoiseSampler3 = this.lowerInterpolatedNoise.getOctave(m);
            if (perlinNoiseSampler3 != null) {
               d += perlinNoiseSampler3.sample(n, o, p, q, (double)j * q) / g;
            }
         }

         if (!bl3) {
            perlinNoiseSampler3 = this.upperInterpolatedNoise.getOctave(m);
            if (perlinNoiseSampler3 != null) {
               e += perlinNoiseSampler3.sample(n, o, p, q, (double)j * q) / g;
            }
         }

         g /= 2.0D;
      }

      return MathHelper.clampedLerp(d / 512.0D, e / 512.0D, h);
   }
}
