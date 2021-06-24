package net.minecraft.predicate.entity;

import com.google.common.base.Joiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import net.minecraft.entity.EntityType;
import net.minecraft.tag.ServerTagManagerHolder;
import net.minecraft.tag.Tag;
import net.minecraft.tag.TagManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import org.jetbrains.annotations.Nullable;

public abstract class EntityTypePredicate {
   public static final EntityTypePredicate ANY = new EntityTypePredicate() {
      public boolean matches(EntityType<?> type) {
         return true;
      }

      public JsonElement toJson() {
         return JsonNull.INSTANCE;
      }
   };
   private static final Joiner COMMA_JOINER = Joiner.on(", ");

   public abstract boolean matches(EntityType<?> type);

   public abstract JsonElement toJson();

   public static EntityTypePredicate fromJson(@Nullable JsonElement json) {
      if (json != null && !json.isJsonNull()) {
         String string = JsonHelper.asString(json, "type");
         Identifier identifier2;
         if (string.startsWith("#")) {
            identifier2 = new Identifier(string.substring(1));
            return new EntityTypePredicate.Tagged(ServerTagManagerHolder.getTagManager().getTag(Registry.ENTITY_TYPE_KEY, identifier2, (identifier) -> {
               return new JsonSyntaxException("Unknown entity tag '" + identifier + "'");
            }));
         } else {
            identifier2 = new Identifier(string);
            EntityType<?> entityType = (EntityType)Registry.ENTITY_TYPE.getOrEmpty(identifier2).orElseThrow(() -> {
               return new JsonSyntaxException("Unknown entity type '" + identifier2 + "', valid types are: " + COMMA_JOINER.join((Iterable)Registry.ENTITY_TYPE.getIds()));
            });
            return new EntityTypePredicate.Single(entityType);
         }
      } else {
         return ANY;
      }
   }

   public static EntityTypePredicate create(EntityType<?> type) {
      return new EntityTypePredicate.Single(type);
   }

   public static EntityTypePredicate create(Tag<EntityType<?>> tag) {
      return new EntityTypePredicate.Tagged(tag);
   }

   static class Tagged extends EntityTypePredicate {
      private final Tag<EntityType<?>> tag;

      public Tagged(Tag<EntityType<?>> tag) {
         this.tag = tag;
      }

      public boolean matches(EntityType<?> type) {
         return type.isIn(this.tag);
      }

      public JsonElement toJson() {
         TagManager var10002 = ServerTagManagerHolder.getTagManager();
         RegistryKey var10003 = Registry.ENTITY_TYPE_KEY;
         Tag var10004 = this.tag;
         return new JsonPrimitive("#" + var10002.getTagId(var10003, var10004, () -> {
            return new IllegalStateException("Unknown entity type tag");
         }));
      }
   }

   static class Single extends EntityTypePredicate {
      private final EntityType<?> type;

      public Single(EntityType<?> type) {
         this.type = type;
      }

      public boolean matches(EntityType<?> type) {
         return this.type == type;
      }

      public JsonElement toJson() {
         return new JsonPrimitive(Registry.ENTITY_TYPE.getId(this.type).toString());
      }
   }
}
