package net.minecraft.world.gen.feature.size;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.util.registry.Registry;

/**
 * In charge of determining the amount of space needed by a tree to generate.
 */
public abstract class FeatureSize {
   public static final Codec<FeatureSize> TYPE_CODEC;
   protected static final int field_31522 = 16;
   protected final OptionalInt minClippedHeight;

   protected static <S extends FeatureSize> RecordCodecBuilder<S, OptionalInt> createCodec() {
      return Codec.intRange(0, 80).optionalFieldOf("min_clipped_height").xmap((optional) -> {
         return (OptionalInt)optional.map(OptionalInt::of).orElse(OptionalInt.empty());
      }, (optionalInt) -> {
         return optionalInt.isPresent() ? Optional.of(optionalInt.getAsInt()) : Optional.empty();
      }).forGetter((featureSize) -> {
         return featureSize.minClippedHeight;
      });
   }

   public FeatureSize(OptionalInt minClippedHeight) {
      this.minClippedHeight = minClippedHeight;
   }

   protected abstract FeatureSizeType<?> getType();

   /**
    * The radius that the tree needs to be empty or replaceable in order for it to generate.
    */
   public abstract int getRadius(int height, int y);

   public OptionalInt getMinClippedHeight() {
      return this.minClippedHeight;
   }

   static {
      TYPE_CODEC = Registry.FEATURE_SIZE_TYPE.dispatch(FeatureSize::getType, FeatureSizeType::getCodec);
   }
}
