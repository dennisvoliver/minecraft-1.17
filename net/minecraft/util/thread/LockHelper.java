package net.minecraft.util.thread;

import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import org.jetbrains.annotations.Nullable;

public class LockHelper {
   public static void checkLock(Semaphore semaphore, @Nullable AtomicStack<Pair<Thread, StackTraceElement[]>> lockStack, String message) {
      boolean bl = semaphore.tryAcquire();
      if (!bl) {
         throw crash(message, lockStack);
      }
   }

   public static CrashException crash(String message, @Nullable AtomicStack<Pair<Thread, StackTraceElement[]>> lockStack) {
      String string = (String)Thread.getAllStackTraces().keySet().stream().filter(Objects::nonNull).map((thread) -> {
         String var10000 = thread.getName();
         return var10000 + ": \n\tat " + (String)Arrays.stream(thread.getStackTrace()).map(Object::toString).collect(Collectors.joining("\n\tat "));
      }).collect(Collectors.joining("\n"));
      CrashReport crashReport = new CrashReport("Accessing " + message + " from multiple threads", new IllegalStateException());
      CrashReportSection crashReportSection = crashReport.addElement("Thread dumps");
      crashReportSection.add("Thread dumps", (Object)string);
      if (lockStack != null) {
         StringBuilder stringBuilder = new StringBuilder();
         List<Pair<Thread, StackTraceElement[]>> list = lockStack.toList();
         Iterator var7 = list.iterator();

         while(var7.hasNext()) {
            Pair<Thread, StackTraceElement[]> pair = (Pair)var7.next();
            stringBuilder.append("Thread ").append(((Thread)pair.getFirst()).getName()).append(": \n\tat ").append((String)Arrays.stream((StackTraceElement[])pair.getSecond()).map(Object::toString).collect(Collectors.joining("\n\tat "))).append("\n");
         }

         crashReportSection.add("Last threads", (Object)stringBuilder.toString());
      }

      return new CrashException(crashReport);
   }
}
