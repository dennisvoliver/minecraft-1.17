package net.minecraft.predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.BuiltInExceptionProvider;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.JsonHelper;
import org.jetbrains.annotations.Nullable;

public abstract class NumberRange<T extends Number> {
   public static final SimpleCommandExceptionType EXCEPTION_EMPTY = new SimpleCommandExceptionType(new TranslatableText("argument.range.empty"));
   public static final SimpleCommandExceptionType EXCEPTION_SWAPPED = new SimpleCommandExceptionType(new TranslatableText("argument.range.swapped"));
   protected final T min;
   protected final T max;

   protected NumberRange(@Nullable T min, @Nullable T max) {
      this.min = min;
      this.max = max;
   }

   @Nullable
   public T getMin() {
      return this.min;
   }

   @Nullable
   public T getMax() {
      return this.max;
   }

   public boolean isDummy() {
      return this.min == null && this.max == null;
   }

   public JsonElement toJson() {
      if (this.isDummy()) {
         return JsonNull.INSTANCE;
      } else if (this.min != null && this.min.equals(this.max)) {
         return new JsonPrimitive(this.min);
      } else {
         JsonObject jsonObject = new JsonObject();
         if (this.min != null) {
            jsonObject.addProperty("min", this.min);
         }

         if (this.max != null) {
            jsonObject.addProperty("max", this.max);
         }

         return jsonObject;
      }
   }

   protected static <T extends Number, R extends NumberRange<T>> R fromJson(@Nullable JsonElement json, R fallback, BiFunction<JsonElement, String, T> asNumber, NumberRange.Factory<T, R> factory) {
      if (json != null && !json.isJsonNull()) {
         if (JsonHelper.isNumber(json)) {
            T number = (Number)asNumber.apply(json, "value");
            return factory.create(number, number);
         } else {
            JsonObject jsonObject = JsonHelper.asObject(json, "value");
            T number2 = jsonObject.has("min") ? (Number)asNumber.apply(jsonObject.get("min"), "min") : null;
            T number3 = jsonObject.has("max") ? (Number)asNumber.apply(jsonObject.get("max"), "max") : null;
            return factory.create(number2, number3);
         }
      } else {
         return fallback;
      }
   }

   protected static <T extends Number, R extends NumberRange<T>> R parse(StringReader commandReader, NumberRange.CommandFactory<T, R> commandFactory, Function<String, T> converter, Supplier<DynamicCommandExceptionType> exceptionTypeSupplier, Function<T, T> mapper) throws CommandSyntaxException {
      if (!commandReader.canRead()) {
         throw EXCEPTION_EMPTY.createWithContext(commandReader);
      } else {
         int i = commandReader.getCursor();

         try {
            T number = (Number)map(fromStringReader(commandReader, converter, exceptionTypeSupplier), mapper);
            Number number3;
            if (commandReader.canRead(2) && commandReader.peek() == '.' && commandReader.peek(1) == '.') {
               commandReader.skip();
               commandReader.skip();
               number3 = (Number)map(fromStringReader(commandReader, converter, exceptionTypeSupplier), mapper);
               if (number == null && number3 == null) {
                  throw EXCEPTION_EMPTY.createWithContext(commandReader);
               }
            } else {
               number3 = number;
            }

            if (number == null && number3 == null) {
               throw EXCEPTION_EMPTY.createWithContext(commandReader);
            } else {
               return commandFactory.create(commandReader, number, number3);
            }
         } catch (CommandSyntaxException var8) {
            commandReader.setCursor(i);
            throw new CommandSyntaxException(var8.getType(), var8.getRawMessage(), var8.getInput(), i);
         }
      }
   }

   @Nullable
   private static <T extends Number> T fromStringReader(StringReader reader, Function<String, T> converter, Supplier<DynamicCommandExceptionType> exceptionTypeSupplier) throws CommandSyntaxException {
      int i = reader.getCursor();

      while(reader.canRead() && isNextCharValid(reader)) {
         reader.skip();
      }

      String string = reader.getString().substring(i, reader.getCursor());
      if (string.isEmpty()) {
         return null;
      } else {
         try {
            return (Number)converter.apply(string);
         } catch (NumberFormatException var6) {
            throw ((DynamicCommandExceptionType)exceptionTypeSupplier.get()).createWithContext(reader, string);
         }
      }
   }

   private static boolean isNextCharValid(StringReader reader) {
      char c = reader.peek();
      if ((c < '0' || c > '9') && c != '-') {
         if (c != '.') {
            return false;
         } else {
            return !reader.canRead(2) || reader.peek(1) != '.';
         }
      } else {
         return true;
      }
   }

   @Nullable
   private static <T> T map(@Nullable T object, Function<T, T> function) {
      return object == null ? null : function.apply(object);
   }

   @FunctionalInterface
   protected interface Factory<T extends Number, R extends NumberRange<T>> {
      R create(@Nullable T min, @Nullable T max);
   }

   @FunctionalInterface
   protected interface CommandFactory<T extends Number, R extends NumberRange<T>> {
      R create(StringReader reader, @Nullable T min, @Nullable T max) throws CommandSyntaxException;
   }

   public static class FloatRange extends NumberRange<Double> {
      public static final NumberRange.FloatRange ANY = new NumberRange.FloatRange((Double)null, (Double)null);
      private final Double squaredMin;
      private final Double squaredMax;

      private static NumberRange.FloatRange create(StringReader reader, @Nullable Double double_, @Nullable Double double2) throws CommandSyntaxException {
         if (double_ != null && double2 != null && double_ > double2) {
            throw EXCEPTION_SWAPPED.createWithContext(reader);
         } else {
            return new NumberRange.FloatRange(double_, double2);
         }
      }

      @Nullable
      private static Double square(@Nullable Double double_) {
         return double_ == null ? null : double_ * double_;
      }

      private FloatRange(@Nullable Double double_, @Nullable Double double2) {
         super(double_, double2);
         this.squaredMin = square(double_);
         this.squaredMax = square(double2);
      }

      public static NumberRange.FloatRange exactly(double d) {
         return new NumberRange.FloatRange(d, d);
      }

      public static NumberRange.FloatRange between(double d, double e) {
         return new NumberRange.FloatRange(d, e);
      }

      public static NumberRange.FloatRange atLeast(double d) {
         return new NumberRange.FloatRange(d, (Double)null);
      }

      public static NumberRange.FloatRange atMost(double d) {
         return new NumberRange.FloatRange((Double)null, d);
      }

      public boolean test(double d) {
         if (this.min != null && (Double)this.min > d) {
            return false;
         } else {
            return this.max == null || !((Double)this.max < d);
         }
      }

      public boolean testSqrt(double value) {
         if (this.squaredMin != null && this.squaredMin > value) {
            return false;
         } else {
            return this.squaredMax == null || !(this.squaredMax < value);
         }
      }

      public static NumberRange.FloatRange fromJson(@Nullable JsonElement element) {
         return (NumberRange.FloatRange)fromJson(element, ANY, JsonHelper::asDouble, NumberRange.FloatRange::new);
      }

      public static NumberRange.FloatRange parse(StringReader reader) throws CommandSyntaxException {
         return parse(reader, (double_) -> {
            return double_;
         });
      }

      public static NumberRange.FloatRange parse(StringReader reader, Function<Double, Double> mapper) throws CommandSyntaxException {
         NumberRange.CommandFactory var10001 = NumberRange.FloatRange::create;
         Function var10002 = Double::parseDouble;
         BuiltInExceptionProvider var10003 = CommandSyntaxException.BUILT_IN_EXCEPTIONS;
         Objects.requireNonNull(var10003);
         return (NumberRange.FloatRange)parse(reader, var10001, var10002, var10003::readerInvalidDouble, mapper);
      }
   }

   public static class IntRange extends NumberRange<Integer> {
      public static final NumberRange.IntRange ANY = new NumberRange.IntRange((Integer)null, (Integer)null);
      private final Long minSquared;
      private final Long maxSquared;

      private static NumberRange.IntRange parse(StringReader reader, @Nullable Integer min, @Nullable Integer max) throws CommandSyntaxException {
         if (min != null && max != null && min > max) {
            throw EXCEPTION_SWAPPED.createWithContext(reader);
         } else {
            return new NumberRange.IntRange(min, max);
         }
      }

      @Nullable
      private static Long squared(@Nullable Integer value) {
         return value == null ? null : value.longValue() * value.longValue();
      }

      private IntRange(@Nullable Integer min, @Nullable Integer max) {
         super(min, max);
         this.minSquared = squared(min);
         this.maxSquared = squared(max);
      }

      public static NumberRange.IntRange exactly(int value) {
         return new NumberRange.IntRange(value, value);
      }

      public static NumberRange.IntRange between(int min, int max) {
         return new NumberRange.IntRange(min, max);
      }

      public static NumberRange.IntRange atLeast(int value) {
         return new NumberRange.IntRange(value, (Integer)null);
      }

      public static NumberRange.IntRange atMost(int value) {
         return new NumberRange.IntRange((Integer)null, value);
      }

      public boolean test(int value) {
         if (this.min != null && (Integer)this.min > value) {
            return false;
         } else {
            return this.max == null || (Integer)this.max >= value;
         }
      }

      public boolean method_35288(long l) {
         if (this.minSquared != null && this.minSquared > l) {
            return false;
         } else {
            return this.maxSquared == null || this.maxSquared >= l;
         }
      }

      public static NumberRange.IntRange fromJson(@Nullable JsonElement element) {
         return (NumberRange.IntRange)fromJson(element, ANY, JsonHelper::asInt, NumberRange.IntRange::new);
      }

      public static NumberRange.IntRange parse(StringReader reader) throws CommandSyntaxException {
         return fromStringReader(reader, (integer) -> {
            return integer;
         });
      }

      public static NumberRange.IntRange fromStringReader(StringReader reader, Function<Integer, Integer> converter) throws CommandSyntaxException {
         NumberRange.CommandFactory var10001 = NumberRange.IntRange::parse;
         Function var10002 = Integer::parseInt;
         BuiltInExceptionProvider var10003 = CommandSyntaxException.BUILT_IN_EXCEPTIONS;
         Objects.requireNonNull(var10003);
         return (NumberRange.IntRange)parse(reader, var10001, var10002, var10003::readerInvalidInt, converter);
      }
   }
}
