package net.minecraft.tag;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class TagManager {
   static final Logger LOGGER = LogManager.getLogger();
   public static final TagManager EMPTY = new TagManager(ImmutableMap.of());
   private final Map<RegistryKey<? extends Registry<?>>, TagGroup<?>> tagGroups;

   TagManager(Map<RegistryKey<? extends Registry<?>>, TagGroup<?>> tagGroups) {
      this.tagGroups = tagGroups;
   }

   @Nullable
   private <T> TagGroup<T> getTagGroup(RegistryKey<? extends Registry<T>> registryKey) {
      return (TagGroup)this.tagGroups.get(registryKey);
   }

   public <T> TagGroup<T> getOrCreateTagGroup(RegistryKey<? extends Registry<T>> registryKey) {
      return (TagGroup)this.tagGroups.getOrDefault(registryKey, TagGroup.createEmpty());
   }

   public <T, E extends Exception> Tag<T> getTag(RegistryKey<? extends Registry<T>> registryKey, Identifier id, Function<Identifier, E> exceptionFactory) throws E {
      TagGroup<T> tagGroup = this.getTagGroup(registryKey);
      if (tagGroup == null) {
         throw (Exception)exceptionFactory.apply(id);
      } else {
         Tag<T> tag = tagGroup.getTag(id);
         if (tag == null) {
            throw (Exception)exceptionFactory.apply(id);
         } else {
            return tag;
         }
      }
   }

   public <T, E extends Exception> Identifier getTagId(RegistryKey<? extends Registry<T>> registryKey, Tag<T> tag, Supplier<E> exceptionSupplier) throws E {
      TagGroup<T> tagGroup = this.getTagGroup(registryKey);
      if (tagGroup == null) {
         throw (Exception)exceptionSupplier.get();
      } else {
         Identifier identifier = tagGroup.getUncheckedTagId(tag);
         if (identifier == null) {
            throw (Exception)exceptionSupplier.get();
         } else {
            return identifier;
         }
      }
   }

   public void accept(TagManager.Visitor visitor) {
      this.tagGroups.forEach((type, group) -> {
         offerTo(visitor, type, group);
      });
   }

   private static <T> void offerTo(TagManager.Visitor visitor, RegistryKey<? extends Registry<?>> type, TagGroup<?> group) {
      visitor.visit(type, group);
   }

   public void apply() {
      RequiredTagListRegistry.updateTagManager(this);
      Blocks.refreshShapeCache();
   }

   public Map<RegistryKey<? extends Registry<?>>, TagGroup.Serialized> toPacket(final DynamicRegistryManager registryManager) {
      final Map<RegistryKey<? extends Registry<?>>, TagGroup.Serialized> map = Maps.newHashMap();
      this.accept(new TagManager.Visitor() {
         public <T> void visit(RegistryKey<? extends Registry<T>> type, TagGroup<T> group) {
            Optional<? extends Registry<T>> optional = registryManager.getOptional(type);
            if (optional.isPresent()) {
               map.put(type, group.serialize((Registry)optional.get()));
            } else {
               TagManager.LOGGER.error((String)"Unknown registry {}", (Object)type);
            }

         }
      });
      return map;
   }

   public static TagManager fromPacket(DynamicRegistryManager registryManager, Map<RegistryKey<? extends Registry<?>>, TagGroup.Serialized> groups) {
      TagManager.Builder builder = new TagManager.Builder();
      groups.forEach((type, group) -> {
         tryAdd(registryManager, builder, type, group);
      });
      return builder.build();
   }

   private static <T> void tryAdd(DynamicRegistryManager registryManager, TagManager.Builder builder, RegistryKey<? extends Registry<? extends T>> type, TagGroup.Serialized group) {
      Optional<? extends Registry<? extends T>> optional = registryManager.getOptional(type);
      if (optional.isPresent()) {
         builder.add(type, TagGroup.deserialize(group, (Registry)optional.get()));
      } else {
         LOGGER.error((String)"Unknown registry {}", (Object)type);
      }

   }

   @FunctionalInterface
   interface Visitor {
      <T> void visit(RegistryKey<? extends Registry<T>> type, TagGroup<T> group);
   }

   public static class Builder {
      private final com.google.common.collect.ImmutableMap.Builder<RegistryKey<? extends Registry<?>>, TagGroup<?>> groups = ImmutableMap.builder();

      public <T> TagManager.Builder add(RegistryKey<? extends Registry<? extends T>> type, TagGroup<T> tagGroup) {
         this.groups.put(type, tagGroup);
         return this;
      }

      public TagManager build() {
         return new TagManager(this.groups.build());
      }
   }
}
