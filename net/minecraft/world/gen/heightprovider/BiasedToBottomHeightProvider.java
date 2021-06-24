package net.minecraft.world.gen.heightprovider;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Random;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.YOffset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BiasedToBottomHeightProvider extends HeightProvider {
   public static final Codec<BiasedToBottomHeightProvider> BIASED_TO_BOTTOM_CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(YOffset.OFFSET_CODEC.fieldOf("min_inclusive").forGetter((biasedToBottomHeightProvider) -> {
         return biasedToBottomHeightProvider.minOffset;
      }), YOffset.OFFSET_CODEC.fieldOf("max_inclusive").forGetter((biasedToBottomHeightProvider) -> {
         return biasedToBottomHeightProvider.maxOffset;
      }), Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("inner", 1).forGetter((biasedToBottomHeightProvider) -> {
         return biasedToBottomHeightProvider.inner;
      })).apply(instance, (Function3)(BiasedToBottomHeightProvider::new));
   });
   private static final Logger LOGGER = LogManager.getLogger();
   private final YOffset minOffset;
   private final YOffset maxOffset;
   private final int inner;

   private BiasedToBottomHeightProvider(YOffset minOffset, YOffset maxOffset, int inner) {
      this.minOffset = minOffset;
      this.maxOffset = maxOffset;
      this.inner = inner;
   }

   public static BiasedToBottomHeightProvider create(YOffset minOffset, YOffset maxOffset, int inner) {
      return new BiasedToBottomHeightProvider(minOffset, maxOffset, inner);
   }

   public int get(Random random, HeightContext context) {
      int i = this.minOffset.getY(context);
      int j = this.maxOffset.getY(context);
      if (j - i - this.inner + 1 <= 0) {
         LOGGER.warn((String)"Empty height range: {}", (Object)this);
         return i;
      } else {
         int k = random.nextInt(j - i - this.inner + 1);
         return random.nextInt(k + this.inner) + i;
      }
   }

   public HeightProviderType<?> getType() {
      return HeightProviderType.BIASED_TO_BOTTOM;
   }

   public String toString() {
      return "biased[" + this.minOffset + "-" + this.maxOffset + " inner: " + this.inner + "]";
   }
}
