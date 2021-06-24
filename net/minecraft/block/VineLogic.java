package net.minecraft.block;

import java.util.Random;

public class VineLogic {
   private static final double field_31198 = 0.826D;
   public static final double field_31197 = 0.1D;

   public static boolean isValidForWeepingStem(BlockState state) {
      return state.isAir();
   }

   public static int getGrowthLength(Random random) {
      double d = 1.0D;

      int i;
      for(i = 0; random.nextDouble() < d; ++i) {
         d *= 0.826D;
      }

      return i;
   }
}
