package net.minecraft.util.math;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringIdentifiable;

public enum VerticalSurfaceType implements StringIdentifiable {
   CEILING(Direction.UP, 1, "ceiling"),
   FLOOR(Direction.DOWN, -1, "floor");

   public static final Codec<VerticalSurfaceType> CODEC = StringIdentifiable.createCodec(VerticalSurfaceType::values, VerticalSurfaceType::byName);
   private final Direction direction;
   private final int offset;
   private final String name;
   private static final VerticalSurfaceType[] VALUES = values();

   private VerticalSurfaceType(Direction direction, int offset, String name) {
      this.direction = direction;
      this.offset = offset;
      this.name = name;
   }

   public Direction getDirection() {
      return this.direction;
   }

   public int getOffset() {
      return this.offset;
   }

   public static VerticalSurfaceType byName(String name) {
      VerticalSurfaceType[] var1 = VALUES;
      int var2 = var1.length;

      for(int var3 = 0; var3 < var2; ++var3) {
         VerticalSurfaceType verticalSurfaceType = var1[var3];
         if (verticalSurfaceType.asString().equals(name)) {
            return verticalSurfaceType;
         }
      }

      throw new IllegalArgumentException("Unknown Surface type: " + name);
   }

   public String asString() {
      return this.name;
   }
}
