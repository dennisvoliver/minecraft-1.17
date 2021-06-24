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
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

public class DarkOakTrunkPlacer extends TrunkPlacer {
   public static final Codec<DarkOakTrunkPlacer> CODEC = RecordCodecBuilder.create((instance) -> {
      return fillTrunkPlacerFields(instance).apply(instance, (Function3)(DarkOakTrunkPlacer::new));
   });

   public DarkOakTrunkPlacer(int i, int j, int k) {
      super(i, j, k);
   }

   protected TrunkPlacerType<?> getType() {
      return TrunkPlacerType.DARK_OAK_TRUNK_PLACER;
   }

   public List<FoliagePlacer.TreeNode> generate(TestableWorld world, BiConsumer<BlockPos, BlockState> replacer, Random random, int height, BlockPos startPos, TreeFeatureConfig config) {
      List<FoliagePlacer.TreeNode> list = Lists.newArrayList();
      BlockPos blockPos = startPos.down();
      setToDirt(world, replacer, random, blockPos, config);
      setToDirt(world, replacer, random, blockPos.east(), config);
      setToDirt(world, replacer, random, blockPos.south(), config);
      setToDirt(world, replacer, random, blockPos.south().east(), config);
      Direction direction = Direction.Type.HORIZONTAL.random(random);
      int i = height - random.nextInt(4);
      int j = 2 - random.nextInt(3);
      int k = startPos.getX();
      int l = startPos.getY();
      int m = startPos.getZ();
      int n = k;
      int o = m;
      int p = l + height - 1;

      int s;
      int t;
      for(s = 0; s < height; ++s) {
         if (s >= i && j > 0) {
            n += direction.getOffsetX();
            o += direction.getOffsetZ();
            --j;
         }

         t = l + s;
         BlockPos blockPos2 = new BlockPos(n, t, o);
         if (TreeFeature.isAirOrLeaves(world, blockPos2)) {
            getAndSetState(world, replacer, random, blockPos2, config);
            getAndSetState(world, replacer, random, blockPos2.east(), config);
            getAndSetState(world, replacer, random, blockPos2.south(), config);
            getAndSetState(world, replacer, random, blockPos2.east().south(), config);
         }
      }

      list.add(new FoliagePlacer.TreeNode(new BlockPos(n, p, o), 0, true));

      for(s = -1; s <= 2; ++s) {
         for(t = -1; t <= 2; ++t) {
            if ((s < 0 || s > 1 || t < 0 || t > 1) && random.nextInt(3) <= 0) {
               int u = random.nextInt(3) + 2;

               for(int v = 0; v < u; ++v) {
                  getAndSetState(world, replacer, random, new BlockPos(k + s, p - v - 1, m + t), config);
               }

               list.add(new FoliagePlacer.TreeNode(new BlockPos(n + s, p, o + t), 0, false));
            }
         }
      }

      return list;
   }
}
