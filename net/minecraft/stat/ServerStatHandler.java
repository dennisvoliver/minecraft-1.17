package net.minecraft.stat;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.DataFixer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.packet.s2c.play.StatisticsS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerStatHandler extends StatHandler {
   private static final Logger LOGGER = LogManager.getLogger();
   private final MinecraftServer server;
   private final File file;
   private final Set<Stat<?>> pendingStats = Sets.newHashSet();

   public ServerStatHandler(MinecraftServer server, File file) {
      this.server = server;
      this.file = file;
      if (file.isFile()) {
         try {
            this.parse(server.getDataFixer(), FileUtils.readFileToString(file));
         } catch (IOException var4) {
            LOGGER.error((String)"Couldn't read statistics file {}", (Object)file, (Object)var4);
         } catch (JsonParseException var5) {
            LOGGER.error((String)"Couldn't parse statistics file {}", (Object)file, (Object)var5);
         }
      }

   }

   public void save() {
      try {
         FileUtils.writeStringToFile(this.file, this.asString());
      } catch (IOException var2) {
         LOGGER.error((String)"Couldn't save stats", (Throwable)var2);
      }

   }

   public void setStat(PlayerEntity player, Stat<?> stat, int value) {
      super.setStat(player, stat, value);
      this.pendingStats.add(stat);
   }

   private Set<Stat<?>> takePendingStats() {
      Set<Stat<?>> set = Sets.newHashSet((Iterable)this.pendingStats);
      this.pendingStats.clear();
      return set;
   }

   public void parse(DataFixer dataFixer, String json) {
      try {
         JsonReader jsonReader = new JsonReader(new StringReader(json));

         label66: {
            try {
               jsonReader.setLenient(false);
               JsonElement jsonElement = Streams.parse(jsonReader);
               if (jsonElement.isJsonNull()) {
                  LOGGER.error((String)"Unable to parse Stat data from {}", (Object)this.file);
                  break label66;
               }

               NbtCompound nbtCompound = jsonToCompound(jsonElement.getAsJsonObject());
               if (!nbtCompound.contains("DataVersion", 99)) {
                  nbtCompound.putInt("DataVersion", 1343);
               }

               nbtCompound = NbtHelper.update(dataFixer, DataFixTypes.STATS, nbtCompound, nbtCompound.getInt("DataVersion"));
               if (nbtCompound.contains("stats", 10)) {
                  NbtCompound nbtCompound2 = nbtCompound.getCompound("stats");
                  Iterator var7 = nbtCompound2.getKeys().iterator();

                  while(var7.hasNext()) {
                     String string = (String)var7.next();
                     if (nbtCompound2.contains(string, 10)) {
                        Util.ifPresentOrElse(Registry.STAT_TYPE.getOrEmpty(new Identifier(string)), (statType) -> {
                           NbtCompound nbtCompound2x = nbtCompound2.getCompound(string);
                           Iterator var5 = nbtCompound2x.getKeys().iterator();

                           while(var5.hasNext()) {
                              String string2 = (String)var5.next();
                              if (nbtCompound2x.contains(string2, 99)) {
                                 Util.ifPresentOrElse(this.createStat(statType, string2), (stat) -> {
                                    this.statMap.put(stat, nbtCompound2x.getInt(string2));
                                 }, () -> {
                                    LOGGER.warn((String)"Invalid statistic in {}: Don't know what {} is", (Object)this.file, (Object)string2);
                                 });
                              } else {
                                 LOGGER.warn((String)"Invalid statistic value in {}: Don't know what {} is for key {}", (Object)this.file, nbtCompound2x.get(string2), string2);
                              }
                           }

                        }, () -> {
                           LOGGER.warn((String)"Invalid statistic type in {}: Don't know what {} is", (Object)this.file, (Object)string);
                        });
                     }
                  }
               }
            } catch (Throwable var10) {
               try {
                  jsonReader.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }

               throw var10;
            }

            jsonReader.close();
            return;
         }

         jsonReader.close();
      } catch (IOException | JsonParseException var11) {
         LOGGER.error((String)"Unable to parse Stat data from {}", (Object)this.file, (Object)var11);
      }
   }

   private <T> Optional<Stat<T>> createStat(StatType<T> type, String id) {
      Optional var10000 = Optional.ofNullable(Identifier.tryParse(id));
      Registry var10001 = type.getRegistry();
      Objects.requireNonNull(var10001);
      var10000 = var10000.flatMap(var10001::getOrEmpty);
      Objects.requireNonNull(type);
      return var10000.map(type::getOrCreateStat);
   }

   private static NbtCompound jsonToCompound(JsonObject json) {
      NbtCompound nbtCompound = new NbtCompound();
      Iterator var2 = json.entrySet().iterator();

      while(var2.hasNext()) {
         Entry<String, JsonElement> entry = (Entry)var2.next();
         JsonElement jsonElement = (JsonElement)entry.getValue();
         if (jsonElement.isJsonObject()) {
            nbtCompound.put((String)entry.getKey(), jsonToCompound(jsonElement.getAsJsonObject()));
         } else if (jsonElement.isJsonPrimitive()) {
            JsonPrimitive jsonPrimitive = jsonElement.getAsJsonPrimitive();
            if (jsonPrimitive.isNumber()) {
               nbtCompound.putInt((String)entry.getKey(), jsonPrimitive.getAsInt());
            }
         }
      }

      return nbtCompound;
   }

   protected String asString() {
      Map<StatType<?>, JsonObject> map = Maps.newHashMap();
      ObjectIterator var2 = this.statMap.object2IntEntrySet().iterator();

      while(var2.hasNext()) {
         it.unimi.dsi.fastutil.objects.Object2IntMap.Entry<Stat<?>> entry = (it.unimi.dsi.fastutil.objects.Object2IntMap.Entry)var2.next();
         Stat<?> stat = (Stat)entry.getKey();
         ((JsonObject)map.computeIfAbsent(stat.getType(), (statType) -> {
            return new JsonObject();
         })).addProperty(getStatId(stat).toString(), (Number)entry.getIntValue());
      }

      JsonObject jsonObject = new JsonObject();
      Iterator var6 = map.entrySet().iterator();

      while(var6.hasNext()) {
         Entry<StatType<?>, JsonObject> entry2 = (Entry)var6.next();
         jsonObject.add(Registry.STAT_TYPE.getId((StatType)entry2.getKey()).toString(), (JsonElement)entry2.getValue());
      }

      JsonObject jsonObject2 = new JsonObject();
      jsonObject2.add("stats", jsonObject);
      jsonObject2.addProperty("DataVersion", (Number)SharedConstants.getGameVersion().getWorldVersion());
      return jsonObject2.toString();
   }

   private static <T> Identifier getStatId(Stat<T> stat) {
      return stat.getType().getRegistry().getId(stat.getValue());
   }

   public void updateStatSet() {
      this.pendingStats.addAll(this.statMap.keySet());
   }

   public void sendStats(ServerPlayerEntity player) {
      Object2IntMap<Stat<?>> object2IntMap = new Object2IntOpenHashMap();
      Iterator var3 = this.takePendingStats().iterator();

      while(var3.hasNext()) {
         Stat<?> stat = (Stat)var3.next();
         object2IntMap.put(stat, this.getStat(stat));
      }

      player.networkHandler.sendPacket(new StatisticsS2CPacket(object2IntMap));
   }
}
