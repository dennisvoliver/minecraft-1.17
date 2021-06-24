package net.minecraft.world.poi;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;

public class PointOfInterestSet {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Short2ObjectMap<PointOfInterest> pointsOfInterestByPos;
   private final Map<PointOfInterestType, Set<PointOfInterest>> pointsOfInterestByType;
   private final Runnable updateListener;
   private boolean valid;

   public static Codec<PointOfInterestSet> createCodec(Runnable updateListener) {
      Codec var10000 = RecordCodecBuilder.create((instance) -> {
         return instance.group(RecordCodecBuilder.point(updateListener), Codec.BOOL.optionalFieldOf("Valid", false).forGetter((poiSet) -> {
            return poiSet.valid;
         }), PointOfInterest.createCodec(updateListener).listOf().fieldOf("Records").forGetter((poiSet) -> {
            return ImmutableList.copyOf((Collection)poiSet.pointsOfInterestByPos.values());
         })).apply(instance, (Function3)(PointOfInterestSet::new));
      });
      Logger var10002 = LOGGER;
      Objects.requireNonNull(var10002);
      return var10000.orElseGet(Util.addPrefix("Failed to read POI section: ", var10002::error), () -> {
         return new PointOfInterestSet(updateListener, false, ImmutableList.of());
      });
   }

   public PointOfInterestSet(Runnable updateListener) {
      this(updateListener, true, ImmutableList.of());
   }

   private PointOfInterestSet(Runnable updateListener, boolean valid, List<PointOfInterest> list) {
      this.pointsOfInterestByPos = new Short2ObjectOpenHashMap();
      this.pointsOfInterestByType = Maps.newHashMap();
      this.updateListener = updateListener;
      this.valid = valid;
      list.forEach(this::add);
   }

   public Stream<PointOfInterest> get(Predicate<PointOfInterestType> predicate, PointOfInterestStorage.OccupationStatus occupationStatus) {
      return this.pointsOfInterestByType.entrySet().stream().filter((entry) -> {
         return predicate.test((PointOfInterestType)entry.getKey());
      }).flatMap((entry) -> {
         return ((Set)entry.getValue()).stream();
      }).filter(occupationStatus.getPredicate());
   }

   public void add(BlockPos pos, PointOfInterestType type) {
      if (this.add(new PointOfInterest(pos, type, this.updateListener))) {
         LOGGER.debug("Added POI of type {} @ {}", () -> {
            return type;
         }, () -> {
            return pos;
         });
         this.updateListener.run();
      }

   }

   private boolean add(PointOfInterest poi) {
      BlockPos blockPos = poi.getPos();
      PointOfInterestType pointOfInterestType = poi.getType();
      short s = ChunkSectionPos.packLocal(blockPos);
      PointOfInterest pointOfInterest = (PointOfInterest)this.pointsOfInterestByPos.get(s);
      if (pointOfInterest != null) {
         if (pointOfInterestType.equals(pointOfInterest.getType())) {
            return false;
         }

         Util.error("POI data mismatch: already registered at " + blockPos);
      }

      this.pointsOfInterestByPos.put(s, poi);
      ((Set)this.pointsOfInterestByType.computeIfAbsent(pointOfInterestType, (poiType) -> {
         return Sets.newHashSet();
      })).add(poi);
      return true;
   }

   public void remove(BlockPos pos) {
      PointOfInterest pointOfInterest = (PointOfInterest)this.pointsOfInterestByPos.remove(ChunkSectionPos.packLocal(pos));
      if (pointOfInterest == null) {
         LOGGER.error((String)"POI data mismatch: never registered at {}", (Object)pos);
      } else {
         ((Set)this.pointsOfInterestByType.get(pointOfInterest.getType())).remove(pointOfInterest);
         Logger var10000 = LOGGER;
         Supplier[] var10002 = new Supplier[2];
         Objects.requireNonNull(pointOfInterest);
         var10002[0] = pointOfInterest::getType;
         Objects.requireNonNull(pointOfInterest);
         var10002[1] = pointOfInterest::getPos;
         var10000.debug("Removed POI of type {} @ {}", var10002);
         this.updateListener.run();
      }
   }

   @Deprecated
   @Debug
   public int method_35157(BlockPos blockPos) {
      return (Integer)this.get(blockPos).map(PointOfInterest::getFreeTickets).orElse(0);
   }

   public boolean releaseTicket(BlockPos pos) {
      PointOfInterest pointOfInterest = (PointOfInterest)this.pointsOfInterestByPos.get(ChunkSectionPos.packLocal(pos));
      if (pointOfInterest == null) {
         throw (IllegalStateException)Util.throwOrPause(new IllegalStateException("POI never registered at " + pos));
      } else {
         boolean bl = pointOfInterest.releaseTicket();
         this.updateListener.run();
         return bl;
      }
   }

   public boolean test(BlockPos pos, Predicate<PointOfInterestType> predicate) {
      return this.getType(pos).filter(predicate).isPresent();
   }

   public Optional<PointOfInterestType> getType(BlockPos pos) {
      return this.get(pos).map(PointOfInterest::getType);
   }

   private Optional<PointOfInterest> get(BlockPos pos) {
      return Optional.ofNullable((PointOfInterest)this.pointsOfInterestByPos.get(ChunkSectionPos.packLocal(pos)));
   }

   public void updatePointsOfInterest(Consumer<BiConsumer<BlockPos, PointOfInterestType>> consumer) {
      if (!this.valid) {
         Short2ObjectMap<PointOfInterest> short2ObjectMap = new Short2ObjectOpenHashMap(this.pointsOfInterestByPos);
         this.clear();
         consumer.accept((pos, poiType) -> {
            short s = ChunkSectionPos.packLocal(pos);
            PointOfInterest pointOfInterest = (PointOfInterest)short2ObjectMap.computeIfAbsent(s, (i) -> {
               return new PointOfInterest(pos, poiType, this.updateListener);
            });
            this.add(pointOfInterest);
         });
         this.valid = true;
         this.updateListener.run();
      }

   }

   private void clear() {
      this.pointsOfInterestByPos.clear();
      this.pointsOfInterestByType.clear();
   }

   boolean isValid() {
      return this.valid;
   }
}
