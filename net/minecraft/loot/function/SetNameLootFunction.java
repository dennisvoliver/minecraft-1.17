package net.minecraft.loot.function;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.JsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class SetNameLootFunction extends ConditionalLootFunction {
   private static final Logger LOGGER = LogManager.getLogger();
   final Text name;
   @Nullable
   final LootContext.EntityTarget entity;

   SetNameLootFunction(LootCondition[] lootConditions, @Nullable Text text, @Nullable LootContext.EntityTarget entityTarget) {
      super(lootConditions);
      this.name = text;
      this.entity = entityTarget;
   }

   public LootFunctionType getType() {
      return LootFunctionTypes.SET_NAME;
   }

   public Set<LootContextParameter<?>> getRequiredParameters() {
      return this.entity != null ? ImmutableSet.of(this.entity.getParameter()) : ImmutableSet.of();
   }

   public static UnaryOperator<Text> applySourceEntity(LootContext context, @Nullable LootContext.EntityTarget sourceEntity) {
      if (sourceEntity != null) {
         Entity entity = (Entity)context.get(sourceEntity.getParameter());
         if (entity != null) {
            ServerCommandSource serverCommandSource = entity.getCommandSource().withLevel(2);
            return (textComponent) -> {
               try {
                  return Texts.parse(serverCommandSource, (Text)textComponent, entity, 0);
               } catch (CommandSyntaxException var4) {
                  LOGGER.warn((String)"Failed to resolve text component", (Throwable)var4);
                  return textComponent;
               }
            };
         }
      }

      return (textComponent) -> {
         return textComponent;
      };
   }

   public ItemStack process(ItemStack stack, LootContext context) {
      if (this.name != null) {
         stack.setCustomName((Text)applySourceEntity(context, this.entity).apply(this.name));
      }

      return stack;
   }

   public static ConditionalLootFunction.Builder<?> builder(Text name) {
      return builder((conditions) -> {
         return new SetNameLootFunction(conditions, name, (LootContext.EntityTarget)null);
      });
   }

   public static ConditionalLootFunction.Builder<?> builder(Text name, LootContext.EntityTarget target) {
      return builder((conditions) -> {
         return new SetNameLootFunction(conditions, name, target);
      });
   }

   public static class Serializer extends ConditionalLootFunction.Serializer<SetNameLootFunction> {
      public void toJson(JsonObject jsonObject, SetNameLootFunction setNameLootFunction, JsonSerializationContext jsonSerializationContext) {
         super.toJson(jsonObject, (ConditionalLootFunction)setNameLootFunction, jsonSerializationContext);
         if (setNameLootFunction.name != null) {
            jsonObject.add("name", Text.Serializer.toJsonTree(setNameLootFunction.name));
         }

         if (setNameLootFunction.entity != null) {
            jsonObject.add("entity", jsonSerializationContext.serialize(setNameLootFunction.entity));
         }

      }

      public SetNameLootFunction fromJson(JsonObject jsonObject, JsonDeserializationContext jsonDeserializationContext, LootCondition[] lootConditions) {
         Text text = Text.Serializer.fromJson(jsonObject.get("name"));
         LootContext.EntityTarget entityTarget = (LootContext.EntityTarget)JsonHelper.deserialize(jsonObject, "entity", (Object)null, jsonDeserializationContext, LootContext.EntityTarget.class);
         return new SetNameLootFunction(lootConditions, text, entityTarget);
      }
   }
}
