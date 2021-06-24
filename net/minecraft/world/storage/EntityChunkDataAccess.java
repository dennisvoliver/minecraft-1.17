package net.minecraft.world.storage;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EntityChunkDataAccess implements ChunkDataAccess<Entity> {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String ENTITIES_KEY = "Entities";
   private static final String POSITION_KEY = "Position";
   private final ServerWorld world;
   private final StorageIoWorker dataLoadWorker;
   private final LongSet emptyChunks = new LongOpenHashSet();
   private final Executor executor;
   protected final DataFixer dataFixer;

   public EntityChunkDataAccess(ServerWorld world, File chunkFile, DataFixer dataFixer, boolean dsync, Executor executor) {
      this.world = world;
      this.dataFixer = dataFixer;
      this.executor = executor;
      this.dataLoadWorker = new StorageIoWorker(chunkFile, dsync, "entities");
   }

   public CompletableFuture<ChunkDataList<Entity>> readChunkData(ChunkPos pos) {
      return this.emptyChunks.contains(pos.toLong()) ? CompletableFuture.completedFuture(emptyDataList(pos)) : this.dataLoadWorker.readChunkData(pos).thenApplyAsync((nbtCompound) -> {
         if (nbtCompound == null) {
            this.emptyChunks.add(pos.toLong());
            return emptyDataList(pos);
         } else {
            try {
               ChunkPos chunkPos2 = getChunkPos(nbtCompound);
               if (!Objects.equals(pos, chunkPos2)) {
                  LOGGER.error((String)"Chunk file at {} is in the wrong location. (Expected {}, got {})", (Object)pos, pos, chunkPos2);
               }
            } catch (Exception var6) {
               LOGGER.warn((String)"Failed to parse chunk {} position info", (Object)pos, (Object)var6);
            }

            NbtCompound nbtCompound2 = this.fixChunkData(nbtCompound);
            NbtList nbtList = nbtCompound2.getList("Entities", 10);
            List<Entity> list = (List)EntityType.streamFromNbt(nbtList, this.world).collect(ImmutableList.toImmutableList());
            return new ChunkDataList(pos, list);
         }
      }, this.executor);
   }

   private static ChunkPos getChunkPos(NbtCompound chunkTag) {
      int[] is = chunkTag.getIntArray("Position");
      return new ChunkPos(is[0], is[1]);
   }

   private static void putChunkPos(NbtCompound chunkTag, ChunkPos pos) {
      chunkTag.put("Position", new NbtIntArray(new int[]{pos.x, pos.z}));
   }

   private static ChunkDataList<Entity> emptyDataList(ChunkPos pos) {
      return new ChunkDataList(pos, ImmutableList.of());
   }

   public void writeChunkData(ChunkDataList<Entity> dataList) {
      ChunkPos chunkPos = dataList.getChunkPos();
      if (dataList.isEmpty()) {
         if (this.emptyChunks.add(chunkPos.toLong())) {
            this.dataLoadWorker.setResult(chunkPos, (NbtCompound)null);
         }

      } else {
         NbtList nbtList = new NbtList();
         dataList.stream().forEach((entity) -> {
            NbtCompound nbtCompound = new NbtCompound();
            if (entity.saveNbt(nbtCompound)) {
               nbtList.add(nbtCompound);
            }

         });
         NbtCompound nbtCompound = new NbtCompound();
         nbtCompound.putInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
         nbtCompound.put("Entities", nbtList);
         putChunkPos(nbtCompound, chunkPos);
         this.dataLoadWorker.setResult(chunkPos, nbtCompound).exceptionally((throwable) -> {
            LOGGER.error((String)"Failed to store chunk {}", (Object)chunkPos, (Object)throwable);
            return null;
         });
         this.emptyChunks.remove(chunkPos.toLong());
      }
   }

   public void awaitAll() {
      this.dataLoadWorker.completeAll().join();
   }

   private NbtCompound fixChunkData(NbtCompound chunkTag) {
      int i = getChunkDataVersion(chunkTag);
      return NbtHelper.update(this.dataFixer, DataFixTypes.ENTITY_CHUNK, chunkTag, i);
   }

   public static int getChunkDataVersion(NbtCompound chunkTag) {
      return chunkTag.contains("DataVersion", 99) ? chunkTag.getInt("DataVersion") : -1;
   }

   public void close() throws IOException {
      this.dataLoadWorker.close();
   }
}
