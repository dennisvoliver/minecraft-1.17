package net.minecraft.structure;

import com.google.common.collect.Lists;
import com.mojang.serialization.DataResult;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.RailBlock;
import net.minecraft.block.WallTorchBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.enums.RailShape;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.vehicle.ChestMinecartEntity;
import net.minecraft.loot.LootTables;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.MineshaftFeature;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class MineshaftGenerator {
   static final Logger LOGGER = LogManager.getLogger();
   private static final int field_31551 = 3;
   private static final int field_31552 = 3;
   private static final int field_31553 = 5;
   private static final int field_31554 = 20;
   private static final int field_31555 = 50;
   private static final int field_31556 = 8;

   private static MineshaftGenerator.MineshaftPart pickPiece(StructurePiecesHolder structurePiecesHolder, Random random, int x, int y, int z, @Nullable Direction orientation, int chainLength, MineshaftFeature.Type type) {
      int i = random.nextInt(100);
      BlockBox blockBox2;
      if (i >= 80) {
         blockBox2 = MineshaftGenerator.MineshaftCrossing.getBoundingBox(structurePiecesHolder, random, x, y, z, orientation);
         if (blockBox2 != null) {
            return new MineshaftGenerator.MineshaftCrossing(chainLength, blockBox2, orientation, type);
         }
      } else if (i >= 70) {
         blockBox2 = MineshaftGenerator.MineshaftStairs.getBoundingBox(structurePiecesHolder, random, x, y, z, orientation);
         if (blockBox2 != null) {
            return new MineshaftGenerator.MineshaftStairs(chainLength, blockBox2, orientation, type);
         }
      } else {
         blockBox2 = MineshaftGenerator.MineshaftCorridor.getBoundingBox(structurePiecesHolder, random, x, y, z, orientation);
         if (blockBox2 != null) {
            return new MineshaftGenerator.MineshaftCorridor(chainLength, random, blockBox2, orientation, type);
         }
      }

      return null;
   }

   static MineshaftGenerator.MineshaftPart pieceGenerator(StructurePiece start, StructurePiecesHolder structurePiecesHolder, Random random, int x, int y, int z, Direction orientation, int chainLength) {
      if (chainLength > 8) {
         return null;
      } else if (Math.abs(x - start.getBoundingBox().getMinX()) <= 80 && Math.abs(z - start.getBoundingBox().getMinZ()) <= 80) {
         MineshaftFeature.Type type = ((MineshaftGenerator.MineshaftPart)start).mineshaftType;
         MineshaftGenerator.MineshaftPart mineshaftPart = pickPiece(structurePiecesHolder, random, x, y, z, orientation, chainLength + 1, type);
         if (mineshaftPart != null) {
            structurePiecesHolder.addPiece(mineshaftPart);
            mineshaftPart.fillOpenings(start, structurePiecesHolder, random);
         }

         return mineshaftPart;
      } else {
         return null;
      }
   }

   public static class MineshaftCrossing extends MineshaftGenerator.MineshaftPart {
      private final Direction direction;
      private final boolean twoFloors;

      public MineshaftCrossing(ServerWorld world, NbtCompound nbt) {
         super(StructurePieceType.MINESHAFT_CROSSING, nbt);
         this.twoFloors = nbt.getBoolean("tf");
         this.direction = Direction.fromHorizontal(nbt.getInt("D"));
      }

      protected void writeNbt(ServerWorld world, NbtCompound nbt) {
         super.writeNbt(world, nbt);
         nbt.putBoolean("tf", this.twoFloors);
         nbt.putInt("D", this.direction.getHorizontal());
      }

      public MineshaftCrossing(int chainLength, BlockBox boundingBox, @Nullable Direction orientation, MineshaftFeature.Type type) {
         super(StructurePieceType.MINESHAFT_CROSSING, chainLength, type, boundingBox);
         this.direction = orientation;
         this.twoFloors = boundingBox.getBlockCountY() > 3;
      }

      @Nullable
      public static BlockBox getBoundingBox(StructurePiecesHolder structurePiecesHolder, Random random, int x, int y, int z, Direction orientation) {
         byte j;
         if (random.nextInt(4) == 0) {
            j = 6;
         } else {
            j = 2;
         }

         BlockBox blockBox4;
         switch(orientation) {
         case NORTH:
         default:
            blockBox4 = new BlockBox(-1, 0, -4, 3, j, 0);
            break;
         case SOUTH:
            blockBox4 = new BlockBox(-1, 0, 0, 3, j, 4);
            break;
         case WEST:
            blockBox4 = new BlockBox(-4, 0, -1, 0, j, 3);
            break;
         case EAST:
            blockBox4 = new BlockBox(0, 0, -1, 4, j, 3);
         }

         blockBox4.move(x, y, z);
         return structurePiecesHolder.getIntersecting(blockBox4) != null ? null : blockBox4;
      }

      public void fillOpenings(StructurePiece start, StructurePiecesHolder structurePiecesHolder, Random random) {
         int i = this.getChainLength();
         switch(this.direction) {
         case NORTH:
         default:
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() - 1, Direction.NORTH, i);
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, Direction.WEST, i);
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, Direction.EAST, i);
            break;
         case SOUTH:
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMaxZ() + 1, Direction.SOUTH, i);
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, Direction.WEST, i);
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, Direction.EAST, i);
            break;
         case WEST:
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() - 1, Direction.NORTH, i);
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMaxZ() + 1, Direction.SOUTH, i);
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, Direction.WEST, i);
            break;
         case EAST:
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() - 1, Direction.NORTH, i);
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMaxZ() + 1, Direction.SOUTH, i);
            MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, Direction.EAST, i);
         }

         if (this.twoFloors) {
            if (random.nextBoolean()) {
               MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY() + 3 + 1, this.boundingBox.getMinZ() - 1, Direction.NORTH, i);
            }

            if (random.nextBoolean()) {
               MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY() + 3 + 1, this.boundingBox.getMinZ() + 1, Direction.WEST, i);
            }

            if (random.nextBoolean()) {
               MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY() + 3 + 1, this.boundingBox.getMinZ() + 1, Direction.EAST, i);
            }

            if (random.nextBoolean()) {
               MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY() + 3 + 1, this.boundingBox.getMaxZ() + 1, Direction.SOUTH, i);
            }
         }

      }

      public boolean generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox boundingBox, ChunkPos chunkPos, BlockPos pos) {
         if (this.method_33999(world, boundingBox)) {
            return false;
         } else {
            BlockState blockState = this.mineshaftType.getPlanks();
            if (this.twoFloors) {
               this.fillWithOutline(world, boundingBox, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ(), this.boundingBox.getMaxX() - 1, this.boundingBox.getMinY() + 3 - 1, this.boundingBox.getMaxZ(), AIR, AIR, false);
               this.fillWithOutline(world, boundingBox, this.boundingBox.getMinX(), this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, this.boundingBox.getMaxX(), this.boundingBox.getMinY() + 3 - 1, this.boundingBox.getMaxZ() - 1, AIR, AIR, false);
               this.fillWithOutline(world, boundingBox, this.boundingBox.getMinX() + 1, this.boundingBox.getMaxY() - 2, this.boundingBox.getMinZ(), this.boundingBox.getMaxX() - 1, this.boundingBox.getMaxY(), this.boundingBox.getMaxZ(), AIR, AIR, false);
               this.fillWithOutline(world, boundingBox, this.boundingBox.getMinX(), this.boundingBox.getMaxY() - 2, this.boundingBox.getMinZ() + 1, this.boundingBox.getMaxX(), this.boundingBox.getMaxY(), this.boundingBox.getMaxZ() - 1, AIR, AIR, false);
               this.fillWithOutline(world, boundingBox, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY() + 3, this.boundingBox.getMinZ() + 1, this.boundingBox.getMaxX() - 1, this.boundingBox.getMinY() + 3, this.boundingBox.getMaxZ() - 1, AIR, AIR, false);
            } else {
               this.fillWithOutline(world, boundingBox, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ(), this.boundingBox.getMaxX() - 1, this.boundingBox.getMaxY(), this.boundingBox.getMaxZ(), AIR, AIR, false);
               this.fillWithOutline(world, boundingBox, this.boundingBox.getMinX(), this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, this.boundingBox.getMaxX(), this.boundingBox.getMaxY(), this.boundingBox.getMaxZ() - 1, AIR, AIR, false);
            }

            this.generateCrossingPillar(world, boundingBox, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, this.boundingBox.getMaxY());
            this.generateCrossingPillar(world, boundingBox, this.boundingBox.getMinX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMaxZ() - 1, this.boundingBox.getMaxY());
            this.generateCrossingPillar(world, boundingBox, this.boundingBox.getMaxX() - 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ() + 1, this.boundingBox.getMaxY());
            this.generateCrossingPillar(world, boundingBox, this.boundingBox.getMaxX() - 1, this.boundingBox.getMinY(), this.boundingBox.getMaxZ() - 1, this.boundingBox.getMaxY());
            int i = this.boundingBox.getMinY() - 1;

            for(int j = this.boundingBox.getMinX(); j <= this.boundingBox.getMaxX(); ++j) {
               for(int k = this.boundingBox.getMinZ(); k <= this.boundingBox.getMaxZ(); ++k) {
                  this.method_33880(world, boundingBox, blockState, j, i, k);
               }
            }

            return true;
         }
      }

      private void generateCrossingPillar(StructureWorldAccess world, BlockBox boundingBox, int x, int minY, int z, int maxY) {
         if (!this.getBlockAt(world, x, maxY + 1, z, boundingBox).isAir()) {
            this.fillWithOutline(world, boundingBox, x, minY, z, x, maxY, z, this.mineshaftType.getPlanks(), AIR, false);
         }

      }
   }

   public static class MineshaftStairs extends MineshaftGenerator.MineshaftPart {
      public MineshaftStairs(int chainLength, BlockBox boundingBox, Direction orientation, MineshaftFeature.Type type) {
         super(StructurePieceType.MINESHAFT_STAIRS, chainLength, type, boundingBox);
         this.setOrientation(orientation);
      }

      public MineshaftStairs(ServerWorld world, NbtCompound nbt) {
         super(StructurePieceType.MINESHAFT_STAIRS, nbt);
      }

      @Nullable
      public static BlockBox getBoundingBox(StructurePiecesHolder structurePiecesHolder, Random random, int x, int y, int z, Direction orientation) {
         BlockBox blockBox4;
         switch(orientation) {
         case NORTH:
         default:
            blockBox4 = new BlockBox(0, -5, -8, 2, 2, 0);
            break;
         case SOUTH:
            blockBox4 = new BlockBox(0, -5, 0, 2, 2, 8);
            break;
         case WEST:
            blockBox4 = new BlockBox(-8, -5, 0, 0, 2, 2);
            break;
         case EAST:
            blockBox4 = new BlockBox(0, -5, 0, 8, 2, 2);
         }

         blockBox4.move(x, y, z);
         return structurePiecesHolder.getIntersecting(blockBox4) != null ? null : blockBox4;
      }

      public void fillOpenings(StructurePiece start, StructurePiecesHolder structurePiecesHolder, Random random) {
         int i = this.getChainLength();
         Direction direction = this.getFacing();
         if (direction != null) {
            switch(direction) {
            case NORTH:
            default:
               MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX(), this.boundingBox.getMinY(), this.boundingBox.getMinZ() - 1, Direction.NORTH, i);
               break;
            case SOUTH:
               MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX(), this.boundingBox.getMinY(), this.boundingBox.getMaxZ() + 1, Direction.SOUTH, i);
               break;
            case WEST:
               MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ(), Direction.WEST, i);
               break;
            case EAST:
               MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY(), this.boundingBox.getMinZ(), Direction.EAST, i);
            }
         }

      }

      public boolean generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox boundingBox, ChunkPos chunkPos, BlockPos pos) {
         if (this.method_33999(world, boundingBox)) {
            return false;
         } else {
            this.fillWithOutline(world, boundingBox, 0, 5, 0, 2, 7, 1, AIR, AIR, false);
            this.fillWithOutline(world, boundingBox, 0, 0, 7, 2, 2, 8, AIR, AIR, false);

            for(int i = 0; i < 5; ++i) {
               this.fillWithOutline(world, boundingBox, 0, 5 - i - (i < 4 ? 1 : 0), 2 + i, 2, 7 - i, 2 + i, AIR, AIR, false);
            }

            return true;
         }
      }
   }

   public static class MineshaftCorridor extends MineshaftGenerator.MineshaftPart {
      private final boolean hasRails;
      private final boolean hasCobwebs;
      private boolean hasSpawner;
      private final int length;

      public MineshaftCorridor(ServerWorld world, NbtCompound nbt) {
         super(StructurePieceType.MINESHAFT_CORRIDOR, nbt);
         this.hasRails = nbt.getBoolean("hr");
         this.hasCobwebs = nbt.getBoolean("sc");
         this.hasSpawner = nbt.getBoolean("hps");
         this.length = nbt.getInt("Num");
      }

      protected void writeNbt(ServerWorld world, NbtCompound nbt) {
         super.writeNbt(world, nbt);
         nbt.putBoolean("hr", this.hasRails);
         nbt.putBoolean("sc", this.hasCobwebs);
         nbt.putBoolean("hps", this.hasSpawner);
         nbt.putInt("Num", this.length);
      }

      public MineshaftCorridor(int chainLength, Random random, BlockBox boundingBox, Direction orientation, MineshaftFeature.Type type) {
         super(StructurePieceType.MINESHAFT_CORRIDOR, chainLength, type, boundingBox);
         this.setOrientation(orientation);
         this.hasRails = random.nextInt(3) == 0;
         this.hasCobwebs = !this.hasRails && random.nextInt(23) == 0;
         if (this.getFacing().getAxis() == Direction.Axis.Z) {
            this.length = boundingBox.getBlockCountZ() / 5;
         } else {
            this.length = boundingBox.getBlockCountX() / 5;
         }

      }

      @Nullable
      public static BlockBox getBoundingBox(StructurePiecesHolder structurePiecesHolder, Random random, int x, int y, int z, Direction orientation) {
         for(int i = random.nextInt(3) + 2; i > 0; --i) {
            int j = i * 5;
            BlockBox blockBox4;
            switch(orientation) {
            case NORTH:
            default:
               blockBox4 = new BlockBox(0, 0, -(j - 1), 2, 2, 0);
               break;
            case SOUTH:
               blockBox4 = new BlockBox(0, 0, 0, 2, 2, j - 1);
               break;
            case WEST:
               blockBox4 = new BlockBox(-(j - 1), 0, 0, 0, 2, 2);
               break;
            case EAST:
               blockBox4 = new BlockBox(0, 0, 0, j - 1, 2, 2);
            }

            blockBox4.move(x, y, z);
            if (structurePiecesHolder.getIntersecting(blockBox4) == null) {
               return blockBox4;
            }
         }

         return null;
      }

      public void fillOpenings(StructurePiece start, StructurePiecesHolder structurePiecesHolder, Random random) {
         int i = this.getChainLength();
         int j = random.nextInt(4);
         Direction direction = this.getFacing();
         if (direction != null) {
            switch(direction) {
            case NORTH:
            default:
               if (j <= 1) {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX(), this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMinZ() - 1, direction, i);
               } else if (j == 2) {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMinZ(), Direction.WEST, i);
               } else {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMinZ(), Direction.EAST, i);
               }
               break;
            case SOUTH:
               if (j <= 1) {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX(), this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMaxZ() + 1, direction, i);
               } else if (j == 2) {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMaxZ() - 3, Direction.WEST, i);
               } else {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMaxZ() - 3, Direction.EAST, i);
               }
               break;
            case WEST:
               if (j <= 1) {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMinZ(), direction, i);
               } else if (j == 2) {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX(), this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMinZ() - 1, Direction.NORTH, i);
               } else {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX(), this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMaxZ() + 1, Direction.SOUTH, i);
               }
               break;
            case EAST:
               if (j <= 1) {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMinZ(), direction, i);
               } else if (j == 2) {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() - 3, this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMinZ() - 1, Direction.NORTH, i);
               } else {
                  MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() - 3, this.boundingBox.getMinY() - 1 + random.nextInt(3), this.boundingBox.getMaxZ() + 1, Direction.SOUTH, i);
               }
            }
         }

         if (i < 8) {
            int k;
            int l;
            if (direction != Direction.NORTH && direction != Direction.SOUTH) {
               for(k = this.boundingBox.getMinX() + 3; k + 3 <= this.boundingBox.getMaxX(); k += 5) {
                  l = random.nextInt(5);
                  if (l == 0) {
                     MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, k, this.boundingBox.getMinY(), this.boundingBox.getMinZ() - 1, Direction.NORTH, i + 1);
                  } else if (l == 1) {
                     MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, k, this.boundingBox.getMinY(), this.boundingBox.getMaxZ() + 1, Direction.SOUTH, i + 1);
                  }
               }
            } else {
               for(k = this.boundingBox.getMinZ() + 3; k + 3 <= this.boundingBox.getMaxZ(); k += 5) {
                  l = random.nextInt(5);
                  if (l == 0) {
                     MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY(), k, Direction.WEST, i + 1);
                  } else if (l == 1) {
                     MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY(), k, Direction.EAST, i + 1);
                  }
               }
            }
         }

      }

      protected boolean addChest(StructureWorldAccess world, BlockBox boundingBox, Random random, int x, int y, int z, Identifier lootTableId) {
         BlockPos blockPos = this.offsetPos(x, y, z);
         if (boundingBox.contains(blockPos) && world.getBlockState(blockPos).isAir() && !world.getBlockState(blockPos.down()).isAir()) {
            BlockState blockState = (BlockState)Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, random.nextBoolean() ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST);
            this.addBlock(world, blockState, x, y, z, boundingBox);
            ChestMinecartEntity chestMinecartEntity = new ChestMinecartEntity(world.toServerWorld(), (double)blockPos.getX() + 0.5D, (double)blockPos.getY() + 0.5D, (double)blockPos.getZ() + 0.5D);
            chestMinecartEntity.setLootTable(lootTableId, random.nextLong());
            world.spawnEntity(chestMinecartEntity);
            return true;
         } else {
            return false;
         }
      }

      public boolean generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox boundingBox, ChunkPos chunkPos, BlockPos pos) {
         if (this.method_33999(world, boundingBox)) {
            return false;
         } else {
            int i = false;
            int j = true;
            int k = false;
            int l = true;
            int m = this.length * 5 - 1;
            BlockState blockState = this.mineshaftType.getPlanks();
            this.fillWithOutline(world, boundingBox, 0, 0, 0, 2, 1, m, AIR, AIR, false);
            this.fillWithOutlineUnderSeaLevel(world, boundingBox, random, 0.8F, 0, 2, 0, 2, 2, m, AIR, AIR, false, false);
            if (this.hasCobwebs) {
               this.fillWithOutlineUnderSeaLevel(world, boundingBox, random, 0.6F, 0, 0, 0, 2, 1, m, Blocks.COBWEB.getDefaultState(), AIR, false, true);
            }

            int r;
            int o;
            for(r = 0; r < this.length; ++r) {
               o = 2 + r * 5;
               this.generateSupports(world, boundingBox, 0, 0, o, 2, 2, random);
               this.addCobwebsUnderground(world, boundingBox, random, 0.1F, 0, 2, o - 1);
               this.addCobwebsUnderground(world, boundingBox, random, 0.1F, 2, 2, o - 1);
               this.addCobwebsUnderground(world, boundingBox, random, 0.1F, 0, 2, o + 1);
               this.addCobwebsUnderground(world, boundingBox, random, 0.1F, 2, 2, o + 1);
               this.addCobwebsUnderground(world, boundingBox, random, 0.05F, 0, 2, o - 2);
               this.addCobwebsUnderground(world, boundingBox, random, 0.05F, 2, 2, o - 2);
               this.addCobwebsUnderground(world, boundingBox, random, 0.05F, 0, 2, o + 2);
               this.addCobwebsUnderground(world, boundingBox, random, 0.05F, 2, 2, o + 2);
               if (random.nextInt(100) == 0) {
                  this.addChest(world, boundingBox, random, 2, 0, o - 1, LootTables.ABANDONED_MINESHAFT_CHEST);
               }

               if (random.nextInt(100) == 0) {
                  this.addChest(world, boundingBox, random, 0, 0, o + 1, LootTables.ABANDONED_MINESHAFT_CHEST);
               }

               if (this.hasCobwebs && !this.hasSpawner) {
                  int p = true;
                  int q = o - 1 + random.nextInt(3);
                  BlockPos blockPos = this.offsetPos(1, 0, q);
                  if (boundingBox.contains(blockPos) && this.isUnderSeaLevel(world, 1, 0, q, boundingBox)) {
                     this.hasSpawner = true;
                     world.setBlockState(blockPos, Blocks.SPAWNER.getDefaultState(), Block.NOTIFY_LISTENERS);
                     BlockEntity blockEntity = world.getBlockEntity(blockPos);
                     if (blockEntity instanceof MobSpawnerBlockEntity) {
                        ((MobSpawnerBlockEntity)blockEntity).getLogic().setEntityId(EntityType.CAVE_SPIDER);
                     }
                  }
               }
            }

            for(r = 0; r <= 2; ++r) {
               for(o = 0; o <= m; ++o) {
                  this.method_33880(world, boundingBox, blockState, r, -1, o);
               }
            }

            int t = true;
            this.fillSupportBeam(world, boundingBox, 0, -1, 2);
            if (this.length > 1) {
               o = m - 2;
               this.fillSupportBeam(world, boundingBox, 0, -1, o);
            }

            if (this.hasRails) {
               BlockState blockState2 = (BlockState)Blocks.RAIL.getDefaultState().with(RailBlock.SHAPE, RailShape.NORTH_SOUTH);

               for(int v = 0; v <= m; ++v) {
                  BlockState blockState3 = this.getBlockAt(world, 1, -1, v, boundingBox);
                  if (!blockState3.isAir() && blockState3.isOpaqueFullCube(world, this.offsetPos(1, -1, v))) {
                     float f = this.isUnderSeaLevel(world, 1, 0, v, boundingBox) ? 0.7F : 0.9F;
                     this.addBlockWithRandomThreshold(world, boundingBox, random, f, 1, 0, v, blockState2);
                  }
               }
            }

            return true;
         }
      }

      private void fillSupportBeam(StructureWorldAccess world, BlockBox box, int x, int y, int z) {
         BlockState blockState = this.mineshaftType.getLog();
         BlockState blockState2 = this.mineshaftType.getPlanks();
         if (this.getBlockAt(world, x, y, z, box).isOf(blockState2.getBlock())) {
            this.method_33879(world, blockState, x, y, z, box);
         }

         if (this.getBlockAt(world, x + 2, y, z, box).isOf(blockState2.getBlock())) {
            this.method_33879(world, blockState, x + 2, y, z, box);
         }

      }

      protected void fillDownwards(StructureWorldAccess world, BlockState state, int x, int y, int z, BlockBox box) {
         BlockPos.Mutable mutable = this.offsetPos(x, y, z);
         if (box.contains(mutable)) {
            int i = mutable.getY();

            while(this.canReplace(world.getBlockState(mutable)) && mutable.getY() > world.getBottomY() + 1) {
               mutable.move(Direction.DOWN);
            }

            if (this.isNotRailOrLava(world.getBlockState(mutable))) {
               while(mutable.getY() < i) {
                  mutable.move(Direction.UP);
                  world.setBlockState(mutable, state, Block.NOTIFY_LISTENERS);
               }

            }
         }
      }

      protected void method_33879(StructureWorldAccess world, BlockState state, int x, int y, int z, BlockBox box) {
         BlockPos.Mutable mutable = this.offsetPos(x, y, z);
         if (box.contains(mutable)) {
            int i = mutable.getY();
            int j = 1;
            boolean bl = true;

            for(boolean bl2 = true; bl || bl2; ++j) {
               BlockState blockState2;
               boolean bl4;
               if (bl) {
                  mutable.setY(i - j);
                  blockState2 = world.getBlockState(mutable);
                  bl4 = this.canReplace(blockState2) && !blockState2.isOf(Blocks.LAVA);
                  if (!bl4 && this.isNotRailOrLava(blockState2)) {
                     fillColumn(world, state, mutable, i - j + 1, i);
                     return;
                  }

                  bl = j <= 20 && bl4 && mutable.getY() > world.getBottomY() + 1;
               }

               if (bl2) {
                  mutable.setY(i + j);
                  blockState2 = world.getBlockState(mutable);
                  bl4 = this.canReplace(blockState2);
                  if (!bl4 && this.sideCoversSmallSquare(world, mutable, blockState2)) {
                     world.setBlockState(mutable.setY(i + 1), this.mineshaftType.getFence(), Block.NOTIFY_LISTENERS);
                     fillColumn(world, Blocks.CHAIN.getDefaultState(), mutable, i + 2, i + j);
                     return;
                  }

                  bl2 = j <= 50 && bl4 && mutable.getY() < world.getTopY() - 1;
               }
            }

         }
      }

      private static void fillColumn(StructureWorldAccess world, BlockState state, BlockPos.Mutable pos, int startY, int endY) {
         for(int i = startY; i < endY; ++i) {
            world.setBlockState(pos.setY(i), state, Block.NOTIFY_LISTENERS);
         }

      }

      private boolean isNotRailOrLava(BlockState state) {
         return !state.isOf(Blocks.RAIL) && !state.isOf(Blocks.LAVA);
      }

      private boolean sideCoversSmallSquare(WorldView world, BlockPos pos, BlockState state) {
         return Block.sideCoversSmallSquare(world, pos, Direction.DOWN) && !(state.getBlock() instanceof FallingBlock);
      }

      private void generateSupports(StructureWorldAccess world, BlockBox boundingBox, int minX, int minY, int z, int maxY, int maxX, Random random) {
         if (this.isSolidCeiling(world, boundingBox, minX, maxX, maxY, z)) {
            BlockState blockState = this.mineshaftType.getPlanks();
            BlockState blockState2 = this.mineshaftType.getFence();
            this.fillWithOutline(world, boundingBox, minX, minY, z, minX, maxY - 1, z, (BlockState)blockState2.with(FenceBlock.WEST, true), AIR, false);
            this.fillWithOutline(world, boundingBox, maxX, minY, z, maxX, maxY - 1, z, (BlockState)blockState2.with(FenceBlock.EAST, true), AIR, false);
            if (random.nextInt(4) == 0) {
               this.fillWithOutline(world, boundingBox, minX, maxY, z, minX, maxY, z, blockState, AIR, false);
               this.fillWithOutline(world, boundingBox, maxX, maxY, z, maxX, maxY, z, blockState, AIR, false);
            } else {
               this.fillWithOutline(world, boundingBox, minX, maxY, z, maxX, maxY, z, blockState, AIR, false);
               this.addBlockWithRandomThreshold(world, boundingBox, random, 0.05F, minX + 1, maxY, z - 1, (BlockState)Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, Direction.NORTH));
               this.addBlockWithRandomThreshold(world, boundingBox, random, 0.05F, minX + 1, maxY, z + 1, (BlockState)Blocks.WALL_TORCH.getDefaultState().with(WallTorchBlock.FACING, Direction.SOUTH));
            }

         }
      }

      private void addCobwebsUnderground(StructureWorldAccess world, BlockBox box, Random random, float threshold, int x, int y, int z) {
         if (this.isUnderSeaLevel(world, x, y, z, box) && random.nextFloat() < threshold && this.method_36422(world, box, x, y, z, 2)) {
            this.addBlock(world, Blocks.COBWEB.getDefaultState(), x, y, z, box);
         }

      }

      private boolean method_36422(StructureWorldAccess world, BlockBox box, int x, int y, int z, int count) {
         BlockPos.Mutable mutable = this.offsetPos(x, y, z);
         int i = 0;
         Direction[] var9 = Direction.values();
         int var10 = var9.length;

         for(int var11 = 0; var11 < var10; ++var11) {
            Direction direction = var9[var11];
            mutable.move(direction);
            if (box.contains(mutable) && world.getBlockState(mutable).isSideSolidFullSquare(world, mutable, direction.getOpposite())) {
               ++i;
               if (i >= count) {
                  return true;
               }
            }

            mutable.move(direction.getOpposite());
         }

         return false;
      }
   }

   private abstract static class MineshaftPart extends StructurePiece {
      protected MineshaftFeature.Type mineshaftType;

      public MineshaftPart(StructurePieceType structurePieceType, int chainLength, MineshaftFeature.Type type, BlockBox box) {
         super(structurePieceType, chainLength, box);
         this.mineshaftType = type;
      }

      public MineshaftPart(StructurePieceType structurePieceType, NbtCompound nbtCompound) {
         super(structurePieceType, nbtCompound);
         this.mineshaftType = MineshaftFeature.Type.byIndex(nbtCompound.getInt("MST"));
      }

      protected boolean canAddBlock(WorldView world, int x, int y, int z, BlockBox box) {
         BlockState blockState = this.getBlockAt(world, x, y, z, box);
         return !blockState.isOf(this.mineshaftType.getPlanks().getBlock()) && !blockState.isOf(this.mineshaftType.getLog().getBlock()) && !blockState.isOf(this.mineshaftType.getFence().getBlock()) && !blockState.isOf(Blocks.CHAIN);
      }

      protected void writeNbt(ServerWorld world, NbtCompound nbt) {
         nbt.putInt("MST", this.mineshaftType.ordinal());
      }

      protected boolean isSolidCeiling(BlockView world, BlockBox boundingBox, int minX, int maxX, int y, int z) {
         for(int i = minX; i <= maxX; ++i) {
            if (this.getBlockAt(world, i, y + 1, z, boundingBox).isAir()) {
               return false;
            }
         }

         return true;
      }

      protected boolean method_33999(BlockView world, BlockBox box) {
         int i = Math.max(this.boundingBox.getMinX() - 1, box.getMinX());
         int j = Math.max(this.boundingBox.getMinY() - 1, box.getMinY());
         int k = Math.max(this.boundingBox.getMinZ() - 1, box.getMinZ());
         int l = Math.min(this.boundingBox.getMaxX() + 1, box.getMaxX());
         int m = Math.min(this.boundingBox.getMaxY() + 1, box.getMaxY());
         int n = Math.min(this.boundingBox.getMaxZ() + 1, box.getMaxZ());
         BlockPos.Mutable mutable = new BlockPos.Mutable();

         int s;
         int t;
         for(s = i; s <= l; ++s) {
            for(t = k; t <= n; ++t) {
               if (world.getBlockState(mutable.set(s, j, t)).getMaterial().isLiquid()) {
                  return true;
               }

               if (world.getBlockState(mutable.set(s, m, t)).getMaterial().isLiquid()) {
                  return true;
               }
            }
         }

         for(s = i; s <= l; ++s) {
            for(t = j; t <= m; ++t) {
               if (world.getBlockState(mutable.set(s, t, k)).getMaterial().isLiquid()) {
                  return true;
               }

               if (world.getBlockState(mutable.set(s, t, n)).getMaterial().isLiquid()) {
                  return true;
               }
            }
         }

         for(s = k; s <= n; ++s) {
            for(t = j; t <= m; ++t) {
               if (world.getBlockState(mutable.set(i, t, s)).getMaterial().isLiquid()) {
                  return true;
               }

               if (world.getBlockState(mutable.set(l, t, s)).getMaterial().isLiquid()) {
                  return true;
               }
            }
         }

         return false;
      }

      protected void method_33880(StructureWorldAccess world, BlockBox box, BlockState state, int x, int y, int z) {
         if (this.isUnderSeaLevel(world, x, y, z, box)) {
            BlockPos blockPos = this.offsetPos(x, y, z);
            BlockState blockState = world.getBlockState(blockPos);
            if (blockState.isAir() || blockState.isOf(Blocks.CHAIN)) {
               world.setBlockState(blockPos, state, Block.NOTIFY_LISTENERS);
            }

         }
      }
   }

   public static class MineshaftRoom extends MineshaftGenerator.MineshaftPart {
      private final List<BlockBox> entrances = Lists.newLinkedList();

      public MineshaftRoom(int chainLength, Random random, int x, int z, MineshaftFeature.Type type) {
         super(StructurePieceType.MINESHAFT_ROOM, chainLength, type, new BlockBox(x, 50, z, x + 7 + random.nextInt(6), 54 + random.nextInt(6), z + 7 + random.nextInt(6)));
         this.mineshaftType = type;
      }

      public MineshaftRoom(ServerWorld world, NbtCompound nbt) {
         super(StructurePieceType.MINESHAFT_ROOM, nbt);
         DataResult var10000 = BlockBox.CODEC.listOf().parse(NbtOps.INSTANCE, nbt.getList("Entrances", 11));
         Logger var10001 = MineshaftGenerator.LOGGER;
         Objects.requireNonNull(var10001);
         Optional var3 = var10000.resultOrPartial(var10001::error);
         List var4 = this.entrances;
         Objects.requireNonNull(var4);
         var3.ifPresent(var4::addAll);
      }

      public void fillOpenings(StructurePiece start, StructurePiecesHolder structurePiecesHolder, Random random) {
         int i = this.getChainLength();
         int j = this.boundingBox.getBlockCountY() - 3 - 1;
         if (j <= 0) {
            j = 1;
         }

         int k;
         MineshaftGenerator.MineshaftPart structurePiece;
         BlockBox blockBox4;
         for(k = 0; k < this.boundingBox.getBlockCountX(); k += 4) {
            k += random.nextInt(this.boundingBox.getBlockCountX());
            if (k + 3 > this.boundingBox.getBlockCountX()) {
               break;
            }

            structurePiece = MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + k, this.boundingBox.getMinY() + random.nextInt(j) + 1, this.boundingBox.getMinZ() - 1, Direction.NORTH, i);
            if (structurePiece != null) {
               blockBox4 = structurePiece.getBoundingBox();
               this.entrances.add(new BlockBox(blockBox4.getMinX(), blockBox4.getMinY(), this.boundingBox.getMinZ(), blockBox4.getMaxX(), blockBox4.getMaxY(), this.boundingBox.getMinZ() + 1));
            }
         }

         for(k = 0; k < this.boundingBox.getBlockCountX(); k += 4) {
            k += random.nextInt(this.boundingBox.getBlockCountX());
            if (k + 3 > this.boundingBox.getBlockCountX()) {
               break;
            }

            structurePiece = MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() + k, this.boundingBox.getMinY() + random.nextInt(j) + 1, this.boundingBox.getMaxZ() + 1, Direction.SOUTH, i);
            if (structurePiece != null) {
               blockBox4 = structurePiece.getBoundingBox();
               this.entrances.add(new BlockBox(blockBox4.getMinX(), blockBox4.getMinY(), this.boundingBox.getMaxZ() - 1, blockBox4.getMaxX(), blockBox4.getMaxY(), this.boundingBox.getMaxZ()));
            }
         }

         for(k = 0; k < this.boundingBox.getBlockCountZ(); k += 4) {
            k += random.nextInt(this.boundingBox.getBlockCountZ());
            if (k + 3 > this.boundingBox.getBlockCountZ()) {
               break;
            }

            structurePiece = MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMinX() - 1, this.boundingBox.getMinY() + random.nextInt(j) + 1, this.boundingBox.getMinZ() + k, Direction.WEST, i);
            if (structurePiece != null) {
               blockBox4 = structurePiece.getBoundingBox();
               this.entrances.add(new BlockBox(this.boundingBox.getMinX(), blockBox4.getMinY(), blockBox4.getMinZ(), this.boundingBox.getMinX() + 1, blockBox4.getMaxY(), blockBox4.getMaxZ()));
            }
         }

         for(k = 0; k < this.boundingBox.getBlockCountZ(); k += 4) {
            k += random.nextInt(this.boundingBox.getBlockCountZ());
            if (k + 3 > this.boundingBox.getBlockCountZ()) {
               break;
            }

            structurePiece = MineshaftGenerator.pieceGenerator(start, structurePiecesHolder, random, this.boundingBox.getMaxX() + 1, this.boundingBox.getMinY() + random.nextInt(j) + 1, this.boundingBox.getMinZ() + k, Direction.EAST, i);
            if (structurePiece != null) {
               blockBox4 = structurePiece.getBoundingBox();
               this.entrances.add(new BlockBox(this.boundingBox.getMaxX() - 1, blockBox4.getMinY(), blockBox4.getMinZ(), this.boundingBox.getMaxX(), blockBox4.getMaxY(), blockBox4.getMaxZ()));
            }
         }

      }

      public boolean generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox boundingBox, ChunkPos chunkPos, BlockPos pos) {
         if (this.method_33999(world, boundingBox)) {
            return false;
         } else {
            this.fillWithOutline(world, boundingBox, this.boundingBox.getMinX(), this.boundingBox.getMinY(), this.boundingBox.getMinZ(), this.boundingBox.getMaxX(), this.boundingBox.getMinY(), this.boundingBox.getMaxZ(), Blocks.DIRT.getDefaultState(), AIR, true);
            this.fillWithOutline(world, boundingBox, this.boundingBox.getMinX(), this.boundingBox.getMinY() + 1, this.boundingBox.getMinZ(), this.boundingBox.getMaxX(), Math.min(this.boundingBox.getMinY() + 3, this.boundingBox.getMaxY()), this.boundingBox.getMaxZ(), AIR, AIR, false);
            Iterator var8 = this.entrances.iterator();

            while(var8.hasNext()) {
               BlockBox blockBox = (BlockBox)var8.next();
               this.fillWithOutline(world, boundingBox, blockBox.getMinX(), blockBox.getMaxY() - 2, blockBox.getMinZ(), blockBox.getMaxX(), blockBox.getMaxY(), blockBox.getMaxZ(), AIR, AIR, false);
            }

            this.fillHalfEllipsoid(world, boundingBox, this.boundingBox.getMinX(), this.boundingBox.getMinY() + 4, this.boundingBox.getMinZ(), this.boundingBox.getMaxX(), this.boundingBox.getMaxY(), this.boundingBox.getMaxZ(), AIR, false);
            return true;
         }
      }

      public void translate(int x, int y, int z) {
         super.translate(x, y, z);
         Iterator var4 = this.entrances.iterator();

         while(var4.hasNext()) {
            BlockBox blockBox = (BlockBox)var4.next();
            blockBox.move(x, y, z);
         }

      }

      protected void writeNbt(ServerWorld world, NbtCompound nbt) {
         super.writeNbt(world, nbt);
         DataResult var10000 = BlockBox.CODEC.listOf().encodeStart(NbtOps.INSTANCE, this.entrances);
         Logger var10001 = MineshaftGenerator.LOGGER;
         Objects.requireNonNull(var10001);
         var10000.resultOrPartial(var10001::error).ifPresent((nbtElement) -> {
            nbt.put("Entrances", nbtElement);
         });
      }
   }
}
