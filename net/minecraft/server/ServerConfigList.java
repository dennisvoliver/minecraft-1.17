package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class ServerConfigList<K, V extends ServerConfigEntry<K>> {
   protected static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
   private final File file;
   private final Map<String, V> map = Maps.newHashMap();

   public ServerConfigList(File file) {
      this.file = file;
   }

   public File getFile() {
      return this.file;
   }

   public void add(V entry) {
      this.map.put(this.toString(entry.getKey()), entry);

      try {
         this.save();
      } catch (IOException var3) {
         LOGGER.warn((String)"Could not save the list after adding a user.", (Throwable)var3);
      }

   }

   @Nullable
   public V get(K key) {
      this.removeInvalidEntries();
      return (ServerConfigEntry)this.map.get(this.toString(key));
   }

   public void remove(K key) {
      this.map.remove(this.toString(key));

      try {
         this.save();
      } catch (IOException var3) {
         LOGGER.warn((String)"Could not save the list after removing a user.", (Throwable)var3);
      }

   }

   public void remove(ServerConfigEntry<K> entry) {
      this.remove(entry.getKey());
   }

   public String[] getNames() {
      return (String[])this.map.keySet().toArray(new String[0]);
   }

   public boolean isEmpty() {
      return this.map.size() < 1;
   }

   protected String toString(K profile) {
      return profile.toString();
   }

   protected boolean contains(K object) {
      return this.map.containsKey(this.toString(object));
   }

   private void removeInvalidEntries() {
      List<K> list = Lists.newArrayList();
      Iterator var2 = this.map.values().iterator();

      while(var2.hasNext()) {
         V serverConfigEntry = (ServerConfigEntry)var2.next();
         if (serverConfigEntry.isInvalid()) {
            list.add(serverConfigEntry.getKey());
         }
      }

      var2 = list.iterator();

      while(var2.hasNext()) {
         K object = var2.next();
         this.map.remove(this.toString(object));
      }

   }

   protected abstract ServerConfigEntry<K> fromJson(JsonObject json);

   public Collection<V> values() {
      return this.map.values();
   }

   public void save() throws IOException {
      JsonArray jsonArray = new JsonArray();
      Stream var10000 = this.map.values().stream().map((entry) -> {
         JsonObject var10000 = new JsonObject();
         Objects.requireNonNull(entry);
         return (JsonObject)Util.make(var10000, entry::fromJson);
      });
      Objects.requireNonNull(jsonArray);
      var10000.forEach(jsonArray::add);
      BufferedWriter bufferedWriter = Files.newWriter(this.file, StandardCharsets.UTF_8);

      try {
         GSON.toJson((JsonElement)jsonArray, (Appendable)bufferedWriter);
      } catch (Throwable var6) {
         if (bufferedWriter != null) {
            try {
               bufferedWriter.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (bufferedWriter != null) {
         bufferedWriter.close();
      }

   }

   public void load() throws IOException {
      if (this.file.exists()) {
         BufferedReader bufferedReader = Files.newReader(this.file, StandardCharsets.UTF_8);

         try {
            JsonArray jsonArray = (JsonArray)GSON.fromJson((Reader)bufferedReader, (Class)JsonArray.class);
            this.map.clear();
            Iterator var3 = jsonArray.iterator();

            while(var3.hasNext()) {
               JsonElement jsonElement = (JsonElement)var3.next();
               JsonObject jsonObject = JsonHelper.asObject(jsonElement, "entry");
               ServerConfigEntry<K> serverConfigEntry = this.fromJson(jsonObject);
               if (serverConfigEntry.getKey() != null) {
                  this.map.put(this.toString(serverConfigEntry.getKey()), serverConfigEntry);
               }
            }
         } catch (Throwable var8) {
            if (bufferedReader != null) {
               try {
                  bufferedReader.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (bufferedReader != null) {
            bufferedReader.close();
         }

      }
   }
}
