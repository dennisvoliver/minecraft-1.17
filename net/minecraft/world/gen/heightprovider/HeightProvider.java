package net.minecraft.world.gen.heightprovider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.YOffset;

public abstract class HeightProvider {
   private static final Codec<Either<YOffset, HeightProvider>> field_31539;
   public static final Codec<HeightProvider> CODEC;

   public abstract int get(Random random, HeightContext context);

   public abstract HeightProviderType<?> getType();

   static {
      field_31539 = Codec.either(YOffset.OFFSET_CODEC, Registry.HEIGHT_PROVIDER_TYPE.dispatch(HeightProvider::getType, HeightProviderType::codec));
      CODEC = field_31539.xmap((either) -> {
         return (HeightProvider)either.map(ConstantHeightProvider::create, (heightProvider) -> {
            return heightProvider;
         });
      }, (heightProvider) -> {
         return heightProvider.getType() == HeightProviderType.CONSTANT ? Either.left(((ConstantHeightProvider)heightProvider).getOffset()) : Either.right(heightProvider);
      });
   }
}
