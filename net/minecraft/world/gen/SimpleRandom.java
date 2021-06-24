package net.minecraft.world.gen;

import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.thread.AtomicStack;
import net.minecraft.util.thread.LockHelper;

public class SimpleRandom implements WorldGenRandom {
   private static final int field_31471 = 48;
   private static final long field_31472 = 281474976710655L;
   private static final long field_31473 = 25214903917L;
   private static final long field_31474 = 11L;
   private static final float field_31475 = 5.9604645E-8F;
   private static final double field_31476 = 1.1102230246251565E-16D;
   private final AtomicLong seed = new AtomicLong();
   private double field_31477;
   private boolean field_31478;

   public SimpleRandom(long seed) {
      this.setSeed(seed);
   }

   public void setSeed(long l) {
      if (!this.seed.compareAndSet(this.seed.get(), (l ^ 25214903917L) & 281474976710655L)) {
         throw LockHelper.crash("SimpleRandomSource", (AtomicStack)null);
      }
   }

   private int next(int bits) {
      long l = this.seed.get();
      long m = l * 25214903917L + 11L & 281474976710655L;
      if (!this.seed.compareAndSet(l, m)) {
         throw LockHelper.crash("SimpleRandomSource", (AtomicStack)null);
      } else {
         return (int)(m >> 48 - bits);
      }
   }

   public int nextInt() {
      return this.next(32);
   }

   public int nextInt(int i) {
      if (i <= 0) {
         throw new IllegalArgumentException("Bound must be positive");
      } else if ((i & i - 1) == 0) {
         return (int)((long)i * (long)this.next(31) >> 31);
      } else {
         int j;
         int k;
         do {
            j = this.next(31);
            k = j % i;
         } while(j - k + (i - 1) < 0);

         return k;
      }
   }

   public long nextLong() {
      int i = this.next(32);
      int j = this.next(32);
      long l = (long)i << 32;
      return l + (long)j;
   }

   public boolean nextBoolean() {
      return this.next(1) != 0;
   }

   public float nextFloat() {
      return (float)this.next(24) * 5.9604645E-8F;
   }

   public double nextDouble() {
      int i = this.next(26);
      int j = this.next(27);
      long l = ((long)i << 27) + (long)j;
      return (double)l * 1.1102230246251565E-16D;
   }

   public double nextGaussian() {
      if (this.field_31478) {
         this.field_31478 = false;
         return this.field_31477;
      } else {
         double d;
         double e;
         double f;
         do {
            do {
               d = 2.0D * this.nextDouble() - 1.0D;
               e = 2.0D * this.nextDouble() - 1.0D;
               f = MathHelper.square(d) + MathHelper.square(e);
            } while(f >= 1.0D);
         } while(f == 0.0D);

         double g = Math.sqrt(-2.0D * Math.log(f) / f);
         this.field_31477 = e * g;
         this.field_31478 = true;
         return d * g;
      }
   }
}
