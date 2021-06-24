package net.minecraft.world.gen.decorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.BiFunction;

public class DecoratedDecoratorConfig implements DecoratorConfig {
   public static final Codec<DecoratedDecoratorConfig> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(ConfiguredDecorator.CODEC.fieldOf("outer").forGetter(DecoratedDecoratorConfig::getOuter), ConfiguredDecorator.CODEC.fieldOf("inner").forGetter(DecoratedDecoratorConfig::getInner)).apply(instance, (BiFunction)(DecoratedDecoratorConfig::new));
   });
   private final ConfiguredDecorator<?> outer;
   private final ConfiguredDecorator<?> inner;

   public DecoratedDecoratorConfig(ConfiguredDecorator<?> outer, ConfiguredDecorator<?> inner) {
      this.outer = outer;
      this.inner = inner;
   }

   public ConfiguredDecorator<?> getOuter() {
      return this.outer;
   }

   public ConfiguredDecorator<?> getInner() {
      return this.inner;
   }
}
