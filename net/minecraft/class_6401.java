package net.minecraft;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.client.util.profiler.SamplingChannel;
import net.minecraft.client.util.profiler.SamplingRecorder;
import net.minecraft.entity.ai.Durations;
import net.minecraft.util.profiler.ProfilerSystem;
import net.minecraft.util.profiler.ReadableProfiler;

public class class_6401 {
   private final Set<String> field_33894 = new ObjectOpenHashSet();

   public Set<SamplingRecorder> method_37194(Supplier<ReadableProfiler> supplier) {
      Set<SamplingRecorder> set = (Set)((ReadableProfiler)supplier.get()).method_37168().stream().filter((pair) -> {
         return !this.field_33894.contains(pair.getLeft());
      }).map((pair) -> {
         return method_37196(supplier, (String)pair.getLeft(), (SamplingChannel)pair.getRight());
      }).collect(Collectors.toSet());
      Iterator var3 = set.iterator();

      while(var3.hasNext()) {
         SamplingRecorder samplingRecorder = (SamplingRecorder)var3.next();
         this.field_33894.add(samplingRecorder.method_37171());
      }

      return set;
   }

   private static SamplingRecorder method_37196(Supplier<ReadableProfiler> supplier, String string, SamplingChannel samplingChannel) {
      return SamplingRecorder.create(string, samplingChannel, () -> {
         ProfilerSystem.LocatedInfo locatedInfo = ((ReadableProfiler)supplier.get()).getInfo(string);
         return locatedInfo == null ? 0.0D : (double)locatedInfo.method_37169() / (double)Durations.field_33869;
      });
   }
}
