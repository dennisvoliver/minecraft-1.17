package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.entity.player.PlayerEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class UserCache {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int MAX_SAVED_ENTRIES = 1000;
   private static final int field_29789 = 1;
   private static boolean useRemote;
   private final Map<String, UserCache.Entry> byName = Maps.newConcurrentMap();
   private final Map<UUID, UserCache.Entry> byUuid = Maps.newConcurrentMap();
   private final Map<String, CompletableFuture<GameProfile>> field_33860 = Maps.newConcurrentMap();
   private final GameProfileRepository profileRepository;
   private final Gson gson = (new GsonBuilder()).create();
   private final File cacheFile;
   private final AtomicLong accessCount = new AtomicLong();
   @Nullable
   private Executor field_33861;

   public UserCache(GameProfileRepository profileRepository, File cacheFile) {
      this.profileRepository = profileRepository;
      this.cacheFile = cacheFile;
      Lists.reverse(this.load()).forEach(this::add);
   }

   private void add(UserCache.Entry entry) {
      GameProfile gameProfile = entry.getProfile();
      entry.setLastAccessed(this.incrementAndGetAccessCount());
      String string = gameProfile.getName();
      if (string != null) {
         this.byName.put(string.toLowerCase(Locale.ROOT), entry);
      }

      UUID uUID = gameProfile.getId();
      if (uUID != null) {
         this.byUuid.put(uUID, entry);
      }

   }

   @Nullable
   private static GameProfile findProfileByName(GameProfileRepository repository, String name) {
      final AtomicReference<GameProfile> atomicReference = new AtomicReference();
      ProfileLookupCallback profileLookupCallback = new ProfileLookupCallback() {
         public void onProfileLookupSucceeded(GameProfile profile) {
            atomicReference.set(profile);
         }

         public void onProfileLookupFailed(GameProfile profile, Exception exception) {
            atomicReference.set((Object)null);
         }
      };
      repository.findProfilesByNames(new String[]{name}, Agent.MINECRAFT, profileLookupCallback);
      GameProfile gameProfile = (GameProfile)atomicReference.get();
      if (!shouldUseRemote() && gameProfile == null) {
         UUID uUID = PlayerEntity.getUuidFromProfile(new GameProfile((UUID)null, name));
         gameProfile = new GameProfile(uUID, name);
      }

      return gameProfile;
   }

   public static void setUseRemote(boolean value) {
      useRemote = value;
   }

   private static boolean shouldUseRemote() {
      return useRemote;
   }

   public void add(GameProfile profile) {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(new Date());
      calendar.add(2, 1);
      Date date = calendar.getTime();
      UserCache.Entry entry = new UserCache.Entry(profile, date);
      this.add(entry);
      this.save();
   }

   private long incrementAndGetAccessCount() {
      return this.accessCount.incrementAndGet();
   }

   @Nullable
   public GameProfile findByName(String name) {
      String string = name.toLowerCase(Locale.ROOT);
      UserCache.Entry entry = (UserCache.Entry)this.byName.get(string);
      boolean bl = false;
      if (entry != null && (new Date()).getTime() >= entry.expirationDate.getTime()) {
         this.byUuid.remove(entry.getProfile().getId());
         this.byName.remove(entry.getProfile().getName().toLowerCase(Locale.ROOT));
         bl = true;
         entry = null;
      }

      GameProfile gameProfile2;
      if (entry != null) {
         entry.setLastAccessed(this.incrementAndGetAccessCount());
         gameProfile2 = entry.getProfile();
      } else {
         gameProfile2 = findProfileByName(this.profileRepository, string);
         if (gameProfile2 != null) {
            this.add(gameProfile2);
            bl = false;
         }
      }

      if (bl) {
         this.save();
      }

      return gameProfile2;
   }

   public void method_37156(String string, Consumer<GameProfile> consumer) {
      if (this.field_33861 == null) {
         throw new IllegalStateException("No executor");
      } else {
         CompletableFuture<GameProfile> completableFuture = (CompletableFuture)this.field_33860.get(string);
         if (completableFuture != null) {
            this.field_33860.put(string, completableFuture.whenCompleteAsync((gameProfile, throwable) -> {
               consumer.accept(gameProfile);
            }, this.field_33861));
         } else {
            this.field_33860.put(string, CompletableFuture.supplyAsync(() -> {
               return this.findByName(string);
            }, Util.getMainWorkerExecutor()).whenCompleteAsync((gameProfile, throwable) -> {
               this.field_33860.remove(string);
            }, this.field_33861).whenCompleteAsync((gameProfile, throwable) -> {
               consumer.accept(gameProfile);
            }, this.field_33861));
         }

      }
   }

   @Nullable
   public GameProfile getByUuid(UUID uuid) {
      UserCache.Entry entry = (UserCache.Entry)this.byUuid.get(uuid);
      if (entry == null) {
         return null;
      } else {
         entry.setLastAccessed(this.incrementAndGetAccessCount());
         return entry.getProfile();
      }
   }

   public void method_37157(Executor executor) {
      this.field_33861 = executor;
   }

   private static DateFormat getDateFormat() {
      return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
   }

   public List<UserCache.Entry> load() {
      ArrayList list = Lists.newArrayList();

      try {
         BufferedReader reader = Files.newReader(this.cacheFile, StandardCharsets.UTF_8);

         label54: {
            ArrayList var4;
            try {
               JsonArray jsonArray = (JsonArray)this.gson.fromJson((Reader)reader, (Class)JsonArray.class);
               if (jsonArray != null) {
                  DateFormat dateFormat = getDateFormat();
                  jsonArray.forEach((json) -> {
                     UserCache.Entry entry = entryFromJson(json, dateFormat);
                     if (entry != null) {
                        list.add(entry);
                     }

                  });
                  break label54;
               }

               var4 = list;
            } catch (Throwable var6) {
               if (reader != null) {
                  try {
                     reader.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (reader != null) {
               reader.close();
            }

            return var4;
         }

         if (reader != null) {
            reader.close();
         }
      } catch (FileNotFoundException var7) {
      } catch (JsonParseException | IOException var8) {
         LOGGER.warn((String)"Failed to load profile cache {}", (Object)this.cacheFile, (Object)var8);
      }

      return list;
   }

   public void save() {
      JsonArray jsonArray = new JsonArray();
      DateFormat dateFormat = getDateFormat();
      this.getLastAccessedEntries(1000).forEach((entry) -> {
         jsonArray.add(entryToJson(entry, dateFormat));
      });
      String string = this.gson.toJson((JsonElement)jsonArray);

      try {
         BufferedWriter writer = Files.newWriter(this.cacheFile, StandardCharsets.UTF_8);

         try {
            writer.write(string);
         } catch (Throwable var8) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (IOException var9) {
      }

   }

   private Stream<UserCache.Entry> getLastAccessedEntries(int limit) {
      return ImmutableList.copyOf(this.byUuid.values()).stream().sorted(Comparator.comparing(UserCache.Entry::getLastAccessed).reversed()).limit((long)limit);
   }

   private static JsonElement entryToJson(UserCache.Entry entry, DateFormat dateFormat) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("name", entry.getProfile().getName());
      UUID uUID = entry.getProfile().getId();
      jsonObject.addProperty("uuid", uUID == null ? "" : uUID.toString());
      jsonObject.addProperty("expiresOn", dateFormat.format(entry.getExpirationDate()));
      return jsonObject;
   }

   @Nullable
   private static UserCache.Entry entryFromJson(JsonElement json, DateFormat dateFormat) {
      if (json.isJsonObject()) {
         JsonObject jsonObject = json.getAsJsonObject();
         JsonElement jsonElement = jsonObject.get("name");
         JsonElement jsonElement2 = jsonObject.get("uuid");
         JsonElement jsonElement3 = jsonObject.get("expiresOn");
         if (jsonElement != null && jsonElement2 != null) {
            String string = jsonElement2.getAsString();
            String string2 = jsonElement.getAsString();
            Date date = null;
            if (jsonElement3 != null) {
               try {
                  date = dateFormat.parse(jsonElement3.getAsString());
               } catch (ParseException var12) {
               }
            }

            if (string2 != null && string != null && date != null) {
               UUID uUID2;
               try {
                  uUID2 = UUID.fromString(string);
               } catch (Throwable var11) {
                  return null;
               }

               return new UserCache.Entry(new GameProfile(uUID2, string2), date);
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static class Entry {
      private final GameProfile profile;
      final Date expirationDate;
      private volatile long lastAccessed;

      Entry(GameProfile profile, Date expirationDate) {
         this.profile = profile;
         this.expirationDate = expirationDate;
      }

      public GameProfile getProfile() {
         return this.profile;
      }

      public Date getExpirationDate() {
         return this.expirationDate;
      }

      public void setLastAccessed(long lastAccessed) {
         this.lastAccessed = lastAccessed;
      }

      public long getLastAccessed() {
         return this.lastAccessed;
      }
   }
}
