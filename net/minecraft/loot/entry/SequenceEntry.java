package net.minecraft.loot.entry;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.loot.condition.LootCondition;

public class SequenceEntry extends CombinedEntry {
   SequenceEntry(LootPoolEntry[] lootPoolEntrys, LootCondition[] lootConditions) {
      super(lootPoolEntrys, lootConditions);
   }

   public LootPoolEntryType getType() {
      return LootPoolEntryTypes.GROUP;
   }

   protected EntryCombiner combine(EntryCombiner[] children) {
      switch(children.length) {
      case 0:
         return ALWAYS_TRUE;
      case 1:
         return children[0];
      case 2:
         EntryCombiner entryCombiner = children[0];
         EntryCombiner entryCombiner2 = children[1];
         return (context, consumer) -> {
            entryCombiner.expand(context, consumer);
            entryCombiner2.expand(context, consumer);
            return true;
         };
      default:
         return (context, lootChoiceExpander) -> {
            EntryCombiner[] var3 = children;
            int var4 = children.length;

            for(int var5 = 0; var5 < var4; ++var5) {
               EntryCombiner entryCombiner = var3[var5];
               entryCombiner.expand(context, lootChoiceExpander);
            }

            return true;
         };
      }
   }

   public static SequenceEntry.Builder create(LootPoolEntry.Builder<?>... entries) {
      return new SequenceEntry.Builder(entries);
   }

   public static class Builder extends LootPoolEntry.Builder<SequenceEntry.Builder> {
      private final List<LootPoolEntry> entries = Lists.newArrayList();

      public Builder(LootPoolEntry.Builder<?>... entries) {
         LootPoolEntry.Builder[] var2 = entries;
         int var3 = entries.length;

         for(int var4 = 0; var4 < var3; ++var4) {
            LootPoolEntry.Builder<?> builder = var2[var4];
            this.entries.add(builder.build());
         }

      }

      protected SequenceEntry.Builder getThisBuilder() {
         return this;
      }

      public SequenceEntry.Builder sequenceEntry(LootPoolEntry.Builder<?> entry) {
         this.entries.add(entry.build());
         return this;
      }

      public LootPoolEntry build() {
         return new SequenceEntry((LootPoolEntry[])this.entries.toArray(new LootPoolEntry[0]), this.getConditions());
      }
   }
}
