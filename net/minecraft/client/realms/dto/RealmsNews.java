package net.minecraft.client.realms.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.realms.util.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class RealmsNews extends ValueObject {
   private static final Logger LOGGER = LogManager.getLogger();
   public String newsLink;

   public static RealmsNews parse(String json) {
      RealmsNews realmsNews = new RealmsNews();

      try {
         JsonParser jsonParser = new JsonParser();
         JsonObject jsonObject = jsonParser.parse(json).getAsJsonObject();
         realmsNews.newsLink = JsonUtils.getStringOr("newsLink", jsonObject, (String)null);
      } catch (Exception var4) {
         LOGGER.error((String)"Could not parse RealmsNews: {}", (Object)var4.getMessage());
      }

      return realmsNews;
   }
}
