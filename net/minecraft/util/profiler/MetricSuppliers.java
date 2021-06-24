package net.minecraft.util.profiler;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
import net.minecraft.client.util.profiler.SamplingRecorder;
import org.jetbrains.annotations.Nullable;

public class MetricSuppliers {
   public static final MetricSuppliers INSTANCE = new MetricSuppliers();
   private final WeakHashMap<MetricSamplerSupplier, Void> samplers = new WeakHashMap();

   private MetricSuppliers() {
   }

   public void add(MetricSamplerSupplier supplier) {
      this.samplers.put(supplier, (Object)null);
   }

   public List<SamplingRecorder> method_37178() {
      Map<String, List<SamplingRecorder>> map = (Map)this.samplers.keySet().stream().flatMap((metricSamplerSupplier) -> {
         return metricSamplerSupplier.getSamplers().stream();
      }).collect(Collectors.groupingBy(SamplingRecorder::method_37171));
      return method_37180(map);
   }

   private static List<SamplingRecorder> method_37180(Map<String, List<SamplingRecorder>> map) {
      return (List)map.entrySet().stream().map((entry) -> {
         String string = (String)entry.getKey();
         List<SamplingRecorder> list = (List)entry.getValue();
         return (SamplingRecorder)(list.size() > 1 ? new MetricSuppliers.class_6399(string, list) : (SamplingRecorder)list.get(0));
      }).collect(Collectors.toList());
   }

   static class class_6399 extends SamplingRecorder {
      private final List<SamplingRecorder> field_33890;

      class_6399(String string, List<SamplingRecorder> list) {
         super(string, ((SamplingRecorder)list.get(0)).method_37172(), () -> {
            return method_37186(list);
         }, () -> {
            method_37185(list);
         }, method_37183(list));
         this.field_33890 = list;
      }

      private static SamplingRecorder.ValueConsumer method_37183(List<SamplingRecorder> list) {
         return (d) -> {
            return list.stream().anyMatch((samplingRecorder) -> {
               return samplingRecorder.writeAction != null ? samplingRecorder.writeAction.accept(d) : false;
            });
         };
      }

      private static void method_37185(List<SamplingRecorder> list) {
         Iterator var1 = list.iterator();

         while(var1.hasNext()) {
            SamplingRecorder samplingRecorder = (SamplingRecorder)var1.next();
            samplingRecorder.start();
         }

      }

      private static double method_37186(List<SamplingRecorder> list) {
         double d = 0.0D;

         SamplingRecorder samplingRecorder;
         for(Iterator var3 = list.iterator(); var3.hasNext(); d += samplingRecorder.method_37170().getAsDouble()) {
            samplingRecorder = (SamplingRecorder)var3.next();
         }

         return d / (double)list.size();
      }

      public boolean equals(@Nullable Object object) {
         if (this == object) {
            return true;
         } else if (object != null && this.getClass() == object.getClass()) {
            if (!super.equals(object)) {
               return false;
            } else {
               MetricSuppliers.class_6399 lv = (MetricSuppliers.class_6399)object;
               return this.field_33890.equals(lv.field_33890);
            }
         } else {
            return false;
         }
      }

      public int hashCode() {
         return Objects.hash(new Object[]{super.hashCode(), this.field_33890});
      }
   }
}
