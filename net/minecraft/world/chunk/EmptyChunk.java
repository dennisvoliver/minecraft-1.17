package net.minecraft.world.chunk;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BuiltinBiomes;
import net.minecraft.world.biome.source.BiomeArray;
import org.jetbrains.annotations.Nullable;

public class EmptyChunk extends WorldChunk {
   public EmptyChunk(World world, ChunkPos pos) {
      super((World)world, (ChunkPos)pos, (BiomeArray)(new EmptyChunk.EmptyBiomeArray(world)));
   }

   public BlockState getBlockState(BlockPos pos) {
      return Blocks.VOID_AIR.getDefaultState();
   }

   @Nullable
   public BlockState setBlockState(BlockPos pos, BlockState state, boolean moved) {
      return null;
   }

   public FluidState getFluidState(BlockPos pos) {
      return Fluids.EMPTY.getDefaultState();
   }

   public int getLuminance(BlockPos pos) {
      return 0;
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType creationType) {
      return null;
   }

   public void addBlockEntity(BlockEntity blockEntity) {
   }

   public void setBlockEntity(BlockEntity blockEntity) {
   }

   public void removeBlockEntity(BlockPos pos) {
   }

   public void markDirty() {
   }

   public boolean isEmpty() {
      return true;
   }

   public boolean areSectionsEmptyBetween(int lowerHeight, int upperHeight) {
      return true;
   }

   public ChunkHolder.LevelType getLevelType() {
      return ChunkHolder.LevelType.BORDER;
   }

   private static class EmptyBiomeArray extends BiomeArray {
      private static final Biome[] EMPTY_ARRAY = new Biome[0];

      public EmptyBiomeArray(World world) {
         super(world.getRegistryManager().get(Registry.BIOME_KEY), world, (Biome[])EMPTY_ARRAY);
      }

      public int[] toIntArray() {
         throw new UnsupportedOperationException("Can not write biomes of an empty chunk");
      }

      public Biome getBiomeForNoiseGen(int biomeX, int biomeY, int biomeZ) {
         return BuiltinBiomes.PLAINS;
      }
   }
}
