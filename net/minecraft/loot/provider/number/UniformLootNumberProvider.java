package net.minecraft.loot.provider.number;

import com.google.common.collect.Sets;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import java.util.Set;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.JsonSerializer;
import net.minecraft.util.math.MathHelper;

public class UniformLootNumberProvider implements LootNumberProvider {
   final LootNumberProvider min;
   final LootNumberProvider max;

   UniformLootNumberProvider(LootNumberProvider lootNumberProvider, LootNumberProvider lootNumberProvider2) {
      this.min = lootNumberProvider;
      this.max = lootNumberProvider2;
   }

   public LootNumberProviderType getType() {
      return LootNumberProviderTypes.UNIFORM;
   }

   public static UniformLootNumberProvider create(float min, float max) {
      return new UniformLootNumberProvider(ConstantLootNumberProvider.create(min), ConstantLootNumberProvider.create(max));
   }

   public int nextInt(LootContext context) {
      return MathHelper.nextInt(context.getRandom(), this.min.nextInt(context), this.max.nextInt(context));
   }

   public float nextFloat(LootContext context) {
      return MathHelper.nextFloat(context.getRandom(), this.min.nextFloat(context), this.max.nextFloat(context));
   }

   public Set<LootContextParameter<?>> getRequiredParameters() {
      return Sets.union(this.min.getRequiredParameters(), this.max.getRequiredParameters());
   }

   public static class Serializer implements JsonSerializer<UniformLootNumberProvider> {
      public UniformLootNumberProvider fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext) {
         LootNumberProvider lootNumberProvider = (LootNumberProvider)JsonHelper.deserialize(jsonObject, "min", jsonDeserializationContext, LootNumberProvider.class);
         LootNumberProvider lootNumberProvider2 = (LootNumberProvider)JsonHelper.deserialize(jsonObject, "max", jsonDeserializationContext, LootNumberProvider.class);
         return new UniformLootNumberProvider(lootNumberProvider, lootNumberProvider2);
      }

      public void toJson(JsonObject jsonObject, UniformLootNumberProvider uniformLootNumberProvider, JsonSerializationContext jsonSerializationContext) {
         jsonObject.add("min", jsonSerializationContext.serialize(uniformLootNumberProvider.min));
         jsonObject.add("max", jsonSerializationContext.serialize(uniformLootNumberProvider.max));
      }
   }
}
