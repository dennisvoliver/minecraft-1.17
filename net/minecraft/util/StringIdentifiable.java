package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public interface StringIdentifiable {
   String asString();

   /**
    * Creates a codec that serializes an enum implementing this interface either
    * using its ordinals (when compressed) or using its {@link #asString()} method
    * and a given decode function.
    */
   static <E extends Enum<E> & StringIdentifiable> Codec<E> createCodec(Supplier<E[]> enumValues, Function<? super String, ? extends E> fromString) {
      E[] enums = (Enum[])enumValues.get();
      return createCodec((object) -> {
         return ((Enum)object).ordinal();
      }, (ordinal) -> {
         return enums[ordinal];
      }, fromString);
   }

   /**
    * Creates a codec that serializes a class implementing this interface using either
    * the given toInt and fromInt mapping functions (when compressed output is
    * requested), or its {@link #asString()} method and a given fromString function.
    */
   static <E extends StringIdentifiable> Codec<E> createCodec(final ToIntFunction<E> compressedEncoder, final IntFunction<E> compressedDecoder, final Function<? super String, ? extends E> decoder) {
      return new Codec<E>() {
         public <T> DataResult<T> encode(E stringIdentifiable, DynamicOps<T> dynamicOps, T object) {
            return dynamicOps.compressMaps() ? dynamicOps.mergeToPrimitive(object, dynamicOps.createInt(compressedEncoder.applyAsInt(stringIdentifiable))) : dynamicOps.mergeToPrimitive(object, dynamicOps.createString(stringIdentifiable.asString()));
         }

         public <T> DataResult<com.mojang.datafixers.util.Pair<E, T>> decode(DynamicOps<T> dynamicOps, T object) {
            return dynamicOps.compressMaps() ? dynamicOps.getNumberValue(object).flatMap((id) -> {
               return (DataResult)Optional.ofNullable((StringIdentifiable)compressedDecoder.apply(id.intValue())).map(DataResult::success).orElseGet(() -> {
                  return DataResult.error("Unknown element id: " + id);
               });
            }).map((stringIdentifiable) -> {
               return com.mojang.datafixers.util.Pair.of(stringIdentifiable, dynamicOps.empty());
            }) : dynamicOps.getStringValue(object).flatMap((name) -> {
               return (DataResult)Optional.ofNullable((StringIdentifiable)decoder.apply(name)).map(DataResult::success).orElseGet(() -> {
                  return DataResult.error("Unknown element name: " + name);
               });
            }).map((stringIdentifiable) -> {
               return com.mojang.datafixers.util.Pair.of(stringIdentifiable, dynamicOps.empty());
            });
         }

         public String toString() {
            return "StringRepresentable[" + compressedEncoder + "]";
         }
      };
   }

   static Keyable toKeyable(final StringIdentifiable[] values) {
      return new Keyable() {
         public <T> Stream<T> keys(DynamicOps<T> dynamicOps) {
            if (dynamicOps.compressMaps()) {
               IntStream var2 = IntStream.range(0, values.length);
               Objects.requireNonNull(dynamicOps);
               return var2.mapToObj(dynamicOps::createInt);
            } else {
               Stream var10000 = Arrays.stream(values).map(StringIdentifiable::asString);
               Objects.requireNonNull(dynamicOps);
               return var10000.map(dynamicOps::createString);
            }
         }
      };
   }
}
