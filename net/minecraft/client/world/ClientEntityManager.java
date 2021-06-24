package net.minecraft.client.world;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.entity.EntityChangeListener;
import net.minecraft.world.entity.EntityHandler;
import net.minecraft.world.entity.EntityIndex;
import net.minecraft.world.entity.EntityLike;
import net.minecraft.world.entity.EntityLookup;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.EntityTrackingStatus;
import net.minecraft.world.entity.SectionedEntityCache;
import net.minecraft.world.entity.SimpleEntityLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ClientEntityManager<T extends EntityLike> {
   static final Logger LOGGER = LogManager.getLogger();
   final EntityHandler<T> handler;
   final EntityIndex<T> index = new EntityIndex();
   final SectionedEntityCache<T> cache;
   private final LongSet tickingChunkSections = new LongOpenHashSet();
   private final EntityLookup<T> lookup;

   public ClientEntityManager(Class<T> entityClass, EntityHandler<T> handler) {
      this.cache = new SectionedEntityCache(entityClass, (pos) -> {
         return this.tickingChunkSections.contains(pos) ? EntityTrackingStatus.TICKING : EntityTrackingStatus.TRACKED;
      });
      this.handler = handler;
      this.lookup = new SimpleEntityLookup(this.index, this.cache);
   }

   public void startTicking(ChunkPos pos) {
      long l = pos.toLong();
      this.tickingChunkSections.add(l);
      this.cache.getTrackingSections(l).forEach((sections) -> {
         EntityTrackingStatus entityTrackingStatus = sections.swapStatus(EntityTrackingStatus.TICKING);
         if (!entityTrackingStatus.shouldTick()) {
            Stream var10000 = sections.stream().filter((e) -> {
               return !e.isPlayer();
            });
            EntityHandler var10001 = this.handler;
            Objects.requireNonNull(var10001);
            var10000.forEach(var10001::startTicking);
         }

      });
   }

   public void stopTicking(ChunkPos pos) {
      long l = pos.toLong();
      this.tickingChunkSections.remove(l);
      this.cache.getTrackingSections(l).forEach((sections) -> {
         EntityTrackingStatus entityTrackingStatus = sections.swapStatus(EntityTrackingStatus.TRACKED);
         if (entityTrackingStatus.shouldTick()) {
            Stream var10000 = sections.stream().filter((e) -> {
               return !e.isPlayer();
            });
            EntityHandler var10001 = this.handler;
            Objects.requireNonNull(var10001);
            var10000.forEach(var10001::stopTicking);
         }

      });
   }

   public EntityLookup<T> getLookup() {
      return this.lookup;
   }

   public void addEntity(T entity) {
      this.index.add(entity);
      long l = ChunkSectionPos.toLong(entity.getBlockPos());
      EntityTrackingSection<T> entityTrackingSection = this.cache.getTrackingSection(l);
      entityTrackingSection.add(entity);
      entity.setListener(new ClientEntityManager.Listener(entity, l, entityTrackingSection));
      this.handler.create(entity);
      this.handler.startTracking(entity);
      if (entity.isPlayer() || entityTrackingSection.getStatus().shouldTick()) {
         this.handler.startTicking(entity);
      }

   }

   @Debug
   public int getEntityCount() {
      return this.index.size();
   }

   void removeIfEmpty(long packedChunkSection, EntityTrackingSection<T> entities) {
      if (entities.isEmpty()) {
         this.cache.removeSection(packedChunkSection);
      }

   }

   @Debug
   public String getDebugString() {
      int var10000 = this.index.size();
      return var10000 + "," + this.cache.sectionCount() + "," + this.tickingChunkSections.size();
   }

   private class Listener implements EntityChangeListener {
      private final T entity;
      private long lastSectionPos;
      private EntityTrackingSection<T> section;

      Listener(T entity, long pos, EntityTrackingSection<T> section) {
         this.entity = entity;
         this.lastSectionPos = pos;
         this.section = section;
      }

      public void updateEntityPosition() {
         BlockPos blockPos = this.entity.getBlockPos();
         long l = ChunkSectionPos.toLong(blockPos);
         if (l != this.lastSectionPos) {
            EntityTrackingStatus entityTrackingStatus = this.section.getStatus();
            if (!this.section.remove(this.entity)) {
               ClientEntityManager.LOGGER.warn((String)"Entity {} wasn't found in section {} (moving to {})", (Object)this.entity, ChunkSectionPos.from(this.lastSectionPos), l);
            }

            ClientEntityManager.this.removeIfEmpty(this.lastSectionPos, this.section);
            EntityTrackingSection<T> entityTrackingSection = ClientEntityManager.this.cache.getTrackingSection(l);
            entityTrackingSection.add(this.entity);
            this.section = entityTrackingSection;
            this.lastSectionPos = l;
            if (!this.entity.isPlayer()) {
               boolean bl = entityTrackingStatus.shouldTick();
               boolean bl2 = entityTrackingSection.getStatus().shouldTick();
               if (bl && !bl2) {
                  ClientEntityManager.this.handler.stopTicking(this.entity);
               } else if (!bl && bl2) {
                  ClientEntityManager.this.handler.startTicking(this.entity);
               }
            }
         }

      }

      public void remove(Entity.RemovalReason reason) {
         if (!this.section.remove(this.entity)) {
            ClientEntityManager.LOGGER.warn((String)"Entity {} wasn't found in section {} (destroying due to {})", (Object)this.entity, ChunkSectionPos.from(this.lastSectionPos), reason);
         }

         EntityTrackingStatus entityTrackingStatus = this.section.getStatus();
         if (entityTrackingStatus.shouldTick() || this.entity.isPlayer()) {
            ClientEntityManager.this.handler.stopTicking(this.entity);
         }

         ClientEntityManager.this.handler.stopTracking(this.entity);
         ClientEntityManager.this.handler.destroy(this.entity);
         ClientEntityManager.this.index.remove(this.entity);
         this.entity.setListener(NONE);
         ClientEntityManager.this.removeIfEmpty(this.lastSectionPos, this.section);
      }
   }
}
