package net.minecraft.world.gen.foliage;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import java.util.function.BiConsumer;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import net.minecraft.world.TestableWorld;
import net.minecraft.world.gen.feature.TreeFeatureConfig;

public class PineFoliagePlacer extends FoliagePlacer {
   public static final Codec<PineFoliagePlacer> CODEC = RecordCodecBuilder.create((instance) -> {
      return fillFoliagePlacerFields(instance).and((App)IntProvider.createValidatingCodec(0, 24).fieldOf("height").forGetter((placer) -> {
         return placer.height;
      })).apply(instance, (Function3)(PineFoliagePlacer::new));
   });
   private final IntProvider height;

   public PineFoliagePlacer(IntProvider radius, IntProvider offset, IntProvider height) {
      super(radius, offset);
      this.height = height;
   }

   protected FoliagePlacerType<?> getType() {
      return FoliagePlacerType.PINE_FOLIAGE_PLACER;
   }

   protected void generate(TestableWorld world, BiConsumer<BlockPos, BlockState> replacer, Random random, TreeFeatureConfig config, int trunkHeight, FoliagePlacer.TreeNode treeNode, int foliageHeight, int radius, int offset) {
      int i = 0;

      for(int j = offset; j >= offset - foliageHeight; --j) {
         this.generateSquare(world, replacer, random, config, treeNode.getCenter(), i, j, treeNode.isGiantTrunk());
         if (i >= 1 && j == offset - foliageHeight + 1) {
            --i;
         } else if (i < radius + treeNode.getFoliageRadius()) {
            ++i;
         }
      }

   }

   public int getRandomRadius(Random random, int baseHeight) {
      return super.getRandomRadius(random, baseHeight) + random.nextInt(Math.max(baseHeight + 1, 1));
   }

   public int getRandomHeight(Random random, int trunkHeight, TreeFeatureConfig config) {
      return this.height.get(random);
   }

   protected boolean isInvalidForLeaves(Random random, int dx, int y, int dz, int radius, boolean giantTrunk) {
      return dx == radius && dz == radius && radius > 0;
   }
}
