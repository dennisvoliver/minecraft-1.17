package net.minecraft.world.gen;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;

public class NoiseHelper {
   public static double lerpFromProgress(DoublePerlinNoiseSampler sampler, double x, double y, double z, double start, double end) {
      double d = sampler.sample(x, y, z);
      return MathHelper.lerpFromProgress(d, -1.0D, 1.0D, start, end);
   }

   public static double method_35479(double d, double e) {
      return d + Math.sin(3.141592653589793D * d) * e / 3.141592653589793D;
   }
}
