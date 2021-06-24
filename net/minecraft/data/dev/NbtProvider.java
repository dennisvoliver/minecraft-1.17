package net.minecraft.data.dev;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import net.minecraft.data.DataCache;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class NbtProvider implements DataProvider {
   private static final Logger LOGGER = LogManager.getLogger();
   private final DataGenerator root;

   public NbtProvider(DataGenerator root) {
      this.root = root;
   }

   public void run(DataCache cache) throws IOException {
      Path path = this.root.getOutput();
      Iterator var3 = this.root.getInputs().iterator();

      while(var3.hasNext()) {
         Path path2 = (Path)var3.next();
         Files.walk(path2).filter((pathx) -> {
            return pathx.toString().endsWith(".nbt");
         }).forEach((path3) -> {
            convertNbtToSnbt(path3, this.getLocation(path2, path3), path);
         });
      }

   }

   public String getName() {
      return "NBT to SNBT";
   }

   private String getLocation(Path targetPath, Path rootPath) {
      String string = targetPath.relativize(rootPath).toString().replaceAll("\\\\", "/");
      return string.substring(0, string.length() - ".nbt".length());
   }

   @Nullable
   public static Path convertNbtToSnbt(Path inputPath, String location, Path outputPath) {
      try {
         writeTo(outputPath.resolve(location + ".snbt"), NbtHelper.toPrettyPrintedString(NbtIo.readCompressed(Files.newInputStream(inputPath))));
         LOGGER.info((String)"Converted {} from NBT to SNBT", (Object)location);
         return outputPath.resolve(location + ".snbt");
      } catch (IOException var4) {
         LOGGER.error((String)"Couldn't convert {} from NBT to SNBT at {}", (Object)location, inputPath, var4);
         return null;
      }
   }

   public static void writeTo(Path file, String content) throws IOException {
      Files.createDirectories(file.getParent());
      BufferedWriter bufferedWriter = Files.newBufferedWriter(file);

      try {
         bufferedWriter.write(content);
         bufferedWriter.write(10);
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
}
