package net.minecraft.tag;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableSet.Builder;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

/**
 * Contains the set of tags all of the same type.
 */
public interface TagGroup<T> {
   Map<Identifier, Tag<T>> getTags();

   @Nullable
   default Tag<T> getTag(Identifier id) {
      return (Tag)this.getTags().get(id);
   }

   Tag<T> getTagOrEmpty(Identifier id);

   @Nullable
   default Identifier getId(Tag.Identified<T> tag) {
      return tag.getId();
   }

   @Nullable
   Identifier getUncheckedTagId(Tag<T> tag);

   default boolean contains(Identifier id) {
      return this.getTags().containsKey(id);
   }

   default Collection<Identifier> getTagIds() {
      return this.getTags().keySet();
   }

   /**
    * Gets the identifiers of all tags an object is applicable to.
    */
   default Collection<Identifier> getTagsFor(T object) {
      List<Identifier> list = Lists.newArrayList();
      Iterator var3 = this.getTags().entrySet().iterator();

      while(var3.hasNext()) {
         Entry<Identifier, Tag<T>> entry = (Entry)var3.next();
         if (((Tag)entry.getValue()).contains(object)) {
            list.add((Identifier)entry.getKey());
         }
      }

      return list;
   }

   /**
    * Serializes this tag group.
    */
   default TagGroup.Serialized serialize(Registry<T> registry) {
      Map<Identifier, Tag<T>> map = this.getTags();
      Map<Identifier, IntList> map2 = Maps.newHashMapWithExpectedSize(map.size());
      map.forEach((id, tag) -> {
         List<T> list = tag.values();
         IntList intList = new IntArrayList(list.size());
         Iterator var6 = list.iterator();

         while(var6.hasNext()) {
            T object = var6.next();
            intList.add(registry.getRawId(object));
         }

         map2.put(id, intList);
      });
      return new TagGroup.Serialized(map2);
   }

   /**
    * Deserializes a serialized tag group.
    */
   static <T> TagGroup<T> deserialize(TagGroup.Serialized serialized, Registry<? extends T> registry) {
      Map<Identifier, Tag<T>> map = Maps.newHashMapWithExpectedSize(serialized.contents.size());
      serialized.contents.forEach((id, entries) -> {
         Builder<T> builder = ImmutableSet.builder();
         IntListIterator var5 = entries.iterator();

         while(var5.hasNext()) {
            int i = (Integer)var5.next();
            builder.add(registry.get(i));
         }

         map.put(id, Tag.of(builder.build()));
      });
      return create(map);
   }

   static <T> TagGroup<T> createEmpty() {
      return create(ImmutableBiMap.of());
   }

   static <T> TagGroup<T> create(Map<Identifier, Tag<T>> tags) {
      final BiMap<Identifier, Tag<T>> biMap = ImmutableBiMap.copyOf(tags);
      return new TagGroup<T>() {
         private final Tag<T> emptyTag = SetTag.empty();

         public Tag<T> getTagOrEmpty(Identifier id) {
            return (Tag)biMap.getOrDefault(id, this.emptyTag);
         }

         @Nullable
         public Identifier getUncheckedTagId(Tag<T> tag) {
            return tag instanceof Tag.Identified ? ((Tag.Identified)tag).getId() : (Identifier)biMap.inverse().get(tag);
         }

         public Map<Identifier, Tag<T>> getTags() {
            return biMap;
         }
      };
   }

   /**
    * A serialization-friendly POJO representation of a {@linkplain
    * TagGroup tag group}. This allows easy transport of tag groups
    * over Minecraft network protocol.
    * 
    * <p>This stores tag entries with raw integer IDs and requires a registry
    * for raw ID access to {@linkplain TagGroup#serialize(Registry) serialize}
    * or {@linkplain TagGroup#deserialize(TagGroup.Serialized, Registry)
    * deserialize} tag groups.
    */
   public static class Serialized {
      final Map<Identifier, IntList> contents;

      Serialized(Map<Identifier, IntList> contents) {
         this.contents = contents;
      }

      public void writeBuf(PacketByteBuf buf) {
         buf.writeMap(this.contents, PacketByteBuf::writeIdentifier, PacketByteBuf::writeIntList);
      }

      public static TagGroup.Serialized fromBuf(PacketByteBuf buf) {
         return new TagGroup.Serialized(buf.readMap(PacketByteBuf::readIdentifier, PacketByteBuf::readIntList));
      }
   }
}
