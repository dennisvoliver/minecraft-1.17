package net.minecraft.client.realms.dto;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsServerPlayerList extends ValueObject {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final JsonParser jsonParser = new JsonParser();
   public long serverId;
   public List<String> players;

   public static RealmsServerPlayerList parse(JsonObject node) {
      RealmsServerPlayerList realmsServerPlayerList = new RealmsServerPlayerList();

      try {
         realmsServerPlayerList.serverId = JsonUtils.getLongOr("serverId", node, -1L);
         String string = JsonUtils.getStringOr("playerList", node, (String)null);
         if (string != null) {
            JsonElement jsonElement = jsonParser.parse(string);
            if (jsonElement.isJsonArray()) {
               realmsServerPlayerList.players = parsePlayers(jsonElement.getAsJsonArray());
            } else {
               realmsServerPlayerList.players = Lists.newArrayList();
            }
         } else {
            realmsServerPlayerList.players = Lists.newArrayList();
         }
      } catch (Exception var4) {
         LOGGER.error((String)"Could not parse RealmsServerPlayerList: {}", (Object)var4.getMessage());
      }

      return realmsServerPlayerList;
   }

   private static List<String> parsePlayers(JsonArray jsonArray) {
      List<String> list = Lists.newArrayList();
      Iterator var2 = jsonArray.iterator();

      while(var2.hasNext()) {
         JsonElement jsonElement = (JsonElement)var2.next();

         try {
            list.add(jsonElement.getAsString());
         } catch (Exception var5) {
         }
      }

      return list;
   }
}
