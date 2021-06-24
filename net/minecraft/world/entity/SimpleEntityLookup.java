package net.minecraft.world.entity;

import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of entity lookup backed by two separate {@link
 * EntityIndex} and {@link SectionedEntityCache}.
 * 
 * <p>It's up to the user to ensure that the index and the cache are
 * consistent with each other.
 * 
 * @param <T> the type of indexed entity
 */
public class SimpleEntityLookup<T extends EntityLike> implements EntityLookup<T> {
   private final EntityIndex<T> index;
   private final SectionedEntityCache<T> cache;

   public SimpleEntityLookup(EntityIndex<T> index, SectionedEntityCache<T> cache) {
      this.index = index;
      this.cache = cache;
   }

   @Nullable
   public T get(int id) {
      return this.index.get(id);
   }

   @Nullable
   public T get(UUID uuid) {
      return this.index.get(uuid);
   }

   public Iterable<T> iterate() {
      return this.index.iterate();
   }

   public <U extends T> void forEach(TypeFilter<T, U> filter, Consumer<U> action) {
      this.index.forEach(filter, action);
   }

   public void forEachIntersects(Box box, Consumer<T> action) {
      this.cache.forEachIntersects(box, action);
   }

   public <U extends T> void forEachIntersects(TypeFilter<T, U> filter, Box box, Consumer<U> action) {
      this.cache.forEachIntersects(filter, box, action);
   }
}
