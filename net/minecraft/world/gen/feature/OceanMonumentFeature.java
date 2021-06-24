package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import net.minecraft.entity.EntityType;
import net.minecraft.structure.OceanMonumentGenerator;
import net.minecraft.structure.StructureManager;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.collection.Pool;
import net.minecraft.util.collection.Weighted;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class OceanMonumentFeature extends StructureFeature<DefaultFeatureConfig> {
   private static final Pool<SpawnSettings.SpawnEntry> MONSTER_SPAWNS;

   public OceanMonumentFeature(Codec<DefaultFeatureConfig> codec) {
      super(codec);
   }

   protected boolean isUniformDistribution() {
      return false;
   }

   protected boolean shouldStartAt(ChunkGenerator chunkGenerator, BiomeSource biomeSource, long l, ChunkRandom chunkRandom, ChunkPos chunkPos, Biome biome, ChunkPos chunkPos2, DefaultFeatureConfig defaultFeatureConfig, HeightLimitView heightLimitView) {
      int i = chunkPos.getOffsetX(9);
      int j = chunkPos.getOffsetZ(9);
      Set<Biome> set = biomeSource.getBiomesInArea(i, chunkGenerator.getSeaLevel(), j, 16);
      Iterator var14 = set.iterator();

      Biome biome2;
      do {
         if (!var14.hasNext()) {
            Set<Biome> set2 = biomeSource.getBiomesInArea(i, chunkGenerator.getSeaLevel(), j, 29);
            Iterator var18 = set2.iterator();

            Biome biome3;
            do {
               if (!var18.hasNext()) {
                  return true;
               }

               biome3 = (Biome)var18.next();
            } while(biome3.getCategory() == Biome.Category.OCEAN || biome3.getCategory() == Biome.Category.RIVER);

            return false;
         }

         biome2 = (Biome)var14.next();
      } while(biome2.getGenerationSettings().hasStructureFeature(this));

      return false;
   }

   public StructureFeature.StructureStartFactory<DefaultFeatureConfig> getStructureStartFactory() {
      return OceanMonumentFeature.Start::new;
   }

   public Pool<SpawnSettings.SpawnEntry> getMonsterSpawns() {
      return MONSTER_SPAWNS;
   }

   static {
      MONSTER_SPAWNS = Pool.of((Weighted[])(new SpawnSettings.SpawnEntry(EntityType.GUARDIAN, 1, 2, 4)));
   }

   public static class Start extends StructureStart<DefaultFeatureConfig> {
      private boolean field_33415;

      public Start(StructureFeature<DefaultFeatureConfig> structureFeature, ChunkPos chunkPos, int i, long l) {
         super(structureFeature, chunkPos, i, l);
      }

      public void init(DynamicRegistryManager dynamicRegistryManager, ChunkGenerator chunkGenerator, StructureManager structureManager, ChunkPos chunkPos, Biome biome, DefaultFeatureConfig defaultFeatureConfig, HeightLimitView heightLimitView) {
         this.method_36216(chunkPos);
      }

      private void method_36216(ChunkPos chunkPos) {
         int i = chunkPos.getStartX() - 29;
         int j = chunkPos.getStartZ() - 29;
         Direction direction = Direction.Type.HORIZONTAL.random(this.random);
         this.addPiece(new OceanMonumentGenerator.Base(this.random, i, j, direction));
         this.field_33415 = true;
      }

      public void generateStructure(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox box, ChunkPos chunkPos) {
         if (!this.field_33415) {
            this.children.clear();
            this.method_36216(this.getPos());
         }

         super.generateStructure(world, structureAccessor, chunkGenerator, random, box, chunkPos);
      }
   }
}
