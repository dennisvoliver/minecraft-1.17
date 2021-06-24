package net.minecraft.util.profiler;

import java.util.Set;
import net.minecraft.client.util.profiler.SamplingChannel;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

public interface ReadableProfiler extends Profiler {
   ProfileResult getResult();

   @Nullable
   ProfilerSystem.LocatedInfo getInfo(String name);

   Set<Pair<String, SamplingChannel>> method_37168();
}
