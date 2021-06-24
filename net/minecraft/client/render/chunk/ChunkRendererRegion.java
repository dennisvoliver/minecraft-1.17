package net.minecraft.client.render.chunk;

import java.util.Iterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import net.minecraft.world.level.ColorResolver;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ChunkRendererRegion implements BlockRenderView {
   protected final int chunkXOffset;
   protected final int chunkZOffset;
   protected final BlockPos offset;
   protected final int sizeX;
   protected final int sizeY;
   protected final int sizeZ;
   protected final WorldChunk[][] chunks;
   protected final BlockState[] blockStates;
   protected final FluidState[] fluidStates;
   protected final World world;

   @Nullable
   public static ChunkRendererRegion create(World world, BlockPos startPos, BlockPos endPos, int chunkRadius) {
      int i = ChunkSectionPos.getSectionCoord(startPos.getX() - chunkRadius);
      int j = ChunkSectionPos.getSectionCoord(startPos.getZ() - chunkRadius);
      int k = ChunkSectionPos.getSectionCoord(endPos.getX() + chunkRadius);
      int l = ChunkSectionPos.getSectionCoord(endPos.getZ() + chunkRadius);
      WorldChunk[][] worldChunks = new WorldChunk[k - i + 1][l - j + 1];

      for(int m = i; m <= k; ++m) {
         for(int n = j; n <= l; ++n) {
            worldChunks[m - i][n - j] = world.getChunk(m, n);
         }
      }

      if (isEmptyBetween(startPos, endPos, i, j, worldChunks)) {
         return null;
      } else {
         int o = true;
         BlockPos blockPos = startPos.add(-1, -1, -1);
         BlockPos blockPos2 = endPos.add(1, 1, 1);
         return new ChunkRendererRegion(world, i, j, worldChunks, blockPos, blockPos2);
      }
   }

   public static boolean isEmptyBetween(BlockPos from, BlockPos to, int i, int j, WorldChunk[][] chunks) {
      for(int k = ChunkSectionPos.getSectionCoord(from.getX()); k <= ChunkSectionPos.getSectionCoord(to.getX()); ++k) {
         for(int l = ChunkSectionPos.getSectionCoord(from.getZ()); l <= ChunkSectionPos.getSectionCoord(to.getZ()); ++l) {
            WorldChunk worldChunk = chunks[k - i][l - j];
            if (!worldChunk.areSectionsEmptyBetween(from.getY(), to.getY())) {
               return false;
            }
         }
      }

      return true;
   }

   public ChunkRendererRegion(World world, int chunkX, int chunkZ, WorldChunk[][] chunks, BlockPos startPos, BlockPos endPos) {
      this.world = world;
      this.chunkXOffset = chunkX;
      this.chunkZOffset = chunkZ;
      this.chunks = chunks;
      this.offset = startPos;
      this.sizeX = endPos.getX() - startPos.getX() + 1;
      this.sizeY = endPos.getY() - startPos.getY() + 1;
      this.sizeZ = endPos.getZ() - startPos.getZ() + 1;
      this.blockStates = new BlockState[this.sizeX * this.sizeY * this.sizeZ];
      this.fluidStates = new FluidState[this.sizeX * this.sizeY * this.sizeZ];

      BlockPos blockPos;
      WorldChunk worldChunk;
      int k;
      for(Iterator var7 = BlockPos.iterate(startPos, endPos).iterator(); var7.hasNext(); this.fluidStates[k] = worldChunk.getFluidState(blockPos)) {
         blockPos = (BlockPos)var7.next();
         int i = ChunkSectionPos.getSectionCoord(blockPos.getX()) - chunkX;
         int j = ChunkSectionPos.getSectionCoord(blockPos.getZ()) - chunkZ;
         worldChunk = chunks[i][j];
         k = this.getIndex(blockPos);
         this.blockStates[k] = worldChunk.getBlockState(blockPos);
      }

   }

   protected final int getIndex(BlockPos pos) {
      return this.getIndex(pos.getX(), pos.getY(), pos.getZ());
   }

   protected int getIndex(int x, int y, int z) {
      int i = x - this.offset.getX();
      int j = y - this.offset.getY();
      int k = z - this.offset.getZ();
      return k * this.sizeX * this.sizeY + j * this.sizeX + i;
   }

   public BlockState getBlockState(BlockPos pos) {
      return this.blockStates[this.getIndex(pos)];
   }

   public FluidState getFluidState(BlockPos pos) {
      return this.fluidStates[this.getIndex(pos)];
   }

   public float getBrightness(Direction direction, boolean shaded) {
      return this.world.getBrightness(direction, shaded);
   }

   public LightingProvider getLightingProvider() {
      return this.world.getLightingProvider();
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos) {
      return this.getBlockEntity(pos, WorldChunk.CreationType.IMMEDIATE);
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pos, WorldChunk.CreationType creationType) {
      int i = ChunkSectionPos.getSectionCoord(pos.getX()) - this.chunkXOffset;
      int j = ChunkSectionPos.getSectionCoord(pos.getZ()) - this.chunkZOffset;
      return this.chunks[i][j].getBlockEntity(pos, creationType);
   }

   public int getColor(BlockPos pos, ColorResolver colorResolver) {
      return this.world.getColor(pos, colorResolver);
   }

   public int getBottomY() {
      return this.world.getBottomY();
   }

   public int getHeight() {
      return this.world.getHeight();
   }
}
