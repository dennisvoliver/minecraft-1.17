package net.minecraft.loot.function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootGsons;
import net.minecraft.loot.LootManager;
import net.minecraft.loot.LootTableReporter;
import net.minecraft.loot.condition.LootConditionManager;
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

public class LootFunctionManager extends JsonDataLoader {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = LootGsons.getFunctionGsonBuilder().create();
   private final LootConditionManager lootConditionManager;
   private final LootManager lootManager;
   private Map<Identifier, LootFunction> functions = ImmutableMap.of();

   public LootFunctionManager(LootConditionManager lootConditionManager, LootManager lootManager) {
      super(GSON, "item_modifiers");
      this.lootConditionManager = lootConditionManager;
      this.lootManager = lootManager;
   }

   @Nullable
   public LootFunction get(Identifier id) {
      return (LootFunction)this.functions.get(id);
   }

   public LootFunction getOrDefault(Identifier id, LootFunction fallback) {
      return (LootFunction)this.functions.getOrDefault(id, fallback);
   }

   protected void apply(Map<Identifier, JsonElement> map, ResourceManager resourceManager, Profiler profiler) {
      Builder<Identifier, LootFunction> builder = ImmutableMap.builder();
      map.forEach((id, json) -> {
         try {
            if (json.isJsonArray()) {
               LootFunction[] lootFunctions = (LootFunction[])GSON.fromJson(json, LootFunction[].class);
               builder.put(id, new LootFunctionManager.AndFunction(lootFunctions));
            } else {
               LootFunction lootFunction = (LootFunction)GSON.fromJson(json, LootFunction.class);
               builder.put(id, lootFunction);
            }
         } catch (Exception var4) {
            LOGGER.error((String)"Couldn't parse item modifier {}", (Object)id, (Object)var4);
         }

      });
      Map<Identifier, LootFunction> map2 = builder.build();
      LootContextType var10002 = LootContextTypes.GENERIC;
      LootConditionManager var10003 = this.lootConditionManager;
      Objects.requireNonNull(var10003);
      Function var7 = var10003::get;
      LootManager var10004 = this.lootManager;
      Objects.requireNonNull(var10004);
      LootTableReporter lootTableReporter = new LootTableReporter(var10002, var7, var10004::getTable);
      map2.forEach((id, lootFunction) -> {
         lootFunction.validate(lootTableReporter);
      });
      lootTableReporter.getMessages().forEach((string, string2) -> {
         LOGGER.warn((String)"Found item modifier validation problem in {}: {}", (Object)string, (Object)string2);
      });
      this.functions = map2;
   }

   public Set<Identifier> getFunctionIds() {
      return Collections.unmodifiableSet(this.functions.keySet());
   }

   static class AndFunction implements LootFunction {
      protected final LootFunction[] functions;
      private final BiFunction<ItemStack, LootContext, ItemStack> field_27905;

      public AndFunction(LootFunction[] functions) {
         this.functions = functions;
         this.field_27905 = LootFunctionTypes.join(functions);
      }

      public ItemStack apply(ItemStack itemStack, LootContext lootContext) {
         return (ItemStack)this.field_27905.apply(itemStack, lootContext);
      }

      public LootFunctionType getType() {
         throw new UnsupportedOperationException();
      }
   }
}
