package net.minecraft;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import net.minecraft.client.util.profiler.SamplingChannel;
import net.minecraft.client.util.profiler.SamplingRecorder;
import net.minecraft.util.profiler.MetricSuppliers;
import net.minecraft.util.profiler.ReadableProfiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

public class class_6402 implements class_6400 {
   private static final Logger field_33988 = LogManager.getLogger();
   private final Set<SamplingRecorder> field_33895 = new ObjectOpenHashSet();
   private final class_6401 field_33896 = new class_6401();

   public class_6402(LongSupplier longSupplier, boolean bl) {
      this.field_33895.add(method_37202(longSupplier));
      if (bl) {
         this.field_33895.addAll(method_37199());
      }

   }

   public static Set<SamplingRecorder> method_37199() {
      Builder builder = ImmutableSet.builder();

      try {
         class_6402.class_6403 lv = new class_6402.class_6403();
         Stream var10000 = IntStream.range(0, lv.field_33897).mapToObj((i) -> {
            return SamplingRecorder.create("cpu#" + i, SamplingChannel.CPU, () -> {
               return lv.method_37205(i);
            });
         });
         Objects.requireNonNull(builder);
         var10000.forEach(builder::add);
      } catch (Throwable var2) {
         field_33988.warn("Failed to query cpu, no cpu stats will be recorded", var2);
      }

      builder.add((Object)SamplingRecorder.create("heap MiB", SamplingChannel.JVM, () -> {
         return (double)((float)(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576.0F);
      }));
      builder.addAll((Iterable)MetricSuppliers.INSTANCE.method_37178());
      return builder.build();
   }

   public Set<SamplingRecorder> method_37189(Supplier<ReadableProfiler> supplier) {
      this.field_33895.addAll(this.field_33896.method_37194(supplier));
      return this.field_33895;
   }

   public static SamplingRecorder method_37202(final LongSupplier longSupplier) {
      Stopwatch stopwatch = Stopwatch.createUnstarted(new Ticker() {
         public long read() {
            return longSupplier.getAsLong();
         }
      });
      ToDoubleFunction<Stopwatch> toDoubleFunction = (stopwatchx) -> {
         if (stopwatchx.isRunning()) {
            stopwatchx.stop();
         }

         long l = stopwatchx.elapsed(TimeUnit.NANOSECONDS);
         stopwatchx.reset();
         return (double)l;
      };
      SamplingRecorder.HighPassValueConsumer highPassValueConsumer = new SamplingRecorder.HighPassValueConsumer(2.0F);
      return SamplingRecorder.create("ticktime", SamplingChannel.TICK_LOOP, (ToDoubleFunction)toDoubleFunction, (Object)stopwatch).startAction(Stopwatch::start).writeAction(highPassValueConsumer).create();
   }

   static class class_6403 {
      private final SystemInfo field_33898 = new SystemInfo();
      private final CentralProcessor field_33899;
      public final int field_33897;
      private long[][] field_33900;
      private double[] field_33901;
      private long field_33902;

      class_6403() {
         this.field_33899 = this.field_33898.getHardware().getProcessor();
         this.field_33897 = this.field_33899.getLogicalProcessorCount();
         this.field_33900 = this.field_33899.getProcessorCpuLoadTicks();
         this.field_33901 = this.field_33899.getProcessorCpuLoadBetweenTicks(this.field_33900);
      }

      public double method_37205(int i) {
         long l = System.currentTimeMillis();
         if (this.field_33902 == 0L || this.field_33902 + 501L < l) {
            this.field_33901 = this.field_33899.getProcessorCpuLoadBetweenTicks(this.field_33900);
            this.field_33900 = this.field_33899.getProcessorCpuLoadTicks();
            this.field_33902 = l;
         }

         return this.field_33901[i] * 100.0D;
      }
   }
}
