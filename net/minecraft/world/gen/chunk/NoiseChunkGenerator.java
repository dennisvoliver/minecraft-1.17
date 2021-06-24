package net.minecraft.world.gen.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.util.Util;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.noise.DoublePerlinNoiseSampler;
import net.minecraft.util.math.noise.InterpolatedNoiseSampler;
import net.minecraft.util.math.noise.NoiseSampler;
import net.minecraft.util.math.noise.OctavePerlinNoiseSampler;
import net.minecraft.util.math.noise.OctaveSimplexNoiseSampler;
import net.minecraft.util.math.noise.SimplexNoiseSampler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.gen.BlockSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.DeepslateBlockSource;
import net.minecraft.world.gen.NoiseCaveSampler;
import net.minecraft.world.gen.NoiseColumnSampler;
import net.minecraft.world.gen.NoiseInterpolator;
import net.minecraft.world.gen.OreVeinGenerator;
import net.minecraft.world.gen.SimpleRandom;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.feature.StructureFeature;
import org.jetbrains.annotations.Nullable;

public final class NoiseChunkGenerator extends ChunkGenerator {
   public static final Codec<NoiseChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter((noiseChunkGenerator) -> {
         return noiseChunkGenerator.populationSource;
      }), Codec.LONG.fieldOf("seed").stable().forGetter((noiseChunkGenerator) -> {
         return noiseChunkGenerator.seed;
      }), ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter((noiseChunkGenerator) -> {
         return noiseChunkGenerator.settings;
      })).apply(instance, (App)instance.stable(NoiseChunkGenerator::new));
   });
   private static final BlockState AIR;
   private static final BlockState[] EMPTY;
   private final int verticalNoiseResolution;
   private final int horizontalNoiseResolution;
   final int noiseSizeX;
   final int noiseSizeY;
   final int noiseSizeZ;
   private final NoiseSampler surfaceDepthNoise;
   private final DoublePerlinNoiseSampler edgeDensityNoise;
   private final DoublePerlinNoiseSampler fluidLevelNoise;
   private final DoublePerlinNoiseSampler fluidTypeNoise;
   protected final BlockState defaultBlock;
   protected final BlockState defaultFluid;
   private final long seed;
   protected final Supplier<ChunkGeneratorSettings> settings;
   private final int worldHeight;
   private final NoiseColumnSampler noiseColumnSampler;
   private final BlockSource deepslateSource;
   final OreVeinGenerator oreVeinGenerator;
   final NoodleCavesGenerator noodleCavesGenerator;

   public NoiseChunkGenerator(BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings) {
      this(biomeSource, biomeSource, seed, settings);
   }

   private NoiseChunkGenerator(BiomeSource populationSource, BiomeSource biomeSource, long seed, Supplier<ChunkGeneratorSettings> settings) {
      super(populationSource, biomeSource, ((ChunkGeneratorSettings)settings.get()).getStructuresConfig(), seed);
      this.seed = seed;
      ChunkGeneratorSettings chunkGeneratorSettings = (ChunkGeneratorSettings)settings.get();
      this.settings = settings;
      GenerationShapeConfig generationShapeConfig = chunkGeneratorSettings.getGenerationShapeConfig();
      this.worldHeight = generationShapeConfig.getHeight();
      this.verticalNoiseResolution = BiomeCoords.toBlock(generationShapeConfig.getSizeVertical());
      this.horizontalNoiseResolution = BiomeCoords.toBlock(generationShapeConfig.getSizeHorizontal());
      this.defaultBlock = chunkGeneratorSettings.getDefaultBlock();
      this.defaultFluid = chunkGeneratorSettings.getDefaultFluid();
      this.noiseSizeX = 16 / this.horizontalNoiseResolution;
      this.noiseSizeY = generationShapeConfig.getHeight() / this.verticalNoiseResolution;
      this.noiseSizeZ = 16 / this.horizontalNoiseResolution;
      ChunkRandom chunkRandom = new ChunkRandom(seed);
      InterpolatedNoiseSampler interpolatedNoiseSampler = new InterpolatedNoiseSampler(chunkRandom);
      this.surfaceDepthNoise = (NoiseSampler)(generationShapeConfig.hasSimplexSurfaceNoise() ? new OctaveSimplexNoiseSampler(chunkRandom, IntStream.rangeClosed(-3, 0)) : new OctavePerlinNoiseSampler(chunkRandom, IntStream.rangeClosed(-3, 0)));
      chunkRandom.skip(2620);
      OctavePerlinNoiseSampler octavePerlinNoiseSampler = new OctavePerlinNoiseSampler(chunkRandom, IntStream.rangeClosed(-15, 0));
      SimplexNoiseSampler simplexNoiseSampler2;
      if (generationShapeConfig.hasIslandNoiseOverride()) {
         ChunkRandom chunkRandom2 = new ChunkRandom(seed);
         chunkRandom2.skip(17292);
         simplexNoiseSampler2 = new SimplexNoiseSampler(chunkRandom2);
      } else {
         simplexNoiseSampler2 = null;
      }

      this.edgeDensityNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(chunkRandom.nextLong()), -3, (double[])(1.0D));
      this.fluidLevelNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(chunkRandom.nextLong()), -3, (double[])(1.0D, 0.0D, 2.0D));
      this.fluidTypeNoise = DoublePerlinNoiseSampler.create(new SimpleRandom(chunkRandom.nextLong()), -1, (double[])(1.0D, 0.0D));
      Object weightSampler2;
      if (chunkGeneratorSettings.hasNoiseCaves()) {
         weightSampler2 = new NoiseCaveSampler(chunkRandom, generationShapeConfig.getMinimumY() / this.verticalNoiseResolution);
      } else {
         weightSampler2 = WeightSampler.DEFAULT;
      }

      this.noiseColumnSampler = new NoiseColumnSampler(populationSource, this.horizontalNoiseResolution, this.verticalNoiseResolution, this.noiseSizeY, generationShapeConfig, interpolatedNoiseSampler, simplexNoiseSampler2, octavePerlinNoiseSampler, (WeightSampler)weightSampler2);
      this.deepslateSource = new DeepslateBlockSource(seed, this.defaultBlock, Blocks.DEEPSLATE.getDefaultState(), chunkGeneratorSettings);
      this.oreVeinGenerator = new OreVeinGenerator(seed, this.defaultBlock, this.horizontalNoiseResolution, this.verticalNoiseResolution, chunkGeneratorSettings.getGenerationShapeConfig().getMinimumY());
      this.noodleCavesGenerator = new NoodleCavesGenerator(seed);
   }

   private boolean hasAquifers() {
      return ((ChunkGeneratorSettings)this.settings.get()).hasAquifers();
   }

   protected Codec<? extends ChunkGenerator> getCodec() {
      return CODEC;
   }

   public ChunkGenerator withSeed(long seed) {
      return new NoiseChunkGenerator(this.populationSource.withSeed(seed), seed, this.settings);
   }

   public boolean matchesSettings(long seed, RegistryKey<ChunkGeneratorSettings> settingsKey) {
      return this.seed == seed && ((ChunkGeneratorSettings)this.settings.get()).equals(settingsKey);
   }

   private double[] sampleNoiseColumn(int x, int z, int minY, int noiseSizeY) {
      double[] ds = new double[noiseSizeY + 1];
      this.sampleNoiseColumn(ds, x, z, minY, noiseSizeY);
      return ds;
   }

   private void sampleNoiseColumn(double[] buffer, int x, int z, int minY, int noiseSizeY) {
      GenerationShapeConfig generationShapeConfig = ((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig();
      this.noiseColumnSampler.sampleNoiseColumn(buffer, x, z, generationShapeConfig, this.getSeaLevel(), minY, noiseSizeY);
   }

   public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world) {
      int i = Math.max(((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig().getMinimumY(), world.getBottomY());
      int j = Math.min(((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig().getMinimumY() + ((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig().getHeight(), world.getTopY());
      int k = MathHelper.floorDiv(i, this.verticalNoiseResolution);
      int l = MathHelper.floorDiv(j - i, this.verticalNoiseResolution);
      return l <= 0 ? world.getBottomY() : this.sampleHeightmap(x, z, (BlockState[])null, heightmap.getBlockPredicate(), k, l).orElse(world.getBottomY());
   }

   public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world) {
      int i = Math.max(((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig().getMinimumY(), world.getBottomY());
      int j = Math.min(((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig().getMinimumY() + ((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig().getHeight(), world.getTopY());
      int k = MathHelper.floorDiv(i, this.verticalNoiseResolution);
      int l = MathHelper.floorDiv(j - i, this.verticalNoiseResolution);
      if (l <= 0) {
         return new VerticalBlockSample(i, EMPTY);
      } else {
         BlockState[] blockStates = new BlockState[l * this.verticalNoiseResolution];
         this.sampleHeightmap(x, z, blockStates, (Predicate)null, k, l);
         return new VerticalBlockSample(i, blockStates);
      }
   }

   public BlockSource getBlockSource() {
      return this.deepslateSource;
   }

   private OptionalInt sampleHeightmap(int x, int z, @Nullable BlockState[] states, @Nullable Predicate<BlockState> predicate, int minY, int noiseSizeY) {
      int i = ChunkSectionPos.getSectionCoord(x);
      int j = ChunkSectionPos.getSectionCoord(z);
      int k = Math.floorDiv(x, this.horizontalNoiseResolution);
      int l = Math.floorDiv(z, this.horizontalNoiseResolution);
      int m = Math.floorMod(x, this.horizontalNoiseResolution);
      int n = Math.floorMod(z, this.horizontalNoiseResolution);
      double d = (double)m / (double)this.horizontalNoiseResolution;
      double e = (double)n / (double)this.horizontalNoiseResolution;
      double[][] ds = new double[][]{this.sampleNoiseColumn(k, l, minY, noiseSizeY), this.sampleNoiseColumn(k, l + 1, minY, noiseSizeY), this.sampleNoiseColumn(k + 1, l, minY, noiseSizeY), this.sampleNoiseColumn(k + 1, l + 1, minY, noiseSizeY)};
      AquiferSampler aquiferSampler = this.createBlockSampler(minY, noiseSizeY, new ChunkPos(i, j));

      for(int o = noiseSizeY - 1; o >= 0; --o) {
         double f = ds[0][o];
         double g = ds[1][o];
         double h = ds[2][o];
         double p = ds[3][o];
         double q = ds[0][o + 1];
         double r = ds[1][o + 1];
         double s = ds[2][o + 1];
         double t = ds[3][o + 1];

         for(int u = this.verticalNoiseResolution - 1; u >= 0; --u) {
            double v = (double)u / (double)this.verticalNoiseResolution;
            double w = MathHelper.lerp3(v, d, e, f, q, h, s, g, r, p, t);
            int y = o * this.verticalNoiseResolution + u;
            int aa = y + minY * this.verticalNoiseResolution;
            BlockState blockState = this.getBlockState(StructureWeightSampler.INSTANCE, aquiferSampler, this.deepslateSource, WeightSampler.DEFAULT, x, aa, z, w);
            if (states != null) {
               states[y] = blockState;
            }

            if (predicate != null && predicate.test(blockState)) {
               return OptionalInt.of(aa + 1);
            }
         }
      }

      return OptionalInt.empty();
   }

   private AquiferSampler createBlockSampler(int startY, int deltaY, ChunkPos pos) {
      return !this.hasAquifers() ? AquiferSampler.seaLevel(this.getSeaLevel(), this.defaultFluid) : AquiferSampler.aquifer(pos, this.edgeDensityNoise, this.fluidLevelNoise, this.fluidTypeNoise, (ChunkGeneratorSettings)this.settings.get(), this.noiseColumnSampler, startY * this.verticalNoiseResolution, deltaY * this.verticalNoiseResolution);
   }

   protected BlockState getBlockState(StructureWeightSampler structures, AquiferSampler aquiferSampler, BlockSource blockInterpolator, WeightSampler weightSampler, int i, int j, int k, double d) {
      double e = MathHelper.clamp(d / 200.0D, -1.0D, 1.0D);
      e = e / 2.0D - e * e * e / 24.0D;
      e = weightSampler.sample(e, i, j, k);
      e += structures.getWeight(i, j, k);
      return aquiferSampler.apply(blockInterpolator, i, j, k, e);
   }

   public void buildSurface(ChunkRegion region, Chunk chunk) {
      ChunkPos chunkPos = chunk.getPos();
      int i = chunkPos.x;
      int j = chunkPos.z;
      ChunkRandom chunkRandom = new ChunkRandom();
      chunkRandom.setTerrainSeed(i, j);
      ChunkPos chunkPos2 = chunk.getPos();
      int k = chunkPos2.getStartX();
      int l = chunkPos2.getStartZ();
      double d = 0.0625D;
      BlockPos.Mutable mutable = new BlockPos.Mutable();

      for(int m = 0; m < 16; ++m) {
         for(int n = 0; n < 16; ++n) {
            int o = k + m;
            int p = l + n;
            int q = chunk.sampleHeightmap(Heightmap.Type.WORLD_SURFACE_WG, m, n) + 1;
            double e = this.surfaceDepthNoise.sample((double)o * 0.0625D, (double)p * 0.0625D, 0.0625D, (double)m * 0.0625D) * 15.0D;
            int r = ((ChunkGeneratorSettings)this.settings.get()).getMinSurfaceLevel();
            region.getBiome(mutable.set(k + m, q, l + n)).buildSurface(chunkRandom, chunk, o, p, q, e, this.defaultBlock, this.defaultFluid, this.getSeaLevel(), r, region.getSeed());
         }
      }

      this.buildBedrock(chunk, chunkRandom);
   }

   private void buildBedrock(Chunk chunk, Random random) {
      BlockPos.Mutable mutable = new BlockPos.Mutable();
      int i = chunk.getPos().getStartX();
      int j = chunk.getPos().getStartZ();
      ChunkGeneratorSettings chunkGeneratorSettings = (ChunkGeneratorSettings)this.settings.get();
      int k = chunkGeneratorSettings.getGenerationShapeConfig().getMinimumY();
      int l = k + chunkGeneratorSettings.getBedrockFloorY();
      int m = this.worldHeight - 1 + k - chunkGeneratorSettings.getBedrockCeilingY();
      int n = true;
      int o = chunk.getBottomY();
      int p = chunk.getTopY();
      boolean bl = m + 5 - 1 >= o && m < p;
      boolean bl2 = l + 5 - 1 >= o && l < p;
      if (bl || bl2) {
         Iterator var15 = BlockPos.iterate(i, 0, j, i + 15, 0, j + 15).iterator();

         while(true) {
            BlockPos blockPos;
            int r;
            do {
               if (!var15.hasNext()) {
                  return;
               }

               blockPos = (BlockPos)var15.next();
               if (bl) {
                  for(r = 0; r < 5; ++r) {
                     if (r <= random.nextInt(5)) {
                        chunk.setBlockState(mutable.set(blockPos.getX(), m - r, blockPos.getZ()), Blocks.BEDROCK.getDefaultState(), false);
                     }
                  }
               }
            } while(!bl2);

            for(r = 4; r >= 0; --r) {
               if (r <= random.nextInt(5)) {
                  chunk.setBlockState(mutable.set(blockPos.getX(), l + r, blockPos.getZ()), Blocks.BEDROCK.getDefaultState(), false);
               }
            }
         }
      }
   }

   public CompletableFuture<Chunk> populateNoise(Executor executor, StructureAccessor accessor, Chunk chunk) {
      GenerationShapeConfig generationShapeConfig = ((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig();
      int i = Math.max(generationShapeConfig.getMinimumY(), chunk.getBottomY());
      int j = Math.min(generationShapeConfig.getMinimumY() + generationShapeConfig.getHeight(), chunk.getTopY());
      int k = MathHelper.floorDiv(i, this.verticalNoiseResolution);
      int l = MathHelper.floorDiv(j - i, this.verticalNoiseResolution);
      if (l <= 0) {
         return CompletableFuture.completedFuture(chunk);
      } else {
         int m = chunk.getSectionIndex(l * this.verticalNoiseResolution - 1 + i);
         int n = chunk.getSectionIndex(i);
         return CompletableFuture.supplyAsync(() -> {
            HashSet set = Sets.newHashSet();
            boolean var15 = false;

            Chunk var17;
            try {
               var15 = true;
               int mx = m;

               while(true) {
                  if (mx < n) {
                     var17 = this.populateNoise(accessor, chunk, k, l);
                     var15 = false;
                     break;
                  }

                  ChunkSection chunkSection = chunk.getSection(mx);
                  chunkSection.lock();
                  set.add(chunkSection);
                  --mx;
               }
            } finally {
               if (var15) {
                  Iterator var12 = set.iterator();

                  while(var12.hasNext()) {
                     ChunkSection chunkSection3 = (ChunkSection)var12.next();
                     chunkSection3.unlock();
                  }

               }
            }

            Iterator var18 = set.iterator();

            while(var18.hasNext()) {
               ChunkSection chunkSection2 = (ChunkSection)var18.next();
               chunkSection2.unlock();
            }

            return var17;
         }, Util.getMainWorkerExecutor());
      }
   }

   private Chunk populateNoise(StructureAccessor accessor, Chunk chunk, int startY, int noiseSizeY) {
      Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
      Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
      ChunkPos chunkPos = chunk.getPos();
      int i = chunkPos.getStartX();
      int j = chunkPos.getStartZ();
      StructureWeightSampler structureWeightSampler = new StructureWeightSampler(accessor, chunk);
      AquiferSampler aquiferSampler = this.createBlockSampler(startY, noiseSizeY, chunkPos);
      NoiseInterpolator noiseInterpolator = new NoiseInterpolator(this.noiseSizeX, noiseSizeY, this.noiseSizeZ, chunkPos, startY, this::sampleNoiseColumn);
      List<NoiseInterpolator> list = Lists.newArrayList((Object[])(noiseInterpolator));
      Objects.requireNonNull(list);
      Consumer<NoiseInterpolator> consumer = list::add;
      DoubleFunction<BlockSource> doubleFunction = this.createBlockSourceFactory(startY, chunkPos, consumer);
      DoubleFunction<WeightSampler> doubleFunction2 = this.createWeightSamplerFactory(startY, chunkPos, consumer);
      list.forEach(NoiseInterpolator::sampleStartNoise);
      BlockPos.Mutable mutable = new BlockPos.Mutable();

      for(int k = 0; k < this.noiseSizeX; ++k) {
         list.forEach((noiseInterpolatorx) -> {
            noiseInterpolatorx.sampleEndNoise(k);
         });

         for(int m = 0; m < this.noiseSizeZ; ++m) {
            ChunkSection chunkSection = chunk.getSection(chunk.countVerticalSections() - 1);

            for(int n = noiseSizeY - 1; n >= 0; --n) {
               list.forEach((noiseInterpolatorx) -> {
                  noiseInterpolatorx.sampleNoiseCorners(n, m);
               });

               for(int q = this.verticalNoiseResolution - 1; q >= 0; --q) {
                  int r = (startY + n) * this.verticalNoiseResolution + q;
                  int s = r & 15;
                  int t = chunk.getSectionIndex(r);
                  if (chunk.getSectionIndex(chunkSection.getYOffset()) != t) {
                     chunkSection = chunk.getSection(t);
                  }

                  double d = (double)q / (double)this.verticalNoiseResolution;
                  list.forEach((noiseInterpolatorx) -> {
                     noiseInterpolatorx.sampleNoiseY(d);
                  });

                  for(int u = 0; u < this.horizontalNoiseResolution; ++u) {
                     int v = i + k * this.horizontalNoiseResolution + u;
                     int w = v & 15;
                     double e = (double)u / (double)this.horizontalNoiseResolution;
                     list.forEach((noiseInterpolatorx) -> {
                        noiseInterpolatorx.sampleNoiseX(e);
                     });

                     for(int x = 0; x < this.horizontalNoiseResolution; ++x) {
                        int y = j + m * this.horizontalNoiseResolution + x;
                        int z = y & 15;
                        double f = (double)x / (double)this.horizontalNoiseResolution;
                        double g = noiseInterpolator.sampleNoise(f);
                        BlockState blockState = this.getBlockState(structureWeightSampler, aquiferSampler, (BlockSource)doubleFunction.apply(f), (WeightSampler)doubleFunction2.apply(f), v, r, y, g);
                        if (blockState != AIR) {
                           if (blockState.getLuminance() != 0 && chunk instanceof ProtoChunk) {
                              mutable.set(v, r, y);
                              ((ProtoChunk)chunk).addLightSource(mutable);
                           }

                           chunkSection.setBlockState(w, s, z, blockState, false);
                           heightmap.trackUpdate(w, r, z, blockState);
                           heightmap2.trackUpdate(w, r, z, blockState);
                           if (aquiferSampler.needsFluidTick() && !blockState.getFluidState().isEmpty()) {
                              mutable.set(v, r, y);
                              chunk.getFluidTickScheduler().schedule(mutable, blockState.getFluidState().getFluid(), 0);
                           }
                        }
                     }
                  }
               }
            }
         }

         list.forEach(NoiseInterpolator::swapBuffers);
      }

      return chunk;
   }

   private DoubleFunction<WeightSampler> createWeightSamplerFactory(int minY, ChunkPos pos, Consumer<NoiseInterpolator> consumer) {
      if (!((ChunkGeneratorSettings)this.settings.get()).hasNoodleCaves()) {
         return (d) -> {
            return WeightSampler.DEFAULT;
         };
      } else {
         NoiseChunkGenerator.NoodleCavesSampler noodleCavesSampler = new NoiseChunkGenerator.NoodleCavesSampler(pos, minY);
         noodleCavesSampler.feed(consumer);
         Objects.requireNonNull(noodleCavesSampler);
         return noodleCavesSampler::setDeltaZ;
      }
   }

   private DoubleFunction<BlockSource> createBlockSourceFactory(int minY, ChunkPos pos, Consumer<NoiseInterpolator> consumer) {
      if (!((ChunkGeneratorSettings)this.settings.get()).hasOreVeins()) {
         return (d) -> {
            return this.deepslateSource;
         };
      } else {
         NoiseChunkGenerator.OreVeinSource oreVeinSource = new NoiseChunkGenerator.OreVeinSource(pos, minY, this.seed + 1L);
         oreVeinSource.feed(consumer);
         BlockSource blockSource = (i, j, k) -> {
            BlockState blockState = oreVeinSource.sample(i, j, k);
            return blockState != this.defaultBlock ? blockState : this.deepslateSource.sample(i, j, k);
         };
         return (deltaZ) -> {
            oreVeinSource.setDeltaZ(deltaZ);
            return blockSource;
         };
      }
   }

   protected AquiferSampler createAquiferSampler(Chunk chunk) {
      ChunkPos chunkPos = chunk.getPos();
      int i = Math.max(((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig().getMinimumY(), chunk.getBottomY());
      int j = MathHelper.floorDiv(i, this.verticalNoiseResolution);
      return this.createBlockSampler(j, this.noiseSizeY, chunkPos);
   }

   public int getWorldHeight() {
      return this.worldHeight;
   }

   public int getSeaLevel() {
      return ((ChunkGeneratorSettings)this.settings.get()).getSeaLevel();
   }

   public int getMinimumY() {
      return ((ChunkGeneratorSettings)this.settings.get()).getGenerationShapeConfig().getMinimumY();
   }

   public Pool<SpawnSettings.SpawnEntry> getEntitySpawnList(Biome biome, StructureAccessor accessor, SpawnGroup group, BlockPos pos) {
      if (accessor.getStructureAt(pos, true, StructureFeature.SWAMP_HUT).hasChildren()) {
         if (group == SpawnGroup.MONSTER) {
            return StructureFeature.SWAMP_HUT.getMonsterSpawns();
         }

         if (group == SpawnGroup.CREATURE) {
            return StructureFeature.SWAMP_HUT.getCreatureSpawns();
         }
      }

      if (group == SpawnGroup.MONSTER) {
         if (accessor.getStructureAt(pos, false, StructureFeature.PILLAGER_OUTPOST).hasChildren()) {
            return StructureFeature.PILLAGER_OUTPOST.getMonsterSpawns();
         }

         if (accessor.getStructureAt(pos, false, StructureFeature.MONUMENT).hasChildren()) {
            return StructureFeature.MONUMENT.getMonsterSpawns();
         }

         if (accessor.getStructureAt(pos, true, StructureFeature.FORTRESS).hasChildren()) {
            return StructureFeature.FORTRESS.getMonsterSpawns();
         }
      }

      return group == SpawnGroup.UNDERGROUND_WATER_CREATURE && accessor.getStructureAt(pos, false, StructureFeature.MONUMENT).hasChildren() ? StructureFeature.MONUMENT.getUndergroundWaterCreatureSpawns() : super.getEntitySpawnList(biome, accessor, group, pos);
   }

   public void populateEntities(ChunkRegion region) {
      if (!((ChunkGeneratorSettings)this.settings.get()).isMobGenerationDisabled()) {
         ChunkPos chunkPos = region.getCenterPos();
         Biome biome = region.getBiome(chunkPos.getStartPos());
         ChunkRandom chunkRandom = new ChunkRandom();
         chunkRandom.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
         SpawnHelper.populateEntities(region, biome, chunkPos, chunkRandom);
      }
   }

   static {
      AIR = Blocks.AIR.getDefaultState();
      EMPTY = new BlockState[0];
   }

   class NoodleCavesSampler implements WeightSampler {
      private final NoiseInterpolator field_33646;
      private final NoiseInterpolator field_33647;
      private final NoiseInterpolator field_33648;
      private final NoiseInterpolator field_33649;
      private double deltaZ;

      public NoodleCavesSampler(ChunkPos pos, int minY) {
         int var10003 = NoiseChunkGenerator.this.noiseSizeX;
         int var10004 = NoiseChunkGenerator.this.noiseSizeY;
         int var10005 = NoiseChunkGenerator.this.noiseSizeZ;
         NoodleCavesGenerator var10008 = NoiseChunkGenerator.this.noodleCavesGenerator;
         Objects.requireNonNull(var10008);
         this.field_33646 = new NoiseInterpolator(var10003, var10004, var10005, pos, minY, var10008::method_36471);
         var10003 = NoiseChunkGenerator.this.noiseSizeX;
         var10004 = NoiseChunkGenerator.this.noiseSizeY;
         var10005 = NoiseChunkGenerator.this.noiseSizeZ;
         var10008 = NoiseChunkGenerator.this.noodleCavesGenerator;
         Objects.requireNonNull(var10008);
         this.field_33647 = new NoiseInterpolator(var10003, var10004, var10005, pos, minY, var10008::method_36474);
         var10003 = NoiseChunkGenerator.this.noiseSizeX;
         var10004 = NoiseChunkGenerator.this.noiseSizeY;
         var10005 = NoiseChunkGenerator.this.noiseSizeZ;
         var10008 = NoiseChunkGenerator.this.noodleCavesGenerator;
         Objects.requireNonNull(var10008);
         this.field_33648 = new NoiseInterpolator(var10003, var10004, var10005, pos, minY, var10008::method_36475);
         var10003 = NoiseChunkGenerator.this.noiseSizeX;
         var10004 = NoiseChunkGenerator.this.noiseSizeY;
         var10005 = NoiseChunkGenerator.this.noiseSizeZ;
         var10008 = NoiseChunkGenerator.this.noodleCavesGenerator;
         Objects.requireNonNull(var10008);
         this.field_33649 = new NoiseInterpolator(var10003, var10004, var10005, pos, minY, var10008::method_36476);
      }

      public WeightSampler setDeltaZ(double deltaZ) {
         this.deltaZ = deltaZ;
         return this;
      }

      public double sample(double weight, int x, int y, int z) {
         double d = this.field_33646.sampleNoise(this.deltaZ);
         double e = this.field_33647.sampleNoise(this.deltaZ);
         double f = this.field_33648.sampleNoise(this.deltaZ);
         double g = this.field_33649.sampleNoise(this.deltaZ);
         return NoiseChunkGenerator.this.noodleCavesGenerator.method_36470(weight, x, y, z, d, e, f, g, NoiseChunkGenerator.this.getMinimumY());
      }

      public void feed(Consumer<NoiseInterpolator> consumer) {
         consumer.accept(this.field_33646);
         consumer.accept(this.field_33647);
         consumer.accept(this.field_33648);
         consumer.accept(this.field_33649);
      }
   }

   private class OreVeinSource implements BlockSource {
      private final NoiseInterpolator field_33581;
      private final NoiseInterpolator field_33582;
      private final NoiseInterpolator field_33583;
      private double deltaZ;
      private final long seed;
      private final ChunkRandom random = new ChunkRandom();

      public OreVeinSource(ChunkPos pos, int minY, long seed) {
         int var10003 = NoiseChunkGenerator.this.noiseSizeX;
         int var10004 = NoiseChunkGenerator.this.noiseSizeY;
         int var10005 = NoiseChunkGenerator.this.noiseSizeZ;
         OreVeinGenerator var10008 = NoiseChunkGenerator.this.oreVeinGenerator;
         Objects.requireNonNull(var10008);
         this.field_33581 = new NoiseInterpolator(var10003, var10004, var10005, pos, minY, var10008::method_36401);
         var10003 = NoiseChunkGenerator.this.noiseSizeX;
         var10004 = NoiseChunkGenerator.this.noiseSizeY;
         var10005 = NoiseChunkGenerator.this.noiseSizeZ;
         var10008 = NoiseChunkGenerator.this.oreVeinGenerator;
         Objects.requireNonNull(var10008);
         this.field_33582 = new NoiseInterpolator(var10003, var10004, var10005, pos, minY, var10008::method_36404);
         var10003 = NoiseChunkGenerator.this.noiseSizeX;
         var10004 = NoiseChunkGenerator.this.noiseSizeY;
         var10005 = NoiseChunkGenerator.this.noiseSizeZ;
         var10008 = NoiseChunkGenerator.this.oreVeinGenerator;
         Objects.requireNonNull(var10008);
         this.field_33583 = new NoiseInterpolator(var10003, var10004, var10005, pos, minY, var10008::method_36405);
         this.seed = seed;
      }

      public void feed(Consumer<NoiseInterpolator> consumer) {
         consumer.accept(this.field_33581);
         consumer.accept(this.field_33582);
         consumer.accept(this.field_33583);
      }

      public void setDeltaZ(double deltaZ) {
         this.deltaZ = deltaZ;
      }

      public BlockState sample(int x, int y, int z) {
         double d = this.field_33581.sampleNoise(this.deltaZ);
         double e = this.field_33582.sampleNoise(this.deltaZ);
         double f = this.field_33583.sampleNoise(this.deltaZ);
         this.random.setGrimstoneSeed(this.seed, x, y, z);
         return NoiseChunkGenerator.this.oreVeinGenerator.sample(this.random, x, y, z, d, e, f);
      }
   }
}
