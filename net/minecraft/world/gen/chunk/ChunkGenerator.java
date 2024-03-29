package net.minecraft.world.gen.chunk;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeArray;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.BlockSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.DefaultBlockSource;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.feature.ConfiguredStructureFeature;
import net.minecraft.world.gen.feature.ConfiguredStructureFeatures;
import net.minecraft.world.gen.feature.StructureFeature;
import org.jetbrains.annotations.Nullable;

/**
 * In charge of shaping, adding biome specific surface blocks, and carving chunks,
 * as well as populating the generated chunks with {@linkplain net.minecraft.world.gen.feature.Feature features} and {@linkplain net.minecraft.entity.Entity entities}.
 * Biome placement starts here, however all vanilla and most modded chunk generators delegate this to a {@linkplain net.minecraft.world.biome.source.BiomeSource biome source}.
 */
public abstract class ChunkGenerator {
   public static final Codec<ChunkGenerator> CODEC;
   /**
    * Used to control the population step without replacing the actual biome that comes from the original {@link #biomeSource}.
    * 
    * <p>This is used by {@link FlatChunkGenerator} to overwrite biome properties like whether lakes generate, while preserving the original biome ID.
    */
   protected final BiomeSource populationSource;
   protected final BiomeSource biomeSource;
   private final StructuresConfig structuresConfig;
   private final long worldSeed;
   private final List<ChunkPos> strongholds;
   private final BlockSource blockSource;

   public ChunkGenerator(BiomeSource biomeSource, StructuresConfig structuresConfig) {
      this(biomeSource, biomeSource, structuresConfig, 0L);
   }

   public ChunkGenerator(BiomeSource populationSource, BiomeSource biomeSource, StructuresConfig structuresConfig, long worldSeed) {
      this.strongholds = Lists.newArrayList();
      this.populationSource = populationSource;
      this.biomeSource = biomeSource;
      this.structuresConfig = structuresConfig;
      this.worldSeed = worldSeed;
      this.blockSource = new DefaultBlockSource(Blocks.STONE.getDefaultState());
   }

   private void generateStrongholdPositions() {
      if (this.strongholds.isEmpty()) {
         StrongholdConfig strongholdConfig = this.structuresConfig.getStronghold();
         if (strongholdConfig != null && strongholdConfig.getCount() != 0) {
            List<Biome> list = Lists.newArrayList();
            Iterator var3 = this.populationSource.getBiomes().iterator();

            while(var3.hasNext()) {
               Biome biome = (Biome)var3.next();
               if (biome.getGenerationSettings().hasStructureFeature(StructureFeature.STRONGHOLD)) {
                  list.add(biome);
               }
            }

            int i = strongholdConfig.getDistance();
            int j = strongholdConfig.getCount();
            int k = strongholdConfig.getSpread();
            Random random = new Random();
            random.setSeed(this.worldSeed);
            double d = random.nextDouble() * 3.141592653589793D * 2.0D;
            int l = 0;
            int m = 0;

            for(int n = 0; n < j; ++n) {
               double e = (double)(4 * i + i * m * 6) + (random.nextDouble() - 0.5D) * (double)i * 2.5D;
               int o = (int)Math.round(Math.cos(d) * e);
               int p = (int)Math.round(Math.sin(d) * e);
               BiomeSource var10000 = this.populationSource;
               int var10001 = ChunkSectionPos.getOffsetPos(o, 8);
               int var10003 = ChunkSectionPos.getOffsetPos(p, 8);
               Objects.requireNonNull(list);
               BlockPos blockPos = var10000.locateBiome(var10001, 0, var10003, 112, list::contains, random);
               if (blockPos != null) {
                  o = ChunkSectionPos.getSectionCoord(blockPos.getX());
                  p = ChunkSectionPos.getSectionCoord(blockPos.getZ());
               }

               this.strongholds.add(new ChunkPos(o, p));
               d += 6.283185307179586D / (double)k;
               ++l;
               if (l == k) {
                  ++m;
                  l = 0;
                  k += 2 * k / (m + 1);
                  k = Math.min(k, j - n);
                  d += random.nextDouble() * 3.141592653589793D * 2.0D;
               }
            }

         }
      }
   }

   protected abstract Codec<? extends ChunkGenerator> getCodec();

   public abstract ChunkGenerator withSeed(long seed);

   public void populateBiomes(Registry<Biome> biomeRegistry, Chunk chunk) {
      ChunkPos chunkPos = chunk.getPos();
      ((ProtoChunk)chunk).setBiomes(new BiomeArray(biomeRegistry, chunk, chunkPos, this.biomeSource));
   }

   /**
    * Generates caves for the given chunk.
    */
   public void carve(long seed, BiomeAccess access, Chunk chunk, GenerationStep.Carver carver) {
      BiomeAccess biomeAccess = access.withSource(this.populationSource);
      ChunkRandom chunkRandom = new ChunkRandom();
      int i = true;
      ChunkPos chunkPos = chunk.getPos();
      CarverContext carverContext = new CarverContext(this);
      AquiferSampler aquiferSampler = this.createAquiferSampler(chunk);
      BitSet bitSet = ((ProtoChunk)chunk).getOrCreateCarvingMask(carver);

      for(int j = -8; j <= 8; ++j) {
         for(int k = -8; k <= 8; ++k) {
            ChunkPos chunkPos2 = new ChunkPos(chunkPos.x + j, chunkPos.z + k);
            GenerationSettings generationSettings = this.populationSource.getBiomeForNoiseGen(BiomeCoords.fromBlock(chunkPos2.getStartX()), 0, BiomeCoords.fromBlock(chunkPos2.getStartZ())).getGenerationSettings();
            List<Supplier<ConfiguredCarver<?>>> list = generationSettings.getCarversForStep(carver);
            ListIterator listIterator = list.listIterator();

            while(listIterator.hasNext()) {
               int l = listIterator.nextIndex();
               ConfiguredCarver<?> configuredCarver = (ConfiguredCarver)((Supplier)listIterator.next()).get();
               chunkRandom.setCarverSeed(seed + (long)l, chunkPos2.x, chunkPos2.z);
               if (configuredCarver.shouldCarve(chunkRandom)) {
                  Objects.requireNonNull(biomeAccess);
                  configuredCarver.carve(carverContext, chunk, biomeAccess::getBiome, chunkRandom, aquiferSampler, chunkPos2, bitSet);
               }
            }
         }
      }

   }

   protected AquiferSampler createAquiferSampler(Chunk chunk) {
      return AquiferSampler.seaLevel(this.getSeaLevel(), Blocks.WATER.getDefaultState());
   }

   /**
    * Tries to find the closest structure of a given type near a given block.
    * <p>
    * New chunks will only be generated up to the {@link net.minecraft.world.chunk.ChunkStatus#STRUCTURE_STARTS} phase by this method.
    * <p>
    * The radius is ignored for strongholds.
    * 
    * @return {@code null} if no structure could be found within the given search radius
    * 
    * @param radius the search radius in chunks around the chunk the given block position is in; a radius of 0 will only search in the given chunk
    * @param skipExistingChunks whether only structures that are not referenced by generated chunks (chunks past the STRUCTURE_STARTS stage) are returned, excluding strongholds
    */
   @Nullable
   public BlockPos locateStructure(ServerWorld world, StructureFeature<?> feature, BlockPos center, int radius, boolean skipExistingChunks) {
      if (!this.populationSource.hasStructureFeature(feature)) {
         return null;
      } else if (feature == StructureFeature.STRONGHOLD) {
         this.generateStrongholdPositions();
         BlockPos blockPos = null;
         double d = Double.MAX_VALUE;
         BlockPos.Mutable mutable = new BlockPos.Mutable();
         Iterator var10 = this.strongholds.iterator();

         while(var10.hasNext()) {
            ChunkPos chunkPos = (ChunkPos)var10.next();
            mutable.set(ChunkSectionPos.getOffsetPos(chunkPos.x, 8), 32, ChunkSectionPos.getOffsetPos(chunkPos.z, 8));
            double e = mutable.getSquaredDistance(center);
            if (blockPos == null) {
               blockPos = new BlockPos(mutable);
               d = e;
            } else if (e < d) {
               blockPos = new BlockPos(mutable);
               d = e;
            }
         }

         return blockPos;
      } else {
         StructureConfig structureConfig = this.structuresConfig.getForType(feature);
         return structureConfig == null ? null : feature.locateStructure(world, world.getStructureAccessor(), center, radius, skipExistingChunks, world.getSeed(), structureConfig);
      }
   }

   public void generateFeatures(ChunkRegion region, StructureAccessor accessor) {
      ChunkPos chunkPos = region.getCenterPos();
      int i = chunkPos.getStartX();
      int j = chunkPos.getStartZ();
      BlockPos blockPos = new BlockPos(i, region.getBottomY(), j);
      Biome biome = this.populationSource.getBiomeForNoiseGen(chunkPos);
      ChunkRandom chunkRandom = new ChunkRandom();
      long l = chunkRandom.setPopulationSeed(region.getSeed(), i, j);

      try {
         biome.generateFeatureStep(accessor, this, region, l, chunkRandom, blockPos);
      } catch (Exception var13) {
         CrashReport crashReport = CrashReport.create(var13, "Biome decoration");
         crashReport.addElement("Generation").add("CenterX", (Object)chunkPos.x).add("CenterZ", (Object)chunkPos.z).add("Seed", (Object)l).add("Biome", (Object)biome);
         throw new CrashException(crashReport);
      }
   }

   /**
    * Places the surface blocks of the biomes after the noise has been generated.
    */
   public abstract void buildSurface(ChunkRegion region, Chunk chunk);

   public void populateEntities(ChunkRegion region) {
   }

   public StructuresConfig getStructuresConfig() {
      return this.structuresConfig;
   }

   public int getSpawnHeight(HeightLimitView world) {
      return 64;
   }

   public BiomeSource getBiomeSource() {
      return this.biomeSource;
   }

   public int getWorldHeight() {
      return 256;
   }

   public Pool<SpawnSettings.SpawnEntry> getEntitySpawnList(Biome biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos) {
      return biome.getSpawnSettings().getSpawnEntries(group);
   }

   /**
    * Determines which structures should start in the given chunk and creates their starting points.
    */
   public void setStructureStarts(DynamicRegistryManager registryManager, StructureAccessor accessor, Chunk chunk, StructureManager structureManager, long worldSeed) {
      Biome biome = this.populationSource.getBiomeForNoiseGen(chunk.getPos());
      this.setStructureStart(ConfiguredStructureFeatures.STRONGHOLD, registryManager, accessor, chunk, structureManager, worldSeed, biome);
      Iterator var8 = biome.getGenerationSettings().getStructureFeatures().iterator();

      while(var8.hasNext()) {
         Supplier<ConfiguredStructureFeature<?, ?>> supplier = (Supplier)var8.next();
         this.setStructureStart((ConfiguredStructureFeature)supplier.get(), registryManager, accessor, chunk, structureManager, worldSeed, biome);
      }

   }

   private void setStructureStart(ConfiguredStructureFeature<?, ?> feature, DynamicRegistryManager registryManager, StructureAccessor accessor, Chunk chunk, StructureManager structureManager, long worldSeed, Biome biome) {
      ChunkPos chunkPos = chunk.getPos();
      ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunk);
      StructureStart<?> structureStart = accessor.getStructureStart(chunkSectionPos, feature.feature, chunk);
      int i = structureStart != null ? structureStart.getReferences() : 0;
      StructureConfig structureConfig = this.structuresConfig.getForType(feature.feature);
      if (structureConfig != null) {
         StructureStart<?> structureStart2 = feature.tryPlaceStart(registryManager, this, this.populationSource, structureManager, worldSeed, chunkPos, biome, i, structureConfig, chunk);
         accessor.setStructureStart(chunkSectionPos, feature.feature, structureStart2, chunk);
      }

   }

   /**
    * Finds all structures that the given chunk intersects, and adds references to their starting chunks to it.
    * A radius of 8 chunks around the given chunk will be searched for structure starts.
    */
   public void addStructureReferences(StructureWorldAccess world, StructureAccessor accessor, Chunk chunk) {
      int i = true;
      ChunkPos chunkPos = chunk.getPos();
      int j = chunkPos.x;
      int k = chunkPos.z;
      int l = chunkPos.getStartX();
      int m = chunkPos.getStartZ();
      ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunk);

      for(int n = j - 8; n <= j + 8; ++n) {
         for(int o = k - 8; o <= k + 8; ++o) {
            long p = ChunkPos.toLong(n, o);
            Iterator var15 = world.getChunk(n, o).getStructureStarts().values().iterator();

            while(var15.hasNext()) {
               StructureStart structureStart = (StructureStart)var15.next();

               try {
                  if (structureStart.hasChildren() && structureStart.setBoundingBoxFromChildren().intersectsXZ(l, m, l + 15, m + 15)) {
                     accessor.addStructureReference(chunkSectionPos, structureStart.getFeature(), p, chunk);
                     DebugInfoSender.sendStructureStart(world, structureStart);
                  }
               } catch (Exception var20) {
                  CrashReport crashReport = CrashReport.create(var20, "Generating structure reference");
                  CrashReportSection crashReportSection = crashReport.addElement("Structure");
                  crashReportSection.add("Id", () -> {
                     return Registry.STRUCTURE_FEATURE.getId(structureStart.getFeature()).toString();
                  });
                  crashReportSection.add("Name", () -> {
                     return structureStart.getFeature().getName();
                  });
                  crashReportSection.add("Class", () -> {
                     return structureStart.getFeature().getClass().getCanonicalName();
                  });
                  throw new CrashException(crashReport);
               }
            }
         }
      }

   }

   /**
    * Generates the base shape of the chunk out of the basic block states as decided by this chunk generator's config.
    */
   public abstract CompletableFuture<Chunk> populateNoise(Executor executor, StructureAccessor accessor, Chunk chunk);

   public int getSeaLevel() {
      return 63;
   }

   public int getMinimumY() {
      return 0;
   }

   /**
    * Returns the raw noise height of a column for use in structure generation.
    */
   public abstract int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world);

   /**
    * Returns a sample of all the block states in a column for use in structure generation.
    */
   public abstract VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world);

   public int getHeightOnGround(int x, int z, Heightmap.Type heightmap, HeightLimitView world) {
      return this.getHeight(x, z, heightmap, world);
   }

   public int getHeightInGround(int x, int z, Heightmap.Type heightmap, HeightLimitView world) {
      return this.getHeight(x, z, heightmap, world) - 1;
   }

   public boolean isStrongholdStartingChunk(ChunkPos pos) {
      this.generateStrongholdPositions();
      return this.strongholds.contains(pos);
   }

   public BlockSource getBlockSource() {
      return this.blockSource;
   }

   static {
      Registry.register(Registry.CHUNK_GENERATOR, (String)"noise", NoiseChunkGenerator.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, (String)"flat", FlatChunkGenerator.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, (String)"debug", DebugChunkGenerator.CODEC);
      CODEC = Registry.CHUNK_GENERATOR.dispatchStable(ChunkGenerator::getCodec, Function.identity());
   }
}
