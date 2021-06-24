package net.minecraft.server;

import java.util.Objects;
import java.util.concurrent.Executor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.world.chunk.ChunkStatus;
import org.jetbrains.annotations.Nullable;

public class QueueingWorldGenerationProgressListener implements WorldGenerationProgressListener {
   private final WorldGenerationProgressListener progressListener;
   private final TaskExecutor<Runnable> queue;

   private QueueingWorldGenerationProgressListener(WorldGenerationProgressListener progressListener, Executor executor) {
      this.progressListener = progressListener;
      this.queue = TaskExecutor.create(executor, "progressListener");
   }

   public static QueueingWorldGenerationProgressListener create(WorldGenerationProgressListener progressListener, Executor executor) {
      QueueingWorldGenerationProgressListener queueingWorldGenerationProgressListener = new QueueingWorldGenerationProgressListener(progressListener, executor);
      queueingWorldGenerationProgressListener.start();
      return queueingWorldGenerationProgressListener;
   }

   public void start(ChunkPos spawnPos) {
      this.queue.send(() -> {
         this.progressListener.start(spawnPos);
      });
   }

   public void setChunkStatus(ChunkPos pos, @Nullable ChunkStatus status) {
      this.queue.send(() -> {
         this.progressListener.setChunkStatus(pos, status);
      });
   }

   public void start() {
      TaskExecutor var10000 = this.queue;
      WorldGenerationProgressListener var10001 = this.progressListener;
      Objects.requireNonNull(var10001);
      var10000.send(var10001::start);
   }

   public void stop() {
      TaskExecutor var10000 = this.queue;
      WorldGenerationProgressListener var10001 = this.progressListener;
      Objects.requireNonNull(var10001);
      var10000.send(var10001::stop);
   }
}
