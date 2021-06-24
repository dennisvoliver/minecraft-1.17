package net.minecraft.structure;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SimpleStructurePiece extends StructurePiece {
   private static final Logger LOGGER = LogManager.getLogger();
   protected final String identifier;
   protected Structure structure;
   protected StructurePlacementData placementData;
   protected BlockPos pos;

   public SimpleStructurePiece(StructurePieceType type, int i, StructureManager structureManager, Identifier identifier, String string, StructurePlacementData placementData, BlockPos pos) {
      super(type, i, structureManager.getStructureOrBlank(identifier).calculateBoundingBox(placementData, pos));
      this.setOrientation(Direction.NORTH);
      this.identifier = string;
      this.pos = pos;
      this.structure = structureManager.getStructureOrBlank(identifier);
      this.placementData = placementData;
   }

   public SimpleStructurePiece(StructurePieceType type, NbtCompound nbtCompound, ServerWorld world, Function<Identifier, StructurePlacementData> function) {
      super(type, nbtCompound);
      this.setOrientation(Direction.NORTH);
      this.identifier = nbtCompound.getString("Template");
      this.pos = new BlockPos(nbtCompound.getInt("TPX"), nbtCompound.getInt("TPY"), nbtCompound.getInt("TPZ"));
      Identifier identifier = this.getId();
      this.structure = world.getStructureManager().getStructureOrBlank(identifier);
      this.placementData = (StructurePlacementData)function.apply(identifier);
      this.boundingBox = this.structure.calculateBoundingBox(this.placementData, this.pos);
   }

   protected Identifier getId() {
      return new Identifier(this.identifier);
   }

   protected void writeNbt(ServerWorld world, NbtCompound nbt) {
      nbt.putInt("TPX", this.pos.getX());
      nbt.putInt("TPY", this.pos.getY());
      nbt.putInt("TPZ", this.pos.getZ());
      nbt.putString("Template", this.identifier);
   }

   public boolean generate(StructureWorldAccess world, StructureAccessor structureAccessor, ChunkGenerator chunkGenerator, Random random, BlockBox boundingBox, ChunkPos chunkPos, BlockPos pos) {
      this.placementData.setBoundingBox(boundingBox);
      this.boundingBox = this.structure.calculateBoundingBox(this.placementData, this.pos);
      if (this.structure.place(world, this.pos, pos, this.placementData, random, Block.NOTIFY_LISTENERS)) {
         List<Structure.StructureBlockInfo> list = this.structure.getInfosForBlock(this.pos, this.placementData, Blocks.STRUCTURE_BLOCK);
         Iterator var9 = list.iterator();

         while(var9.hasNext()) {
            Structure.StructureBlockInfo structureBlockInfo = (Structure.StructureBlockInfo)var9.next();
            if (structureBlockInfo.nbt != null) {
               StructureBlockMode structureBlockMode = StructureBlockMode.valueOf(structureBlockInfo.nbt.getString("mode"));
               if (structureBlockMode == StructureBlockMode.DATA) {
                  this.handleMetadata(structureBlockInfo.nbt.getString("metadata"), structureBlockInfo.pos, world, random, boundingBox);
               }
            }
         }

         List<Structure.StructureBlockInfo> list2 = this.structure.getInfosForBlock(this.pos, this.placementData, Blocks.JIGSAW);
         Iterator var18 = list2.iterator();

         while(var18.hasNext()) {
            Structure.StructureBlockInfo structureBlockInfo2 = (Structure.StructureBlockInfo)var18.next();
            if (structureBlockInfo2.nbt != null) {
               String string = structureBlockInfo2.nbt.getString("final_state");
               BlockArgumentParser blockArgumentParser = new BlockArgumentParser(new StringReader(string), false);
               BlockState blockState = Blocks.AIR.getDefaultState();

               try {
                  blockArgumentParser.parse(true);
                  BlockState blockState2 = blockArgumentParser.getBlockState();
                  if (blockState2 != null) {
                     blockState = blockState2;
                  } else {
                     LOGGER.error((String)"Error while parsing blockstate {} in jigsaw block @ {}", (Object)string, (Object)structureBlockInfo2.pos);
                  }
               } catch (CommandSyntaxException var16) {
                  LOGGER.error((String)"Error while parsing blockstate {} in jigsaw block @ {}", (Object)string, (Object)structureBlockInfo2.pos);
               }

               world.setBlockState(structureBlockInfo2.pos, blockState, Block.NOTIFY_ALL);
            }
         }
      }

      return true;
   }

   protected abstract void handleMetadata(String metadata, BlockPos pos, ServerWorldAccess world, Random random, BlockBox boundingBox);

   public void translate(int x, int y, int z) {
      super.translate(x, y, z);
      this.pos = this.pos.add(x, y, z);
   }

   public BlockRotation getRotation() {
      return this.placementData.getRotation();
   }
}
