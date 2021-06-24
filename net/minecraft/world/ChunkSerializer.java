package net.minecraft.world;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import it.unimi.dsi.fastutil.shorts.ShortListIterator;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLongArray;
import net.minecraft.nbt.NbtShort;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.SimpleTickScheduler;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkManager;
import net.minecraft.world.chunk.ChunkNibbleArray;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.ReadOnlyChunk;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.poi.PointOfInterestStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class ChunkSerializer {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final String field_31413 = "UpgradeData";

   public static ProtoChunk deserialize(ServerWorld world, StructureManager structureManager, PointOfInterestStorage poiStorage, ChunkPos pos, NbtCompound nbt) {
      ChunkGenerator chunkGenerator = world.getChunkManager().getChunkGenerator();
      BiomeSource biomeSource = chunkGenerator.getBiomeSource();
      NbtCompound nbtCompound = nbt.getCompound("Level");
      ChunkPos chunkPos = new ChunkPos(nbtCompound.getInt("xPos"), nbtCompound.getInt("zPos"));
      if (!Objects.equals(pos, chunkPos)) {
         LOGGER.error((String)"Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", (Object)pos, pos, chunkPos);
      }

      BiomeArray biomeArray = new BiomeArray(world.getRegistryManager().get(Registry.BIOME_KEY), world, pos, biomeSource, nbtCompound.contains("Biomes", 11) ? nbtCompound.getIntArray("Biomes") : null);
      UpgradeData upgradeData = nbtCompound.contains("UpgradeData", 10) ? new UpgradeData(nbtCompound.getCompound("UpgradeData"), world) : UpgradeData.NO_UPGRADE_DATA;
      ChunkTickScheduler<Block> chunkTickScheduler = new ChunkTickScheduler((block) -> {
         return block == null || block.getDefaultState().isAir();
      }, pos, nbtCompound.getList("ToBeTicked", 9), world);
      ChunkTickScheduler<Fluid> chunkTickScheduler2 = new ChunkTickScheduler((fluid) -> {
         return fluid == null || fluid == Fluids.EMPTY;
      }, pos, nbtCompound.getList("LiquidsToBeTicked", 9), world);
      boolean bl = nbtCompound.getBoolean("isLightOn");
      NbtList nbtList = nbtCompound.getList("Sections", 10);
      int i = world.countVerticalSections();
      ChunkSection[] chunkSections = new ChunkSection[i];
      boolean bl2 = world.getDimension().hasSkyLight();
      ChunkManager chunkManager = world.getChunkManager();
      LightingProvider lightingProvider = chunkManager.getLightingProvider();
      if (bl) {
         lightingProvider.setRetainData(pos, true);
      }

      for(int j = 0; j < nbtList.size(); ++j) {
         NbtCompound nbtCompound2 = nbtList.getCompound(j);
         int k = nbtCompound2.getByte("Y");
         if (nbtCompound2.contains("Palette", 9) && nbtCompound2.contains("BlockStates", 12)) {
            ChunkSection chunkSection = new ChunkSection(k);
            chunkSection.getContainer().read(nbtCompound2.getList("Palette", 10), nbtCompound2.getLongArray("BlockStates"));
            chunkSection.calculateCounts();
            if (!chunkSection.isEmpty()) {
               chunkSections[world.sectionCoordToIndex(k)] = chunkSection;
            }

            poiStorage.initForPalette(pos, chunkSection);
         }

         if (bl) {
            if (nbtCompound2.contains("BlockLight", 7)) {
               lightingProvider.enqueueSectionData(LightType.BLOCK, ChunkSectionPos.from(pos, k), new ChunkNibbleArray(nbtCompound2.getByteArray("BlockLight")), true);
            }

            if (bl2 && nbtCompound2.contains("SkyLight", 7)) {
               lightingProvider.enqueueSectionData(LightType.SKY, ChunkSectionPos.from(pos, k), new ChunkNibbleArray(nbtCompound2.getByteArray("SkyLight")), true);
            }
         }
      }

      long l = nbtCompound.getLong("InhabitedTime");
      ChunkStatus.ChunkType chunkType = getChunkType(nbt);
      Object chunk2;
      if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
         NbtList var10000;
         DefaultedRegistry var10001;
         DefaultedRegistry var10002;
         Object tickScheduler2;
         Function var36;
         if (nbtCompound.contains("TileTicks", 9)) {
            var10000 = nbtCompound.getList("TileTicks", 10);
            var10001 = Registry.BLOCK;
            Objects.requireNonNull(var10001);
            var36 = var10001::getId;
            var10002 = Registry.BLOCK;
            Objects.requireNonNull(var10002);
            tickScheduler2 = SimpleTickScheduler.fromNbt(var10000, var36, var10002::get);
         } else {
            tickScheduler2 = chunkTickScheduler;
         }

         Object tickScheduler4;
         if (nbtCompound.contains("LiquidTicks", 9)) {
            var10000 = nbtCompound.getList("LiquidTicks", 10);
            var10001 = Registry.FLUID;
            Objects.requireNonNull(var10001);
            var36 = var10001::getId;
            var10002 = Registry.FLUID;
            Objects.requireNonNull(var10002);
            tickScheduler4 = SimpleTickScheduler.fromNbt(var10000, var36, var10002::get);
         } else {
            tickScheduler4 = chunkTickScheduler2;
         }

         chunk2 = new WorldChunk(world.toServerWorld(), pos, biomeArray, upgradeData, (TickScheduler)tickScheduler2, (TickScheduler)tickScheduler4, l, chunkSections, (worldChunk) -> {
            loadEntities(world, nbtCompound, worldChunk);
         });
      } else {
         ProtoChunk protoChunk = new ProtoChunk(pos, upgradeData, chunkSections, chunkTickScheduler, chunkTickScheduler2, world);
         protoChunk.setBiomes(biomeArray);
         chunk2 = protoChunk;
         protoChunk.setInhabitedTime(l);
         protoChunk.setStatus(ChunkStatus.byId(nbtCompound.getString("Status")));
         if (protoChunk.getStatus().isAtLeast(ChunkStatus.FEATURES)) {
            protoChunk.setLightingProvider(lightingProvider);
         }

         if (!bl && protoChunk.getStatus().isAtLeast(ChunkStatus.LIGHT)) {
            Iterator var42 = BlockPos.iterate(pos.getStartX(), world.getBottomY(), pos.getStartZ(), pos.getEndX(), world.getTopY() - 1, pos.getEndZ()).iterator();

            while(var42.hasNext()) {
               BlockPos blockPos = (BlockPos)var42.next();
               if (((Chunk)chunk2).getBlockState(blockPos).getLuminance() != 0) {
                  protoChunk.addLightSource(blockPos);
               }
            }
         }
      }

      ((Chunk)chunk2).setLightOn(bl);
      NbtCompound nbtCompound3 = nbtCompound.getCompound("Heightmaps");
      EnumSet<Heightmap.Type> enumSet = EnumSet.noneOf(Heightmap.Type.class);
      Iterator var44 = ((Chunk)chunk2).getStatus().getHeightmapTypes().iterator();

      while(var44.hasNext()) {
         Heightmap.Type type = (Heightmap.Type)var44.next();
         String string = type.getName();
         if (nbtCompound3.contains(string, 12)) {
            ((Chunk)chunk2).setHeightmap(type, nbtCompound3.getLongArray(string));
         } else {
            enumSet.add(type);
         }
      }

      Heightmap.populateHeightmaps((Chunk)chunk2, enumSet);
      NbtCompound nbtCompound4 = nbtCompound.getCompound("Structures");
      ((Chunk)chunk2).setStructureStarts(readStructureStarts(world, nbtCompound4, world.getSeed()));
      ((Chunk)chunk2).setStructureReferences(readStructureReferences(pos, nbtCompound4));
      if (nbtCompound.getBoolean("shouldSave")) {
         ((Chunk)chunk2).setShouldSave(true);
      }

      NbtList nbtList2 = nbtCompound.getList("PostProcessing", 9);

      NbtList nbtList4;
      int o;
      for(int m = 0; m < nbtList2.size(); ++m) {
         nbtList4 = nbtList2.getList(m);

         for(o = 0; o < nbtList4.size(); ++o) {
            ((Chunk)chunk2).markBlockForPostProcessing(nbtList4.getShort(o), m);
         }
      }

      if (chunkType == ChunkStatus.ChunkType.LEVELCHUNK) {
         return new ReadOnlyChunk((WorldChunk)chunk2);
      } else {
         ProtoChunk protoChunk2 = (ProtoChunk)chunk2;
         nbtList4 = nbtCompound.getList("Entities", 10);

         for(o = 0; o < nbtList4.size(); ++o) {
            protoChunk2.addEntity(nbtList4.getCompound(o));
         }

         NbtList nbtList5 = nbtCompound.getList("TileEntities", 10);

         NbtCompound nbtCompound6;
         for(int p = 0; p < nbtList5.size(); ++p) {
            nbtCompound6 = nbtList5.getCompound(p);
            ((Chunk)chunk2).addPendingBlockEntityNbt(nbtCompound6);
         }

         NbtList nbtList6 = nbtCompound.getList("Lights", 9);

         for(int q = 0; q < nbtList6.size(); ++q) {
            NbtList nbtList7 = nbtList6.getList(q);

            for(int r = 0; r < nbtList7.size(); ++r) {
               protoChunk2.addLightSource(nbtList7.getShort(r), q);
            }
         }

         nbtCompound6 = nbtCompound.getCompound("CarvingMasks");
         Iterator var52 = nbtCompound6.getKeys().iterator();

         while(var52.hasNext()) {
            String string2 = (String)var52.next();
            GenerationStep.Carver carver = GenerationStep.Carver.valueOf(string2);
            protoChunk2.setCarvingMask(carver, BitSet.valueOf(nbtCompound6.getByteArray(string2)));
         }

         return protoChunk2;
      }
   }

   public static NbtCompound serialize(ServerWorld world, Chunk chunk) {
      ChunkPos chunkPos = chunk.getPos();
      NbtCompound nbtCompound = new NbtCompound();
      NbtCompound nbtCompound2 = new NbtCompound();
      nbtCompound.putInt("DataVersion", SharedConstants.getGameVersion().getWorldVersion());
      nbtCompound.put("Level", nbtCompound2);
      nbtCompound2.putInt("xPos", chunkPos.x);
      nbtCompound2.putInt("zPos", chunkPos.z);
      nbtCompound2.putLong("LastUpdate", world.getTime());
      nbtCompound2.putLong("InhabitedTime", chunk.getInhabitedTime());
      nbtCompound2.putString("Status", chunk.getStatus().getId());
      UpgradeData upgradeData = chunk.getUpgradeData();
      if (!upgradeData.isDone()) {
         nbtCompound2.put("UpgradeData", upgradeData.toNbt());
      }

      ChunkSection[] chunkSections = chunk.getSectionArray();
      NbtList nbtList = new NbtList();
      LightingProvider lightingProvider = world.getChunkManager().getLightingProvider();
      boolean bl = chunk.isLightOn();

      for(int i = lightingProvider.getBottomY(); i < lightingProvider.getTopY(); ++i) {
         ChunkSection chunkSection = (ChunkSection)Arrays.stream(chunkSections).filter((chunkSectionx) -> {
            return chunkSectionx != null && ChunkSectionPos.getSectionCoord(chunkSectionx.getYOffset()) == i;
         }).findFirst().orElse(WorldChunk.EMPTY_SECTION);
         ChunkNibbleArray chunkNibbleArray = lightingProvider.get(LightType.BLOCK).getLightSection(ChunkSectionPos.from(chunkPos, i));
         ChunkNibbleArray chunkNibbleArray2 = lightingProvider.get(LightType.SKY).getLightSection(ChunkSectionPos.from(chunkPos, i));
         if (chunkSection != WorldChunk.EMPTY_SECTION || chunkNibbleArray != null || chunkNibbleArray2 != null) {
            NbtCompound nbtCompound3 = new NbtCompound();
            nbtCompound3.putByte("Y", (byte)(i & 255));
            if (chunkSection != WorldChunk.EMPTY_SECTION) {
               chunkSection.getContainer().write(nbtCompound3, "Palette", "BlockStates");
            }

            if (chunkNibbleArray != null && !chunkNibbleArray.isUninitialized()) {
               nbtCompound3.putByteArray("BlockLight", chunkNibbleArray.asByteArray());
            }

            if (chunkNibbleArray2 != null && !chunkNibbleArray2.isUninitialized()) {
               nbtCompound3.putByteArray("SkyLight", chunkNibbleArray2.asByteArray());
            }

            nbtList.add(nbtCompound3);
         }
      }

      nbtCompound2.put("Sections", nbtList);
      if (bl) {
         nbtCompound2.putBoolean("isLightOn", true);
      }

      BiomeArray biomeArray = chunk.getBiomeArray();
      if (biomeArray != null) {
         nbtCompound2.putIntArray("Biomes", biomeArray.toIntArray());
      }

      NbtList nbtList2 = new NbtList();
      Iterator var21 = chunk.getBlockEntityPositions().iterator();

      NbtCompound nbtCompound6;
      while(var21.hasNext()) {
         BlockPos blockPos = (BlockPos)var21.next();
         nbtCompound6 = chunk.getPackedBlockEntityNbt(blockPos);
         if (nbtCompound6 != null) {
            nbtList2.add(nbtCompound6);
         }
      }

      nbtCompound2.put("TileEntities", nbtList2);
      if (chunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
         ProtoChunk protoChunk = (ProtoChunk)chunk;
         NbtList nbtList3 = new NbtList();
         nbtList3.addAll(protoChunk.getEntities());
         nbtCompound2.put("Entities", nbtList3);
         nbtCompound2.put("Lights", toNbt(protoChunk.getLightSourcesBySection()));
         nbtCompound6 = new NbtCompound();
         GenerationStep.Carver[] var28 = GenerationStep.Carver.values();
         int var16 = var28.length;

         for(int var17 = 0; var17 < var16; ++var17) {
            GenerationStep.Carver carver = var28[var17];
            BitSet bitSet = protoChunk.getCarvingMask(carver);
            if (bitSet != null) {
               nbtCompound6.putByteArray(carver.toString(), bitSet.toByteArray());
            }
         }

         nbtCompound2.put("CarvingMasks", nbtCompound6);
      }

      TickScheduler<Block> tickScheduler = chunk.getBlockTickScheduler();
      if (tickScheduler instanceof ChunkTickScheduler) {
         nbtCompound2.put("ToBeTicked", ((ChunkTickScheduler)tickScheduler).toNbt());
      } else if (tickScheduler instanceof SimpleTickScheduler) {
         nbtCompound2.put("TileTicks", ((SimpleTickScheduler)tickScheduler).toNbt());
      } else {
         nbtCompound2.put("TileTicks", world.getBlockTickScheduler().toNbt(chunkPos));
      }

      TickScheduler<Fluid> tickScheduler2 = chunk.getFluidTickScheduler();
      if (tickScheduler2 instanceof ChunkTickScheduler) {
         nbtCompound2.put("LiquidsToBeTicked", ((ChunkTickScheduler)tickScheduler2).toNbt());
      } else if (tickScheduler2 instanceof SimpleTickScheduler) {
         nbtCompound2.put("LiquidTicks", ((SimpleTickScheduler)tickScheduler2).toNbt());
      } else {
         nbtCompound2.put("LiquidTicks", world.getFluidTickScheduler().toNbt(chunkPos));
      }

      nbtCompound2.put("PostProcessing", toNbt(chunk.getPostProcessingLists()));
      nbtCompound6 = new NbtCompound();
      Iterator var29 = chunk.getHeightmaps().iterator();

      while(var29.hasNext()) {
         Entry<Heightmap.Type, Heightmap> entry = (Entry)var29.next();
         if (chunk.getStatus().getHeightmapTypes().contains(entry.getKey())) {
            nbtCompound6.put(((Heightmap.Type)entry.getKey()).getName(), new NbtLongArray(((Heightmap)entry.getValue()).asLongArray()));
         }
      }

      nbtCompound2.put("Heightmaps", nbtCompound6);
      nbtCompound2.put("Structures", writeStructures(world, chunkPos, chunk.getStructureStarts(), chunk.getStructureReferences()));
      return nbtCompound;
   }

   public static ChunkStatus.ChunkType getChunkType(@Nullable NbtCompound nbt) {
      if (nbt != null) {
         ChunkStatus chunkStatus = ChunkStatus.byId(nbt.getCompound("Level").getString("Status"));
         if (chunkStatus != null) {
            return chunkStatus.getChunkType();
         }
      }

      return ChunkStatus.ChunkType.PROTOCHUNK;
   }

   private static void loadEntities(ServerWorld world, NbtCompound nbt, WorldChunk chunk) {
      NbtList nbtList2;
      if (nbt.contains("Entities", 9)) {
         nbtList2 = nbt.getList("Entities", 10);
         if (!nbtList2.isEmpty()) {
            world.loadEntities(EntityType.streamFromNbt(nbtList2, world));
         }
      }

      nbtList2 = nbt.getList("TileEntities", 10);

      for(int i = 0; i < nbtList2.size(); ++i) {
         NbtCompound nbtCompound = nbtList2.getCompound(i);
         boolean bl = nbtCompound.getBoolean("keepPacked");
         if (bl) {
            chunk.addPendingBlockEntityNbt(nbtCompound);
         } else {
            BlockPos blockPos = new BlockPos(nbtCompound.getInt("x"), nbtCompound.getInt("y"), nbtCompound.getInt("z"));
            BlockEntity blockEntity = BlockEntity.createFromNbt(blockPos, chunk.getBlockState(blockPos), nbtCompound);
            if (blockEntity != null) {
               chunk.setBlockEntity(blockEntity);
            }
         }
      }

   }

   private static NbtCompound writeStructures(ServerWorld world, ChunkPos chunkPos, Map<StructureFeature<?>, StructureStart<?>> map, Map<StructureFeature<?>, LongSet> map2) {
      NbtCompound nbtCompound = new NbtCompound();
      NbtCompound nbtCompound2 = new NbtCompound();
      Iterator var6 = map.entrySet().iterator();

      while(var6.hasNext()) {
         Entry<StructureFeature<?>, StructureStart<?>> entry = (Entry)var6.next();
         nbtCompound2.put(((StructureFeature)entry.getKey()).getName(), ((StructureStart)entry.getValue()).toNbt(world, chunkPos));
      }

      nbtCompound.put("Starts", nbtCompound2);
      NbtCompound nbtCompound3 = new NbtCompound();
      Iterator var10 = map2.entrySet().iterator();

      while(var10.hasNext()) {
         Entry<StructureFeature<?>, LongSet> entry2 = (Entry)var10.next();
         nbtCompound3.put(((StructureFeature)entry2.getKey()).getName(), new NbtLongArray((LongSet)entry2.getValue()));
      }

      nbtCompound.put("References", nbtCompound3);
      return nbtCompound;
   }

   private static Map<StructureFeature<?>, StructureStart<?>> readStructureStarts(ServerWorld serverWorld, NbtCompound nbt, long worldSeed) {
      Map<StructureFeature<?>, StructureStart<?>> map = Maps.newHashMap();
      NbtCompound nbtCompound = nbt.getCompound("Starts");
      Iterator var6 = nbtCompound.getKeys().iterator();

      while(var6.hasNext()) {
         String string = (String)var6.next();
         String string2 = string.toLowerCase(Locale.ROOT);
         StructureFeature<?> structureFeature = (StructureFeature)StructureFeature.STRUCTURES.get(string2);
         if (structureFeature == null) {
            LOGGER.error((String)"Unknown structure start: {}", (Object)string2);
         } else {
            StructureStart<?> structureStart = StructureFeature.readStructureStart(serverWorld, nbtCompound.getCompound(string), worldSeed);
            if (structureStart != null) {
               map.put(structureFeature, structureStart);
            }
         }
      }

      return map;
   }

   private static Map<StructureFeature<?>, LongSet> readStructureReferences(ChunkPos pos, NbtCompound nbt) {
      Map<StructureFeature<?>, LongSet> map = Maps.newHashMap();
      NbtCompound nbtCompound = nbt.getCompound("References");
      Iterator var4 = nbtCompound.getKeys().iterator();

      while(var4.hasNext()) {
         String string = (String)var4.next();
         map.put((StructureFeature)StructureFeature.STRUCTURES.get(string.toLowerCase(Locale.ROOT)), new LongOpenHashSet(Arrays.stream(nbtCompound.getLongArray(string)).filter((packedPos) -> {
            ChunkPos chunkPos2 = new ChunkPos(packedPos);
            if (chunkPos2.getChebyshevDistance(pos) > 8) {
               LOGGER.warn((String)"Found invalid structure reference [ {} @ {} ] for chunk {}.", (Object)string, chunkPos2, pos);
               return false;
            } else {
               return true;
            }
         }).toArray()));
      }

      return map;
   }

   public static NbtList toNbt(ShortList[] lists) {
      NbtList nbtList = new NbtList();
      ShortList[] var2 = lists;
      int var3 = lists.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         ShortList shortList = var2[var4];
         NbtList nbtList2 = new NbtList();
         if (shortList != null) {
            ShortListIterator var7 = shortList.iterator();

            while(var7.hasNext()) {
               Short short_ = (Short)var7.next();
               nbtList2.add(NbtShort.of(short_));
            }
         }

         nbtList.add(nbtList2);
      }

      return nbtList;
   }
}
