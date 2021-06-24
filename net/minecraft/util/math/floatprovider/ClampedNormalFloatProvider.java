package net.minecraft.util.math.floatprovider;

import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.math.MathHelper;

public class ClampedNormalFloatProvider extends FloatProvider {
   public static final Codec<ClampedNormalFloatProvider> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(Codec.FLOAT.fieldOf("mean").forGetter((clampedNormalFloatProvider) -> {
         return clampedNormalFloatProvider.mean;
      }), Codec.FLOAT.fieldOf("deviation").forGetter((clampedNormalFloatProvider) -> {
         return clampedNormalFloatProvider.deviation;
      }), Codec.FLOAT.fieldOf("min").forGetter((clampedNormalFloatProvider) -> {
         return clampedNormalFloatProvider.min;
      }), Codec.FLOAT.fieldOf("max").forGetter((clampedNormalFloatProvider) -> {
         return clampedNormalFloatProvider.max;
      })).apply(instance, (Function4)(ClampedNormalFloatProvider::new));
   }).comapFlatMap((clampedNormalFloatProvider) -> {
      return clampedNormalFloatProvider.max < clampedNormalFloatProvider.min ? DataResult.error("Max must be larger than min: [" + clampedNormalFloatProvider.min + ", " + clampedNormalFloatProvider.max + "]") : DataResult.success(clampedNormalFloatProvider);
   }, Function.identity());
   private float mean;
   private float deviation;
   private float min;
   private float max;

   public static ClampedNormalFloatProvider create(float mean, float deviation, float min, float max) {
      return new ClampedNormalFloatProvider(mean, deviation, min, max);
   }

   private ClampedNormalFloatProvider(float mean, float deviation, float min, float max) {
      this.mean = mean;
      this.deviation = deviation;
      this.min = min;
      this.max = max;
   }

   public float get(Random random) {
      return get(random, this.mean, this.deviation, this.min, this.max);
   }

   public static float get(Random random, float mean, float deviation, float min, float max) {
      return MathHelper.clamp(MathHelper.nextGaussian(random, mean, deviation), min, max);
   }

   public float getMin() {
      return this.min;
   }

   public float getMax() {
      return this.max;
   }

   public FloatProviderType<?> getType() {
      return FloatProviderType.CLAMPED_NORMAL;
   }

   public String toString() {
      return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.min + "-" + this.max + "]";
   }
}
