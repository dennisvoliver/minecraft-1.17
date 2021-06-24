package net.minecraft.world;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DataFixer;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtIo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class PersistentStateManager {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Map<String, PersistentState> loadedStates = Maps.newHashMap();
   private final DataFixer dataFixer;
   private final File directory;

   public PersistentStateManager(File directory, DataFixer dataFixer) {
      this.dataFixer = dataFixer;
      this.directory = directory;
   }

   private File getFile(String id) {
      return new File(this.directory, id + ".dat");
   }

   public <T extends PersistentState> T getOrCreate(Function<NbtCompound, T> function, Supplier<T> supplier, String string) {
      T persistentState = this.get(function, string);
      if (persistentState != null) {
         return persistentState;
      } else {
         T persistentState2 = (PersistentState)supplier.get();
         this.set(string, persistentState2);
         return persistentState2;
      }
   }

   @Nullable
   public <T extends PersistentState> T get(Function<NbtCompound, T> function, String id) {
      PersistentState persistentState = (PersistentState)this.loadedStates.get(id);
      if (persistentState == null && !this.loadedStates.containsKey(id)) {
         persistentState = this.readFromFile(function, id);
         this.loadedStates.put(id, persistentState);
      }

      return persistentState;
   }

   @Nullable
   private <T extends PersistentState> T readFromFile(Function<NbtCompound, T> function, String id) {
      try {
         File file = this.getFile(id);
         if (file.exists()) {
            NbtCompound nbtCompound = this.readNbt(id, SharedConstants.getGameVersion().getWorldVersion());
            return (PersistentState)function.apply(nbtCompound.getCompound("data"));
         }
      } catch (Exception var5) {
         LOGGER.error((String)"Error loading saved data: {}", (Object)id, (Object)var5);
      }

      return null;
   }

   public void set(String string, PersistentState persistentState) {
      this.loadedStates.put(string, persistentState);
   }

   public NbtCompound readNbt(String id, int dataVersion) throws IOException {
      File file = this.getFile(id);
      FileInputStream fileInputStream = new FileInputStream(file);

      NbtCompound var8;
      try {
         PushbackInputStream pushbackInputStream = new PushbackInputStream(fileInputStream, 2);

         try {
            NbtCompound nbtCompound3;
            if (this.isCompressed(pushbackInputStream)) {
               nbtCompound3 = NbtIo.readCompressed((InputStream)pushbackInputStream);
            } else {
               DataInputStream dataInputStream = new DataInputStream(pushbackInputStream);

               try {
                  nbtCompound3 = NbtIo.read((DataInput)dataInputStream);
               } catch (Throwable var13) {
                  try {
                     dataInputStream.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }

                  throw var13;
               }

               dataInputStream.close();
            }

            int i = nbtCompound3.contains("DataVersion", 99) ? nbtCompound3.getInt("DataVersion") : 1343;
            var8 = NbtHelper.update(this.dataFixer, DataFixTypes.SAVED_DATA, nbtCompound3, i, dataVersion);
         } catch (Throwable var14) {
            try {
               pushbackInputStream.close();
            } catch (Throwable var11) {
               var14.addSuppressed(var11);
            }

            throw var14;
         }

         pushbackInputStream.close();
      } catch (Throwable var15) {
         try {
            fileInputStream.close();
         } catch (Throwable var10) {
            var15.addSuppressed(var10);
         }

         throw var15;
      }

      fileInputStream.close();
      return var8;
   }

   private boolean isCompressed(PushbackInputStream pushbackInputStream) throws IOException {
      byte[] bs = new byte[2];
      boolean bl = false;
      int i = pushbackInputStream.read(bs, 0, 2);
      if (i == 2) {
         int j = (bs[1] & 255) << 8 | bs[0] & 255;
         if (j == 35615) {
            bl = true;
         }
      }

      if (i != 0) {
         pushbackInputStream.unread(bs, 0, i);
      }

      return bl;
   }

   public void save() {
      this.loadedStates.forEach((string, persistentState) -> {
         if (persistentState != null) {
            persistentState.save(this.getFile(string));
         }

      });
   }
}
