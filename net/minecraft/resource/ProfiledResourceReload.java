package net.minecraft.resource;

import com.google.common.base.Stopwatch;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.ProfileResult;
import net.minecraft.util.profiler.ProfilerSystem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An implementation of resource reload that includes an additional profiling
 * summary for each reloader.
 */
public class ProfiledResourceReload extends SimpleResourceReload<ProfiledResourceReload.Summary> {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Stopwatch reloadTimer = Stopwatch.createUnstarted();

   public ProfiledResourceReload(ResourceManager manager, List<ResourceReloader> reloaders, Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage) {
      super(prepareExecutor, applyExecutor, manager, reloaders, (synchronizer, resourceManager, reloader, prepare, apply) -> {
         AtomicLong atomicLong = new AtomicLong();
         AtomicLong atomicLong2 = new AtomicLong();
         ProfilerSystem profilerSystem = new ProfilerSystem(Util.nanoTimeSupplier, () -> {
            return 0;
         }, false);
         ProfilerSystem profilerSystem2 = new ProfilerSystem(Util.nanoTimeSupplier, () -> {
            return 0;
         }, false);
         CompletableFuture<Void> completableFuture = reloader.reload(synchronizer, resourceManager, profilerSystem, profilerSystem2, (preparation) -> {
            prepare.execute(() -> {
               long l = Util.getMeasuringTimeNano();
               preparation.run();
               atomicLong.addAndGet(Util.getMeasuringTimeNano() - l);
            });
         }, (application) -> {
            apply.execute(() -> {
               long l = Util.getMeasuringTimeNano();
               application.run();
               atomicLong2.addAndGet(Util.getMeasuringTimeNano() - l);
            });
         });
         return completableFuture.thenApplyAsync((void_) -> {
            return new ProfiledResourceReload.Summary(reloader.getName(), profilerSystem.getResult(), profilerSystem2.getResult(), atomicLong, atomicLong2);
         }, applyExecutor);
      }, initialStage);
      this.reloadTimer.start();
      this.applyStageFuture.thenAcceptAsync(this::finish, applyExecutor);
   }

   private void finish(List<ProfiledResourceReload.Summary> summaries) {
      this.reloadTimer.stop();
      int i = 0;
      LOGGER.info((String)"Resource reload finished after {} ms", (Object)this.reloadTimer.elapsed(TimeUnit.MILLISECONDS));

      int k;
      for(Iterator var3 = summaries.iterator(); var3.hasNext(); i += k) {
         ProfiledResourceReload.Summary summary = (ProfiledResourceReload.Summary)var3.next();
         ProfileResult profileResult = summary.prepareProfile;
         ProfileResult profileResult2 = summary.applyProfile;
         int j = (int)((double)summary.prepareTimeMs.get() / 1000000.0D);
         k = (int)((double)summary.applyTimeMs.get() / 1000000.0D);
         int l = j + k;
         String string = summary.name;
         LOGGER.info((String)"{} took approximately {} ms ({} ms preparing, {} ms applying)", (Object)string, l, j, k);
      }

      LOGGER.info((String)"Total blocking time: {} ms", (Object)i);
   }

   /**
    * The profiling summary for each reloader in the reload.
    */
   public static class Summary {
      final String name;
      final ProfileResult prepareProfile;
      final ProfileResult applyProfile;
      final AtomicLong prepareTimeMs;
      final AtomicLong applyTimeMs;

      Summary(String string, ProfileResult profileResult, ProfileResult profileResult2, AtomicLong atomicLong, AtomicLong atomicLong2) {
         this.name = string;
         this.prepareProfile = profileResult;
         this.applyProfile = profileResult2;
         this.prepareTimeMs = atomicLong;
         this.applyTimeMs = atomicLong2;
      }
   }
}
