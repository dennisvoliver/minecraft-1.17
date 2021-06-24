package net.minecraft.client.util.profiler;

import net.minecraft.util.profiler.DummyProfiler;
import net.minecraft.util.profiler.Profiler;

public class DummyRecorder implements Recorder {
   public static final Recorder INSTANCE = new DummyRecorder();

   public void sample() {
   }

   public void start() {
   }

   public boolean isActive() {
      return false;
   }

   public Profiler getProfiler() {
      return DummyProfiler.INSTANCE;
   }

   public void read() {
   }
}
