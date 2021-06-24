package net.minecraft.util.registry;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.structure.processor.StructureProcessorType;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.dynamic.RegistryOps;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.surfacebuilder.ConfiguredSurfaceBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * A manager of dynamic registries. It allows users to access non-hardcoded
 * registries reliably.
 * 
 * <p>Each minecraft server has a dynamic registry manager for file-loaded
 * registries, while each client play network handler has a dynamic registry
 * manager for server-sent dynamic registries.
 * 
 * <p>The {@link DynamicRegistryManager.Impl}
 * class serves as an immutable implementation of any particular collection
 * or configuration of dynamic registries.
 */
public abstract class DynamicRegistryManager {
   private static final Logger LOGGER = LogManager.getLogger();
   static final Map<RegistryKey<? extends Registry<?>>, DynamicRegistryManager.Info<?>> INFOS = (Map)Util.make(() -> {
      Builder<RegistryKey<? extends Registry<?>>, DynamicRegistryManager.Info<?>> builder = ImmutableMap.builder();
      register(builder, Registry.DIMENSION_TYPE_KEY, DimensionType.CODEC, DimensionType.CODEC);
      register(builder, Registry.BIOME_KEY, Biome.CODEC, Biome.field_26633);
      register(builder, Registry.CONFIGURED_SURFACE_BUILDER_KEY, ConfiguredSurfaceBuilder.CODEC);
      register(builder, Registry.CONFIGURED_CARVER_KEY, ConfiguredCarver.CODEC);
      register(builder, Registry.CONFIGURED_FEATURE_KEY, ConfiguredFeature.CODEC);
      register(builder, Registry.CONFIGURED_STRUCTURE_FEATURE_KEY, ConfiguredStructureFeature.CODEC);
      register(builder, Registry.STRUCTURE_PROCESSOR_LIST_KEY, StructureProcessorType.field_25876);
      register(builder, Registry.STRUCTURE_POOL_KEY, StructurePool.CODEC);
      register(builder, Registry.CHUNK_GENERATOR_SETTINGS_KEY, ChunkGeneratorSettings.CODEC);
      return builder.build();
   });
   private static final DynamicRegistryManager.Impl BUILTIN = (DynamicRegistryManager.Impl)Util.make(() -> {
      DynamicRegistryManager.Impl impl = new DynamicRegistryManager.Impl();
      DimensionType.addRegistryDefaults(impl);
      INFOS.keySet().stream().filter((registryKey) -> {
         return !registryKey.equals(Registry.DIMENSION_TYPE_KEY);
      }).forEach((registryKey) -> {
         copyFromBuiltin(impl, registryKey);
      });
      return impl;
   });

   /**
    * Retrieves a registry optionally from this manager.
    */
   public abstract <E> Optional<MutableRegistry<E>> getOptionalMutable(RegistryKey<? extends Registry<? extends E>> key);

   public <E> MutableRegistry<E> getMutable(RegistryKey<? extends Registry<? extends E>> key) {
      return (MutableRegistry)this.getOptionalMutable(key).orElseThrow(() -> {
         return new IllegalStateException("Missing registry: " + key);
      });
   }

   public <E> Optional<? extends Registry<E>> getOptional(RegistryKey<? extends Registry<? extends E>> key) {
      Optional<? extends Registry<E>> optional = this.getOptionalMutable(key);
      return optional.isPresent() ? optional : Registry.REGISTRIES.getOrEmpty(key.getValue());
   }

   /**
    * Retrieves a registry from this manager, or throws an exception when the
    * registry does not exist.
    * 
    * @throws IllegalStateException if the registry does not exist
    */
   public <E> Registry<E> get(RegistryKey<? extends Registry<? extends E>> key) {
      return (Registry)this.getOptional(key).orElseThrow(() -> {
         return new IllegalStateException("Missing registry: " + key);
      });
   }

   private static <E> void register(Builder<RegistryKey<? extends Registry<?>>, DynamicRegistryManager.Info<?>> infosBuilder, RegistryKey<? extends Registry<E>> registryRef, Codec<E> entryCodec) {
      infosBuilder.put(registryRef, new DynamicRegistryManager.Info(registryRef, entryCodec, (Codec)null));
   }

   private static <E> void register(Builder<RegistryKey<? extends Registry<?>>, DynamicRegistryManager.Info<?>> infosBuilder, RegistryKey<? extends Registry<E>> registryRef, Codec<E> entryCodec, Codec<E> networkEntryCodec) {
      infosBuilder.put(registryRef, new DynamicRegistryManager.Info(registryRef, entryCodec, networkEntryCodec));
   }

   /**
    * Creates a default dynamic registry manager.
    */
   public static DynamicRegistryManager.Impl create() {
      DynamicRegistryManager.Impl impl = new DynamicRegistryManager.Impl();
      RegistryOps.EntryLoader.Impl impl2 = new RegistryOps.EntryLoader.Impl();
      Iterator var2 = INFOS.values().iterator();

      while(var2.hasNext()) {
         DynamicRegistryManager.Info<?> info = (DynamicRegistryManager.Info)var2.next();
         method_31141(impl, impl2, info);
      }

      RegistryOps.method_36575(JsonOps.INSTANCE, impl2, impl);
      return impl;
   }

   private static <E> void method_31141(DynamicRegistryManager.Impl registryManager, RegistryOps.EntryLoader.Impl entryLoader, DynamicRegistryManager.Info<E> info) {
      RegistryKey<? extends Registry<E>> registryKey = info.getRegistry();
      boolean bl = !registryKey.equals(Registry.CHUNK_GENERATOR_SETTINGS_KEY) && !registryKey.equals(Registry.DIMENSION_TYPE_KEY);
      Registry<E> registry = BUILTIN.get(registryKey);
      MutableRegistry<E> mutableRegistry = registryManager.getMutable(registryKey);
      Iterator var7 = registry.getEntries().iterator();

      while(var7.hasNext()) {
         Entry<RegistryKey<E>, E> entry = (Entry)var7.next();
         RegistryKey<E> registryKey2 = (RegistryKey)entry.getKey();
         E object = entry.getValue();
         if (bl) {
            entryLoader.add(BUILTIN, registryKey2, info.getEntryCodec(), registry.getRawId(object), object, registry.getEntryLifecycle(object));
         } else {
            mutableRegistry.set(registry.getRawId(object), registryKey2, object, registry.getEntryLifecycle(object));
         }
      }

   }

   /**
    * Add all entries of the registry referred by {@code registryRef} to the
    * corresponding registry within this manager.
    */
   private static <R extends Registry<?>> void copyFromBuiltin(DynamicRegistryManager.Impl manager, RegistryKey<R> registryRef) {
      Registry<R> registry = BuiltinRegistries.REGISTRIES;
      Registry<?> registry2 = (Registry)registry.getOrThrow(registryRef);
      addBuiltinEntries(manager, registry2);
   }

   /**
    * Add all entries of the {@code registry} to the corresponding registry
    * within this manager.
    */
   private static <E> void addBuiltinEntries(DynamicRegistryManager.Impl manager, Registry<E> registry) {
      MutableRegistry<E> mutableRegistry = manager.getMutable(registry.getKey());
      Iterator var3 = registry.getEntries().iterator();

      while(var3.hasNext()) {
         Entry<RegistryKey<E>, E> entry = (Entry)var3.next();
         E object = entry.getValue();
         mutableRegistry.set(registry.getRawId(object), (RegistryKey)entry.getKey(), object, registry.getEntryLifecycle(object));
      }

   }

   /**
    * Loads a dynamic registry manager from the resource manager's data files.
    */
   public static void load(DynamicRegistryManager dynamicRegistryManager, RegistryOps<?> registryOps) {
      Iterator var2 = INFOS.values().iterator();

      while(var2.hasNext()) {
         DynamicRegistryManager.Info<?> info = (DynamicRegistryManager.Info)var2.next();
         load(registryOps, dynamicRegistryManager, info);
      }

   }

   /**
    * Loads elements from the {@code ops} into the registry specified by {@code
    * info} within the {@code manager}. Note that the resource manager instance
    * is kept within the {@code ops}.
    */
   private static <E> void load(RegistryOps<?> ops, DynamicRegistryManager dynamicRegistryManager, DynamicRegistryManager.Info<E> info) {
      RegistryKey<? extends Registry<E>> registryKey = info.getRegistry();
      SimpleRegistry<E> simpleRegistry = (SimpleRegistry)dynamicRegistryManager.getMutable(registryKey);
      DataResult<SimpleRegistry<E>> dataResult = ops.loadToRegistry(simpleRegistry, info.getRegistry(), info.getEntryCodec());
      dataResult.error().ifPresent((partialResult) -> {
         throw new JsonParseException("Error loading registry data: " + partialResult.message());
      });
   }

   /**
    * Represents the serialization behavior of the registries, including the
    * id of the registry, the codec for its elements, and whether the registry
    * should be sent to the client.
    */
   static final class Info<E> {
      private final RegistryKey<? extends Registry<E>> registry;
      private final Codec<E> entryCodec;
      @Nullable
      private final Codec<E> networkEntryCodec;

      public Info(RegistryKey<? extends Registry<E>> registry, Codec<E> entryCodec, @Nullable Codec<E> networkEntryCodec) {
         this.registry = registry;
         this.entryCodec = entryCodec;
         this.networkEntryCodec = networkEntryCodec;
      }

      public RegistryKey<? extends Registry<E>> getRegistry() {
         return this.registry;
      }

      public Codec<E> getEntryCodec() {
         return this.entryCodec;
      }

      @Nullable
      public Codec<E> getNetworkEntryCodec() {
         return this.networkEntryCodec;
      }

      public boolean isSynced() {
         return this.networkEntryCodec != null;
      }
   }

   /**
    * An immutable implementation of the dynamic registry manager, representing
    * a specialized configuration of registries. It has a codec that allows
    * conversion from and to data pack JSON or packet NBT.
    */
   public static final class Impl extends DynamicRegistryManager {
      public static final Codec<DynamicRegistryManager.Impl> CODEC = setupCodec();
      private final Map<? extends RegistryKey<? extends Registry<?>>, ? extends SimpleRegistry<?>> registries;

      private static <E> Codec<DynamicRegistryManager.Impl> setupCodec() {
         Codec<RegistryKey<? extends Registry<E>>> codec = Identifier.CODEC.xmap(RegistryKey::ofRegistry, RegistryKey::getValue);
         Codec<SimpleRegistry<E>> codec2 = codec.partialDispatch("type", (simpleRegistry) -> {
            return DataResult.success(simpleRegistry.getKey());
         }, (registryKey) -> {
            return getDataResultForCodec(registryKey).map((codec) -> {
               return SimpleRegistry.createRegistryManagerCodec(registryKey, Lifecycle.experimental(), codec);
            });
         });
         UnboundedMapCodec<? extends RegistryKey<? extends Registry<?>>, ? extends SimpleRegistry<?>> unboundedMapCodec = Codec.unboundedMap(codec, codec2);
         return fromRegistryCodecs(unboundedMapCodec);
      }

      private static <K extends RegistryKey<? extends Registry<?>>, V extends SimpleRegistry<?>> Codec<DynamicRegistryManager.Impl> fromRegistryCodecs(UnboundedMapCodec<K, V> unboundedMapCodec) {
         return unboundedMapCodec.xmap(DynamicRegistryManager.Impl::new, (impl) -> {
            return (Map)impl.registries.entrySet().stream().filter((entry) -> {
               return ((DynamicRegistryManager.Info)DynamicRegistryManager.INFOS.get(entry.getKey())).isSynced();
            }).collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
         });
      }

      private static <E> DataResult<? extends Codec<E>> getDataResultForCodec(RegistryKey<? extends Registry<E>> registryRef) {
         return (DataResult)Optional.ofNullable((DynamicRegistryManager.Info)DynamicRegistryManager.INFOS.get(registryRef)).map((info) -> {
            return info.getNetworkEntryCodec();
         }).map(DataResult::success).orElseGet(() -> {
            return DataResult.error("Unknown or not serializable registry: " + registryRef);
         });
      }

      public Impl() {
         this((Map)DynamicRegistryManager.INFOS.keySet().stream().collect(Collectors.toMap(Function.identity(), DynamicRegistryManager.Impl::createRegistry)));
      }

      private Impl(Map<? extends RegistryKey<? extends Registry<?>>, ? extends SimpleRegistry<?>> registries) {
         this.registries = registries;
      }

      private static <E> SimpleRegistry<?> createRegistry(RegistryKey<? extends Registry<?>> registryRef) {
         return new SimpleRegistry(registryRef, Lifecycle.stable());
      }

      public <E> Optional<MutableRegistry<E>> getOptionalMutable(RegistryKey<? extends Registry<? extends E>> key) {
         return Optional.ofNullable((SimpleRegistry)this.registries.get(key)).map((simpleRegistry) -> {
            return simpleRegistry;
         });
      }
   }
}
