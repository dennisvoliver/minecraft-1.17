package net.minecraft.server.world;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.Util;
import net.minecraft.util.annotation.Debug;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.BlockView;
import net.minecraft.world.GameRules;
import net.minecraft.world.LightType;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ChunkStatusChangeListener;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.jetbrains.annotations.Nullable;

public class ServerChunkManager extends ChunkManager {
   private static final List<ChunkStatus> CHUNK_STATUSES = ChunkStatus.createOrderedList();
   private final ChunkTicketManager ticketManager;
   private final ChunkGenerator chunkGenerator;
   final ServerWorld world;
   final Thread serverThread;
   final ServerLightingProvider lightProvider;
   private final ServerChunkManager.MainThreadExecutor mainThreadExecutor;
   public final ThreadedAnvilChunkStorage threadedAnvilChunkStorage;
   private final PersistentStateManager persistentStateManager;
   private long lastMobSpawningTime;
   private boolean spawnMonsters = true;
   private boolean spawnAnimals = true;
   private static final int field_29766 = 4;
   private final long[] chunkPosCache = new long[4];
   private final ChunkStatus[] chunkStatusCache = new ChunkStatus[4];
   private final Chunk[] chunkCache = new Chunk[4];
   @Nullable
   @Debug
   private SpawnHelper.Info spawnEntry;

   public ServerChunkManager(ServerWorld world, LevelStorage.Session session, DataFixer dataFixer, StructureManager structureManager, Executor workerExecutor, ChunkGenerator chunkGenerator, int viewDistance, boolean bl, WorldGenerationProgressListener worldGenerationProgressListener, ChunkStatusChangeListener chunkStatusChangeListener, Supplier<PersistentStateManager> supplier) {
      this.world = world;
      this.mainThreadExecutor = new ServerChunkManager.MainThreadExecutor(world);
      this.chunkGenerator = chunkGenerator;
      this.serverThread = Thread.currentThread();
      File file = session.getWorldDirectory(world.getRegistryKey());
      File file2 = new File(file, "data");
      file2.mkdirs();
      this.persistentStateManager = new PersistentStateManager(file2, dataFixer);
      this.threadedAnvilChunkStorage = new ThreadedAnvilChunkStorage(world, session, dataFixer, structureManager, workerExecutor, this.mainThreadExecutor, this, this.getChunkGenerator(), worldGenerationProgressListener, chunkStatusChangeListener, supplier, viewDistance, bl);
      this.lightProvider = this.threadedAnvilChunkStorage.getLightProvider();
      this.ticketManager = this.threadedAnvilChunkStorage.getTicketManager();
      this.initChunkCaches();
   }

   public ServerLightingProvider getLightingProvider() {
      return this.lightProvider;
   }

   @Nullable
   private ChunkHolder getChunkHolder(long pos) {
      return this.threadedAnvilChunkStorage.getChunkHolder(pos);
   }

   public int getTotalChunksLoadedCount() {
      return this.threadedAnvilChunkStorage.getTotalChunksLoadedCount();
   }

   private void putInCache(long pos, Chunk chunk, ChunkStatus status) {
      for(int i = 3; i > 0; --i) {
         this.chunkPosCache[i] = this.chunkPosCache[i - 1];
         this.chunkStatusCache[i] = this.chunkStatusCache[i - 1];
         this.chunkCache[i] = this.chunkCache[i - 1];
      }

      this.chunkPosCache[0] = pos;
      this.chunkStatusCache[0] = status;
      this.chunkCache[0] = chunk;
   }

   @Nullable
   public Chunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create) {
      if (Thread.currentThread() != this.serverThread) {
         return (Chunk)CompletableFuture.supplyAsync(() -> {
            return this.getChunk(x, z, leastStatus, create);
         }, this.mainThreadExecutor).join();
      } else {
         Profiler profiler = this.world.getProfiler();
         profiler.visit("getChunk");
         long l = ChunkPos.toLong(x, z);

         Chunk chunk;
         for(int i = 0; i < 4; ++i) {
            if (l == this.chunkPosCache[i] && leastStatus == this.chunkStatusCache[i]) {
               chunk = this.chunkCache[i];
               if (chunk != null || !create) {
                  return chunk;
               }
            }
         }

         profiler.visit("getChunkCacheMiss");
         CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> completableFuture = this.getChunkFuture(x, z, leastStatus, create);
         ServerChunkManager.MainThreadExecutor var10000 = this.mainThreadExecutor;
         Objects.requireNonNull(completableFuture);
         var10000.runTasks(completableFuture::isDone);
         chunk = (Chunk)((Either)completableFuture.join()).map((chunkx) -> {
            return chunkx;
         }, (unloaded) -> {
            if (create) {
               throw (IllegalStateException)Util.throwOrPause(new IllegalStateException("Chunk not there when requested: " + unloaded));
            } else {
               return null;
            }
         });
         this.putInCache(l, chunk, leastStatus);
         return chunk;
      }
   }

   @Nullable
   public WorldChunk getWorldChunk(int chunkX, int chunkZ) {
      if (Thread.currentThread() != this.serverThread) {
         return null;
      } else {
         this.world.getProfiler().visit("getChunkNow");
         long l = ChunkPos.toLong(chunkX, chunkZ);

         for(int i = 0; i < 4; ++i) {
            if (l == this.chunkPosCache[i] && this.chunkStatusCache[i] == ChunkStatus.FULL) {
               Chunk chunk = this.chunkCache[i];
               return chunk instanceof WorldChunk ? (WorldChunk)chunk : null;
            }
         }

         ChunkHolder chunkHolder = this.getChunkHolder(l);
         if (chunkHolder == null) {
            return null;
         } else {
            Either<Chunk, ChunkHolder.Unloaded> either = (Either)chunkHolder.getValidFutureFor(ChunkStatus.FULL).getNow((Object)null);
            if (either == null) {
               return null;
            } else {
               Chunk chunk2 = (Chunk)either.left().orElse((Object)null);
               if (chunk2 != null) {
                  this.putInCache(l, chunk2, ChunkStatus.FULL);
                  if (chunk2 instanceof WorldChunk) {
                     return (WorldChunk)chunk2;
                  }
               }

               return null;
            }
         }
      }
   }

   private void initChunkCaches() {
      Arrays.fill(this.chunkPosCache, ChunkPos.MARKER);
      Arrays.fill(this.chunkStatusCache, (Object)null);
      Arrays.fill(this.chunkCache, (Object)null);
   }

   public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getChunkFutureSyncOnMainThread(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
      boolean bl = Thread.currentThread() == this.serverThread;
      CompletableFuture completableFuture2;
      if (bl) {
         completableFuture2 = this.getChunkFuture(chunkX, chunkZ, leastStatus, create);
         ServerChunkManager.MainThreadExecutor var10000 = this.mainThreadExecutor;
         Objects.requireNonNull(completableFuture2);
         var10000.runTasks(completableFuture2::isDone);
      } else {
         completableFuture2 = CompletableFuture.supplyAsync(() -> {
            return this.getChunkFuture(chunkX, chunkZ, leastStatus, create);
         }, this.mainThreadExecutor).thenCompose((completableFuture) -> {
            return completableFuture;
         });
      }

      return completableFuture2;
   }

   private CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getChunkFuture(int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create) {
      ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
      long l = chunkPos.toLong();
      int i = 33 + ChunkStatus.getDistanceFromFull(leastStatus);
      ChunkHolder chunkHolder = this.getChunkHolder(l);
      if (create) {
         this.ticketManager.addTicketWithLevel(ChunkTicketType.UNKNOWN, chunkPos, i, chunkPos);
         if (this.isMissingForLevel(chunkHolder, i)) {
            Profiler profiler = this.world.getProfiler();
            profiler.push("chunkLoad");
            this.tick();
            chunkHolder = this.getChunkHolder(l);
            profiler.pop();
            if (this.isMissingForLevel(chunkHolder, i)) {
               throw (IllegalStateException)Util.throwOrPause(new IllegalStateException("No chunk holder after ticket has been added"));
            }
         }
      }

      return this.isMissingForLevel(chunkHolder, i) ? ChunkHolder.UNLOADED_CHUNK_FUTURE : chunkHolder.getChunkAt(leastStatus, this.threadedAnvilChunkStorage);
   }

   private boolean isMissingForLevel(@Nullable ChunkHolder holder, int maxLevel) {
      return holder == null || holder.getLevel() > maxLevel;
   }

   public boolean isChunkLoaded(int x, int z) {
      ChunkHolder chunkHolder = this.getChunkHolder((new ChunkPos(x, z)).toLong());
      int i = 33 + ChunkStatus.getDistanceFromFull(ChunkStatus.FULL);
      return !this.isMissingForLevel(chunkHolder, i);
   }

   public BlockView getChunk(int chunkX, int chunkZ) {
      long l = ChunkPos.toLong(chunkX, chunkZ);
      ChunkHolder chunkHolder = this.getChunkHolder(l);
      if (chunkHolder == null) {
         return null;
      } else {
         int i = CHUNK_STATUSES.size() - 1;

         while(true) {
            ChunkStatus chunkStatus = (ChunkStatus)CHUNK_STATUSES.get(i);
            Optional<Chunk> optional = ((Either)chunkHolder.getFutureFor(chunkStatus).getNow(ChunkHolder.UNLOADED_CHUNK)).left();
            if (optional.isPresent()) {
               return (BlockView)optional.get();
            }

            if (chunkStatus == ChunkStatus.LIGHT.getPrevious()) {
               return null;
            }

            --i;
         }
      }
   }

   public World getWorld() {
      return this.world;
   }

   public boolean executeQueuedTasks() {
      return this.mainThreadExecutor.runTask();
   }

   boolean tick() {
      boolean bl = this.ticketManager.tick(this.threadedAnvilChunkStorage);
      boolean bl2 = this.threadedAnvilChunkStorage.updateHolderMap();
      if (!bl && !bl2) {
         return false;
      } else {
         this.initChunkCaches();
         return true;
      }
   }

   public boolean method_37114(long l) {
      return this.isFutureReady(l, ChunkHolder::getTickingFuture);
   }

   private boolean isFutureReady(long pos, Function<ChunkHolder, CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>>> futureFunction) {
      ChunkHolder chunkHolder = this.getChunkHolder(pos);
      if (chunkHolder == null) {
         return false;
      } else {
         Either<WorldChunk, ChunkHolder.Unloaded> either = (Either)((CompletableFuture)futureFunction.apply(chunkHolder)).getNow(ChunkHolder.UNLOADED_WORLD_CHUNK);
         return either.left().isPresent();
      }
   }

   public void save(boolean flush) {
      this.tick();
      this.threadedAnvilChunkStorage.save(flush);
   }

   public void close() throws IOException {
      this.save(true);
      this.lightProvider.close();
      this.threadedAnvilChunkStorage.close();
   }

   public void tick(BooleanSupplier booleanSupplier) {
      this.world.getProfiler().push("purge");
      this.ticketManager.purge();
      this.tick();
      this.world.getProfiler().swap("chunks");
      this.tickChunks();
      this.world.getProfiler().swap("unload");
      this.threadedAnvilChunkStorage.tick(booleanSupplier);
      this.world.getProfiler().pop();
      this.initChunkCaches();
   }

   private void tickChunks() {
      long l = this.world.getTime();
      long m = l - this.lastMobSpawningTime;
      this.lastMobSpawningTime = l;
      WorldProperties worldProperties = this.world.getLevelProperties();
      boolean bl = this.world.isDebugWorld();
      boolean bl2 = this.world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING);
      if (!bl) {
         this.world.getProfiler().push("pollingChunks");
         int i = this.world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED);
         boolean bl3 = worldProperties.getTime() % 400L == 0L;
         this.world.getProfiler().push("naturalSpawnCount");
         int j = this.ticketManager.getSpawningChunkCount();
         SpawnHelper.Info info = SpawnHelper.setupSpawn(j, this.world.iterateEntities(), this::ifChunkLoaded);
         this.spawnEntry = info;
         this.world.getProfiler().pop();
         List<ChunkHolder> list = Lists.newArrayList(this.threadedAnvilChunkStorage.entryIterator());
         Collections.shuffle(list);
         list.forEach((chunkHolder) -> {
            Optional<WorldChunk> optional = ((Either)chunkHolder.getTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK)).left();
            if (optional.isPresent()) {
               this.world.getProfiler().push("broadcast");
               WorldChunk worldChunk = (WorldChunk)optional.get();
               chunkHolder.flushUpdates(worldChunk);
               this.world.getProfiler().pop();
               ChunkPos chunkPos = worldChunk.getPos();
               if (this.world.method_37115(chunkPos) && !this.threadedAnvilChunkStorage.isTooFarFromPlayersToSpawnMobs(chunkPos)) {
                  worldChunk.setInhabitedTime(worldChunk.getInhabitedTime() + m);
                  if (bl2 && (this.spawnMonsters || this.spawnAnimals) && this.world.getWorldBorder().contains(chunkPos)) {
                     SpawnHelper.spawn(this.world, worldChunk, info, this.spawnAnimals, this.spawnMonsters, bl3);
                  }

                  this.world.tickChunk(worldChunk, i);
               }
            }
         });
         this.world.getProfiler().push("customSpawners");
         if (bl2) {
            this.world.tickSpawners(this.spawnMonsters, this.spawnAnimals);
         }

         this.world.getProfiler().pop();
         this.world.getProfiler().pop();
      }

      this.threadedAnvilChunkStorage.tickEntityMovement();
   }

   private void ifChunkLoaded(long pos, Consumer<WorldChunk> chunkConsumer) {
      ChunkHolder chunkHolder = this.getChunkHolder(pos);
      if (chunkHolder != null) {
         ((Either)chunkHolder.getAccessibleFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK)).left().ifPresent(chunkConsumer);
      }

   }

   public String getDebugString() {
      return Integer.toString(this.getLoadedChunkCount());
   }

   @VisibleForTesting
   public int getPendingTasks() {
      return this.mainThreadExecutor.getTaskCount();
   }

   public ChunkGenerator getChunkGenerator() {
      return this.chunkGenerator;
   }

   public int getLoadedChunkCount() {
      return this.threadedAnvilChunkStorage.getLoadedChunkCount();
   }

   public void markForUpdate(BlockPos pos) {
      int i = ChunkSectionPos.getSectionCoord(pos.getX());
      int j = ChunkSectionPos.getSectionCoord(pos.getZ());
      ChunkHolder chunkHolder = this.getChunkHolder(ChunkPos.toLong(i, j));
      if (chunkHolder != null) {
         chunkHolder.markForBlockUpdate(pos);
      }

   }

   public void onLightUpdate(LightType type, ChunkSectionPos pos) {
      this.mainThreadExecutor.execute(() -> {
         ChunkHolder chunkHolder = this.getChunkHolder(pos.toChunkPos().toLong());
         if (chunkHolder != null) {
            chunkHolder.markForLightUpdate(type, pos.getSectionY());
         }

      });
   }

   /**
    * Adds a chunk ticket to the ticket manager.
    * 
    * <p>Addition of a ticket may load chunk(s) at some point in the future depending on the loading level in the ticket's vicinity.
    */
   public <T> void addTicket(ChunkTicketType<T> ticketType, ChunkPos pos, int radius, T argument) {
      this.ticketManager.addTicket(ticketType, pos, radius, argument);
   }

   /**
    * Removes a chunk ticket from the ticket manager.
    * 
    * <p>Removal of a ticket may unload chunk(s) at some point in the future depending on the loading levels in the ticket's vicinity after removal.
    */
   public <T> void removeTicket(ChunkTicketType<T> ticketType, ChunkPos pos, int radius, T argument) {
      this.ticketManager.removeTicket(ticketType, pos, radius, argument);
   }

   public void setChunkForced(ChunkPos pos, boolean forced) {
      this.ticketManager.setChunkForced(pos, forced);
   }

   /**
    * Updates the chunk section position of the {@code player}. This can either be a
    * result of the player's movement or its camera entity's movement.
    * 
    * <p>This updates the section position player's client is currently watching and
    * the player's position in its entity tracker.
    */
   public void updatePosition(ServerPlayerEntity player) {
      this.threadedAnvilChunkStorage.updatePosition(player);
   }

   public void unloadEntity(Entity entity) {
      this.threadedAnvilChunkStorage.unloadEntity(entity);
   }

   public void loadEntity(Entity entity) {
      this.threadedAnvilChunkStorage.loadEntity(entity);
   }

   public void sendToNearbyPlayers(Entity entity, Packet<?> packet) {
      this.threadedAnvilChunkStorage.sendToNearbyPlayers(entity, packet);
   }

   public void sendToOtherNearbyPlayers(Entity entity, Packet<?> packet) {
      this.threadedAnvilChunkStorage.sendToOtherNearbyPlayers(entity, packet);
   }

   public void applyViewDistance(int watchDistance) {
      this.threadedAnvilChunkStorage.setViewDistance(watchDistance);
   }

   public void setMobSpawnOptions(boolean spawnMonsters, boolean spawnAnimals) {
      this.spawnMonsters = spawnMonsters;
      this.spawnAnimals = spawnAnimals;
   }

   public String getChunkLoadingDebugInfo(ChunkPos pos) {
      return this.threadedAnvilChunkStorage.getChunkLoadingDebugInfo(pos);
   }

   public PersistentStateManager getPersistentStateManager() {
      return this.persistentStateManager;
   }

   public PointOfInterestStorage getPointOfInterestStorage() {
      return this.threadedAnvilChunkStorage.getPointOfInterestStorage();
   }

   @Nullable
   @Debug
   public SpawnHelper.Info getSpawnInfo() {
      return this.spawnEntry;
   }

   final class MainThreadExecutor extends ThreadExecutor<Runnable> {
      MainThreadExecutor(World world) {
         super("Chunk source main thread executor for " + world.getRegistryKey().getValue());
      }

      protected Runnable createTask(Runnable runnable) {
         return runnable;
      }

      protected boolean canExecute(Runnable task) {
         return true;
      }

      protected boolean shouldExecuteAsync() {
         return true;
      }

      protected Thread getThread() {
         return ServerChunkManager.this.serverThread;
      }

      protected void executeTask(Runnable task) {
         ServerChunkManager.this.world.getProfiler().visit("runTask");
         super.executeTask(task);
      }

      protected boolean runTask() {
         if (ServerChunkManager.this.tick()) {
            return true;
         } else {
            ServerChunkManager.this.lightProvider.tick();
            return super.runTask();
         }
      }
   }
}
