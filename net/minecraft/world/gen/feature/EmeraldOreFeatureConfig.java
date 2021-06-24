package net.minecraft.world.gen.feature;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.function.Function;
import net.minecraft.block.BlockState;
import net.minecraft.structure.rule.BlockStateMatchRuleTest;

public class EmeraldOreFeatureConfig implements FeatureConfig {
   public static final Codec<EmeraldOreFeatureConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(Codec.list(OreFeatureConfig.Target.CODEC).fieldOf("targets").forGetter((emeraldOreFeatureConfig) -> {
         return emeraldOreFeatureConfig.target;
      })).apply(instance, (Function)(EmeraldOreFeatureConfig::new));
   });
   public final List<OreFeatureConfig.Target> target;

   public EmeraldOreFeatureConfig(BlockState target, BlockState state) {
      this(ImmutableList.of(OreFeatureConfig.createTarget(new BlockStateMatchRuleTest(target), state)));
   }

   public EmeraldOreFeatureConfig(List<OreFeatureConfig.Target> list) {
      this.target = list;
   }
}
