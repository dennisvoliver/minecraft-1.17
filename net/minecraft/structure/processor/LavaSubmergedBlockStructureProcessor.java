package net.minecraft.structure.processor;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.structure.Structure;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class LavaSubmergedBlockStructureProcessor extends StructureProcessor {
   public static final Codec<LavaSubmergedBlockStructureProcessor> CODEC = Codec.unit(() -> {
      return INSTANCE;
   });
   public static final LavaSubmergedBlockStructureProcessor INSTANCE = new LavaSubmergedBlockStructureProcessor();

   @Nullable
   public Structure.StructureBlockInfo process(WorldView world, BlockPos pos, BlockPos pivot, Structure.StructureBlockInfo structureBlockInfo, Structure.StructureBlockInfo structureBlockInfo2, StructurePlacementData data) {
      BlockPos blockPos = structureBlockInfo2.pos;
      boolean bl = world.getBlockState(blockPos).isOf(Blocks.LAVA);
      return bl && !Block.isShapeFullCube(structureBlockInfo2.state.getOutlineShape(world, blockPos)) ? new Structure.StructureBlockInfo(blockPos, Blocks.LAVA.getDefaultState(), structureBlockInfo2.nbt) : structureBlockInfo2;
   }

   protected StructureProcessorType<?> getType() {
      return StructureProcessorType.LAVA_SUBMERGED_BLOCK;
   }
}
