package net.minecraft.client.realms.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsServerAddress extends ValueObject {
   private static final Logger LOGGER = LogManager.getLogger();
   public String address;
   public String resourcePackUrl;
   public String resourcePackHash;

   public static RealmsServerAddress parse(String json) {
      JsonParser jsonParser = new JsonParser();
      RealmsServerAddress realmsServerAddress = new RealmsServerAddress();

      try {
         JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
         realmsServerAddress.address = JsonUtils.getStringOr("address", jsonObject, (String)null);
         realmsServerAddress.resourcePackUrl = JsonUtils.getStringOr("resourcePackUrl", jsonObject, (String)null);
         realmsServerAddress.resourcePackHash = JsonUtils.getStringOr("resourcePackHash", jsonObject, (String)null);
      } catch (Exception var4) {
         LOGGER.error((String)"Could not parse RealmsServerAddress: {}", (Object)var4.getMessage());
      }

      return realmsServerAddress;
   }
}
