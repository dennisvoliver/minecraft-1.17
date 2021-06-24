package net.minecraft.server.world;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ScheduledTick;
import net.minecraft.world.TickPriority;
import net.minecraft.world.TickScheduler;
import org.jetbrains.annotations.Nullable;

public class ServerTickScheduler<T> implements TickScheduler<T> {
   public static final int field_30975 = 65536;
   protected final Predicate<T> invalidObjPredicate;
   private final Function<T, Identifier> idToName;
   private final Set<ScheduledTick<T>> scheduledTickActions = Sets.newHashSet();
   private final Set<ScheduledTick<T>> scheduledTickActionsInOrder = Sets.newTreeSet(ScheduledTick.getComparator());
   private final ServerWorld world;
   private final Queue<ScheduledTick<T>> currentTickActions = Queues.newArrayDeque();
   private final List<ScheduledTick<T>> consumedTickActions = Lists.newArrayList();
   private final Consumer<ScheduledTick<T>> tickConsumer;

   public ServerTickScheduler(ServerWorld world, Predicate<T> invalidObjPredicate, Function<T, Identifier> idToName, Consumer<ScheduledTick<T>> tickConsumer) {
      this.invalidObjPredicate = invalidObjPredicate;
      this.idToName = idToName;
      this.world = world;
      this.tickConsumer = tickConsumer;
   }

   public void tick() {
      int i = this.scheduledTickActionsInOrder.size();
      if (i != this.scheduledTickActions.size()) {
         throw new IllegalStateException("TickNextTick list out of synch");
      } else {
         if (i > 65536) {
            i = 65536;
         }

         Iterator<ScheduledTick<T>> iterator = this.scheduledTickActionsInOrder.iterator();
         this.world.getProfiler().push("cleaning");

         ScheduledTick scheduledTick2;
         while(i > 0 && iterator.hasNext()) {
            scheduledTick2 = (ScheduledTick)iterator.next();
            if (scheduledTick2.time > this.world.getTime()) {
               break;
            }

            if (this.world.method_37117(scheduledTick2.pos)) {
               iterator.remove();
               this.scheduledTickActions.remove(scheduledTick2);
               this.currentTickActions.add(scheduledTick2);
               --i;
            }
         }

         this.world.getProfiler().swap("ticking");

         while((scheduledTick2 = (ScheduledTick)this.currentTickActions.poll()) != null) {
            if (this.world.method_37117(scheduledTick2.pos)) {
               try {
                  this.consumedTickActions.add(scheduledTick2);
                  this.tickConsumer.accept(scheduledTick2);
               } catch (Throwable var7) {
                  CrashReport crashReport = CrashReport.create(var7, "Exception while ticking");
                  CrashReportSection crashReportSection = crashReport.addElement("Block being ticked");
                  CrashReportSection.addBlockInfo(crashReportSection, this.world, scheduledTick2.pos, (BlockState)null);
                  throw new CrashException(crashReport);
               }
            } else {
               this.schedule(scheduledTick2.pos, scheduledTick2.getObject(), 0);
            }
         }

         this.world.getProfiler().pop();
         this.consumedTickActions.clear();
         this.currentTickActions.clear();
      }
   }

   public boolean isTicking(BlockPos pos, T object) {
      return this.currentTickActions.contains(new ScheduledTick(pos, object));
   }

   public List<ScheduledTick<T>> getScheduledTicksInChunk(ChunkPos pos, boolean updateState, boolean getStaleTicks) {
      int i = pos.getStartX() - 2;
      int j = i + 16 + 2;
      int k = pos.getStartZ() - 2;
      int l = k + 16 + 2;
      return this.getScheduledTicks(new BlockBox(i, this.world.getBottomY(), k, j, this.world.getTopY(), l), updateState, getStaleTicks);
   }

   public List<ScheduledTick<T>> getScheduledTicks(BlockBox bounds, boolean updateState, boolean getStaleTicks) {
      List<ScheduledTick<T>> list = this.transferTicksInBounds((List)null, this.scheduledTickActionsInOrder, bounds, updateState);
      if (updateState && list != null) {
         this.scheduledTickActions.removeAll(list);
      }

      list = this.transferTicksInBounds(list, this.currentTickActions, bounds, updateState);
      if (!getStaleTicks) {
         list = this.transferTicksInBounds(list, this.consumedTickActions, bounds, updateState);
      }

      return list == null ? Collections.emptyList() : list;
   }

   @Nullable
   private List<ScheduledTick<T>> transferTicksInBounds(@Nullable List<ScheduledTick<T>> dst, Collection<ScheduledTick<T>> src, BlockBox bounds, boolean move) {
      Iterator iterator = src.iterator();

      while(iterator.hasNext()) {
         ScheduledTick<T> scheduledTick = (ScheduledTick)iterator.next();
         BlockPos blockPos = scheduledTick.pos;
         if (blockPos.getX() >= bounds.getMinX() && blockPos.getX() < bounds.getMaxX() && blockPos.getZ() >= bounds.getMinZ() && blockPos.getZ() < bounds.getMaxZ()) {
            if (move) {
               iterator.remove();
            }

            if (dst == null) {
               dst = Lists.newArrayList();
            }

            ((List)dst).add(scheduledTick);
         }
      }

      return (List)dst;
   }

   public void copyScheduledTicks(BlockBox box, BlockPos offset) {
      List<ScheduledTick<T>> list = this.getScheduledTicks(box, false, false);
      Iterator var4 = list.iterator();

      while(var4.hasNext()) {
         ScheduledTick<T> scheduledTick = (ScheduledTick)var4.next();
         if (box.contains(scheduledTick.pos)) {
            BlockPos blockPos = scheduledTick.pos.add(offset);
            T object = scheduledTick.getObject();
            this.addScheduledTick(new ScheduledTick(blockPos, object, scheduledTick.time, scheduledTick.priority));
         }
      }

   }

   public NbtList toNbt(ChunkPos chunkPos) {
      List<ScheduledTick<T>> list = this.getScheduledTicksInChunk(chunkPos, false, true);
      return serializeScheduledTicks(this.idToName, list, this.world.getTime());
   }

   private static <T> NbtList serializeScheduledTicks(Function<T, Identifier> identifierProvider, Iterable<ScheduledTick<T>> scheduledTicks, long time) {
      NbtList nbtList = new NbtList();
      Iterator var5 = scheduledTicks.iterator();

      while(var5.hasNext()) {
         ScheduledTick<T> scheduledTick = (ScheduledTick)var5.next();
         NbtCompound nbtCompound = new NbtCompound();
         nbtCompound.putString("i", ((Identifier)identifierProvider.apply(scheduledTick.getObject())).toString());
         nbtCompound.putInt("x", scheduledTick.pos.getX());
         nbtCompound.putInt("y", scheduledTick.pos.getY());
         nbtCompound.putInt("z", scheduledTick.pos.getZ());
         nbtCompound.putInt("t", (int)(scheduledTick.time - time));
         nbtCompound.putInt("p", scheduledTick.priority.getIndex());
         nbtList.add(nbtCompound);
      }

      return nbtList;
   }

   public boolean isScheduled(BlockPos pos, T object) {
      return this.scheduledTickActions.contains(new ScheduledTick(pos, object));
   }

   public void schedule(BlockPos pos, T object, int delay, TickPriority priority) {
      if (!this.invalidObjPredicate.test(object)) {
         this.addScheduledTick(new ScheduledTick(pos, object, (long)delay + this.world.getTime(), priority));
      }

   }

   private void addScheduledTick(ScheduledTick<T> tick) {
      if (!this.scheduledTickActions.contains(tick)) {
         this.scheduledTickActions.add(tick);
         this.scheduledTickActionsInOrder.add(tick);
      }

   }

   public int getTicks() {
      return this.scheduledTickActions.size();
   }
}
