package net.minecraft.loot.condition;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.loot.LootGsons;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextType;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class LootConditionManager extends JsonDataLoader {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = LootGsons.getConditionGsonBuilder().create();
   private Map<Identifier, LootCondition> conditions = ImmutableMap.of();

   public LootConditionManager() {
      super(GSON, "predicates");
   }

   @Nullable
   public LootCondition get(Identifier id) {
      return (LootCondition)this.conditions.get(id);
   }

   protected void apply(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler) {
      Builder<Identifier, LootCondition> builder = ImmutableMap.builder();
      map.forEach((id, json) -> {
         try {
            if (json.isJsonArray()) {
               LootCondition[] lootConditions = (LootCondition[])GSON.fromJson(json, LootCondition[].class);
               builder.put(id, new LootConditionManager.AndCondition(lootConditions));
            } else {
               LootCondition lootCondition = (LootCondition)GSON.fromJson(json, LootCondition.class);
               builder.put(id, lootCondition);
            }
         } catch (Exception var4) {
            LOGGER.error((String)"Couldn't parse loot table {}", (Object)id, (Object)var4);
         }

      });
      Map<Identifier, LootCondition> map2 = builder.build();
      LootContextType var10002 = LootContextTypes.GENERIC;
      Objects.requireNonNull(map2);
      LootTableReporter lootTableReporter = new LootTableReporter(var10002, map2::get, (id) -> {
         return null;
      });
      map2.forEach((id, condition) -> {
         condition.validate(lootTableReporter.withCondition("{" + id + "}", id));
      });
      lootTableReporter.getMessages().forEach((string, string2) -> {
         LOGGER.warn((String)"Found validation problem in {}: {}", (Object)string, (Object)string2);
      });
      this.conditions = map2;
   }

   public Set<Identifier> getIds() {
      return Collections.unmodifiableSet(this.conditions.keySet());
   }

   private static class AndCondition implements LootCondition {
      private final LootCondition[] terms;
      private final Predicate<LootContext> predicate;

      AndCondition(LootCondition[] lootConditions) {
         this.terms = lootConditions;
         this.predicate = LootConditionTypes.joinAnd(lootConditions);
      }

      public final boolean test(LootContext lootContext) {
         return this.predicate.test(lootContext);
      }

      public void validate(LootTableReporter reporter) {
         LootCondition.super.validate(reporter);

         for(int i = 0; i < this.terms.length; ++i) {
            this.terms[i].validate(reporter.makeChild(".term[" + i + "]"));
         }

      }

      public LootConditionType getType() {
         throw new UnsupportedOperationException();
      }
   }
}
