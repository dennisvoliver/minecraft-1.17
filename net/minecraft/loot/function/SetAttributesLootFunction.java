package net.minecraft.loot.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.provider.number.LootNumberProvider;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.Nullable;

public class SetAttributesLootFunction extends ConditionalLootFunction {
   final List<SetAttributesLootFunction.Attribute> attributes;

   SetAttributesLootFunction(LootCondition[] lootConditions, List<SetAttributesLootFunction.Attribute> list) {
      super(lootConditions);
      this.attributes = ImmutableList.copyOf((Collection)list);
   }

   public LootFunctionType getType() {
      return LootFunctionTypes.SET_ATTRIBUTES;
   }

   public Set<LootContextParameter<?>> getRequiredParameters() {
      return (Set)this.attributes.stream().flatMap((attribute) -> {
         return attribute.amount.getRequiredParameters().stream();
      }).collect(ImmutableSet.toImmutableSet());
   }

   public ItemStack process(ItemStack stack, LootContext context) {
      Random random = context.getRandom();
      Iterator var4 = this.attributes.iterator();

      while(var4.hasNext()) {
         SetAttributesLootFunction.Attribute attribute = (SetAttributesLootFunction.Attribute)var4.next();
         UUID uUID = attribute.id;
         if (uUID == null) {
            uUID = UUID.randomUUID();
         }

         EquipmentSlot equipmentSlot = (EquipmentSlot)Util.getRandom((Object[])attribute.slots, random);
         stack.addAttributeModifier(attribute.attribute, new EntityAttributeModifier(uUID, attribute.name, (double)attribute.amount.nextFloat(context), attribute.operation), equipmentSlot);
      }

      return stack;
   }

   public static SetAttributesLootFunction.AttributeBuilder create(String name, EntityAttribute attribute, EntityAttributeModifier.Operation operation, LootNumberProvider amountRange) {
      return new SetAttributesLootFunction.AttributeBuilder(name, attribute, operation, amountRange);
   }

   public static SetAttributesLootFunction.Builder create() {
      return new SetAttributesLootFunction.Builder();
   }

   private static class Attribute {
      final String name;
      final EntityAttribute attribute;
      final EntityAttributeModifier.Operation operation;
      final LootNumberProvider amount;
      @Nullable
      final UUID id;
      final EquipmentSlot[] slots;

      Attribute(String string, EntityAttribute entityAttribute, EntityAttributeModifier.Operation operation, LootNumberProvider lootNumberProvider, EquipmentSlot[] equipmentSlots, @Nullable UUID uUID) {
         this.name = string;
         this.attribute = entityAttribute;
         this.operation = operation;
         this.amount = lootNumberProvider;
         this.id = uUID;
         this.slots = equipmentSlots;
      }

      public JsonObject serialize(JsonSerializationContext context) {
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("name", this.name);
         jsonObject.addProperty("attribute", Registry.ATTRIBUTE.getId(this.attribute).toString());
         jsonObject.addProperty("operation", getName(this.operation));
         jsonObject.add("amount", context.serialize(this.amount));
         if (this.id != null) {
            jsonObject.addProperty("id", this.id.toString());
         }

         if (this.slots.length == 1) {
            jsonObject.addProperty("slot", this.slots[0].getName());
         } else {
            JsonArray jsonArray = new JsonArray();
            EquipmentSlot[] var4 = this.slots;
            int var5 = var4.length;

            for(int var6 = 0; var6 < var5; ++var6) {
               EquipmentSlot equipmentSlot = var4[var6];
               jsonArray.add((JsonElement)(new JsonPrimitive(equipmentSlot.getName())));
            }

            jsonObject.add("slot", jsonArray);
         }

         return jsonObject;
      }

      public static SetAttributesLootFunction.Attribute deserialize(JsonObject json, JsonDeserializationContext context) {
         String string = JsonHelper.getString(json, "name");
         Identifier identifier = new Identifier(JsonHelper.getString(json, "attribute"));
         EntityAttribute entityAttribute = (EntityAttribute)Registry.ATTRIBUTE.get(identifier);
         if (entityAttribute == null) {
            throw new JsonSyntaxException("Unknown attribute: " + identifier);
         } else {
            EntityAttributeModifier.Operation operation = fromName(JsonHelper.getString(json, "operation"));
            LootNumberProvider lootNumberProvider = (LootNumberProvider)JsonHelper.deserialize(json, "amount", context, LootNumberProvider.class);
            UUID uUID = null;
            EquipmentSlot[] equipmentSlots2;
            if (JsonHelper.hasString(json, "slot")) {
               equipmentSlots2 = new EquipmentSlot[]{EquipmentSlot.byName(JsonHelper.getString(json, "slot"))};
            } else {
               if (!JsonHelper.hasArray(json, "slot")) {
                  throw new JsonSyntaxException("Invalid or missing attribute modifier slot; must be either string or array of strings.");
               }

               JsonArray jsonArray = JsonHelper.getArray(json, "slot");
               equipmentSlots2 = new EquipmentSlot[jsonArray.size()];
               int i = 0;

               JsonElement jsonElement;
               for(Iterator var11 = jsonArray.iterator(); var11.hasNext(); equipmentSlots2[i++] = EquipmentSlot.byName(JsonHelper.asString(jsonElement, "slot"))) {
                  jsonElement = (JsonElement)var11.next();
               }

               if (equipmentSlots2.length == 0) {
                  throw new JsonSyntaxException("Invalid attribute modifier slot; must contain at least one entry.");
               }
            }

            if (json.has("id")) {
               String string2 = JsonHelper.getString(json, "id");

               try {
                  uUID = UUID.fromString(string2);
               } catch (IllegalArgumentException var13) {
                  throw new JsonSyntaxException("Invalid attribute modifier id '" + string2 + "' (must be UUID format, with dashes)");
               }
            }

            return new SetAttributesLootFunction.Attribute(string, entityAttribute, operation, lootNumberProvider, equipmentSlots2, uUID);
         }
      }

      private static String getName(EntityAttributeModifier.Operation operation) {
         switch(operation) {
         case ADDITION:
            return "addition";
         case MULTIPLY_BASE:
            return "multiply_base";
         case MULTIPLY_TOTAL:
            return "multiply_total";
         default:
            throw new IllegalArgumentException("Unknown operation " + operation);
         }
      }

      private static EntityAttributeModifier.Operation fromName(String name) {
         byte var2 = -1;
         switch(name.hashCode()) {
         case -1226589444:
            if (name.equals("addition")) {
               var2 = 0;
            }
            break;
         case -78229492:
            if (name.equals("multiply_base")) {
               var2 = 1;
            }
            break;
         case 1886894441:
            if (name.equals("multiply_total")) {
               var2 = 2;
            }
         }

         switch(var2) {
         case 0:
            return EntityAttributeModifier.Operation.ADDITION;
         case 1:
            return EntityAttributeModifier.Operation.MULTIPLY_BASE;
         case 2:
            return EntityAttributeModifier.Operation.MULTIPLY_TOTAL;
         default:
            throw new JsonSyntaxException("Unknown attribute modifier operation " + name);
         }
      }
   }

   public static class AttributeBuilder {
      private final String name;
      private final EntityAttribute attribute;
      private final EntityAttributeModifier.Operation operation;
      private final LootNumberProvider amount;
      @Nullable
      private UUID uuid;
      private final Set<EquipmentSlot> slots = EnumSet.noneOf(EquipmentSlot.class);

      public AttributeBuilder(String name, EntityAttribute attribute, EntityAttributeModifier.Operation operation, LootNumberProvider amount) {
         this.name = name;
         this.attribute = attribute;
         this.operation = operation;
         this.amount = amount;
      }

      public SetAttributesLootFunction.AttributeBuilder slot(EquipmentSlot slot) {
         this.slots.add(slot);
         return this;
      }

      public SetAttributesLootFunction.AttributeBuilder uuid(UUID uuid) {
         this.uuid = uuid;
         return this;
      }

      public SetAttributesLootFunction.Attribute build() {
         return new SetAttributesLootFunction.Attribute(this.name, this.attribute, this.operation, this.amount, (EquipmentSlot[])this.slots.toArray(new EquipmentSlot[0]), this.uuid);
      }
   }

   public static class Builder extends ConditionalLootFunction.Builder<SetAttributesLootFunction.Builder> {
      private final List<SetAttributesLootFunction.Attribute> attributes = Lists.newArrayList();

      protected SetAttributesLootFunction.Builder getThisBuilder() {
         return this;
      }

      public SetAttributesLootFunction.Builder attribute(SetAttributesLootFunction.AttributeBuilder attribute) {
         this.attributes.add(attribute.build());
         return this;
      }

      public LootFunction build() {
         return new SetAttributesLootFunction(this.getConditions(), this.attributes);
      }
   }

   public static class Serializer extends ConditionalLootFunction.Serializer<SetAttributesLootFunction> {
      public void toJson(JsonObject jsonObject, SetAttributesLootFunction setAttributesLootFunction, JsonSerializationContext jsonSerializationContext) {
         super.toJson(jsonObject, (ConditionalLootFunction)setAttributesLootFunction, jsonSerializationContext);
         JsonArray jsonArray = new JsonArray();
         Iterator var5 = setAttributesLootFunction.attributes.iterator();

         while(var5.hasNext()) {
            SetAttributesLootFunction.Attribute attribute = (SetAttributesLootFunction.Attribute)var5.next();
            jsonArray.add((JsonElement)attribute.serialize(jsonSerializationContext));
         }

         jsonObject.add("modifiers", jsonArray);
      }

      public SetAttributesLootFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
         JsonArray jsonArray = JsonHelper.getArray(jsonObject, "modifiers");
         List<SetAttributesLootFunction.Attribute> list = Lists.newArrayListWithExpectedSize(jsonArray.size());
         Iterator var6 = jsonArray.iterator();

         while(var6.hasNext()) {
            JsonElement jsonElement = (JsonElement)var6.next();
            list.add(SetAttributesLootFunction.Attribute.deserialize(JsonHelper.asObject(jsonElement, "modifier"), jsonDeserializationContext));
         }

         if (list.isEmpty()) {
            throw new JsonSyntaxException("Invalid attribute modifiers array; cannot be empty");
         } else {
            return new SetAttributesLootFunction(lootConditions, list);
         }
      }
   }
}
