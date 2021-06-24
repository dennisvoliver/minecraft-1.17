package net.minecraft.stat;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public class StatType<T> implements Iterable<Stat<T>> {
   private final Registry<T> registry;
   private final Map<T, Stat<T>> stats = new IdentityHashMap();
   @Nullable
   private Text name;

   public StatType(Registry<T> registry) {
      this.registry = registry;
   }

   public boolean hasStat(T key) {
      return this.stats.containsKey(key);
   }

   public Stat<T> getOrCreateStat(T key, StatFormatter formatter) {
      return (Stat)this.stats.computeIfAbsent(key, (object) -> {
         return new Stat(this, object, formatter);
      });
   }

   public Registry<T> getRegistry() {
      return this.registry;
   }

   public Iterator<Stat<T>> iterator() {
      return this.stats.values().iterator();
   }

   public Stat<T> getOrCreateStat(T key) {
      return this.getOrCreateStat(key, StatFormatter.DEFAULT);
   }

   public String getTranslationKey() {
      String var10000 = Registry.STAT_TYPE.getId(this).toString();
      return "stat_type." + var10000.replace(':', '.');
   }

   public Text getName() {
      if (this.name == null) {
         this.name = new TranslatableText(this.getTranslationKey());
      }

      return this.name;
   }
}
