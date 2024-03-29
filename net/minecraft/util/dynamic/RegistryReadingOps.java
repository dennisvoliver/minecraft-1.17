package net.minecraft.util.dynamic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.MutableRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;

/**
 * A dynamic ops that encode an id for a registry element rather than a full object.
 * 
 * @see RegistryElementCodec#encode(Object, DynamicOps, Object)
 */
public class RegistryReadingOps<T> extends ForwardingDynamicOps<T> {
   private final DynamicRegistryManager manager;

   public static <T> RegistryReadingOps<T> of(DynamicOps<T> delegate, DynamicRegistryManager tracker) {
      return new RegistryReadingOps(delegate, tracker);
   }

   private RegistryReadingOps(DynamicOps<T> delegate, DynamicRegistryManager tracker) {
      super(delegate);
      this.manager = tracker;
   }

   /**
    * Encode an id for a registry element than a full object if possible.
    * 
    * <p>This method is called by casting an arbitrary dynamic ops to a registry
    * reading ops.
    * 
    * @see RegistryOps#decodeOrId(Object, RegistryKey, Codec, boolean)
    */
   protected <E> DataResult<T> encodeOrId(E input, T prefix, RegistryKey<? extends Registry<E>> registryReference, Codec<E> codec) {
      Optional<MutableRegistry<E>> optional = this.manager.getOptionalMutable(registryReference);
      if (optional.isPresent()) {
         MutableRegistry<E> mutableRegistry = (MutableRegistry)optional.get();
         Optional<RegistryKey<E>> optional2 = mutableRegistry.getKey(input);
         if (optional2.isPresent()) {
            RegistryKey<E> registryKey = (RegistryKey)optional2.get();
            return Identifier.CODEC.encode(registryKey.getValue(), this.delegate, prefix);
         }
      }

      return codec.encode(input, this, prefix);
   }
}
