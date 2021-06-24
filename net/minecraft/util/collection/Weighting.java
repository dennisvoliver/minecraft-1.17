package net.minecraft.util.collection;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.util.Util;

public class Weighting {
   private Weighting() {
   }

   public static int getWeightSum(List<? extends Weighted> pool) {
      long l = 0L;

      Weighted weighted;
      for(Iterator var3 = pool.iterator(); var3.hasNext(); l += (long)weighted.getWeight().getValue()) {
         weighted = (Weighted)var3.next();
      }

      if (l > 2147483647L) {
         throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
      } else {
         return (int)l;
      }
   }

   public static <T extends Weighted> Optional<T> getRandom(Random random, List<T> pool, int totalWeight) {
      if (totalWeight < 0) {
         throw (IllegalArgumentException)Util.throwOrPause(new IllegalArgumentException("Negative total weight in getRandomItem"));
      } else if (totalWeight == 0) {
         return Optional.empty();
      } else {
         int i = random.nextInt(totalWeight);
         return getAt(pool, i);
      }
   }

   public static <T extends Weighted> Optional<T> getAt(List<T> pool, int totalWeight) {
      Iterator var2 = pool.iterator();

      Weighted weighted;
      do {
         if (!var2.hasNext()) {
            return Optional.empty();
         }

         weighted = (Weighted)var2.next();
         totalWeight -= weighted.getWeight().getValue();
      } while(totalWeight >= 0);

      return Optional.of(weighted);
   }

   public static <T extends Weighted> Optional<T> getRandom(Random random, List<T> pool) {
      return getRandom(random, pool, getWeightSum(pool));
   }
}
