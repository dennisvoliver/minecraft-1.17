package net.minecraft.world;

import java.util.function.Function;
import net.minecraft.util.math.BlockPos;

public class MultiTickScheduler<T> implements TickScheduler<T> {
   private final Function<BlockPos, TickScheduler<T>> mapper;

   public MultiTickScheduler(Function<BlockPos, TickScheduler<T>> mapper) {
      this.mapper = mapper;
   }

   public boolean isScheduled(BlockPos pos, T object) {
      return ((TickScheduler)this.mapper.apply(pos)).isScheduled(pos, object);
   }

   public void schedule(BlockPos pos, T object, int delay, TickPriority priority) {
      ((TickScheduler)this.mapper.apply(pos)).schedule(pos, object, delay, priority);
   }

   public boolean isTicking(BlockPos pos, T object) {
      return false;
   }

   public int getTicks() {
      return 0;
   }
}
