package net.minecraft.world.storage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.ThrowableDeliverer;
import net.minecraft.util.math.ChunkPos;
import org.jetbrains.annotations.Nullable;

public final class RegionBasedStorage implements AutoCloseable {
   public static final String field_31425 = ".mca";
   private static final int field_31426 = 256;
   private final Long2ObjectLinkedOpenHashMap<RegionFile> cachedRegionFiles = new Long2ObjectLinkedOpenHashMap();
   private final File directory;
   private final boolean dsync;

   RegionBasedStorage(File directory, boolean dsync) {
      this.directory = directory;
      this.dsync = dsync;
   }

   private RegionFile getRegionFile(ChunkPos pos) throws IOException {
      long l = ChunkPos.toLong(pos.getRegionX(), pos.getRegionZ());
      RegionFile regionFile = (RegionFile)this.cachedRegionFiles.getAndMoveToFirst(l);
      if (regionFile != null) {
         return regionFile;
      } else {
         if (this.cachedRegionFiles.size() >= 256) {
            ((RegionFile)this.cachedRegionFiles.removeLast()).close();
         }

         if (!this.directory.exists()) {
            this.directory.mkdirs();
         }

         File var10002 = this.directory;
         int var10003 = pos.getRegionX();
         File file = new File(var10002, "r." + var10003 + "." + pos.getRegionZ() + ".mca");
         RegionFile regionFile2 = new RegionFile(file, this.directory, this.dsync);
         this.cachedRegionFiles.putAndMoveToFirst(l, regionFile2);
         return regionFile2;
      }
   }

   @Nullable
   public NbtCompound getTagAt(ChunkPos pos) throws IOException {
      RegionFile regionFile = this.getRegionFile(pos);
      DataInputStream dataInputStream = regionFile.getChunkInputStream(pos);

      NbtCompound var4;
      label43: {
         try {
            if (dataInputStream == null) {
               var4 = null;
               break label43;
            }

            var4 = NbtIo.read((DataInput)dataInputStream);
         } catch (Throwable var7) {
            if (dataInputStream != null) {
               try {
                  dataInputStream.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (dataInputStream != null) {
            dataInputStream.close();
         }

         return var4;
      }

      if (dataInputStream != null) {
         dataInputStream.close();
      }

      return var4;
   }

   protected void write(ChunkPos pos, @Nullable NbtCompound nbt) throws IOException {
      RegionFile regionFile = this.getRegionFile(pos);
      if (nbt == null) {
         regionFile.method_31740(pos);
      } else {
         DataOutputStream dataOutputStream = regionFile.getChunkOutputStream(pos);

         try {
            NbtIo.write((NbtCompound)nbt, (DataOutput)dataOutputStream);
         } catch (Throwable var8) {
            if (dataOutputStream != null) {
               try {
                  dataOutputStream.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (dataOutputStream != null) {
            dataOutputStream.close();
         }
      }

   }

   public void close() throws IOException {
      ThrowableDeliverer<IOException> throwableDeliverer = new ThrowableDeliverer();
      ObjectIterator var2 = this.cachedRegionFiles.values().iterator();

      while(var2.hasNext()) {
         RegionFile regionFile = (RegionFile)var2.next();

         try {
            regionFile.close();
         } catch (IOException var5) {
            throwableDeliverer.add(var5);
         }
      }

      throwableDeliverer.deliver();
   }

   public void sync() throws IOException {
      ObjectIterator var1 = this.cachedRegionFiles.values().iterator();

      while(var1.hasNext()) {
         RegionFile regionFile = (RegionFile)var1.next();
         regionFile.sync();
      }

   }
}
