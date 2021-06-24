package net.minecraft.predicate;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.criterion.CriterionProgress;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.recipe.book.RecipeBook;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.StatHandler;
import net.minecraft.stat.StatType;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.GameMode;
import org.jetbrains.annotations.Nullable;

public class PlayerPredicate {
   public static final PlayerPredicate ANY = (new PlayerPredicate.Builder()).build();
   public static final int field_33928 = 100;
   private final NumberRange.IntRange experienceLevel;
   @Nullable
   private final GameMode gameMode;
   private final Map<Stat<?>, NumberRange.IntRange> stats;
   private final Object2BooleanMap<Identifier> recipes;
   private final Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements;
   private final EntityPredicate lookingAt;

   private static PlayerPredicate.AdvancementPredicate criterionFromJson(JsonElement json) {
      if (json.isJsonPrimitive()) {
         boolean bl = json.getAsBoolean();
         return new PlayerPredicate.CompletedAdvancementPredicate(bl);
      } else {
         Object2BooleanMap<String> object2BooleanMap = new Object2BooleanOpenHashMap();
         JsonObject jsonObject = JsonHelper.asObject(json, "criterion data");
         jsonObject.entrySet().forEach((entry) -> {
            boolean bl = JsonHelper.asBoolean((JsonElement)entry.getValue(), "criterion test");
            object2BooleanMap.put((String)entry.getKey(), bl);
         });
         return new PlayerPredicate.AdvancementCriteriaPredicate(object2BooleanMap);
      }
   }

   PlayerPredicate(NumberRange.IntRange experienceLevel, @Nullable GameMode gameMode, Map<Stat<?>, NumberRange.IntRange> stats, Object2BooleanMap<Identifier> recipes, Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements, EntityPredicate lookingAt) {
      this.experienceLevel = experienceLevel;
      this.gameMode = gameMode;
      this.stats = stats;
      this.recipes = recipes;
      this.advancements = advancements;
      this.lookingAt = lookingAt;
   }

   public boolean test(Entity entity) {
      if (this == ANY) {
         return true;
      } else if (!(entity instanceof ServerPlayerEntity)) {
         return false;
      } else {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
         if (!this.experienceLevel.test(serverPlayerEntity.experienceLevel)) {
            return false;
         } else if (this.gameMode != null && this.gameMode != serverPlayerEntity.interactionManager.getGameMode()) {
            return false;
         } else {
            StatHandler statHandler = serverPlayerEntity.getStatHandler();
            Iterator var4 = this.stats.entrySet().iterator();

            while(var4.hasNext()) {
               Entry<Stat<?>, NumberRange.IntRange> entry = (Entry)var4.next();
               int i = statHandler.getStat((Stat)entry.getKey());
               if (!((NumberRange.IntRange)entry.getValue()).test(i)) {
                  return false;
               }
            }

            RecipeBook recipeBook = serverPlayerEntity.getRecipeBook();
            ObjectIterator var11 = this.recipes.object2BooleanEntrySet().iterator();

            while(var11.hasNext()) {
               it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry<Identifier> entry2 = (it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry)var11.next();
               if (recipeBook.contains((Identifier)entry2.getKey()) != entry2.getBooleanValue()) {
                  return false;
               }
            }

            if (!this.advancements.isEmpty()) {
               PlayerAdvancementTracker playerAdvancementTracker = serverPlayerEntity.getAdvancementTracker();
               ServerAdvancementLoader serverAdvancementLoader = serverPlayerEntity.getServer().getAdvancementLoader();
               Iterator var7 = this.advancements.entrySet().iterator();

               while(var7.hasNext()) {
                  Entry<Identifier, PlayerPredicate.AdvancementPredicate> entry3 = (Entry)var7.next();
                  Advancement advancement = serverAdvancementLoader.get((Identifier)entry3.getKey());
                  if (advancement == null || !((PlayerPredicate.AdvancementPredicate)entry3.getValue()).test(playerAdvancementTracker.getProgress(advancement))) {
                     return false;
                  }
               }
            }

            if (this.lookingAt != EntityPredicate.ANY) {
               Vec3d vec3d = serverPlayerEntity.getEyePos();
               Vec3d vec3d2 = serverPlayerEntity.getRotationVec(1.0F);
               Vec3d vec3d3 = vec3d.add(vec3d2.x * 100.0D, vec3d2.y * 100.0D, vec3d2.z * 100.0D);
               EntityHitResult entityHitResult = ProjectileUtil.method_37226(serverPlayerEntity.world, serverPlayerEntity, vec3d, vec3d3, (new Box(vec3d, vec3d3)).expand(1.0D), (entityx) -> {
                  return !entityx.isSpectator();
               }, 0.0F);
               if (entityHitResult == null || entityHitResult.getType() != HitResult.Type.ENTITY) {
                  return false;
               }

               Entity entity2 = entityHitResult.getEntity();
               if (!this.lookingAt.test(serverPlayerEntity, entity2) || !serverPlayerEntity.canSee(entity2)) {
                  return false;
               }
            }

            return true;
         }
      }
   }

   public static PlayerPredicate fromJson(@Nullable JsonElement json) {
      if (json != null && !json.isJsonNull()) {
         JsonObject jsonObject = JsonHelper.asObject(json, "player");
         NumberRange.IntRange intRange = NumberRange.IntRange.fromJson(jsonObject.get("level"));
         String string = JsonHelper.getString(jsonObject, "gamemode", "");
         GameMode gameMode = GameMode.byName(string, (GameMode)null);
         Map<Stat<?>, NumberRange.IntRange> map = Maps.newHashMap();
         JsonArray jsonArray = JsonHelper.getArray(jsonObject, "stats", (JsonArray)null);
         if (jsonArray != null) {
            Iterator var7 = jsonArray.iterator();

            while(var7.hasNext()) {
               JsonElement jsonElement = (JsonElement)var7.next();
               JsonObject jsonObject2 = JsonHelper.asObject(jsonElement, "stats entry");
               Identifier identifier = new Identifier(JsonHelper.getString(jsonObject2, "type"));
               StatType<?> statType = (StatType)Registry.STAT_TYPE.get(identifier);
               if (statType == null) {
                  throw new JsonParseException("Invalid stat type: " + identifier);
               }

               Identifier identifier2 = new Identifier(JsonHelper.getString(jsonObject2, "stat"));
               Stat<?> stat = getStat(statType, identifier2);
               NumberRange.IntRange intRange2 = NumberRange.IntRange.fromJson(jsonObject2.get("value"));
               map.put(stat, intRange2);
            }
         }

         Object2BooleanMap<Identifier> object2BooleanMap = new Object2BooleanOpenHashMap();
         JsonObject jsonObject3 = JsonHelper.getObject(jsonObject, "recipes", new JsonObject());
         Iterator var17 = jsonObject3.entrySet().iterator();

         while(var17.hasNext()) {
            Entry<String, JsonElement> entry = (Entry)var17.next();
            Identifier identifier3 = new Identifier((String)entry.getKey());
            boolean bl = JsonHelper.asBoolean((JsonElement)entry.getValue(), "recipe present");
            object2BooleanMap.put(identifier3, bl);
         }

         Map<Identifier, PlayerPredicate.AdvancementPredicate> map2 = Maps.newHashMap();
         JsonObject jsonObject4 = JsonHelper.getObject(jsonObject, "advancements", new JsonObject());
         Iterator var22 = jsonObject4.entrySet().iterator();

         while(var22.hasNext()) {
            Entry<String, JsonElement> entry2 = (Entry)var22.next();
            Identifier identifier4 = new Identifier((String)entry2.getKey());
            PlayerPredicate.AdvancementPredicate advancementPredicate = criterionFromJson((JsonElement)entry2.getValue());
            map2.put(identifier4, advancementPredicate);
         }

         EntityPredicate entityPredicate = EntityPredicate.fromJson(jsonObject.get("looking_at"));
         return new PlayerPredicate(intRange, gameMode, map, object2BooleanMap, map2, entityPredicate);
      } else {
         return ANY;
      }
   }

   private static <T> Stat<T> getStat(StatType<T> type, Identifier id) {
      Registry<T> registry = type.getRegistry();
      T object = registry.get(id);
      if (object == null) {
         throw new JsonParseException("Unknown object " + id + " for stat type " + Registry.STAT_TYPE.getId(type));
      } else {
         return type.getOrCreateStat(object);
      }
   }

   private static <T> Identifier getStatId(Stat<T> stat) {
      return stat.getType().getRegistry().getId(stat.getValue());
   }

   public JsonElement toJson() {
      if (this == ANY) {
         return JsonNull.INSTANCE;
      } else {
         JsonObject jsonObject = new JsonObject();
         jsonObject.add("level", this.experienceLevel.toJson());
         if (this.gameMode != null) {
            jsonObject.addProperty("gamemode", this.gameMode.getName());
         }

         if (!this.stats.isEmpty()) {
            JsonArray jsonArray = new JsonArray();
            this.stats.forEach((stat, intRange) -> {
               JsonObject jsonObject = new JsonObject();
               jsonObject.addProperty("type", Registry.STAT_TYPE.getId(stat.getType()).toString());
               jsonObject.addProperty("stat", getStatId(stat).toString());
               jsonObject.add("value", intRange.toJson());
               jsonArray.add((JsonElement)jsonObject);
            });
            jsonObject.add("stats", jsonArray);
         }

         JsonObject jsonObject3;
         if (!this.recipes.isEmpty()) {
            jsonObject3 = new JsonObject();
            this.recipes.forEach((id, boolean_) -> {
               jsonObject3.addProperty(id.toString(), boolean_);
            });
            jsonObject.add("recipes", jsonObject3);
         }

         if (!this.advancements.isEmpty()) {
            jsonObject3 = new JsonObject();
            this.advancements.forEach((id, advancementPredicate) -> {
               jsonObject3.add(id.toString(), advancementPredicate.toJson());
            });
            jsonObject.add("advancements", jsonObject3);
         }

         jsonObject.add("looking_at", this.lookingAt.toJson());
         return jsonObject;
      }
   }

   static class CompletedAdvancementPredicate implements PlayerPredicate.AdvancementPredicate {
      private final boolean done;

      public CompletedAdvancementPredicate(boolean done) {
         this.done = done;
      }

      public JsonElement toJson() {
         return new JsonPrimitive(this.done);
      }

      public boolean test(AdvancementProgress advancementProgress) {
         return advancementProgress.isDone() == this.done;
      }
   }

   static class AdvancementCriteriaPredicate implements PlayerPredicate.AdvancementPredicate {
      private final Object2BooleanMap<String> criteria;

      public AdvancementCriteriaPredicate(Object2BooleanMap<String> criteria) {
         this.criteria = criteria;
      }

      public JsonElement toJson() {
         JsonObject jsonObject = new JsonObject();
         Object2BooleanMap var10000 = this.criteria;
         Objects.requireNonNull(jsonObject);
         var10000.forEach(jsonObject::addProperty);
         return jsonObject;
      }

      public boolean test(AdvancementProgress advancementProgress) {
         ObjectIterator var2 = this.criteria.object2BooleanEntrySet().iterator();

         it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry entry;
         CriterionProgress criterionProgress;
         do {
            if (!var2.hasNext()) {
               return true;
            }

            entry = (it.unimi.dsi.fastutil.objects.Object2BooleanMap.Entry)var2.next();
            criterionProgress = advancementProgress.getCriterionProgress((String)entry.getKey());
         } while(criterionProgress != null && criterionProgress.isObtained() == entry.getBooleanValue());

         return false;
      }
   }

   private interface AdvancementPredicate extends Predicate<AdvancementProgress> {
      JsonElement toJson();
   }

   public static class Builder {
      private NumberRange.IntRange experienceLevel;
      @Nullable
      private GameMode gameMode;
      private final Map<Stat<?>, NumberRange.IntRange> stats;
      private final Object2BooleanMap<Identifier> recipes;
      private final Map<Identifier, PlayerPredicate.AdvancementPredicate> advancements;
      private EntityPredicate lookingAt;

      public Builder() {
         this.experienceLevel = NumberRange.IntRange.ANY;
         this.stats = Maps.newHashMap();
         this.recipes = new Object2BooleanOpenHashMap();
         this.advancements = Maps.newHashMap();
         this.lookingAt = EntityPredicate.ANY;
      }

      public static PlayerPredicate.Builder create() {
         return new PlayerPredicate.Builder();
      }

      public PlayerPredicate.Builder experienceLevel(NumberRange.IntRange experienceLevel) {
         this.experienceLevel = experienceLevel;
         return this;
      }

      public PlayerPredicate.Builder stat(Stat<?> stat, NumberRange.IntRange value) {
         this.stats.put(stat, value);
         return this;
      }

      public PlayerPredicate.Builder recipe(Identifier id, boolean unlocked) {
         this.recipes.put(id, unlocked);
         return this;
      }

      public PlayerPredicate.Builder gameMode(GameMode gameMode) {
         this.gameMode = gameMode;
         return this;
      }

      public PlayerPredicate.Builder lookingAt(EntityPredicate lookingAt) {
         this.lookingAt = lookingAt;
         return this;
      }

      public PlayerPredicate.Builder advancement(Identifier id, boolean done) {
         this.advancements.put(id, new PlayerPredicate.CompletedAdvancementPredicate(done));
         return this;
      }

      public PlayerPredicate.Builder advancement(Identifier id, Map<String, Boolean> criteria) {
         this.advancements.put(id, new PlayerPredicate.AdvancementCriteriaPredicate(new Object2BooleanOpenHashMap(criteria)));
         return this;
      }

      public PlayerPredicate build() {
         return new PlayerPredicate(this.experienceLevel, this.gameMode, this.stats, this.recipes, this.advancements, this.lookingAt);
      }
   }
}
