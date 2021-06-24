package net.minecraft.client.resource;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class DirectResourceIndex extends ResourceIndex {
   private final File assetDir;

   public DirectResourceIndex(File assetDir) {
      this.assetDir = assetDir;
   }

   public File getResource(Identifier identifier) {
      return new File(this.assetDir, identifier.toString().replace(':', '/'));
   }

   public File findFile(String path) {
      return new File(this.assetDir, path);
   }

   public Collection<Identifier> getFilesRecursively(String string, String string2, int i, Predicate<String> predicate) {
      Path path = this.assetDir.toPath().resolve(string2);

      try {
         Stream stream = Files.walk(path.resolve(string), i, new FileVisitOption[0]);

         Collection var7;
         try {
            var7 = (Collection)stream.filter((pathx) -> {
               return Files.isRegularFile(pathx, new LinkOption[0]);
            }).filter((pathx) -> {
               return !pathx.endsWith(".mcmeta");
            }).filter((pathx) -> {
               return predicate.test(pathx.getFileName().toString());
            }).map((path2) -> {
               return new Identifier(string2, path.relativize(path2).toString().replaceAll("\\\\", "/"));
            }).collect(Collectors.toList());
         } catch (Throwable var10) {
            if (stream != null) {
               try {
                  stream.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (stream != null) {
            stream.close();
         }

         return var7;
      } catch (NoSuchFileException var11) {
      } catch (IOException var12) {
         LOGGER.warn((String)"Unable to getFiles on {}", (Object)string, (Object)var12);
      }

      return Collections.emptyList();
   }
}
