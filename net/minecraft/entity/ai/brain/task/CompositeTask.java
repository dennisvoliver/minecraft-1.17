package net.minecraft.entity.ai.brain.task;

import com.mojang.datafixers.util.Pair;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.collection.WeightedList;

public class CompositeTask<E extends LivingEntity> extends Task<E> {
   private final Set<MemoryModuleType<?>> memoriesToForgetWhenStopped;
   private final CompositeTask.Order order;
   private final CompositeTask.RunMode runMode;
   private final WeightedList<Task<? super E>> tasks = new WeightedList();

   public CompositeTask(Map<MemoryModuleType<?>, MemoryModuleState> requiredMemoryState, Set<MemoryModuleType<?>> memoriesToForgetWhenStopped, CompositeTask.Order order, CompositeTask.RunMode runMode, List<Pair<Task<? super E>, Integer>> tasks) {
      super(requiredMemoryState);
      this.memoriesToForgetWhenStopped = memoriesToForgetWhenStopped;
      this.order = order;
      this.runMode = runMode;
      tasks.forEach((pair) -> {
         this.tasks.add((Task)pair.getFirst(), (Integer)pair.getSecond());
      });
   }

   protected boolean shouldKeepRunning(ServerWorld world, E entity, long time) {
      return this.tasks.stream().filter((task) -> {
         return task.getStatus() == Task.Status.RUNNING;
      }).anyMatch((task) -> {
         return task.shouldKeepRunning(world, entity, time);
      });
   }

   protected boolean isTimeLimitExceeded(long time) {
      return false;
   }

   protected void run(ServerWorld world, E entity, long time) {
      this.order.apply(this.tasks);
      this.runMode.run(this.tasks.stream(), world, entity, time);
   }

   protected void keepRunning(ServerWorld world, E entity, long time) {
      this.tasks.stream().filter((task) -> {
         return task.getStatus() == Task.Status.RUNNING;
      }).forEach((task) -> {
         task.tick(world, entity, time);
      });
   }

   protected void finishRunning(ServerWorld world, E entity, long time) {
      this.tasks.stream().filter((task) -> {
         return task.getStatus() == Task.Status.RUNNING;
      }).forEach((task) -> {
         task.stop(world, entity, time);
      });
      Set var10000 = this.memoriesToForgetWhenStopped;
      Brain var10001 = entity.getBrain();
      Objects.requireNonNull(var10001);
      var10000.forEach(var10001::forget);
   }

   public String toString() {
      Set<? extends Task<? super E>> set = (Set)this.tasks.stream().filter((task) -> {
         return task.getStatus() == Task.Status.RUNNING;
      }).collect(Collectors.toSet());
      String var10000 = this.getClass().getSimpleName();
      return "(" + var10000 + "): " + set;
   }

   public static enum Order {
      ORDERED((weightedList) -> {
      }),
      SHUFFLED(WeightedList::shuffle);

      private final Consumer<WeightedList<?>> listModifier;

      private Order(Consumer<WeightedList<?>> listModifier) {
         this.listModifier = listModifier;
      }

      public void apply(WeightedList<?> list) {
         this.listModifier.accept(list);
      }
   }

   public static enum RunMode {
      RUN_ONE {
         public <E extends LivingEntity> void run(Stream<Task<? super E>> tasks, ServerWorld world, E entity, long time) {
            tasks.filter((task) -> {
               return task.getStatus() == Task.Status.STOPPED;
            }).filter((task) -> {
               return task.tryStarting(world, entity, time);
            }).findFirst();
         }
      },
      TRY_ALL {
         public <E extends LivingEntity> void run(Stream<Task<? super E>> tasks, ServerWorld world, E entity, long time) {
            tasks.filter((task) -> {
               return task.getStatus() == Task.Status.STOPPED;
            }).forEach((task) -> {
               task.tryStarting(world, entity, time);
            });
         }
      };

      public abstract <E extends LivingEntity> void run(Stream<Task<? super E>> tasks, ServerWorld world, E entity, long time);
   }
}
