package net.minecraft.world.gen;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import java.util.function.Function;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.world.dimension.DimensionType;

public abstract class YOffset {
   public static final Codec<YOffset> OFFSET_CODEC;
   private static final YOffset BOTTOM;
   private static final YOffset TOP;
   private final int offset;

   protected YOffset(int offset) {
      this.offset = offset;
   }

   public static YOffset fixed(int offset) {
      return new YOffset.Fixed(offset);
   }

   public static YOffset aboveBottom(int offset) {
      return new YOffset.AboveBottom(offset);
   }

   public static YOffset belowTop(int offset) {
      return new YOffset.BelowTop(offset);
   }

   public static YOffset getBottom() {
      return BOTTOM;
   }

   public static YOffset getTop() {
      return TOP;
   }

   private static YOffset fromEither(Either<YOffset.Fixed, Either<YOffset.AboveBottom, YOffset.BelowTop>> either) {
      return (YOffset)either.map(Function.identity(), (eitherx) -> {
         return (YOffset)eitherx.map(Function.identity(), Function.identity());
      });
   }

   private static Either<YOffset.Fixed, Either<YOffset.AboveBottom, YOffset.BelowTop>> map(YOffset yOffset) {
      return yOffset instanceof YOffset.Fixed ? Either.left((YOffset.Fixed)yOffset) : Either.right(yOffset instanceof YOffset.AboveBottom ? Either.left((YOffset.AboveBottom)yOffset) : Either.right((YOffset.BelowTop)yOffset));
   }

   protected int getOffset() {
      return this.offset;
   }

   public abstract int getY(HeightContext context);

   static {
      OFFSET_CODEC = Codecs.xor(YOffset.Fixed.CODEC, Codecs.xor(YOffset.AboveBottom.CODEC, YOffset.BelowTop.CODEC)).xmap(YOffset::fromEither, YOffset::map);
      BOTTOM = aboveBottom(0);
      TOP = belowTop(0);
   }

   static final class Fixed extends YOffset {
      public static final Codec<YOffset.Fixed> CODEC;

      protected Fixed(int i) {
         super(i);
      }

      public int getY(HeightContext context) {
         return this.getOffset();
      }

      public String toString() {
         return this.getOffset() + " absolute";
      }

      static {
         CODEC = Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT).fieldOf("absolute").xmap(YOffset.Fixed::new, YOffset::getOffset).codec();
      }
   }

   private static final class AboveBottom extends YOffset {
      public static final Codec<YOffset.AboveBottom> CODEC;

      protected AboveBottom(int i) {
         super(i);
      }

      public int getY(HeightContext context) {
         return context.getMinY() + this.getOffset();
      }

      public String toString() {
         return this.getOffset() + " above bottom";
      }

      static {
         CODEC = Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT).fieldOf("above_bottom").xmap(YOffset.AboveBottom::new, YOffset::getOffset).codec();
      }
   }

   static final class BelowTop extends YOffset {
      public static final Codec<YOffset.BelowTop> CODEC;

      protected BelowTop(int i) {
         super(i);
      }

      public int getY(HeightContext context) {
         return context.getMaxY() - 1 + context.getMinY() - this.getOffset();
      }

      public String toString() {
         return this.getOffset() + " below top";
      }

      static {
         CODEC = Codec.intRange(DimensionType.MIN_HEIGHT, DimensionType.MAX_COLUMN_HEIGHT).fieldOf("below_top").xmap(YOffset.BelowTop::new, YOffset::getOffset).codec();
      }
   }
}
