package net.minecraft.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import net.minecraft.client.font.TextVisitFactory;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Language {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Gson GSON = new Gson();
   private static final Pattern TOKEN_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
   public static final String DEFAULT_LANGUAGE = "en_us";
   private static volatile Language instance = create();

   private static Language create() {
      Builder<String, String> builder = ImmutableMap.builder();
      Objects.requireNonNull(builder);
      BiConsumer<String, String> biConsumer = builder::put;
      String var2 = "/assets/minecraft/lang/en_us.json";

      try {
         InputStream inputStream = Language.class.getResourceAsStream("/assets/minecraft/lang/en_us.json");

         try {
            load(inputStream, biConsumer);
         } catch (Throwable var7) {
            if (inputStream != null) {
               try {
                  inputStream.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (inputStream != null) {
            inputStream.close();
         }
      } catch (JsonParseException | IOException var8) {
         LOGGER.error((String)"Couldn't read strings from {}", (Object)"/assets/minecraft/lang/en_us.json", (Object)var8);
      }

      final Map<String, String> map = builder.build();
      return new Language() {
         public String get(String key) {
            return (String)map.getOrDefault(key, key);
         }

         public boolean hasTranslation(String key) {
            return map.containsKey(key);
         }

         public boolean isRightToLeft() {
            return false;
         }

         public OrderedText reorder(StringVisitable text) {
            return (visitor) -> {
               return text.visit((style, string) -> {
                  return TextVisitFactory.visitFormatted(string, style, visitor) ? Optional.empty() : StringVisitable.TERMINATE_VISIT;
               }, Style.EMPTY).isPresent();
            };
         }
      };
   }

   public static void load(InputStream inputStream, BiConsumer<String, String> entryConsumer) {
      JsonObject jsonObject = (JsonObject)GSON.fromJson((Reader)(new InputStreamReader(inputStream, StandardCharsets.UTF_8)), (Class)JsonObject.class);
      Iterator var3 = jsonObject.entrySet().iterator();

      while(var3.hasNext()) {
         Entry<String, JsonElement> entry = (Entry)var3.next();
         String string = TOKEN_PATTERN.matcher(JsonHelper.asString((JsonElement)entry.getValue(), (String)entry.getKey())).replaceAll("%$1s");
         entryConsumer.accept((String)entry.getKey(), string);
      }

   }

   public static Language getInstance() {
      return instance;
   }

   public static void setInstance(Language language) {
      instance = language;
   }

   public abstract String get(String key);

   public abstract boolean hasTranslation(String key);

   public abstract boolean isRightToLeft();

   public abstract OrderedText reorder(StringVisitable text);

   public List<OrderedText> reorder(List<StringVisitable> texts) {
      return (List)texts.stream().map(this::reorder).collect(ImmutableList.toImmutableList());
   }
}
