package net.minecraft.client.util;

import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class RawTextureDataLoader {
   @Deprecated
   public static int[] loadRawTextureData(ResourceManager resourceManager, Identifier id) throws IOException {
      Resource resource = resourceManager.getResource(id);

      int[] var4;
      try {
         NativeImage nativeImage = NativeImage.read(resource.getInputStream());

         try {
            var4 = nativeImage.makePixelArray();
         } catch (Throwable var8) {
            if (nativeImage != null) {
               try {
                  nativeImage.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (nativeImage != null) {
            nativeImage.close();
         }
      } catch (Throwable var9) {
         if (resource != null) {
            try {
               resource.close();
            } catch (Throwable var6) {
               var9.addSuppressed(var6);
            }
         }

         throw var9;
      }

      if (resource != null) {
         resource.close();
      }

      return var4;
   }
}
