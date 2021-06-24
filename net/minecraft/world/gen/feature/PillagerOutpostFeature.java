package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.entity.EntityType;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.StructureConfig;

public class PillagerOutpostFeature extends JigsawFeature {
   private static final Pool<SpawnSettings.SpawnEntry> MONSTER_SPAWNS;

   public PillagerOutpostFeature(Codec<StructurePoolFeatureConfig> codec) {
      super(codec, 0, true, true);
   }

   public Pool<SpawnSettings.SpawnEntry> getMonsterSpawns() {
      return MONSTER_SPAWNS;
   }

   protected boolean shouldStartAt(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long l, ChunkRandom chunkRandom, ChunkPos chunkPos, Biome biome, ChunkPos chunkPos2, StructurePoolFeatureConfig structurePoolFeatureConfig, HeightLimitView heightLimitView) {
      int i = chunkPos.x >> 4;
      int j = chunkPos.z >> 4;
      chunkRandom.setSeed((long)(i ^ j << 4) ^ l);
      chunkRandom.nextInt();
      if (chunkRandom.nextInt(5) != 0) {
         return false;
      } else {
         return !this.isVillageNearby(chunkGenerator, l, chunkRandom, chunkPos);
      }
   }

   private boolean isVillageNearby(ChunkGenerator generator, long worldSeed, ChunkRandom random, ChunkPos pos) {
      StructureConfig structureConfig = generator.getStructuresConfig().getForType(StructureFeature.VILLAGE);
      if (structureConfig == null) {
         return false;
      } else {
         int i = pos.x;
         int j = pos.z;

         for(int k = i - 10; k <= i + 10; ++k) {
            for(int l = j - 10; l <= j + 10; ++l) {
               ChunkPos chunkPos = StructureFeature.VILLAGE.getStartChunk(structureConfig, worldSeed, random, k, l);
               if (k == chunkPos.x && l == chunkPos.z) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   static {
      MONSTER_SPAWNS = Pool.of((Weighted[])(new SpawnSettings.SpawnEntry(EntityType.PILLAGER, 1, 1, 1)));
   }
}
