package net.minecraft.world.gen.carver;

import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class CarverContext implements HeightContext {
   private final ChunkGenerator generator;

   public CarverContext(ChunkGenerator generator) {
      this.generator = generator;
   }

   public int getMinY() {
      return this.generator.getMinimumY();
   }

   public int getMaxY() {
      return this.generator.getWorldHeight();
   }
}
