package net.minecraft.util;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.LongSupplier;
import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.profiler.ProfilerSystem;
import net.minecraft.util.profiler.ReadableProfiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class TickDurationMonitor {
   private static final Logger LOGGER = LogManager.getLogger();
   private final LongSupplier timeGetter;
   private final long overtime;
   private int tickCount;
   private final File tickResultsDirectory;
   private ReadableProfiler profiler;

   public TickDurationMonitor(LongSupplier timeGetter, String filename, long overtime) {
      this.profiler = DummyProfiler.INSTANCE;
      this.timeGetter = timeGetter;
      this.tickResultsDirectory = new File("debug", filename);
      this.overtime = overtime;
   }

   public Profiler nextProfiler() {
      this.profiler = new ProfilerSystem(this.timeGetter, () -> {
         return this.tickCount;
      }, false);
      ++this.tickCount;
      return this.profiler;
   }

   public void endTick() {
      if (this.profiler != DummyProfiler.INSTANCE) {
         ProfileResult profileResult = this.profiler.getResult();
         this.profiler = DummyProfiler.INSTANCE;
         if (profileResult.getTimeSpan() >= this.overtime) {
            File var10002 = this.tickResultsDirectory;
            SimpleDateFormat var10003 = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
            Date var10004 = new Date();
            File file = new File(var10002, "tick-results-" + var10003.format(var10004) + ".txt");
            profileResult.save(file.toPath());
            LOGGER.info((String)"Recorded long tick -- wrote info to: {}", (Object)file.getAbsolutePath());
         }

      }
   }

   @Nullable
   public static TickDurationMonitor create(String name) {
      return null;
   }

   public static Profiler tickProfiler(Profiler profiler, @Nullable TickDurationMonitor monitor) {
      return monitor != null ? Profiler.union(monitor.nextProfiler(), profiler) : profiler;
   }
}
