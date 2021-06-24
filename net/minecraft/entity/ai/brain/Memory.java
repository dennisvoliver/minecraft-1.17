package net.minecraft.entity.ai.brain;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.BiFunction;
import net.minecraft.util.annotation.Debug;

public class Memory<T> {
   private final T value;
   private long expiry;

   public Memory(T value, long expiry) {
      this.value = value;
      this.expiry = expiry;
   }

   public void tick() {
      if (this.isTimed()) {
         --this.expiry;
      }

   }

   /**
    * Creates a memory without an expiry time.
    */
   public static <T> Memory<T> permanent(T value) {
      return new Memory(value, Long.MAX_VALUE);
   }

   /**
    * Creates a memory that has an expiry time.
    */
   public static <T> Memory<T> timed(T value, long expiry) {
      return new Memory(value, expiry);
   }

   public long getExpiry() {
      return this.expiry;
   }

   public T getValue() {
      return this.value;
   }

   public boolean isExpired() {
      return this.expiry <= 0L;
   }

   public String toString() {
      Object var10000 = this.value;
      return var10000 + (this.isTimed() ? " (ttl: " + this.expiry + ")" : "");
   }

   @Debug
   public boolean isTimed() {
      return this.expiry != Long.MAX_VALUE;
   }

   public static <T> Codec<Memory<T>> createCodec(Codec<T> codec) {
      return RecordCodecBuilder.create((instance) -> {
         return instance.group(codec.fieldOf("value").forGetter((memory) -> {
            return memory.value;
         }), Codec.LONG.optionalFieldOf("ttl").forGetter((memory) -> {
            return memory.isTimed() ? Optional.of(memory.expiry) : Optional.empty();
         })).apply(instance, (BiFunction)((object, optional) -> {
            return new Memory(object, (Long)optional.orElse(Long.MAX_VALUE));
         }));
      });
   }
}
