package net.minecraft.world.gen.feature;

import com.mojang.serialization.Codec;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.loot.LootTables;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class BonusChestFeature extends Feature<DefaultFeatureConfig> {
   public BonusChestFeature(Codec<DefaultFeatureConfig> codec) {
      super(codec);
   }

   public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
      Random random = context.getRandom();
      StructureWorldAccess structureWorldAccess = context.getWorld();
      ChunkPos chunkPos = new ChunkPos(context.getOrigin());
      List<Integer> list = (List)IntStream.rangeClosed(chunkPos.getStartX(), chunkPos.getEndX()).boxed().collect(Collectors.toList());
      Collections.shuffle(list, random);
      List<Integer> list2 = (List)IntStream.rangeClosed(chunkPos.getStartZ(), chunkPos.getEndZ()).boxed().collect(Collectors.toList());
      Collections.shuffle(list2, random);
      BlockPos.Mutable mutable = new BlockPos.Mutable();
      Iterator var8 = list.iterator();

      while(var8.hasNext()) {
         Integer integer = (Integer)var8.next();
         Iterator var10 = list2.iterator();

         while(var10.hasNext()) {
            Integer integer2 = (Integer)var10.next();
            mutable.set(integer, 0, integer2);
            BlockPos blockPos = structureWorldAccess.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, mutable);
            if (structureWorldAccess.isAir(blockPos) || structureWorldAccess.getBlockState(blockPos).getCollisionShape(structureWorldAccess, blockPos).isEmpty()) {
               structureWorldAccess.setBlockState(blockPos, Blocks.CHEST.getDefaultState(), Block.NOTIFY_LISTENERS);
               LootableContainerBlockEntity.setLootTable(structureWorldAccess, random, blockPos, LootTables.SPAWN_BONUS_CHEST);
               BlockState blockState = Blocks.TORCH.getDefaultState();
               Iterator var14 = Direction.Type.HORIZONTAL.iterator();

               while(var14.hasNext()) {
                  Direction direction = (Direction)var14.next();
                  BlockPos blockPos2 = blockPos.offset(direction);
                  if (blockState.canPlaceAt(structureWorldAccess, blockPos2)) {
                     structureWorldAccess.setBlockState(blockPos2, blockState, Block.NOTIFY_LISTENERS);
                  }
               }

               return true;
            }
         }
      }

      return false;
   }
}
