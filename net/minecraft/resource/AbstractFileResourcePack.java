package net.minecraft.resource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractFileResourcePack implements ResourcePack {
   private static final Logger LOGGER = LogManager.getLogger();
   protected final File base;

   public AbstractFileResourcePack(File base) {
      this.base = base;
   }

   private static String getFilename(ResourceType type, Identifier id) {
      return String.format("%s/%s/%s", type.getDirectory(), id.getNamespace(), id.getPath());
   }

   protected static String relativize(File base, File target) {
      return base.toURI().relativize(target.toURI()).getPath();
   }

   public InputStream open(ResourceType type, Identifier id) throws IOException {
      return this.openFile(getFilename(type, id));
   }

   public boolean contains(ResourceType type, Identifier id) {
      return this.containsFile(getFilename(type, id));
   }

   protected abstract InputStream openFile(String name) throws IOException;

   public InputStream openRoot(String fileName) throws IOException {
      if (!fileName.contains("/") && !fileName.contains("\\")) {
         return this.openFile(fileName);
      } else {
         throw new IllegalArgumentException("Root resources can only be filenames, not paths (no / allowed!)");
      }
   }

   protected abstract boolean containsFile(String name);

   protected void warnNonLowerCaseNamespace(String namespace) {
      LOGGER.warn((String)"ResourcePack: ignored non-lowercase namespace: {} in {}", (Object)namespace, (Object)this.base);
   }

   @Nullable
   public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
      InputStream inputStream = this.openFile("pack.mcmeta");

      Object var3;
      try {
         var3 = parseMetadata(metaReader, inputStream);
      } catch (Throwable var6) {
         if (inputStream != null) {
            try {
               inputStream.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (inputStream != null) {
         inputStream.close();
      }

      return var3;
   }

   @Nullable
   public static <T> T parseMetadata(ResourceMetadataReader<T> metaReader, InputStream inputStream) {
      JsonObject jsonObject3;
      try {
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

         try {
            jsonObject3 = JsonHelper.deserialize((Reader)bufferedReader);
         } catch (Throwable var8) {
            try {
               bufferedReader.close();
            } catch (Throwable var6) {
               var8.addSuppressed(var6);
            }

            throw var8;
         }

         bufferedReader.close();
      } catch (JsonParseException | IOException var9) {
         LOGGER.error((String)"Couldn't load {} metadata", (Object)metaReader.getKey(), (Object)var9);
         return null;
      }

      if (!jsonObject3.has(metaReader.getKey())) {
         return null;
      } else {
         try {
            return metaReader.fromJson(JsonHelper.getObject(jsonObject3, metaReader.getKey()));
         } catch (JsonParseException var7) {
            LOGGER.error((String)"Couldn't load {} metadata", (Object)metaReader.getKey(), (Object)var7);
            return null;
         }
      }
   }

   public String getName() {
      return this.base.getName();
   }
}
