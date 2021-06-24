package net.minecraft.world.gen.trunk;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

public class ForkingTrunkPlacer extends TrunkPlacer {
   public static final Codec<ForkingTrunkPlacer> CODEC = RecordCodecBuilder.create((instance) -> {
      return fillTrunkPlacerFields(instance).apply(instance, (Function3)(ForkingTrunkPlacer::new));
   });

   public ForkingTrunkPlacer(int i, int j, int k) {
      super(i, j, k);
   }

   protected TrunkPlacerType<?> getType() {
      return TrunkPlacerType.FORKING_TRUNK_PLACER;
   }

   public List<FoliagePlacer.TreeNode> generate(TestableWorld world, BiConsumer<BlockPos, BlockState> replacer, Random random, int height, BlockPos startPos, TreeFeatureConfig config) {
      setToDirt(world, replacer, random, startPos.down(), config);
      List<FoliagePlacer.TreeNode> list = Lists.newArrayList();
      Direction direction = Direction.Type.HORIZONTAL.random(random);
      int i = height - random.nextInt(4) - 1;
      int j = 3 - random.nextInt(3);
      BlockPos.Mutable mutable = new BlockPos.Mutable();
      int k = startPos.getX();
      int l = startPos.getZ();
      int m = 0;

      int o;
      for(int n = 0; n < height; ++n) {
         o = startPos.getY() + n;
         if (n >= i && j > 0) {
            k += direction.getOffsetX();
            l += direction.getOffsetZ();
            --j;
         }

         if (getAndSetState(world, replacer, random, mutable.set(k, o, l), config)) {
            m = o + 1;
         }
      }

      list.add(new FoliagePlacer.TreeNode(new BlockPos(k, m, l), 1, false));
      k = startPos.getX();
      l = startPos.getZ();
      Direction direction2 = Direction.Type.HORIZONTAL.random(random);
      if (direction2 != direction) {
         o = i - random.nextInt(2) - 1;
         int q = 1 + random.nextInt(3);
         m = 0;

         for(int r = o; r < height && q > 0; --q) {
            if (r >= 1) {
               int s = startPos.getY() + r;
               k += direction2.getOffsetX();
               l += direction2.getOffsetZ();
               if (getAndSetState(world, replacer, random, mutable.set(k, s, l), config)) {
                  m = s + 1;
               }
            }

            ++r;
         }

         if (m > 1) {
            list.add(new FoliagePlacer.TreeNode(new BlockPos(k, m, l), 0, false));
         }
      }

      return list;
   }
}
