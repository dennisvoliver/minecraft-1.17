package net.minecraft.world.gen.treedecorator;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.block.BlockState;
import net.minecraft.block.VineBlock;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.Feature;

public class LeavesVineTreeDecorator extends TreeDecorator {
   public static final Codec<LeavesVineTreeDecorator> CODEC = Codec.unit(() -> {
      return INSTANCE;
   });
   public static final LeavesVineTreeDecorator INSTANCE = new LeavesVineTreeDecorator();

   protected TreeDecoratorType<?> getType() {
      return TreeDecoratorType.LEAVE_VINE;
   }

   public void generate(TestableWorld world, BiConsumer<BlockPos, BlockState> replacer, Random random, List<BlockPos> logPositions, List<BlockPos> leavesPositions) {
      leavesPositions.forEach((pos) -> {
         BlockPos blockPos4;
         if (random.nextInt(4) == 0) {
            blockPos4 = pos.west();
            if (Feature.isAir(world, blockPos4)) {
               placeVines(world, blockPos4, VineBlock.EAST, replacer);
            }
         }

         if (random.nextInt(4) == 0) {
            blockPos4 = pos.east();
            if (Feature.isAir(world, blockPos4)) {
               placeVines(world, blockPos4, VineBlock.WEST, replacer);
            }
         }

         if (random.nextInt(4) == 0) {
            blockPos4 = pos.north();
            if (Feature.isAir(world, blockPos4)) {
               placeVines(world, blockPos4, VineBlock.SOUTH, replacer);
            }
         }

         if (random.nextInt(4) == 0) {
            blockPos4 = pos.south();
            if (Feature.isAir(world, blockPos4)) {
               placeVines(world, blockPos4, VineBlock.NORTH, replacer);
            }
         }

      });
   }

   /**
    * Places a vine at a given position and then up to 4 more vines going downwards.
    */
   private static void placeVines(TestableWorld world, BlockPos pos, BooleanProperty facing, BiConsumer<BlockPos, BlockState> replacer) {
      placeVine(replacer, pos, facing);
      int i = 4;

      for(pos = pos.down(); Feature.isAir(world, pos) && i > 0; --i) {
         placeVine(replacer, pos, facing);
         pos = pos.down();
      }

   }
}
