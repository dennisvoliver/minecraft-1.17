package net.minecraft.world.gen.feature;

import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.intprovider.IntProvider;

public class DiskFeatureConfig implements FeatureConfig {
   public static final Codec<DiskFeatureConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(BlockState.CODEC.fieldOf("state").forGetter((diskFeatureConfig) -> {
         return diskFeatureConfig.state;
      }), IntProvider.createValidatingCodec(0, 8).fieldOf("radius").forGetter((diskFeatureConfig) -> {
         return diskFeatureConfig.radius;
      }), Codec.intRange(0, 4).fieldOf("half_height").forGetter((diskFeatureConfig) -> {
         return diskFeatureConfig.halfHeight;
      }), BlockState.CODEC.listOf().fieldOf("targets").forGetter((diskFeatureConfig) -> {
         return diskFeatureConfig.targets;
      })).apply(instance, (Function4)(DiskFeatureConfig::new));
   });
   public final BlockState state;
   public final IntProvider radius;
   public final int halfHeight;
   public final List<BlockState> targets;

   public DiskFeatureConfig(BlockState state, IntProvider radius, int halfHeight, List<BlockState> targets) {
      this.state = state;
      this.radius = radius;
      this.halfHeight = halfHeight;
      this.targets = targets;
   }
}
