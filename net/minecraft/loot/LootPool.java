package net.minecraft.loot;

import com.google.common.collect.Lists;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionConsumingBuilder;
import net.minecraft.loot.condition.LootConditionTypes;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.entry.LootPoolEntry;
import net.minecraft.loot.function.LootFunction;
import net.minecraft.loot.function.LootFunctionConsumingBuilder;
import net.minecraft.loot.function.LootFunctionTypes;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.mutable.MutableInt;

public class LootPool {
   final LootPoolEntry[] entries;
   final LootCondition[] conditions;
   private final Predicate<LootContext> predicate;
   final LootFunction[] functions;
   private final BiFunction<ItemStack, LootContext, ItemStack> javaFunctions;
   final LootNumberProvider rolls;
   final LootNumberProvider bonusRolls;

   LootPool(LootPoolEntry[] entries, LootCondition[] conditions, LootFunction[] functions, LootNumberProvider rolls, LootNumberProvider bonusRolls) {
      this.entries = entries;
      this.conditions = conditions;
      this.predicate = LootConditionTypes.joinAnd(conditions);
      this.functions = functions;
      this.javaFunctions = LootFunctionTypes.join(functions);
      this.rolls = rolls;
      this.bonusRolls = bonusRolls;
   }

   private void supplyOnce(Consumer<ItemStack> lootConsumer, LootContext context) {
      Random random = context.getRandom();
      List<LootChoice> list = Lists.newArrayList();
      MutableInt mutableInt = new MutableInt();
      LootPoolEntry[] var6 = this.entries;
      int j = var6.length;

      for(int var8 = 0; var8 < j; ++var8) {
         LootPoolEntry lootPoolEntry = var6[var8];
         lootPoolEntry.expand(context, (choice) -> {
            int i = choice.getWeight(context.getLuck());
            if (i > 0) {
               list.add(choice);
               mutableInt.add(i);
            }

         });
      }

      int i = list.size();
      if (mutableInt.intValue() != 0 && i != 0) {
         if (i == 1) {
            ((LootChoice)list.get(0)).generateLoot(lootConsumer, context);
         } else {
            j = random.nextInt(mutableInt.intValue());
            Iterator var11 = list.iterator();

            LootChoice lootChoice;
            do {
               if (!var11.hasNext()) {
                  return;
               }

               lootChoice = (LootChoice)var11.next();
               j -= lootChoice.getWeight(context.getLuck());
            } while(j >= 0);

            lootChoice.generateLoot(lootConsumer, context);
         }
      }
   }

   public void addGeneratedLoot(Consumer<ItemStack> lootConsumer, LootContext context) {
      if (this.predicate.test(context)) {
         Consumer<ItemStack> consumer = LootFunction.apply(this.javaFunctions, lootConsumer, context);
         int i = this.rolls.nextInt(context) + MathHelper.floor(this.bonusRolls.nextFloat(context) * context.getLuck());

         for(int j = 0; j < i; ++j) {
            this.supplyOnce(consumer, context);
         }

      }
   }

   public void validate(LootTableReporter reporter) {
      int k;
      for(k = 0; k < this.conditions.length; ++k) {
         this.conditions[k].validate(reporter.makeChild(".condition[" + k + "]"));
      }

      for(k = 0; k < this.functions.length; ++k) {
         this.functions[k].validate(reporter.makeChild(".functions[" + k + "]"));
      }

      for(k = 0; k < this.entries.length; ++k) {
         this.entries[k].validate(reporter.makeChild(".entries[" + k + "]"));
      }

      this.rolls.validate(reporter.makeChild(".rolls"));
      this.bonusRolls.validate(reporter.makeChild(".bonusRolls"));
   }

   public static LootPool.Builder builder() {
      return new LootPool.Builder();
   }

   public static class Builder implements LootFunctionConsumingBuilder<LootPool.Builder>, LootConditionConsumingBuilder<LootPool.Builder> {
      private final List<LootPoolEntry> entries = Lists.newArrayList();
      private final List<LootCondition> conditions = Lists.newArrayList();
      private final List<LootFunction> functions = Lists.newArrayList();
      private LootNumberProvider rolls = ConstantLootNumberProvider.create(1.0F);
      private LootNumberProvider bonusRollsRange = ConstantLootNumberProvider.create(0.0F);

      public LootPool.Builder rolls(LootNumberProvider rolls) {
         this.rolls = rolls;
         return this;
      }

      public LootPool.Builder getThis() {
         return this;
      }

      public LootPool.Builder bonusRolls(LootNumberProvider bonusRolls) {
         this.bonusRollsRange = bonusRolls;
         return this;
      }

      public LootPool.Builder with(LootPoolEntry.Builder<?> entry) {
         this.entries.add(entry.build());
         return this;
      }

      public LootPool.Builder conditionally(LootCondition.Builder builder) {
         this.conditions.add(builder.build());
         return this;
      }

      public LootPool.Builder apply(LootFunction.Builder builder) {
         this.functions.add(builder.build());
         return this;
      }

      public LootPool build() {
         if (this.rolls == null) {
            throw new IllegalArgumentException("Rolls not set");
         } else {
            return new LootPool((LootPoolEntry[])this.entries.toArray(new LootPoolEntry[0]), (LootCondition[])this.conditions.toArray(new LootCondition[0]), (LootFunction[])this.functions.toArray(new LootFunction[0]), this.rolls, this.bonusRollsRange);
         }
      }
   }

   public static class Serializer implements JsonDeserializer<LootPool>, JsonSerializer<LootPool> {
      public LootPool deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
         JsonObject jsonObject = JsonHelper.asObject(jsonElement, "loot pool");
         LootPoolEntry[] lootPoolEntrys = (LootPoolEntry[])JsonHelper.deserialize(jsonObject, "entries", jsonDeserializationContext, LootPoolEntry[].class);
         LootCondition[] lootConditions = (LootCondition[])JsonHelper.deserialize(jsonObject, "conditions", new LootCondition[0], jsonDeserializationContext, LootCondition[].class);
         LootFunction[] lootFunctions = (LootFunction[])JsonHelper.deserialize(jsonObject, "functions", new LootFunction[0], jsonDeserializationContext, LootFunction[].class);
         LootNumberProvider lootNumberProvider = (LootNumberProvider)JsonHelper.deserialize(jsonObject, "rolls", jsonDeserializationContext, LootNumberProvider.class);
         LootNumberProvider lootNumberProvider2 = (LootNumberProvider)JsonHelper.deserialize(jsonObject, "bonus_rolls", ConstantLootNumberProvider.create(0.0F), jsonDeserializationContext, LootNumberProvider.class);
         return new LootPool(lootPoolEntrys, lootConditions, lootFunctions, lootNumberProvider, lootNumberProvider2);
      }

      public JsonElement serialize(LootPool lootPool, Type type, JsonSerializationContext jsonSerializationContext) {
         JsonObject jsonObject = new JsonObject();
         jsonObject.add("rolls", jsonSerializationContext.serialize(lootPool.rolls));
         jsonObject.add("bonus_rolls", jsonSerializationContext.serialize(lootPool.bonusRolls));
         jsonObject.add("entries", jsonSerializationContext.serialize(lootPool.entries));
         if (!ArrayUtils.isEmpty((Object[])lootPool.conditions)) {
            jsonObject.add("conditions", jsonSerializationContext.serialize(lootPool.conditions));
         }

         if (!ArrayUtils.isEmpty((Object[])lootPool.functions)) {
            jsonObject.add("functions", jsonSerializationContext.serialize(lootPool.functions));
         }

         return jsonObject;
      }
   }
}
