package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.block.BlockState;
import net.minecraft.block.VineBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.Feature;

public class TrunkVineTreeDecorator extends TreeDecorator {
   public static final Codec<TrunkVineTreeDecorator> CODEC = Codec.unit(() -> {
      return INSTANCE;
   });
   public static final TrunkVineTreeDecorator INSTANCE = new TrunkVineTreeDecorator();

   protected TreeDecoratorType<?> getType() {
      return TreeDecoratorType.TRUNK_VINE;
   }

   public void generate(TestableWorld world, BiConsumer<BlockPos, BlockState> replacer, Random random, List<BlockPos> logPositions, List<BlockPos> leavesPositions) {
      logPositions.forEach((pos) -> {
         BlockPos blockPos4;
         if (random.nextInt(3) > 0) {
            blockPos4 = pos.west();
            if (Feature.isAir(world, blockPos4)) {
               placeVine(replacer, blockPos4, VineBlock.EAST);
            }
         }

         if (random.nextInt(3) > 0) {
            blockPos4 = pos.east();
            if (Feature.isAir(world, blockPos4)) {
               placeVine(replacer, blockPos4, VineBlock.WEST);
            }
         }

         if (random.nextInt(3) > 0) {
            blockPos4 = pos.north();
            if (Feature.isAir(world, blockPos4)) {
               placeVine(replacer, blockPos4, VineBlock.SOUTH);
            }
         }

         if (random.nextInt(3) > 0) {
            blockPos4 = pos.south();
            if (Feature.isAir(world, blockPos4)) {
               placeVine(replacer, blockPos4, VineBlock.NORTH);
            }
         }

      });
   }
}
