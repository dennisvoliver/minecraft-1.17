package net.minecraft.resource;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An abstract implementation of resource reloader that reads JSON files
 * into Gson representations in the prepare stage.
 */
public abstract class JsonDataLoader extends SinglePreparationResourceReloader<Map<Identifier, JsonElement>> {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String FILE_SUFFIX = ".json";
   private static final int FILE_SUFFIX_LENGTH = ".json".length();
   private final Gson gson;
   private final String dataType;

   public JsonDataLoader(Gson gson, String dataType) {
      this.gson = gson;
      this.dataType = dataType;
   }

   protected Map<Identifier, JsonElement> prepare(ResourceManager resourceManager, Profiler profiler) {
      Map<Identifier, JsonElement> map = Maps.newHashMap();
      int i = this.dataType.length() + 1;
      Iterator var5 = resourceManager.findResources(this.dataType, (stringx) -> {
         return stringx.endsWith(".json");
      }).iterator();

      while(var5.hasNext()) {
         Identifier identifier = (Identifier)var5.next();
         String string = identifier.getPath();
         Identifier identifier2 = new Identifier(identifier.getNamespace(), string.substring(i, string.length() - FILE_SUFFIX_LENGTH));

         try {
            Resource resource = resourceManager.getResource(identifier);

            try {
               InputStream inputStream = resource.getInputStream();

               try {
                  BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

                  try {
                     JsonElement jsonElement = (JsonElement)JsonHelper.deserialize(this.gson, (Reader)reader, (Class)JsonElement.class);
                     if (jsonElement != null) {
                        JsonElement jsonElement2 = (JsonElement)map.put(identifier2, jsonElement);
                        if (jsonElement2 != null) {
                           throw new IllegalStateException("Duplicate data file ignored with ID " + identifier2);
                        }
                     } else {
                        LOGGER.error((String)"Couldn't load data file {} from {} as it's null or empty", (Object)identifier2, (Object)identifier);
                     }
                  } catch (Throwable var17) {
                     try {
                        reader.close();
                     } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                     }

                     throw var17;
                  }

                  reader.close();
               } catch (Throwable var18) {
                  if (inputStream != null) {
                     try {
                        inputStream.close();
                     } catch (Throwable var15) {
                        var18.addSuppressed(var15);
                     }
                  }

                  throw var18;
               }

               if (inputStream != null) {
                  inputStream.close();
               }
            } catch (Throwable var19) {
               if (resource != null) {
                  try {
                     resource.close();
                  } catch (Throwable var14) {
                     var19.addSuppressed(var14);
                  }
               }

               throw var19;
            }

            if (resource != null) {
               resource.close();
            }
         } catch (IllegalArgumentException | IOException | JsonParseException var20) {
            LOGGER.error((String)"Couldn't parse data file {} from {}", (Object)identifier2, identifier, var20);
         }
      }

      return map;
   }
}
