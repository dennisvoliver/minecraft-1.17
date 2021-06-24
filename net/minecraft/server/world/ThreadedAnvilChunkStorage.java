package net.minecraft.server.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectBidirectionalIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonPart;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkRenderDistanceCenterS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityAttachS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.network.packet.s2c.play.LightUpdateS2CPacket;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.profiler.CsvWriter;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.thread.MessageListener;
import net.minecraft.util.thread.TaskExecutor;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.GameRules;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ChunkStatusChangeListener;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.ReadOnlyChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class ThreadedAnvilChunkStorage extends VersionedChunkStorage implements ChunkHolder.PlayersWatchingChunkProvider {
   private static final byte field_29671 = -1;
   private static final byte field_29672 = 0;
   private static final byte field_29673 = 1;
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int field_29674 = 200;
   private static final int field_29675 = 3;
   public static final int field_29669 = 33;
   /**
    * Specifies the maximum ticket level a chunk can be before a chunk's {@link net.minecraft.server.world.ChunkHolder.LevelType} is {@link net.minecraft.server.world.ChunkHolder.LevelType#BORDER}.
    */
   public static final int MAX_LEVEL = 33 + ChunkStatus.getMaxDistanceFromFull();
   public static final int field_29670 = 31;
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> currentChunkHolders = new Long2ObjectLinkedOpenHashMap();
   private volatile Long2ObjectLinkedOpenHashMap<ChunkHolder> chunkHolders;
   private final Long2ObjectLinkedOpenHashMap<ChunkHolder> chunksToUnload;
   private final LongSet loadedChunks;
   final ServerWorld world;
   private final ServerLightingProvider serverLightingProvider;
   private final ThreadExecutor<Runnable> mainThreadExecutor;
   private final ChunkGenerator chunkGenerator;
   private final Supplier<PersistentStateManager> persistentStateManagerFactory;
   private final PointOfInterestStorage pointOfInterestStorage;
   final LongSet unloadedChunks;
   private boolean chunkHolderListDirty;
   private final ChunkTaskPrioritySystem chunkTaskPrioritySystem;
   private final MessageListener<ChunkTaskPrioritySystem.Task<Runnable>> worldGenExecutor;
   private final MessageListener<ChunkTaskPrioritySystem.Task<Runnable>> mainExecutor;
   private final WorldGenerationProgressListener worldGenerationProgressListener;
   private final ChunkStatusChangeListener chunkStatusChangeListener;
   private final ThreadedAnvilChunkStorage.TicketManager ticketManager;
   private final AtomicInteger totalChunksLoadedCount;
   private final StructureManager structureManager;
   private final File saveDir;
   private final PlayerChunkWatchingManager playerChunkWatchingManager;
   private final Int2ObjectMap<ThreadedAnvilChunkStorage.EntityTracker> entityTrackers;
   private final Long2ByteMap chunkToType;
   private final Queue<Runnable> unloadTaskQueue;
   int watchDistance;

   public ThreadedAnvilChunkStorage(ServerWorld world, LevelStorage.Session session, DataFixer dataFixer, StructureManager structureManager, Executor executor, ThreadExecutor<Runnable> mainThreadExecutor, ChunkProvider chunkProvider, ChunkGenerator chunkGenerator, WorldGenerationProgressListener worldGenerationProgressListener, ChunkStatusChangeListener chunkStatusChangeListener, Supplier<PersistentStateManager> persistentStateManagerFactory, int viewDistance, boolean dsync) {
      super(new File(session.getWorldDirectory(world.getRegistryKey()), "region"), dataFixer, dsync);
      this.chunkHolders = this.currentChunkHolders.clone();
      this.chunksToUnload = new Long2ObjectLinkedOpenHashMap();
      this.loadedChunks = new LongOpenHashSet();
      this.unloadedChunks = new LongOpenHashSet();
      this.totalChunksLoadedCount = new AtomicInteger();
      this.playerChunkWatchingManager = new PlayerChunkWatchingManager();
      this.entityTrackers = new Int2ObjectOpenHashMap();
      this.chunkToType = new Long2ByteOpenHashMap();
      this.unloadTaskQueue = Queues.newConcurrentLinkedQueue();
      this.structureManager = structureManager;
      this.saveDir = session.getWorldDirectory(world.getRegistryKey());
      this.world = world;
      this.chunkGenerator = chunkGenerator;
      this.mainThreadExecutor = mainThreadExecutor;
      TaskExecutor<Runnable> taskExecutor = TaskExecutor.create(executor, "worldgen");
      Objects.requireNonNull(mainThreadExecutor);
      MessageListener<Runnable> messageListener = MessageListener.create("main", mainThreadExecutor::send);
      this.worldGenerationProgressListener = worldGenerationProgressListener;
      this.chunkStatusChangeListener = chunkStatusChangeListener;
      TaskExecutor<Runnable> taskExecutor2 = TaskExecutor.create(executor, "light");
      this.chunkTaskPrioritySystem = new ChunkTaskPrioritySystem(ImmutableList.of(taskExecutor, messageListener, taskExecutor2), executor, Integer.MAX_VALUE);
      this.worldGenExecutor = this.chunkTaskPrioritySystem.createExecutor(taskExecutor, false);
      this.mainExecutor = this.chunkTaskPrioritySystem.createExecutor(messageListener, false);
      this.serverLightingProvider = new ServerLightingProvider(chunkProvider, this, this.world.getDimension().hasSkyLight(), taskExecutor2, this.chunkTaskPrioritySystem.createExecutor(taskExecutor2, false));
      this.ticketManager = new ThreadedAnvilChunkStorage.TicketManager(executor, mainThreadExecutor);
      this.persistentStateManagerFactory = persistentStateManagerFactory;
      this.pointOfInterestStorage = new PointOfInterestStorage(new File(this.saveDir, "poi"), dataFixer, dsync, world);
      this.setViewDistance(viewDistance);
   }

   private static double getSquaredDistance(ChunkPos pos, Entity entity) {
      double d = (double)ChunkSectionPos.getOffsetPos(pos.x, 8);
      double e = (double)ChunkSectionPos.getOffsetPos(pos.z, 8);
      double f = d - entity.getX();
      double g = e - entity.getZ();
      return f * f + g * g;
   }

   private static int getChebyshevDistance(ChunkPos pos, ServerPlayerEntity player, boolean useWatchedPosition) {
      int k;
      int l;
      if (useWatchedPosition) {
         ChunkSectionPos chunkSectionPos = player.getWatchedSection();
         k = chunkSectionPos.getSectionX();
         l = chunkSectionPos.getSectionZ();
      } else {
         k = ChunkSectionPos.getSectionCoord(player.getBlockX());
         l = ChunkSectionPos.getSectionCoord(player.getBlockZ());
      }

      return getChebyshevDistance(pos, k, l);
   }

   private static int getChebyshevDistance(ChunkPos chunkPos, Entity entity) {
      return getChebyshevDistance(chunkPos, ChunkSectionPos.getSectionCoord(entity.getBlockX()), ChunkSectionPos.getSectionCoord(entity.getBlockZ()));
   }

   private static int getChebyshevDistance(ChunkPos pos, int x, int z) {
      int i = pos.x - x;
      int j = pos.z - z;
      return Math.max(Math.abs(i), Math.abs(j));
   }

   protected ServerLightingProvider getLightProvider() {
      return this.serverLightingProvider;
   }

   @Nullable
   protected ChunkHolder getCurrentChunkHolder(long pos) {
      return (ChunkHolder)this.currentChunkHolders.get(pos);
   }

   @Nullable
   protected ChunkHolder getChunkHolder(long pos) {
      return (ChunkHolder)this.chunkHolders.get(pos);
   }

   protected IntSupplier getCompletedLevelSupplier(long pos) {
      return () -> {
         ChunkHolder chunkHolder = this.getChunkHolder(pos);
         return chunkHolder == null ? LevelPrioritizedQueue.LEVEL_COUNT - 1 : Math.min(chunkHolder.getCompletedLevel(), LevelPrioritizedQueue.LEVEL_COUNT - 1);
      };
   }

   public String getChunkLoadingDebugInfo(ChunkPos chunkPos) {
      ChunkHolder chunkHolder = this.getChunkHolder(chunkPos.toLong());
      if (chunkHolder == null) {
         return "null";
      } else {
         String string = chunkHolder.getLevel() + "\n";
         ChunkStatus chunkStatus = chunkHolder.getCurrentStatus();
         Chunk chunk = chunkHolder.getCurrentChunk();
         if (chunkStatus != null) {
            string = string + "St: §" + chunkStatus.getIndex() + chunkStatus + "§r\n";
         }

         if (chunk != null) {
            string = string + "Ch: §" + chunk.getStatus().getIndex() + chunk.getStatus() + "§r\n";
         }

         ChunkHolder.LevelType levelType = chunkHolder.getLevelType();
         string = string + "§" + levelType.ordinal() + levelType;
         return string + "§r";
      }
   }

   private CompletableFuture<Either<List<Chunk>, ChunkHolder.Unloaded>> getRegion(ChunkPos centerChunk, int margin, IntFunction<ChunkStatus> distanceToStatus) {
      List<CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>>> list = Lists.newArrayList();
      int i = centerChunk.x;
      int j = centerChunk.z;

      for(int k = -margin; k <= margin; ++k) {
         for(int l = -margin; l <= margin; ++l) {
            int m = Math.max(Math.abs(l), Math.abs(k));
            final ChunkPos chunkPos = new ChunkPos(i + l, j + k);
            long n = chunkPos.toLong();
            ChunkHolder chunkHolder = this.getCurrentChunkHolder(n);
            if (chunkHolder == null) {
               return CompletableFuture.completedFuture(Either.right(new ChunkHolder.Unloaded() {
                  public String toString() {
                     return "Unloaded " + chunkPos;
                  }
               }));
            }

            ChunkStatus chunkStatus = (ChunkStatus)distanceToStatus.apply(m);
            CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> completableFuture = chunkHolder.getChunkAt(chunkStatus, this);
            list.add(completableFuture);
         }
      }

      CompletableFuture<List<Either<Chunk, ChunkHolder.Unloaded>>> completableFuture2 = Util.combineSafe(list);
      return completableFuture2.thenApply((listx) -> {
         List<Chunk> list2 = Lists.newArrayList();
         final int l = 0;

         for(Iterator var7 = listx.iterator(); var7.hasNext(); ++l) {
            final Either<Chunk, ChunkHolder.Unloaded> either = (Either)var7.next();
            Optional<Chunk> optional = either.left();
            if (!optional.isPresent()) {
               return Either.right(new ChunkHolder.Unloaded() {
                  public String toString() {
                     ChunkPos var10000 = new ChunkPos(i + l % (j * 2 + 1), k + l / (j * 2 + 1));
                     return "Unloaded " + var10000 + " " + either.right().get();
                  }
               });
            }

            list2.add((Chunk)optional.get());
         }

         return Either.left(list2);
      });
   }

   public CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>> makeChunkEntitiesTickable(ChunkPos pos) {
      return this.getRegion(pos, 2, (i) -> {
         return ChunkStatus.FULL;
      }).thenApplyAsync((either) -> {
         return either.mapLeft((list) -> {
            return (WorldChunk)list.get(list.size() / 2);
         });
      }, this.mainThreadExecutor);
   }

   @Nullable
   ChunkHolder setLevel(long pos, int level, @Nullable ChunkHolder holder, int i) {
      if (i > MAX_LEVEL && level > MAX_LEVEL) {
         return holder;
      } else {
         if (holder != null) {
            holder.setLevel(level);
         }

         if (holder != null) {
            if (level > MAX_LEVEL) {
               this.unloadedChunks.add(pos);
            } else {
               this.unloadedChunks.remove(pos);
            }
         }

         if (level <= MAX_LEVEL && holder == null) {
            holder = (ChunkHolder)this.chunksToUnload.remove(pos);
            if (holder != null) {
               holder.setLevel(level);
            } else {
               holder = new ChunkHolder(new ChunkPos(pos), level, this.world, this.serverLightingProvider, this.chunkTaskPrioritySystem, this);
            }

            this.currentChunkHolders.put(pos, holder);
            this.chunkHolderListDirty = true;
         }

         return holder;
      }
   }

   public void close() throws IOException {
      try {
         this.chunkTaskPrioritySystem.close();
         this.pointOfInterestStorage.close();
      } finally {
         super.close();
      }

   }

   protected void save(boolean flush) {
      if (flush) {
         List<ChunkHolder> list = (List)this.chunkHolders.values().stream().filter(ChunkHolder::isAccessible).peek(ChunkHolder::updateAccessibleStatus).collect(Collectors.toList());
         MutableBoolean mutableBoolean = new MutableBoolean();

         do {
            mutableBoolean.setFalse();
            list.stream().map((chunkHolder) -> {
               CompletableFuture completableFuture;
               do {
                  completableFuture = chunkHolder.getSavingFuture();
                  ThreadExecutor var10000 = this.mainThreadExecutor;
                  Objects.requireNonNull(completableFuture);
                  var10000.runTasks(completableFuture::isDone);
               } while(completableFuture != chunkHolder.getSavingFuture());

               return (Chunk)completableFuture.join();
            }).filter((chunk) -> {
               return chunk instanceof ReadOnlyChunk || chunk instanceof WorldChunk;
            }).filter(this::save).forEach((chunk) -> {
               mutableBoolean.setTrue();
            });
         } while(mutableBoolean.isTrue());

         this.unloadChunks(() -> {
            return true;
         });
         this.completeAll();
         LOGGER.info((String)"ThreadedAnvilChunkStorage ({}): All chunks are saved", (Object)this.saveDir.getName());
      } else {
         this.chunkHolders.values().stream().filter(ChunkHolder::isAccessible).forEach((chunkHolder) -> {
            Chunk chunk = (Chunk)chunkHolder.getSavingFuture().getNow((Object)null);
            if (chunk instanceof ReadOnlyChunk || chunk instanceof WorldChunk) {
               this.save(chunk);
               chunkHolder.updateAccessibleStatus();
            }

         });
      }

   }

   protected void tick(BooleanSupplier shouldKeepTicking) {
      Profiler profiler = this.world.getProfiler();
      profiler.push("poi");
      this.pointOfInterestStorage.tick(shouldKeepTicking);
      profiler.swap("chunk_unload");
      if (!this.world.isSavingDisabled()) {
         this.unloadChunks(shouldKeepTicking);
      }

      profiler.pop();
   }

   private void unloadChunks(BooleanSupplier shouldKeepTicking) {
      LongIterator longIterator = this.unloadedChunks.iterator();

      for(int i = 0; longIterator.hasNext() && (shouldKeepTicking.getAsBoolean() || i < 200 || this.unloadedChunks.size() > 2000); longIterator.remove()) {
         long l = longIterator.nextLong();
         ChunkHolder chunkHolder = (ChunkHolder)this.currentChunkHolders.remove(l);
         if (chunkHolder != null) {
            this.chunksToUnload.put(l, chunkHolder);
            this.chunkHolderListDirty = true;
            ++i;
            this.tryUnloadChunk(l, chunkHolder);
         }
      }

      Runnable runnable;
      while((shouldKeepTicking.getAsBoolean() || this.unloadTaskQueue.size() > 2000) && (runnable = (Runnable)this.unloadTaskQueue.poll()) != null) {
         runnable.run();
      }

   }

   private void tryUnloadChunk(long pos, ChunkHolder holder) {
      CompletableFuture<Chunk> completableFuture = holder.getSavingFuture();
      Consumer var10001 = (chunk) -> {
         CompletableFuture<Chunk> completableFuture2 = holder.getSavingFuture();
         if (completableFuture2 != completableFuture) {
            this.tryUnloadChunk(pos, holder);
         } else {
            if (this.chunksToUnload.remove(pos, holder) && chunk != null) {
               if (chunk instanceof WorldChunk) {
                  ((WorldChunk)chunk).setLoadedToWorld(false);
               }

               this.save(chunk);
               if (this.loadedChunks.remove(pos) && chunk instanceof WorldChunk) {
                  WorldChunk worldChunk = (WorldChunk)chunk;
                  this.world.unloadEntities(worldChunk);
               }

               this.serverLightingProvider.updateChunkStatus(chunk.getPos());
               this.serverLightingProvider.tick();
               this.worldGenerationProgressListener.setChunkStatus(chunk.getPos(), (ChunkStatus)null);
            }

         }
      };
      Queue var10002 = this.unloadTaskQueue;
      Objects.requireNonNull(var10002);
      completableFuture.thenAcceptAsync(var10001, var10002::add).whenComplete((void_, throwable) -> {
         if (throwable != null) {
            LOGGER.error((String)"Failed to save chunk {}", (Object)holder.getPos(), (Object)throwable);
         }

      });
   }

   protected boolean updateHolderMap() {
      if (!this.chunkHolderListDirty) {
         return false;
      } else {
         this.chunkHolders = this.currentChunkHolders.clone();
         this.chunkHolderListDirty = false;
         return true;
      }
   }

   public CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> getChunk(ChunkHolder holder, ChunkStatus requiredStatus) {
      ChunkPos chunkPos = holder.getPos();
      if (requiredStatus == ChunkStatus.EMPTY) {
         return this.loadChunk(chunkPos);
      } else {
         if (requiredStatus == ChunkStatus.LIGHT) {
            this.ticketManager.addTicketWithLevel(ChunkTicketType.LIGHT, chunkPos, 33 + ChunkStatus.getDistanceFromFull(ChunkStatus.FEATURES), chunkPos);
         }

         Optional<Chunk> optional = ((Either)holder.getChunkAt(requiredStatus.getPrevious(), this).getNow(ChunkHolder.UNLOADED_CHUNK)).left();
         if (optional.isPresent() && ((Chunk)optional.get()).getStatus().isAtLeast(requiredStatus)) {
            CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> completableFuture = requiredStatus.runLoadTask(this.world, this.structureManager, this.serverLightingProvider, (chunk) -> {
               return this.convertToFullChunk(holder);
            }, (Chunk)optional.get());
            this.worldGenerationProgressListener.setChunkStatus(chunkPos, requiredStatus);
            return completableFuture;
         } else {
            return this.upgradeChunk(holder, requiredStatus);
         }
      }
   }

   private CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> loadChunk(ChunkPos pos) {
      return CompletableFuture.supplyAsync(() -> {
         try {
            this.world.getProfiler().visit("chunkLoad");
            NbtCompound nbtCompound = this.getUpdatedChunkNbt(pos);
            if (nbtCompound != null) {
               boolean bl = nbtCompound.contains("Level", 10) && nbtCompound.getCompound("Level").contains("Status", 8);
               if (bl) {
                  Chunk chunk = ChunkSerializer.deserialize(this.world, this.structureManager, this.pointOfInterestStorage, pos, nbtCompound);
                  this.method_27053(pos, chunk.getStatus().getChunkType());
                  return Either.left(chunk);
               }

               LOGGER.error((String)"Chunk file at {} is missing level data, skipping", (Object)pos);
            }
         } catch (CrashException var5) {
            Throwable throwable = var5.getCause();
            if (!(throwable instanceof IOException)) {
               this.method_27054(pos);
               throw var5;
            }

            LOGGER.error((String)"Couldn't load chunk {}", (Object)pos, (Object)throwable);
         } catch (Exception var6) {
            LOGGER.error((String)"Couldn't load chunk {}", (Object)pos, (Object)var6);
         }

         this.method_27054(pos);
         return Either.left(new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA, this.world));
      }, this.mainThreadExecutor);
   }

   private void method_27054(ChunkPos chunkPos) {
      this.chunkToType.put(chunkPos.toLong(), (byte)-1);
   }

   private byte method_27053(ChunkPos chunkPos, ChunkStatus.ChunkType chunkType) {
      return this.chunkToType.put(chunkPos.toLong(), (byte)(chunkType == ChunkStatus.ChunkType.PROTOCHUNK ? -1 : 1));
   }

   private CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> upgradeChunk(ChunkHolder holder, ChunkStatus requiredStatus) {
      ChunkPos chunkPos = holder.getPos();
      CompletableFuture<Either<List<Chunk>, ChunkHolder.Unloaded>> completableFuture = this.getRegion(chunkPos, requiredStatus.getTaskMargin(), (i) -> {
         return this.getRequiredStatusForGeneration(requiredStatus, i);
      });
      this.world.getProfiler().visit(() -> {
         return "chunkGenerate " + requiredStatus.getId();
      });
      Executor executor = (runnable) -> {
         this.worldGenExecutor.send(ChunkTaskPrioritySystem.createMessage(holder, runnable));
      };
      return completableFuture.thenComposeAsync((either) -> {
         return (CompletionStage)either.map((list) -> {
            try {
               CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> completableFuture = requiredStatus.runGenerationTask(executor, this.world, this.chunkGenerator, this.structureManager, this.serverLightingProvider, (chunk) -> {
                  return this.convertToFullChunk(holder);
               }, list);
               this.worldGenerationProgressListener.setChunkStatus(chunkPos, requiredStatus);
               return completableFuture;
            } catch (Exception var9) {
               var9.getStackTrace();
               CrashReport crashReport = CrashReport.create(var9, "Exception generating new chunk");
               CrashReportSection crashReportSection = crashReport.addElement("Chunk to be generated");
               crashReportSection.add("Location", (Object)String.format("%d,%d", chunkPos.x, chunkPos.z));
               crashReportSection.add("Position hash", (Object)ChunkPos.toLong(chunkPos.x, chunkPos.z));
               crashReportSection.add("Generator", (Object)this.chunkGenerator);
               throw new CrashException(crashReport);
            }
         }, (unloaded) -> {
            this.releaseLightTicket(chunkPos);
            return CompletableFuture.completedFuture(Either.right(unloaded));
         });
      }, executor);
   }

   protected void releaseLightTicket(ChunkPos pos) {
      this.mainThreadExecutor.send(Util.debugRunnable(() -> {
         this.ticketManager.removeTicketWithLevel(ChunkTicketType.LIGHT, pos, 33 + ChunkStatus.getDistanceFromFull(ChunkStatus.FEATURES), pos);
      }, () -> {
         return "release light ticket " + pos;
      }));
   }

   private ChunkStatus getRequiredStatusForGeneration(ChunkStatus centerChunkTargetStatus, int distance) {
      ChunkStatus chunkStatus2;
      if (distance == 0) {
         chunkStatus2 = centerChunkTargetStatus.getPrevious();
      } else {
         chunkStatus2 = ChunkStatus.byDistanceFromFull(ChunkStatus.getDistanceFromFull(centerChunkTargetStatus) + distance);
      }

      return chunkStatus2;
   }

   private static void method_31413(ServerWorld serverWorld, List<NbtCompound> list) {
      if (!list.isEmpty()) {
         serverWorld.addEntities(EntityType.streamFromNbt(list, serverWorld));
      }

   }

   private CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> convertToFullChunk(ChunkHolder chunkHolder) {
      CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> completableFuture = chunkHolder.getFutureFor(ChunkStatus.FULL.getPrevious());
      return completableFuture.thenApplyAsync((either) -> {
         ChunkStatus chunkStatus = ChunkHolder.getTargetStatusForLevel(chunkHolder.getLevel());
         return !chunkStatus.isAtLeast(ChunkStatus.FULL) ? ChunkHolder.UNLOADED_CHUNK : either.mapLeft((chunk) -> {
            ChunkPos chunkPos = chunkHolder.getPos();
            ProtoChunk protoChunk = (ProtoChunk)chunk;
            WorldChunk worldChunk2;
            if (protoChunk instanceof ReadOnlyChunk) {
               worldChunk2 = ((ReadOnlyChunk)protoChunk).getWrappedChunk();
            } else {
               worldChunk2 = new WorldChunk(this.world, protoChunk, (worldChunk) -> {
                  method_31413(this.world, protoChunk.getEntities());
               });
               chunkHolder.setCompletedChunk(new ReadOnlyChunk(worldChunk2));
            }

            worldChunk2.setLevelTypeProvider(() -> {
               return ChunkHolder.getLevelType(chunkHolder.getLevel());
            });
            worldChunk2.loadToWorld();
            if (this.loadedChunks.add(chunkPos.toLong())) {
               worldChunk2.setLoadedToWorld(true);
               worldChunk2.updateAllBlockEntities();
            }

            return worldChunk2;
         });
      }, (runnable) -> {
         MessageListener var10000 = this.mainExecutor;
         long var10002 = chunkHolder.getPos().toLong();
         Objects.requireNonNull(chunkHolder);
         var10000.send(ChunkTaskPrioritySystem.createMessage(runnable, var10002, chunkHolder::getLevel));
      });
   }

   public CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>> makeChunkTickable(ChunkHolder holder) {
      ChunkPos chunkPos = holder.getPos();
      CompletableFuture<Either<List<Chunk>, ChunkHolder.Unloaded>> completableFuture = this.getRegion(chunkPos, 1, (i) -> {
         return ChunkStatus.FULL;
      });
      CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>> completableFuture2 = completableFuture.thenApplyAsync((either) -> {
         return either.flatMap((list) -> {
            WorldChunk worldChunk = (WorldChunk)list.get(list.size() / 2);
            worldChunk.runPostProcessing();
            return Either.left(worldChunk);
         });
      }, (runnable) -> {
         this.mainExecutor.send(ChunkTaskPrioritySystem.createMessage(holder, runnable));
      });
      completableFuture2.thenAcceptAsync((either) -> {
         either.ifLeft((worldChunk) -> {
            this.totalChunksLoadedCount.getAndIncrement();
            Packet<?>[] packets = new Packet[2];
            this.getPlayersWatchingChunk(chunkPos, false).forEach((serverPlayerEntity) -> {
               this.sendChunkDataPackets(serverPlayerEntity, packets, worldChunk);
            });
         });
      }, (runnable) -> {
         this.mainExecutor.send(ChunkTaskPrioritySystem.createMessage(holder, runnable));
      });
      return completableFuture2;
   }

   public CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>> method_31417(ChunkHolder chunkHolder) {
      return this.getRegion(chunkHolder.getPos(), 1, ChunkStatus::byDistanceFromFull).thenApplyAsync((either) -> {
         return either.mapLeft((list) -> {
            WorldChunk worldChunk = (WorldChunk)list.get(list.size() / 2);
            worldChunk.disableTickSchedulers();
            return worldChunk;
         });
      }, (runnable) -> {
         this.mainExecutor.send(ChunkTaskPrioritySystem.createMessage(chunkHolder, runnable));
      });
   }

   public int getTotalChunksLoadedCount() {
      return this.totalChunksLoadedCount.get();
   }

   private boolean save(Chunk chunk) {
      this.pointOfInterestStorage.saveChunk(chunk.getPos());
      if (!chunk.needsSaving()) {
         return false;
      } else {
         chunk.setShouldSave(false);
         ChunkPos chunkPos = chunk.getPos();

         try {
            ChunkStatus chunkStatus = chunk.getStatus();
            if (chunkStatus.getChunkType() != ChunkStatus.ChunkType.LEVELCHUNK) {
               if (this.method_27055(chunkPos)) {
                  return false;
               }

               if (chunkStatus == ChunkStatus.EMPTY && chunk.getStructureStarts().values().stream().noneMatch(StructureStart::hasChildren)) {
                  return false;
               }
            }

            this.world.getProfiler().visit("chunkSave");
            NbtCompound nbtCompound = ChunkSerializer.serialize(this.world, chunk);
            this.setNbt(chunkPos, nbtCompound);
            this.method_27053(chunkPos, chunkStatus.getChunkType());
            return true;
         } catch (Exception var5) {
            LOGGER.error((String)"Failed to save chunk {},{}", (Object)chunkPos.x, chunkPos.z, var5);
            return false;
         }
      }
   }

   private boolean method_27055(ChunkPos chunkPos) {
      byte b = this.chunkToType.get(chunkPos.toLong());
      if (b != 0) {
         return b == 1;
      } else {
         NbtCompound nbtCompound2;
         try {
            nbtCompound2 = this.getUpdatedChunkNbt(chunkPos);
            if (nbtCompound2 == null) {
               this.method_27054(chunkPos);
               return false;
            }
         } catch (Exception var5) {
            LOGGER.error((String)"Failed to read chunk {}", (Object)chunkPos, (Object)var5);
            this.method_27054(chunkPos);
            return false;
         }

         ChunkStatus.ChunkType chunkType = ChunkSerializer.getChunkType(nbtCompound2);
         return this.method_27053(chunkPos, chunkType) == 1;
      }
   }

   protected void setViewDistance(int watchDistance) {
      int i = MathHelper.clamp((int)(watchDistance + 1), (int)3, (int)33);
      if (i != this.watchDistance) {
         int j = this.watchDistance;
         this.watchDistance = i;
         this.ticketManager.setWatchDistance(this.watchDistance);
         ObjectIterator var4 = this.currentChunkHolders.values().iterator();

         while(var4.hasNext()) {
            ChunkHolder chunkHolder = (ChunkHolder)var4.next();
            ChunkPos chunkPos = chunkHolder.getPos();
            Packet<?>[] packets = new Packet[2];
            this.getPlayersWatchingChunk(chunkPos, false).forEach((serverPlayerEntity) -> {
               int jx = getChebyshevDistance(chunkPos, serverPlayerEntity, true);
               boolean bl = jx <= j;
               boolean bl2 = jx <= this.watchDistance;
               this.sendWatchPackets(serverPlayerEntity, chunkPos, packets, bl, bl2);
            });
         }
      }

   }

   protected void sendWatchPackets(ServerPlayerEntity player, ChunkPos pos, Packet<?>[] packets, boolean withinMaxWatchDistance, boolean withinViewDistance) {
      if (player.world == this.world) {
         if (withinViewDistance && !withinMaxWatchDistance) {
            ChunkHolder chunkHolder = this.getChunkHolder(pos.toLong());
            if (chunkHolder != null) {
               WorldChunk worldChunk = chunkHolder.getWorldChunk();
               if (worldChunk != null) {
                  this.sendChunkDataPackets(player, packets, worldChunk);
               }

               DebugInfoSender.sendChunkWatchingChange(this.world, pos);
            }
         }

         if (!withinViewDistance && withinMaxWatchDistance) {
            player.sendUnloadChunkPacket(pos);
         }

      }
   }

   public int getLoadedChunkCount() {
      return this.chunkHolders.size();
   }

   protected ChunkTicketManager getTicketManager() {
      return this.ticketManager;
   }

   protected Iterable<ChunkHolder> entryIterator() {
      return Iterables.unmodifiableIterable((Iterable)this.chunkHolders.values());
   }

   void dump(Writer writer) throws IOException {
      CsvWriter csvWriter = CsvWriter.makeHeader().addColumn("x").addColumn("z").addColumn("level").addColumn("in_memory").addColumn("status").addColumn("full_status").addColumn("accessible_ready").addColumn("ticking_ready").addColumn("entity_ticking_ready").addColumn("ticket").addColumn("spawning").addColumn("block_entity_count").startBody(writer);
      ObjectBidirectionalIterator var3 = this.chunkHolders.long2ObjectEntrySet().iterator();

      while(var3.hasNext()) {
         Entry<ChunkHolder> entry = (Entry)var3.next();
         ChunkPos chunkPos = new ChunkPos(entry.getLongKey());
         ChunkHolder chunkHolder = (ChunkHolder)entry.getValue();
         Optional<Chunk> optional = Optional.ofNullable(chunkHolder.getCurrentChunk());
         Optional<WorldChunk> optional2 = optional.flatMap((chunk) -> {
            return chunk instanceof WorldChunk ? Optional.of((WorldChunk)chunk) : Optional.empty();
         });
         csvWriter.printRow(chunkPos.x, chunkPos.z, chunkHolder.getLevel(), optional.isPresent(), optional.map(Chunk::getStatus).orElse((Object)null), optional2.map(WorldChunk::getLevelType).orElse((Object)null), getFutureStatus(chunkHolder.getAccessibleFuture()), getFutureStatus(chunkHolder.getTickingFuture()), getFutureStatus(chunkHolder.getEntityTickingFuture()), this.ticketManager.getTicket(entry.getLongKey()), !this.isTooFarFromPlayersToSpawnMobs(chunkPos), optional2.map((worldChunk) -> {
            return worldChunk.getBlockEntities().size();
         }).orElse(0));
      }

   }

   private static String getFutureStatus(CompletableFuture<Either<WorldChunk, ChunkHolder.Unloaded>> completableFuture) {
      try {
         Either<WorldChunk, ChunkHolder.Unloaded> either = (Either)completableFuture.getNow((Object)null);
         return either != null ? (String)either.map((worldChunk) -> {
            return "done";
         }, (unloaded) -> {
            return "unloaded";
         }) : "not completed";
      } catch (CompletionException var2) {
         return "failed " + var2.getCause().getMessage();
      } catch (CancellationException var3) {
         return "cancelled";
      }
   }

   @Nullable
   private NbtCompound getUpdatedChunkNbt(ChunkPos pos) throws IOException {
      NbtCompound nbtCompound = this.getNbt(pos);
      return nbtCompound == null ? null : this.updateChunkNbt(this.world.getRegistryKey(), this.persistentStateManagerFactory, nbtCompound);
   }

   boolean isTooFarFromPlayersToSpawnMobs(ChunkPos chunkPos) {
      long l = chunkPos.toLong();
      return !this.ticketManager.method_20800(l) ? true : this.playerChunkWatchingManager.getPlayersWatchingChunk(l).noneMatch((serverPlayerEntity) -> {
         return !serverPlayerEntity.isSpectator() && getSquaredDistance(chunkPos, serverPlayerEntity) < 16384.0D;
      });
   }

   private boolean doesNotGenerateChunks(ServerPlayerEntity player) {
      return player.isSpectator() && !this.world.getGameRules().getBoolean(GameRules.SPECTATORS_GENERATE_CHUNKS);
   }

   void handlePlayerAddedOrRemoved(ServerPlayerEntity player, boolean added) {
      boolean bl = this.doesNotGenerateChunks(player);
      boolean bl2 = this.playerChunkWatchingManager.isWatchInactive(player);
      int i = ChunkSectionPos.getSectionCoord(player.getBlockX());
      int j = ChunkSectionPos.getSectionCoord(player.getBlockZ());
      if (added) {
         this.playerChunkWatchingManager.add(ChunkPos.toLong(i, j), player, bl);
         this.updateWatchedSection(player);
         if (!bl) {
            this.ticketManager.handleChunkEnter(ChunkSectionPos.from((Entity)player), player);
         }
      } else {
         ChunkSectionPos chunkSectionPos = player.getWatchedSection();
         this.playerChunkWatchingManager.remove(chunkSectionPos.toChunkPos().toLong(), player);
         if (!bl2) {
            this.ticketManager.handleChunkLeave(chunkSectionPos, player);
         }
      }

      for(int k = i - this.watchDistance; k <= i + this.watchDistance; ++k) {
         for(int l = j - this.watchDistance; l <= j + this.watchDistance; ++l) {
            ChunkPos chunkPos = new ChunkPos(k, l);
            this.sendWatchPackets(player, chunkPos, new Packet[2], !added, added);
         }
      }

   }

   /**
    * Updates the watched chunk section position for the {@code player}, and sends a
    * render distance update packet to the client.
    */
   private ChunkSectionPos updateWatchedSection(ServerPlayerEntity player) {
      ChunkSectionPos chunkSectionPos = ChunkSectionPos.from((Entity)player);
      player.setWatchedSection(chunkSectionPos);
      player.networkHandler.sendPacket(new ChunkRenderDistanceCenterS2CPacket(chunkSectionPos.getSectionX(), chunkSectionPos.getSectionZ()));
      return chunkSectionPos;
   }

   /**
    * Updates the chunk section position of the {@code player}. This updates the player
    * position for both entity tracking and chunk loading (watching) logic.
    * 
    * @see ServerChunkManager#updatePosition(ServerPlayerEntity)
    */
   public void updatePosition(ServerPlayerEntity player) {
      ObjectIterator var2 = this.entityTrackers.values().iterator();

      while(var2.hasNext()) {
         ThreadedAnvilChunkStorage.EntityTracker entityTracker = (ThreadedAnvilChunkStorage.EntityTracker)var2.next();
         if (entityTracker.entity == player) {
            entityTracker.updateTrackedStatus(this.world.getPlayers());
         } else {
            entityTracker.updateTrackedStatus(player);
         }
      }

      int i = ChunkSectionPos.getSectionCoord(player.getBlockX());
      int j = ChunkSectionPos.getSectionCoord(player.getBlockZ());
      ChunkSectionPos chunkSectionPos = player.getWatchedSection();
      ChunkSectionPos chunkSectionPos2 = ChunkSectionPos.from((Entity)player);
      long l = chunkSectionPos.toChunkPos().toLong();
      long m = chunkSectionPos2.toChunkPos().toLong();
      boolean bl = this.playerChunkWatchingManager.isWatchDisabled(player);
      boolean bl2 = this.doesNotGenerateChunks(player);
      boolean bl3 = chunkSectionPos.asLong() != chunkSectionPos2.asLong();
      if (bl3 || bl != bl2) {
         this.updateWatchedSection(player);
         if (!bl) {
            this.ticketManager.handleChunkLeave(chunkSectionPos, player);
         }

         if (!bl2) {
            this.ticketManager.handleChunkEnter(chunkSectionPos2, player);
         }

         if (!bl && bl2) {
            this.playerChunkWatchingManager.disableWatch(player);
         }

         if (bl && !bl2) {
            this.playerChunkWatchingManager.enableWatch(player);
         }

         if (l != m) {
            this.playerChunkWatchingManager.movePlayer(l, m, player);
         }
      }

      int k = chunkSectionPos.getSectionX();
      int n = chunkSectionPos.getSectionZ();
      int w;
      int x;
      if (Math.abs(k - i) <= this.watchDistance * 2 && Math.abs(n - j) <= this.watchDistance * 2) {
         w = Math.min(i, k) - this.watchDistance;
         x = Math.min(j, n) - this.watchDistance;
         int q = Math.max(i, k) + this.watchDistance;
         int r = Math.max(j, n) + this.watchDistance;

         for(int s = w; s <= q; ++s) {
            for(int t = x; t <= r; ++t) {
               ChunkPos chunkPos = new ChunkPos(s, t);
               boolean bl4 = getChebyshevDistance(chunkPos, k, n) <= this.watchDistance;
               boolean bl5 = getChebyshevDistance(chunkPos, i, j) <= this.watchDistance;
               this.sendWatchPackets(player, chunkPos, new Packet[2], bl4, bl5);
            }
         }
      } else {
         ChunkPos chunkPos3;
         boolean bl8;
         boolean bl9;
         for(w = k - this.watchDistance; w <= k + this.watchDistance; ++w) {
            for(x = n - this.watchDistance; x <= n + this.watchDistance; ++x) {
               chunkPos3 = new ChunkPos(w, x);
               bl8 = true;
               bl9 = false;
               this.sendWatchPackets(player, chunkPos3, new Packet[2], true, false);
            }
         }

         for(w = i - this.watchDistance; w <= i + this.watchDistance; ++w) {
            for(x = j - this.watchDistance; x <= j + this.watchDistance; ++x) {
               chunkPos3 = new ChunkPos(w, x);
               bl8 = false;
               bl9 = true;
               this.sendWatchPackets(player, chunkPos3, new Packet[2], false, true);
            }
         }
      }

   }

   public Stream<ServerPlayerEntity> getPlayersWatchingChunk(ChunkPos chunkPos, boolean onlyOnWatchDistanceEdge) {
      return this.playerChunkWatchingManager.getPlayersWatchingChunk(chunkPos.toLong()).filter((serverPlayerEntity) -> {
         int i = getChebyshevDistance(chunkPos, serverPlayerEntity, true);
         if (i > this.watchDistance) {
            return false;
         } else {
            return !onlyOnWatchDistanceEdge || i == this.watchDistance;
         }
      });
   }

   protected void loadEntity(Entity entity) {
      if (!(entity instanceof EnderDragonPart)) {
         EntityType<?> entityType = entity.getType();
         int i = entityType.getMaxTrackDistance() * 16;
         if (i != 0) {
            int j = entityType.getTrackTickInterval();
            if (this.entityTrackers.containsKey(entity.getId())) {
               throw (IllegalStateException)Util.throwOrPause(new IllegalStateException("Entity is already tracked!"));
            } else {
               ThreadedAnvilChunkStorage.EntityTracker entityTracker = new ThreadedAnvilChunkStorage.EntityTracker(entity, i, j, entityType.alwaysUpdateVelocity());
               this.entityTrackers.put(entity.getId(), entityTracker);
               entityTracker.updateTrackedStatus(this.world.getPlayers());
               if (entity instanceof ServerPlayerEntity) {
                  ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
                  this.handlePlayerAddedOrRemoved(serverPlayerEntity, true);
                  ObjectIterator var7 = this.entityTrackers.values().iterator();

                  while(var7.hasNext()) {
                     ThreadedAnvilChunkStorage.EntityTracker entityTracker2 = (ThreadedAnvilChunkStorage.EntityTracker)var7.next();
                     if (entityTracker2.entity != serverPlayerEntity) {
                        entityTracker2.updateTrackedStatus(serverPlayerEntity);
                     }
                  }
               }

            }
         }
      }
   }

   protected void unloadEntity(Entity entity) {
      if (entity instanceof ServerPlayerEntity) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
         this.handlePlayerAddedOrRemoved(serverPlayerEntity, false);
         ObjectIterator var3 = this.entityTrackers.values().iterator();

         while(var3.hasNext()) {
            ThreadedAnvilChunkStorage.EntityTracker entityTracker = (ThreadedAnvilChunkStorage.EntityTracker)var3.next();
            entityTracker.stopTracking(serverPlayerEntity);
         }
      }

      ThreadedAnvilChunkStorage.EntityTracker entityTracker2 = (ThreadedAnvilChunkStorage.EntityTracker)this.entityTrackers.remove(entity.getId());
      if (entityTracker2 != null) {
         entityTracker2.stopTracking();
      }

   }

   /**
    * Ticks and updates the tracked status of each tracker.
    * 
    * <p>This first checks if entities have changed chunk sections, and updates
    * tracking status of those entities to all players. It then checks if any player
    * has changed chunk sections, and updates all entities tracking status to those
    * players. This ensures all possible updates are accounted for.
    */
   protected void tickEntityMovement() {
      List<ServerPlayerEntity> list = Lists.newArrayList();
      List<ServerPlayerEntity> list2 = this.world.getPlayers();

      ObjectIterator var3;
      ThreadedAnvilChunkStorage.EntityTracker entityTracker2;
      for(var3 = this.entityTrackers.values().iterator(); var3.hasNext(); entityTracker2.entry.tick()) {
         entityTracker2 = (ThreadedAnvilChunkStorage.EntityTracker)var3.next();
         ChunkSectionPos chunkSectionPos = entityTracker2.trackedSection;
         ChunkSectionPos chunkSectionPos2 = ChunkSectionPos.from(entityTracker2.entity);
         if (!Objects.equals(chunkSectionPos, chunkSectionPos2)) {
            entityTracker2.updateTrackedStatus(list2);
            Entity entity = entityTracker2.entity;
            if (entity instanceof ServerPlayerEntity) {
               list.add((ServerPlayerEntity)entity);
            }

            entityTracker2.trackedSection = chunkSectionPos2;
         }
      }

      if (!list.isEmpty()) {
         var3 = this.entityTrackers.values().iterator();

         while(var3.hasNext()) {
            entityTracker2 = (ThreadedAnvilChunkStorage.EntityTracker)var3.next();
            entityTracker2.updateTrackedStatus((List)list);
         }
      }

   }

   public void sendToOtherNearbyPlayers(Entity entity, Packet<?> packet) {
      ThreadedAnvilChunkStorage.EntityTracker entityTracker = (ThreadedAnvilChunkStorage.EntityTracker)this.entityTrackers.get(entity.getId());
      if (entityTracker != null) {
         entityTracker.sendToOtherNearbyPlayers(packet);
      }

   }

   protected void sendToNearbyPlayers(Entity entity, Packet<?> packet) {
      ThreadedAnvilChunkStorage.EntityTracker entityTracker = (ThreadedAnvilChunkStorage.EntityTracker)this.entityTrackers.get(entity.getId());
      if (entityTracker != null) {
         entityTracker.sendToNearbyPlayers(packet);
      }

   }

   private void sendChunkDataPackets(ServerPlayerEntity player, Packet<?>[] packets, WorldChunk chunk) {
      if (packets[0] == null) {
         packets[0] = new ChunkDataS2CPacket(chunk);
         packets[1] = new LightUpdateS2CPacket(chunk.getPos(), this.serverLightingProvider, (BitSet)null, (BitSet)null, true);
      }

      player.sendInitialChunkPackets(chunk.getPos(), packets[0], packets[1]);
      DebugInfoSender.sendChunkWatchingChange(this.world, chunk.getPos());
      List<Entity> list = Lists.newArrayList();
      List<Entity> list2 = Lists.newArrayList();
      ObjectIterator var6 = this.entityTrackers.values().iterator();

      while(var6.hasNext()) {
         ThreadedAnvilChunkStorage.EntityTracker entityTracker = (ThreadedAnvilChunkStorage.EntityTracker)var6.next();
         Entity entity = entityTracker.entity;
         if (entity != player && entity.getChunkPos().equals(chunk.getPos())) {
            entityTracker.updateTrackedStatus(player);
            if (entity instanceof MobEntity && ((MobEntity)entity).getHoldingEntity() != null) {
               list.add(entity);
            }

            if (!entity.getPassengerList().isEmpty()) {
               list2.add(entity);
            }
         }
      }

      Iterator var9;
      Entity entity3;
      if (!list.isEmpty()) {
         var9 = list.iterator();

         while(var9.hasNext()) {
            entity3 = (Entity)var9.next();
            player.networkHandler.sendPacket(new EntityAttachS2CPacket(entity3, ((MobEntity)entity3).getHoldingEntity()));
         }
      }

      if (!list2.isEmpty()) {
         var9 = list2.iterator();

         while(var9.hasNext()) {
            entity3 = (Entity)var9.next();
            player.networkHandler.sendPacket(new EntityPassengersSetS2CPacket(entity3));
         }
      }

   }

   protected PointOfInterestStorage getPointOfInterestStorage() {
      return this.pointOfInterestStorage;
   }

   public CompletableFuture<Void> enableTickSchedulers(WorldChunk chunk) {
      return this.mainThreadExecutor.submit(() -> {
         chunk.enableTickSchedulers(this.world);
      });
   }

   void method_31414(ChunkPos chunkPos, ChunkHolder.LevelType levelType) {
      this.chunkStatusChangeListener.onChunkStatusChange(chunkPos, levelType);
   }

   private class TicketManager extends ChunkTicketManager {
      protected TicketManager(Executor mainThreadExecutor, Executor executor) {
         super(mainThreadExecutor, executor);
      }

      protected boolean isUnloaded(long pos) {
         return ThreadedAnvilChunkStorage.this.unloadedChunks.contains(pos);
      }

      @Nullable
      protected ChunkHolder getChunkHolder(long pos) {
         return ThreadedAnvilChunkStorage.this.getCurrentChunkHolder(pos);
      }

      @Nullable
      protected ChunkHolder setLevel(long pos, int level, @Nullable ChunkHolder holder, int i) {
         return ThreadedAnvilChunkStorage.this.setLevel(pos, level, holder, i);
      }
   }

   /**
    * An entity tracker governs which players' clients can see an entity. Each
    * tracker corresponds to one entity in a server world and is mapped from the
    * entity's network ID.
    * 
    * @see ThreadedAnvilChunkStorage#entityTrackers
    */
   private class EntityTracker {
      final EntityTrackerEntry entry;
      final Entity entity;
      private final int maxDistance;
      /**
       * The chunk section position of the tracked entity, may be outdated as an entity
       * ticks. This is used by {@link ThreadedAnvilChunkStorage#tickEntityMovement()
       * tickEntityMovement()} to bypass unnecessary status updates before calling
       * {@link #updateTrackedStatus(ServerPlayerEntity) updateTrackedStatus()}.
       */
      ChunkSectionPos trackedSection;
      private final Set<EntityTrackingListener> listeners = Sets.newIdentityHashSet();

      public EntityTracker(Entity entity, int maxDistance, int tickInterval, boolean alwaysUpdateVelocity) {
         this.entry = new EntityTrackerEntry(ThreadedAnvilChunkStorage.this.world, entity, tickInterval, alwaysUpdateVelocity, this::sendToOtherNearbyPlayers);
         this.entity = entity;
         this.maxDistance = maxDistance;
         this.trackedSection = ChunkSectionPos.from(entity);
      }

      public boolean equals(Object o) {
         if (o instanceof ThreadedAnvilChunkStorage.EntityTracker) {
            return ((ThreadedAnvilChunkStorage.EntityTracker)o).entity.getId() == this.entity.getId();
         } else {
            return false;
         }
      }

      public int hashCode() {
         return this.entity.getId();
      }

      public void sendToOtherNearbyPlayers(Packet<?> packet) {
         Iterator var2 = this.listeners.iterator();

         while(var2.hasNext()) {
            EntityTrackingListener entityTrackingListener = (EntityTrackingListener)var2.next();
            entityTrackingListener.sendPacket(packet);
         }

      }

      public void sendToNearbyPlayers(Packet<?> packet) {
         this.sendToOtherNearbyPlayers(packet);
         if (this.entity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity)this.entity).networkHandler.sendPacket(packet);
         }

      }

      public void stopTracking() {
         Iterator var1 = this.listeners.iterator();

         while(var1.hasNext()) {
            EntityTrackingListener entityTrackingListener = (EntityTrackingListener)var1.next();
            this.entry.stopTracking(entityTrackingListener.getPlayer());
         }

      }

      public void stopTracking(ServerPlayerEntity player) {
         if (this.listeners.remove(player.networkHandler)) {
            this.entry.stopTracking(player);
         }

      }

      /**
       * Updates the tracked status of this tracker's entity for the {@code player}.
       * 
       * <p>If this tracker should be listened by the player, the player's tracking
       * listener is added if it is not in the listeners; if this tracker should not be
       * listened by the player, the player's tracking listener is removed if it is in
       * the listeners.
       */
      public void updateTrackedStatus(ServerPlayerEntity player) {
         if (player != this.entity) {
            Vec3d vec3d = player.getPos().subtract(this.entry.getLastPos());
            int i = Math.min(this.getMaxTrackDistance(), (ThreadedAnvilChunkStorage.this.watchDistance - 1) * 16);
            boolean bl = vec3d.x >= (double)(-i) && vec3d.x <= (double)i && vec3d.z >= (double)(-i) && vec3d.z <= (double)i && this.entity.canBeSpectated(player);
            if (bl) {
               if (this.listeners.add(player.networkHandler)) {
                  this.entry.startTracking(player);
               }
            } else if (this.listeners.remove(player.networkHandler)) {
               this.entry.stopTracking(player);
            }

         }
      }

      private int adjustTrackingDistance(int initialDistance) {
         return ThreadedAnvilChunkStorage.this.world.getServer().adjustTrackingDistance(initialDistance);
      }

      private int getMaxTrackDistance() {
         int i = this.maxDistance;
         Iterator var2 = this.entity.getPassengersDeep().iterator();

         while(var2.hasNext()) {
            Entity entity = (Entity)var2.next();
            int j = entity.getType().getMaxTrackDistance() * 16;
            if (j > i) {
               i = j;
            }
         }

         return this.adjustTrackingDistance(i);
      }

      /**
       * Updates the tracked status of this tracker's entity for the given players.
       * 
       * @see updateTrackedStatus(ServerPlayerEntity)
       */
      public void updateTrackedStatus(List<ServerPlayerEntity> players) {
         Iterator var2 = players.iterator();

         while(var2.hasNext()) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var2.next();
            this.updateTrackedStatus(serverPlayerEntity);
         }

      }
   }
}
