package net.minecraft.util.math.intprovider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.util.registry.Registry;

public abstract class IntProvider {
   private static final Codec<Either<Integer, IntProvider>> INT_CODEC;
   public static final Codec<IntProvider> VALUE_CODEC;
   public static final Codec<IntProvider> field_33450;
   public static final Codec<IntProvider> field_33451;

   public static Codec<IntProvider> createValidatingCodec(int min, int max) {
      Function<IntProvider, DataResult<IntProvider>> function = (provider) -> {
         if (provider.getMin() < min) {
            return DataResult.error("Value provider too low: " + min + " [" + provider.getMin() + "-" + provider.getMax() + "]");
         } else {
            return provider.getMax() > max ? DataResult.error("Value provider too high: " + max + " [" + provider.getMin() + "-" + provider.getMax() + "]") : DataResult.success(provider);
         }
      };
      return VALUE_CODEC.flatXmap(function, function);
   }

   public abstract int get(Random random);

   public abstract int getMin();

   public abstract int getMax();

   public abstract IntProviderType<?> getType();

   static {
      INT_CODEC = Codec.either(Codec.INT, Registry.INT_PROVIDER_TYPE.dispatch(IntProvider::getType, IntProviderType::codec));
      VALUE_CODEC = INT_CODEC.xmap((either) -> {
         return (IntProvider)either.map(ConstantIntProvider::create, (intProvider) -> {
            return intProvider;
         });
      }, (intProvider) -> {
         return intProvider.getType() == IntProviderType.CONSTANT ? Either.left(((ConstantIntProvider)intProvider).getValue()) : Either.right(intProvider);
      });
      field_33450 = createValidatingCodec(0, Integer.MAX_VALUE);
      field_33451 = createValidatingCodec(1, Integer.MAX_VALUE);
   }
}
