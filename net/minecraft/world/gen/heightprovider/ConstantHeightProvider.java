package net.minecraft.world.gen.heightprovider;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.YOffset;

public class ConstantHeightProvider extends HeightProvider {
   public static final ConstantHeightProvider ZERO = new ConstantHeightProvider(YOffset.fixed(0));
   public static final Codec<ConstantHeightProvider> CONSTANT_CODEC;
   private final YOffset offset;

   public static ConstantHeightProvider create(YOffset offset) {
      return new ConstantHeightProvider(offset);
   }

   private ConstantHeightProvider(YOffset offset) {
      this.offset = offset;
   }

   public YOffset getOffset() {
      return this.offset;
   }

   public int get(Random random, HeightContext context) {
      return this.offset.getY(context);
   }

   public HeightProviderType<?> getType() {
      return HeightProviderType.CONSTANT;
   }

   public String toString() {
      return this.offset.toString();
   }

   static {
      CONSTANT_CODEC = Codec.either(YOffset.OFFSET_CODEC, RecordCodecBuilder.create((instance) -> {
         return instance.group(YOffset.OFFSET_CODEC.fieldOf("value").forGetter((constantHeightProvider) -> {
            return constantHeightProvider.offset;
         })).apply(instance, (Function)(ConstantHeightProvider::new));
      })).xmap((either) -> {
         return (ConstantHeightProvider)either.map(ConstantHeightProvider::create, (constantHeightProvider) -> {
            return constantHeightProvider;
         });
      }, (constantHeightProvider) -> {
         return Either.left(constantHeightProvider.offset);
      });
   }
}
