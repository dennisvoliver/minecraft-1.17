package net.minecraft.util.math.floatprovider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;

public class ConstantFloatProvider extends FloatProvider {
   public static final ConstantFloatProvider ZERO = new ConstantFloatProvider(0.0F);
   public static final Codec<ConstantFloatProvider> CODEC;
   private final float value;

   public static ConstantFloatProvider create(float value) {
      return value == 0.0F ? ZERO : new ConstantFloatProvider(value);
   }

   private ConstantFloatProvider(float value) {
      this.value = value;
   }

   public float getValue() {
      return this.value;
   }

   public float get(Random random) {
      return this.value;
   }

   public float getMin() {
      return this.value;
   }

   public float getMax() {
      return this.value + 1.0F;
   }

   public FloatProviderType<?> getType() {
      return FloatProviderType.CONSTANT;
   }

   public String toString() {
      return Float.toString(this.value);
   }

   static {
      CODEC = Codec.either(Codec.FLOAT, RecordCodecBuilder.create((instance) -> {
         return instance.group(Codec.FLOAT.fieldOf("value").forGetter((constantFloatProvider) -> {
            return constantFloatProvider.value;
         })).apply(instance, (Function)(ConstantFloatProvider::new));
      })).xmap((either) -> {
         return (ConstantFloatProvider)either.map(ConstantFloatProvider::create, (constantFloatProvider) -> {
            return constantFloatProvider;
         });
      }, (constantFloatProvider) -> {
         return Either.left(constantFloatProvider.value);
      });
   }
}
