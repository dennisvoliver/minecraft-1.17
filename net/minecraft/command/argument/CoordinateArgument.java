package net.minecraft.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.text.TranslatableText;

public class CoordinateArgument {
   private static final char field_32972 = '~';
   public static final SimpleCommandExceptionType MISSING_COORDINATE = new SimpleCommandExceptionType(new TranslatableText("argument.pos.missing.double"));
   public static final SimpleCommandExceptionType MISSING_BLOCK_POSITION = new SimpleCommandExceptionType(new TranslatableText("argument.pos.missing.int"));
   private final boolean relative;
   private final double value;

   public CoordinateArgument(boolean relative, double value) {
      this.relative = relative;
      this.value = value;
   }

   public double toAbsoluteCoordinate(double offset) {
      return this.relative ? this.value + offset : this.value;
   }

   public static CoordinateArgument parse(StringReader reader, boolean centerIntegers) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '^') {
         throw Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader);
      } else if (!reader.canRead()) {
         throw MISSING_COORDINATE.createWithContext(reader);
      } else {
         boolean bl = isRelative(reader);
         int i = reader.getCursor();
         double d = reader.canRead() && reader.peek() != ' ' ? reader.readDouble() : 0.0D;
         String string = reader.getString().substring(i, reader.getCursor());
         if (bl && string.isEmpty()) {
            return new CoordinateArgument(true, 0.0D);
         } else {
            if (!string.contains(".") && !bl && centerIntegers) {
               d += 0.5D;
            }

            return new CoordinateArgument(bl, d);
         }
      }
   }

   public static CoordinateArgument parse(StringReader reader) throws CommandSyntaxException {
      if (reader.canRead() && reader.peek() == '^') {
         throw Vec3ArgumentType.MIXED_COORDINATE_EXCEPTION.createWithContext(reader);
      } else if (!reader.canRead()) {
         throw MISSING_BLOCK_POSITION.createWithContext(reader);
      } else {
         boolean bl = isRelative(reader);
         double e;
         if (reader.canRead() && reader.peek() != ' ') {
            e = bl ? reader.readDouble() : (double)reader.readInt();
         } else {
            e = 0.0D;
         }

         return new CoordinateArgument(bl, e);
      }
   }

   public static boolean isRelative(StringReader reader) {
      boolean bl2;
      if (reader.peek() == '~') {
         bl2 = true;
         reader.skip();
      } else {
         bl2 = false;
      }

      return bl2;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (!(o instanceof CoordinateArgument)) {
         return false;
      } else {
         CoordinateArgument coordinateArgument = (CoordinateArgument)o;
         if (this.relative != coordinateArgument.relative) {
            return false;
         } else {
            return Double.compare(coordinateArgument.value, this.value) == 0;
         }
      }
   }

   public int hashCode() {
      int i = this.relative ? 1 : 0;
      long l = Double.doubleToLongBits(this.value);
      i = 31 * i + (int)(l ^ l >>> 32);
      return i;
   }

   public boolean isRelative() {
      return this.relative;
   }
}
