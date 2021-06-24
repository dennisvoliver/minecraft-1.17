package net.minecraft.util.collection;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Stream;

public class WeightedList<U> {
   protected final List<WeightedList.Entry<U>> entries;
   private final Random random = new Random();

   public WeightedList() {
      this.entries = Lists.newArrayList();
   }

   private WeightedList(List<WeightedList.Entry<U>> list) {
      this.entries = Lists.newArrayList((Iterable)list);
   }

   public static <U> Codec<WeightedList<U>> createCodec(Codec<U> codec) {
      return WeightedList.Entry.createCodec(codec).listOf().xmap(WeightedList::new, (weightedList) -> {
         return weightedList.entries;
      });
   }

   public WeightedList<U> add(U data, int weight) {
      this.entries.add(new WeightedList.Entry(data, weight));
      return this;
   }

   public WeightedList<U> shuffle() {
      this.entries.forEach((entry) -> {
         entry.setShuffledOrder(this.random.nextFloat());
      });
      this.entries.sort(Comparator.comparingDouble(WeightedList.Entry::getShuffledOrder));
      return this;
   }

   public Stream<U> stream() {
      return this.entries.stream().map(WeightedList.Entry::getElement);
   }

   public String toString() {
      return "ShufflingList[" + this.entries + "]";
   }

   public static class Entry<T> {
      final T data;
      final int weight;
      private double shuffledOrder;

      Entry(T data, int weight) {
         this.weight = weight;
         this.data = data;
      }

      private double getShuffledOrder() {
         return this.shuffledOrder;
      }

      void setShuffledOrder(float random) {
         this.shuffledOrder = -Math.pow((double)random, (double)(1.0F / (float)this.weight));
      }

      public T getElement() {
         return this.data;
      }

      public int getWeight() {
         return this.weight;
      }

      public String toString() {
         return this.weight + ":" + this.data;
      }

      public static <E> Codec<WeightedList.Entry<E>> createCodec(final Codec<E> codec) {
         return new Codec<WeightedList.Entry<E>>() {
            public <T> DataResult<Pair<WeightedList.Entry<E>, T>> decode(DynamicOps<T> ops, T data) {
               Dynamic<T> dynamic = new Dynamic(ops, data);
               OptionalDynamic var10000 = dynamic.get("data");
               Codec var10001 = codec;
               Objects.requireNonNull(var10001);
               return var10000.flatMap(var10001::parse).map((datax) -> {
                  return new WeightedList.Entry(datax, dynamic.get("weight").asInt(1));
               }).map((entry) -> {
                  return Pair.of(entry, ops.empty());
               });
            }

            public <T> DataResult<T> encode(WeightedList.Entry<E> entry, DynamicOps<T> dynamicOps, T object) {
               return dynamicOps.mapBuilder().add("weight", dynamicOps.createInt(entry.weight)).add("data", codec.encodeStart(dynamicOps, entry.data)).build(object);
            }
         };
      }
   }
}
