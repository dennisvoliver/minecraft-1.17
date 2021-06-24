package net.minecraft.stat;

import java.util.Objects;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public class Stat<T> extends ScoreboardCriterion {
   private final StatFormatter formatter;
   private final T value;
   private final StatType<T> type;

   protected Stat(StatType<T> type, T value, StatFormatter formatter) {
      super(getName(type, value));
      this.type = type;
      this.formatter = formatter;
      this.value = value;
   }

   public static <T> String getName(StatType<T> type, T value) {
      String var10000 = getName(Registry.STAT_TYPE.getId(type));
      return var10000 + ":" + getName(type.getRegistry().getId(value));
   }

   private static <T> String getName(@Nullable Identifier id) {
      return id.toString().replace(':', '.');
   }

   public StatType<T> getType() {
      return this.type;
   }

   public T getValue() {
      return this.value;
   }

   public String format(int i) {
      return this.formatter.format(i);
   }

   public boolean equals(Object o) {
      return this == o || o instanceof Stat && Objects.equals(this.getName(), ((Stat)o).getName());
   }

   public int hashCode() {
      return this.getName().hashCode();
   }

   public String toString() {
      String var10000 = this.getName();
      return "Stat{name=" + var10000 + ", formatter=" + this.formatter + "}";
   }
}
