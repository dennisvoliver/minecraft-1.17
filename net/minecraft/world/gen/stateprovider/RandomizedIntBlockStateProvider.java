package net.minecraft.world.gen.stateprovider;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.intprovider.IntProvider;
import org.jetbrains.annotations.Nullable;

/**
 * A {@linkplain BlockStateProvider block state provider} that randomizes a single {@link IntProperty} of a block state provided by another provider.
 */
public class RandomizedIntBlockStateProvider extends BlockStateProvider {
   public static final Codec<RandomizedIntBlockStateProvider> CODEC = RecordCodecBuilder.create((instance) -> {
      return instance.group(BlockStateProvider.TYPE_CODEC.fieldOf("source").forGetter((randomizedIntBlockStateProvider) -> {
         return randomizedIntBlockStateProvider.source;
      }), Codec.STRING.fieldOf("property").forGetter((randomizedIntBlockStateProvider) -> {
         return randomizedIntBlockStateProvider.propertyName;
      }), IntProvider.VALUE_CODEC.fieldOf("values").forGetter((randomizedIntBlockStateProvider) -> {
         return randomizedIntBlockStateProvider.values;
      })).apply(instance, (Function3)(RandomizedIntBlockStateProvider::new));
   });
   private final BlockStateProvider source;
   private final String propertyName;
   @Nullable
   private IntProperty property;
   private final IntProvider values;

   public RandomizedIntBlockStateProvider(BlockStateProvider source, IntProperty property, IntProvider values) {
      this.source = source;
      this.property = property;
      this.propertyName = property.getName();
      this.values = values;
      Collection<Integer> collection = property.getValues();

      for(int i = values.getMin(); i <= values.getMax(); ++i) {
         if (!collection.contains(i)) {
            String var10002 = property.getName();
            throw new IllegalArgumentException("Property value out of range: " + var10002 + ": " + i);
         }
      }

   }

   public RandomizedIntBlockStateProvider(BlockStateProvider source, String propertyName, IntProvider values) {
      this.source = source;
      this.propertyName = propertyName;
      this.values = values;
   }

   protected BlockStateProviderType<?> getType() {
      return BlockStateProviderType.RANDOMIZED_INT_STATE_PROVIDER;
   }

   public BlockState getBlockState(Random random, BlockPos pos) {
      BlockState blockState = this.source.getBlockState(random, pos);
      if (this.property == null || !blockState.contains(this.property)) {
         this.property = getIntPropertyByName(blockState, this.propertyName);
      }

      return (BlockState)blockState.with(this.property, this.values.get(random));
   }

   private static IntProperty getIntPropertyByName(BlockState state, String propertyName) {
      Collection<Property<?>> collection = state.getProperties();
      Optional<IntProperty> optional = collection.stream().filter((property) -> {
         return property.getName().equals(propertyName);
      }).filter((property) -> {
         return property instanceof IntProperty;
      }).map((property) -> {
         return (IntProperty)property;
      }).findAny();
      return (IntProperty)optional.orElseThrow(() -> {
         return new IllegalArgumentException("Illegal property: " + propertyName);
      });
   }
}
