package net.minecraft.world.gen;

import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.TheEndBiomeSource;
import net.minecraft.world.gen.chunk.GenerationShapeConfig;
import net.minecraft.world.gen.chunk.WeightSampler;
import org.jetbrains.annotations.Nullable;

/**
 * Samples noise values for use in chunk generation.
 */
public class NoiseColumnSampler {
   private static final int field_31470 = 32;
   /**
    * Table of weights used to weight faraway biomes less than nearby biomes.
    */
   private static final float[] BIOME_WEIGHT_TABLE = (float[])Util.make(new float[25], (array) -> {
      for(int i = -2; i <= 2; ++i) {
         for(int j = -2; j <= 2; ++j) {
            float f = 10.0F / MathHelper.sqrt((float)(i * i + j * j) + 0.2F);
            array[i + 2 + (j + 2) * 5] = f;
         }
      }

   });
   private final BiomeSource biomeSource;
   private final int horizontalNoiseResolution;
   private final int verticalNoiseResolution;
   private final int noiseSizeY;
   private final GenerationShapeConfig config;
   private final InterpolatedNoiseSampler noise;
   @Nullable
   private final SimplexNoiseSampler islandNoise;
   private final OctavePerlinNoiseSampler densityNoise;
   private final double topSlideTarget;
   private final double topSlideSize;
   private final double topSlideOffset;
   private final double bottomSlideTarget;
   private final double bottomSlideSize;
   private final double bottomSlideOffset;
   private final double densityFactor;
   private final double densityOffset;
   private final WeightSampler field_33653;

   public NoiseColumnSampler(BiomeSource biomeSource, int horizontalNoiseResolution, int verticalNoiseResolution, int noiseSizeY, GenerationShapeConfig config, InterpolatedNoiseSampler noise, @Nullable SimplexNoiseSampler islandNoise, OctavePerlinNoiseSampler densityNoise, WeightSampler weightSampler) {
      this.horizontalNoiseResolution = horizontalNoiseResolution;
      this.verticalNoiseResolution = verticalNoiseResolution;
      this.biomeSource = biomeSource;
      this.noiseSizeY = noiseSizeY;
      this.config = config;
      this.noise = noise;
      this.islandNoise = islandNoise;
      this.densityNoise = densityNoise;
      this.topSlideTarget = (double)config.getTopSlide().getTarget();
      this.topSlideSize = (double)config.getTopSlide().getSize();
      this.topSlideOffset = (double)config.getTopSlide().getOffset();
      this.bottomSlideTarget = (double)config.getBottomSlide().getTarget();
      this.bottomSlideSize = (double)config.getBottomSlide().getSize();
      this.bottomSlideOffset = (double)config.getBottomSlide().getOffset();
      this.densityFactor = config.getDensityFactor();
      this.densityOffset = config.getDensityOffset();
      this.field_33653 = weightSampler;
   }

   /**
    * Samples the noise for the given column and stores it in the buffer parameter.
    */
   public void sampleNoiseColumn(double[] buffer, int x, int z, GenerationShapeConfig config, int seaLevel, int minY, int noiseSizeY) {
      double ac;
      double ad;
      double ai;
      if (this.islandNoise != null) {
         ac = (double)(TheEndBiomeSource.getNoiseAt(this.islandNoise, x, z) - 8.0F);
         if (ac > 0.0D) {
            ad = 0.25D;
         } else {
            ad = 1.0D;
         }
      } else {
         float g = 0.0F;
         float h = 0.0F;
         float i = 0.0F;
         int j = true;
         int k = seaLevel;
         float l = this.biomeSource.getBiomeForNoiseGen(x, seaLevel, z).getDepth();

         for(int m = -2; m <= 2; ++m) {
            for(int n = -2; n <= 2; ++n) {
               Biome biome = this.biomeSource.getBiomeForNoiseGen(x + m, k, z + n);
               float o = biome.getDepth();
               float p = biome.getScale();
               float s;
               float t;
               if (config.isAmplified() && o > 0.0F) {
                  s = 1.0F + o * 2.0F;
                  t = 1.0F + p * 4.0F;
               } else {
                  s = o;
                  t = p;
               }

               float u = o > l ? 0.5F : 1.0F;
               float v = u * BIOME_WEIGHT_TABLE[m + 2 + (n + 2) * 5] / (s + 2.0F);
               g += t * v;
               h += s * v;
               i += v;
            }
         }

         float w = h / i;
         float y = g / i;
         ai = (double)(w * 0.5F - 0.125F);
         double ab = (double)(y * 0.9F + 0.1F);
         ac = ai * 0.265625D;
         ad = 96.0D / ab;
      }

      double ae = 684.412D * config.getSampling().getXZScale();
      double af = 684.412D * config.getSampling().getYScale();
      double ag = ae / config.getSampling().getXZFactor();
      double ah = af / config.getSampling().getYFactor();
      ai = config.hasRandomDensityOffset() ? this.getDensityNoise(x, z) : 0.0D;

      for(int aj = 0; aj <= noiseSizeY; ++aj) {
         int ak = aj + minY;
         double al = this.noise.sample(x, ak, z, ae, af, ag, ah);
         double am = this.getOffset(ak, ac, ad, ai) + al;
         am = this.field_33653.sample(am, ak * this.verticalNoiseResolution, z * this.horizontalNoiseResolution, x * this.horizontalNoiseResolution);
         am = this.applySlides(am, ak);
         buffer[aj] = am;
      }

   }

   /**
    * Calculates an offset for the noise.
    * <p>For example in the overworld, this makes lower y values solid while making higher y values air.
    */
   private double getOffset(int y, double depth, double scale, double randomDensityOffset) {
      double d = 1.0D - (double)y * 2.0D / 32.0D + randomDensityOffset;
      double e = d * this.densityFactor + this.densityOffset;
      double f = (e + depth) * scale;
      return f * (double)(f > 0.0D ? 4 : 1);
   }

   /**
    * Interpolates the noise at the top and bottom of the world.
    */
   private double applySlides(double noise, int y) {
      int i = MathHelper.floorDiv(this.config.getMinimumY(), this.verticalNoiseResolution);
      int j = y - i;
      double e;
      if (this.topSlideSize > 0.0D) {
         e = ((double)(this.noiseSizeY - j) - this.topSlideOffset) / this.topSlideSize;
         noise = MathHelper.clampedLerp(this.topSlideTarget, noise, e);
      }

      if (this.bottomSlideSize > 0.0D) {
         e = ((double)j - this.bottomSlideOffset) / this.bottomSlideSize;
         noise = MathHelper.clampedLerp(this.bottomSlideTarget, noise, e);
      }

      return noise;
   }

   /**
    * Applies a random change to the density to subtly vary the height of the terrain.
    */
   private double getDensityNoise(int x, int z) {
      double d = this.densityNoise.sample((double)(x * 200), 10.0D, (double)(z * 200), 1.0D, 0.0D, true);
      double f;
      if (d < 0.0D) {
         f = -d * 0.3D;
      } else {
         f = d;
      }

      double g = f * 24.575625D - 2.0D;
      return g < 0.0D ? g * 0.009486607142857142D : Math.min(g, 1.0D) * 0.006640625D;
   }
}
