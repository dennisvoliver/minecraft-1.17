package net.minecraft.entity.ai.brain.sensor;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.world.ServerWorld;

public class NearestItemsSensor extends Sensor<MobEntity> {
   private static final long HORIZONTAL_RANGE = 8L;
   private static final long VERTICAL_RANGE = 4L;
   public static final int MAX_RANGE = 9;

   public Set<MemoryModuleType<?>> getOutputMemoryModules() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
   }

   protected void sense(ServerWorld serverWorld, MobEntity mobEntity) {
      Brain<?> brain = mobEntity.getBrain();
      List<ItemEntity> list = serverWorld.getEntitiesByClass(ItemEntity.class, mobEntity.getBoundingBox().expand(8.0D, 4.0D, 8.0D), (itemEntity) -> {
         return true;
      });
      Objects.requireNonNull(mobEntity);
      list.sort(Comparator.comparingDouble(mobEntity::squaredDistanceTo));
      Stream var10000 = list.stream().filter((itemEntity) -> {
         return mobEntity.canGather(itemEntity.getStack());
      }).filter((itemEntity) -> {
         return itemEntity.isInRange(mobEntity, 9.0D);
      });
      Objects.requireNonNull(mobEntity);
      Optional<ItemEntity> optional = var10000.filter(mobEntity::canSee).findFirst();
      brain.remember(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, optional);
   }
}
