package net.minecraft.util.math.noise;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntBidirectionalIterator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSortedSet;
import java.util.List;
import java.util.function.LongFunction;
import java.util.stream.IntStream;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.WorldGenRandom;
import org.jetbrains.annotations.Nullable;

public class OctavePerlinNoiseSampler implements NoiseSampler {
   private static final int field_31704 = 33554432;
   private final PerlinNoiseSampler[] octaveSamplers;
   private final DoubleList amplitudes;
   private final double persistence;
   private final double lacunarity;

   public OctavePerlinNoiseSampler(WorldGenRandom random, IntStream octaves) {
      this(random, (List)octaves.boxed().collect(ImmutableList.toImmutableList()));
   }

   public OctavePerlinNoiseSampler(WorldGenRandom random, List<Integer> octaves) {
      this(random, (IntSortedSet)(new IntRBTreeSet(octaves)));
   }

   public static OctavePerlinNoiseSampler create(WorldGenRandom random, int offset, double... amplitudes) {
      return create(random, offset, (DoubleList)(new DoubleArrayList(amplitudes)));
   }

   public static OctavePerlinNoiseSampler create(WorldGenRandom random, int offset, DoubleList amplitudes) {
      return new OctavePerlinNoiseSampler(random, Pair.of(offset, amplitudes));
   }

   private static Pair<Integer, DoubleList> calculateAmplitudes(IntSortedSet octaves) {
      if (octaves.isEmpty()) {
         throw new IllegalArgumentException("Need some octaves!");
      } else {
         int i = -octaves.firstInt();
         int j = octaves.lastInt();
         int k = i + j + 1;
         if (k < 1) {
            throw new IllegalArgumentException("Total number of octaves needs to be >= 1");
         } else {
            DoubleList doubleList = new DoubleArrayList(new double[k]);
            IntBidirectionalIterator intBidirectionalIterator = octaves.iterator();

            while(intBidirectionalIterator.hasNext()) {
               int l = intBidirectionalIterator.nextInt();
               doubleList.set(l + i, 1.0D);
            }

            return Pair.of(-i, doubleList);
         }
      }
   }

   private OctavePerlinNoiseSampler(WorldGenRandom random, IntSortedSet octaves) {
      this(random, octaves, ChunkRandom::new);
   }

   private OctavePerlinNoiseSampler(WorldGenRandom random, IntSortedSet octaves, LongFunction<WorldGenRandom> randomFunction) {
      this(random, calculateAmplitudes(octaves), randomFunction);
   }

   protected OctavePerlinNoiseSampler(WorldGenRandom random, Pair<Integer, DoubleList> offsetAndAmplitudes) {
      this(random, offsetAndAmplitudes, ChunkRandom::new);
   }

   protected OctavePerlinNoiseSampler(WorldGenRandom random, Pair<Integer, DoubleList> octaves, LongFunction<WorldGenRandom> randomFunction) {
      int i = (Integer)octaves.getFirst();
      this.amplitudes = (DoubleList)octaves.getSecond();
      PerlinNoiseSampler perlinNoiseSampler = new PerlinNoiseSampler(random);
      int j = this.amplitudes.size();
      int k = -i;
      this.octaveSamplers = new PerlinNoiseSampler[j];
      if (k >= 0 && k < j) {
         double d = this.amplitudes.getDouble(k);
         if (d != 0.0D) {
            this.octaveSamplers[k] = perlinNoiseSampler;
         }
      }

      for(int l = k - 1; l >= 0; --l) {
         if (l < j) {
            double e = this.amplitudes.getDouble(l);
            if (e != 0.0D) {
               this.octaveSamplers[l] = new PerlinNoiseSampler(random);
            } else {
               skipCalls(random);
            }
         } else {
            skipCalls(random);
         }
      }

      if (k < j - 1) {
         throw new IllegalArgumentException("Positive octaves are temporarily disabled");
      } else {
         this.lacunarity = Math.pow(2.0D, (double)(-k));
         this.persistence = Math.pow(2.0D, (double)(j - 1)) / (Math.pow(2.0D, (double)j) - 1.0D);
      }
   }

   private static void skipCalls(WorldGenRandom random) {
      random.skip(262);
   }

   public double sample(double x, double y, double z) {
      return this.sample(x, y, z, 0.0D, 0.0D, false);
   }

   @Deprecated
   public double sample(double x, double y, double z, double yScale, double yMax, boolean useOrigin) {
      double d = 0.0D;
      double e = this.lacunarity;
      double f = this.persistence;

      for(int i = 0; i < this.octaveSamplers.length; ++i) {
         PerlinNoiseSampler perlinNoiseSampler = this.octaveSamplers[i];
         if (perlinNoiseSampler != null) {
            double g = perlinNoiseSampler.sample(maintainPrecision(x * e), useOrigin ? -perlinNoiseSampler.originY : maintainPrecision(y * e), maintainPrecision(z * e), yScale * e, yMax * e);
            d += this.amplitudes.getDouble(i) * g * f;
         }

         e *= 2.0D;
         f /= 2.0D;
      }

      return d;
   }

   @Nullable
   public PerlinNoiseSampler getOctave(int octave) {
      return this.octaveSamplers[this.octaveSamplers.length - 1 - octave];
   }

   public static double maintainPrecision(double value) {
      return value - (double)MathHelper.lfloor(value / 3.3554432E7D + 0.5D) * 3.3554432E7D;
   }

   public double sample(double x, double y, double yScale, double yMax) {
      return this.sample(x, y, 0.0D, yScale, yMax, false);
   }
}
