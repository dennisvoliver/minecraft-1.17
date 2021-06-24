package net.minecraft.loot.function;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SuspiciousStewItem;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;

public class SetStewEffectLootFunction extends ConditionalLootFunction {
   final Map<StatusEffect, LootNumberProvider> effects;

   SetStewEffectLootFunction(LootCondition[] lootConditions, Map<StatusEffect, LootNumberProvider> map) {
      super(lootConditions);
      this.effects = ImmutableMap.copyOf(map);
   }

   public LootFunctionType getType() {
      return LootFunctionTypes.SET_STEW_EFFECT;
   }

   public Set<LootContextParameter<?>> getRequiredParameters() {
      return (Set)this.effects.values().stream().flatMap((numberProvider) -> {
         return numberProvider.getRequiredParameters().stream();
      }).collect(ImmutableSet.toImmutableSet());
   }

   public ItemStack process(ItemStack stack, LootContext context) {
      if (stack.isOf(Items.SUSPICIOUS_STEW) && !this.effects.isEmpty()) {
         Random random = context.getRandom();
         int i = random.nextInt(this.effects.size());
         Entry<StatusEffect, LootNumberProvider> entry = (Entry)Iterables.get(this.effects.entrySet(), i);
         StatusEffect statusEffect = (StatusEffect)entry.getKey();
         int j = ((LootNumberProvider)entry.getValue()).nextInt(context);
         if (!statusEffect.isInstant()) {
            j *= 20;
         }

         SuspiciousStewItem.addEffectToStew(stack, statusEffect, j);
         return stack;
      } else {
         return stack;
      }
   }

   public static SetStewEffectLootFunction.Builder builder() {
      return new SetStewEffectLootFunction.Builder();
   }

   public static class Builder extends ConditionalLootFunction.Builder<SetStewEffectLootFunction.Builder> {
      private final Map<StatusEffect, LootNumberProvider> map = Maps.newHashMap();

      protected SetStewEffectLootFunction.Builder getThisBuilder() {
         return this;
      }

      public SetStewEffectLootFunction.Builder withEffect(StatusEffect effect, LootNumberProvider durationRange) {
         this.map.put(effect, durationRange);
         return this;
      }

      public LootFunction build() {
         return new SetStewEffectLootFunction(this.getConditions(), this.map);
      }
   }

   public static class Serializer extends ConditionalLootFunction.Serializer<SetStewEffectLootFunction> {
      public void toJson(JsonObject jsonObject, SetStewEffectLootFunction setStewEffectLootFunction, JsonSerializationContext jsonSerializationContext) {
         super.toJson(jsonObject, (ConditionalLootFunction)setStewEffectLootFunction, jsonSerializationContext);
         if (!setStewEffectLootFunction.effects.isEmpty()) {
            JsonArray jsonArray = new JsonArray();
            Iterator var5 = setStewEffectLootFunction.effects.keySet().iterator();

            while(var5.hasNext()) {
               StatusEffect statusEffect = (StatusEffect)var5.next();
               JsonObject jsonObject2 = new JsonObject();
               Identifier identifier = Registry.STATUS_EFFECT.getId(statusEffect);
               if (identifier == null) {
                  throw new IllegalArgumentException("Don't know how to serialize mob effect " + statusEffect);
               }

               jsonObject2.add("type", new JsonPrimitive(identifier.toString()));
               jsonObject2.add("duration", jsonSerializationContext.serialize(setStewEffectLootFunction.effects.get(statusEffect)));
               jsonArray.add((JsonElement)jsonObject2);
            }

            jsonObject.add("effects", jsonArray);
         }

      }

      public SetStewEffectLootFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
         Map<StatusEffect, LootNumberProvider> map = Maps.newHashMap();
         if (jsonObject.has("effects")) {
            JsonArray jsonArray = JsonHelper.getArray(jsonObject, "effects");
            Iterator var6 = jsonArray.iterator();

            while(var6.hasNext()) {
               JsonElement jsonElement = (JsonElement)var6.next();
               String string = JsonHelper.getString(jsonElement.getAsJsonObject(), "type");
               StatusEffect statusEffect = (StatusEffect)Registry.STATUS_EFFECT.getOrEmpty(new Identifier(string)).orElseThrow(() -> {
                  return new JsonSyntaxException("Unknown mob effect '" + string + "'");
               });
               LootNumberProvider lootNumberProvider = (LootNumberProvider)JsonHelper.deserialize(jsonElement.getAsJsonObject(), "duration", jsonDeserializationContext, LootNumberProvider.class);
               map.put(statusEffect, lootNumberProvider);
            }
         }

         return new SetStewEffectLootFunction(lootConditions, map);
      }
   }
}
