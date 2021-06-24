package net.minecraft.util.math.intprovider;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.math.MathHelper;

public class ClampedIntProvider extends IntProvider {
   public static final Codec<ClampedIntProvider> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(IntProvider.VALUE_CODEC.fieldOf("source").forGetter((clampedIntProvider) -> {
         return clampedIntProvider.source;
      }), Codec.INT.fieldOf("min_inclusive").forGetter((clampedIntProvider) -> {
         return clampedIntProvider.min;
      }), Codec.INT.fieldOf("max_inclusive").forGetter((clampedIntProvider) -> {
         return clampedIntProvider.max;
      })).apply(instance, (Function3)(ClampedIntProvider::new));
   }).comapFlatMap((clampedIntProvider) -> {
      return clampedIntProvider.max < clampedIntProvider.min ? DataResult.error("Max must be at least min, min_inclusive: " + clampedIntProvider.min + ", max_inclusive: " + clampedIntProvider.max) : DataResult.success(clampedIntProvider);
   }, Function.identity());
   private final IntProvider source;
   private int min;
   private int max;

   public static ClampedIntProvider create(IntProvider source, int min, int max) {
      return new ClampedIntProvider(source, min, max);
   }

   public ClampedIntProvider(IntProvider source, int min, int max) {
      this.source = source;
      this.min = min;
      this.max = max;
   }

   public int get(Random random) {
      return MathHelper.clamp(this.source.get(random), this.min, this.max);
   }

   public int getMin() {
      return Math.max(this.min, this.source.getMin());
   }

   public int getMax() {
      return Math.min(this.max, this.source.getMax());
   }

   public IntProviderType<?> getType() {
      return IntProviderType.CLAMPED;
   }
}
