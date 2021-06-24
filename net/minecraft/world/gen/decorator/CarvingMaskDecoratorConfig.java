package net.minecraft.world.gen.decorator;

import com.mojang.serialization.Codec;
import net.minecraft.world.gen.GenerationStep;

public class CarvingMaskDecoratorConfig implements DecoratorConfig {
   public static final Codec<CarvingMaskDecoratorConfig> CODEC;
   protected final GenerationStep.Carver carver;

   public CarvingMaskDecoratorConfig(GenerationStep.Carver carver) {
      this.carver = carver;
   }

   static {
      CODEC = GenerationStep.Carver.CODEC.fieldOf("step").xmap(CarvingMaskDecoratorConfig::new, (config) -> {
         return config.carver;
      }).codec();
   }
}
