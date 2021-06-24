package net.minecraft.world.gen.trunk;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Function5;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.block.BlockState;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeature;
import net.minecraft.world.gen.feature.TreeFeatureConfig;
import net.minecraft.world.gen.foliage.FoliagePlacer;

public class BendingTrunkPlacer extends TrunkPlacer {
   public static final Codec<BendingTrunkPlacer> CODEC = RecordCodecBuilder.create((instance) -> {
      return fillTrunkPlacerFields(instance).and(instance.group(Codecs.field_33442.optionalFieldOf("min_height_for_leaves", 1).forGetter((placer) -> {
         return placer.minHeightForLeaves;
      }), IntProvider.createValidatingCodec(1, 64).fieldOf("bend_length").forGetter((placer) -> {
         return placer.bendLength;
      }))).apply(instance, (Function5)(BendingTrunkPlacer::new));
   });
   private final int minHeightForLeaves;
   private final IntProvider bendLength;

   public BendingTrunkPlacer(int baseHeight, int firstRandomHeight, int secondRandomHeight, int minHeightForLeaves, IntProvider bendLength) {
      super(baseHeight, firstRandomHeight, secondRandomHeight);
      this.minHeightForLeaves = minHeightForLeaves;
      this.bendLength = bendLength;
   }

   protected TrunkPlacerType<?> getType() {
      return TrunkPlacerType.BENDING_TRUNK_PLACER;
   }

   public List<FoliagePlacer.TreeNode> generate(TestableWorld world, BiConsumer<BlockPos, BlockState> replacer, Random random, int height, BlockPos startPos, TreeFeatureConfig config) {
      Direction direction = Direction.Type.HORIZONTAL.random(random);
      int i = height - 1;
      BlockPos.Mutable mutable = startPos.mutableCopy();
      BlockPos blockPos = mutable.down();
      setToDirt(world, replacer, random, blockPos, config);
      List<FoliagePlacer.TreeNode> list = Lists.newArrayList();

      int j;
      for(j = 0; j <= i; ++j) {
         if (j + 1 >= i + random.nextInt(2)) {
            mutable.move(direction);
         }

         if (TreeFeature.canReplace(world, mutable)) {
            getAndSetState(world, replacer, random, mutable, config);
         }

         if (j >= this.minHeightForLeaves) {
            list.add(new FoliagePlacer.TreeNode(mutable.toImmutable(), 0, false));
         }

         mutable.move(Direction.UP);
      }

      j = this.bendLength.get(random);

      for(int l = 0; l <= j; ++l) {
         if (TreeFeature.canReplace(world, mutable)) {
            getAndSetState(world, replacer, random, mutable, config);
         }

         list.add(new FoliagePlacer.TreeNode(mutable.toImmutable(), 0, false));
         mutable.move(direction);
      }

      return list;
   }
}
