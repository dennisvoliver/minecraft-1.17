package net.minecraft.world.gen;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.Iterator;
import net.minecraft.structure.JigsawJunction;
import net.minecraft.structure.PoolStructurePiece;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.pool.StructurePool;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.feature.StructureFeature;

/**
 * Applies weights to noise values if they are near structures, placing terrain under them and hollowing out the space above them.
 */
public class StructureWeightSampler {
   public static final StructureWeightSampler INSTANCE = new StructureWeightSampler();
   public static final int field_31461 = 12;
   private static final int field_31462 = 24;
   private static final float[] STRUCTURE_WEIGHT_TABLE = (float[])Util.make(new float[13824], (array) -> {
      for(int i = 0; i < 24; ++i) {
         for(int j = 0; j < 24; ++j) {
            for(int k = 0; k < 24; ++k) {
               array[i * 24 * 24 + j * 24 + k] = (float)calculateStructureWeight(j - 12, k - 12, i - 12);
            }
         }
      }

   });
   private final ObjectList<StructurePiece> pieces;
   private final ObjectList<JigsawJunction> junctions;
   private final ObjectListIterator<StructurePiece> pieceIterator;
   private final ObjectListIterator<JigsawJunction> junctionIterator;

   protected StructureWeightSampler(StructureAccessor accessor, Chunk chunk) {
      ChunkPos chunkPos = chunk.getPos();
      int i = chunkPos.getStartX();
      int j = chunkPos.getStartZ();
      this.junctions = new ObjectArrayList(32);
      this.pieces = new ObjectArrayList(10);
      Iterator var6 = StructureFeature.JIGSAW_STRUCTURES.iterator();

      while(var6.hasNext()) {
         StructureFeature<?> structureFeature = (StructureFeature)var6.next();
         accessor.getStructuresWithChildren(ChunkSectionPos.from(chunk), structureFeature).forEach((start) -> {
            Iterator var5 = start.getChildren().iterator();

            while(true) {
               while(true) {
                  StructurePiece structurePiece;
                  do {
                     if (!var5.hasNext()) {
                        return;
                     }

                     structurePiece = (StructurePiece)var5.next();
                  } while(!structurePiece.intersectsChunk(chunkPos, 12));

                  if (structurePiece instanceof PoolStructurePiece) {
                     PoolStructurePiece poolStructurePiece = (PoolStructurePiece)structurePiece;
                     StructurePool.Projection projection = poolStructurePiece.getPoolElement().getProjection();
                     if (projection == StructurePool.Projection.RIGID) {
                        this.pieces.add(poolStructurePiece);
                     }

                     Iterator var9 = poolStructurePiece.getJunctions().iterator();

                     while(var9.hasNext()) {
                        JigsawJunction jigsawJunction = (JigsawJunction)var9.next();
                        int k = jigsawJunction.getSourceX();
                        int l = jigsawJunction.getSourceZ();
                        if (k > i - 12 && l > j - 12 && k < i + 15 + 12 && l < j + 15 + 12) {
                           this.junctions.add(jigsawJunction);
                        }
                     }
                  } else {
                     this.pieces.add(structurePiece);
                  }
               }
            }
         });
      }

      this.pieceIterator = this.pieces.iterator();
      this.junctionIterator = this.junctions.iterator();
   }

   private StructureWeightSampler() {
      this.junctions = new ObjectArrayList();
      this.pieces = new ObjectArrayList();
      this.pieceIterator = this.pieces.iterator();
      this.junctionIterator = this.junctions.iterator();
   }

   /**
    * Gets the weight of the structures near the given position.
    */
   protected double getWeight(int x, int y, int z) {
      double d = 0.0D;

      int i;
      int j;
      while(this.pieceIterator.hasNext()) {
         StructurePiece structurePiece = (StructurePiece)this.pieceIterator.next();
         BlockBox blockBox = structurePiece.getBoundingBox();
         i = Math.max(0, Math.max(blockBox.getMinX() - x, x - blockBox.getMaxX()));
         j = y - (blockBox.getMinY() + (structurePiece instanceof PoolStructurePiece ? ((PoolStructurePiece)structurePiece).getGroundLevelDelta() : 0));
         int k = Math.max(0, Math.max(blockBox.getMinZ() - z, z - blockBox.getMaxZ()));
         StructureWeightType structureWeightType = structurePiece.method_33882();
         if (structureWeightType == StructureWeightType.BURY) {
            d += getMagnitudeWeight(i, j, k);
         } else if (structureWeightType == StructureWeightType.BEARD) {
            d += getStructureWeight(i, j, k) * 0.8D;
         }
      }

      this.pieceIterator.back(this.pieces.size());

      while(this.junctionIterator.hasNext()) {
         JigsawJunction jigsawJunction = (JigsawJunction)this.junctionIterator.next();
         int l = x - jigsawJunction.getSourceX();
         i = y - jigsawJunction.getSourceGroundY();
         j = z - jigsawJunction.getSourceZ();
         d += getStructureWeight(l, i, j) * 0.4D;
      }

      this.junctionIterator.back(this.junctions.size());
      return d;
   }

   private static double getMagnitudeWeight(int x, int y, int z) {
      double d = MathHelper.magnitude(x, (double)y / 2.0D, z);
      return MathHelper.clampedLerpFromProgress(d, 0.0D, 6.0D, 1.0D, 0.0D);
   }

   /**
    * Gets the structure weight from the array from the given position, or 0 if the position is out of bounds.
    */
   private static double getStructureWeight(int x, int y, int z) {
      int i = x + 12;
      int j = y + 12;
      int k = z + 12;
      if (i >= 0 && i < 24) {
         if (j >= 0 && j < 24) {
            return k >= 0 && k < 24 ? (double)STRUCTURE_WEIGHT_TABLE[k * 24 * 24 + i * 24 + j] : 0.0D;
         } else {
            return 0.0D;
         }
      } else {
         return 0.0D;
      }
   }

   /**
    * Calculates the structure weight for the given position.
    * <p>The weight increases as x and z approach {@code (0, 0)}, and positive y values make the weight negative while negative y values make the weight positive.
    */
   private static double calculateStructureWeight(int x, int y, int z) {
      double d = (double)(x * x + z * z);
      double e = (double)y + 0.5D;
      double f = e * e;
      double g = Math.pow(2.718281828459045D, -(f / 16.0D + d / 16.0D));
      double h = -e * MathHelper.fastInverseSqrt(f / 2.0D + d / 2.0D) / 2.0D;
      return h * g;
   }
}
