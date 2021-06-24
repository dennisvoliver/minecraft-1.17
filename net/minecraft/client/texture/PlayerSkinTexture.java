package net.minecraft.client.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PlayerSkinTexture extends ResourceTexture {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int WIDTH = 64;
   private static final int HEIGHT = 64;
   private static final int OLD_HEIGHT = 32;
   @Nullable
   private final File cacheFile;
   private final String url;
   private final boolean convertLegacy;
   @Nullable
   private final Runnable loadedCallback;
   @Nullable
   private CompletableFuture<?> loader;
   private boolean loaded;

   public PlayerSkinTexture(@Nullable File cacheFile, String url, Identifier fallbackSkin, boolean convertLegacy, @Nullable Runnable callback) {
      super(fallbackSkin);
      this.cacheFile = cacheFile;
      this.url = url;
      this.convertLegacy = convertLegacy;
      this.loadedCallback = callback;
   }

   private void onTextureLoaded(NativeImage image) {
      if (this.loadedCallback != null) {
         this.loadedCallback.run();
      }

      MinecraftClient.getInstance().execute(() -> {
         this.loaded = true;
         if (!RenderSystem.isOnRenderThread()) {
            RenderSystem.recordRenderCall(() -> {
               this.uploadTexture(image);
            });
         } else {
            this.uploadTexture(image);
         }

      });
   }

   private void uploadTexture(NativeImage image) {
      TextureUtil.prepareImage(this.getGlId(), image.getWidth(), image.getHeight());
      image.upload(0, 0, 0, true);
   }

   public void load(ResourceManager manager) throws IOException {
      MinecraftClient.getInstance().execute(() -> {
         if (!this.loaded) {
            try {
               super.load(manager);
            } catch (IOException var3) {
               LOGGER.warn((String)"Failed to load texture: {}", (Object)this.location, (Object)var3);
            }

            this.loaded = true;
         }

      });
      if (this.loader == null) {
         NativeImage nativeImage2;
         if (this.cacheFile != null && this.cacheFile.isFile()) {
            LOGGER.debug((String)"Loading http texture from local cache ({})", (Object)this.cacheFile);
            FileInputStream fileInputStream = new FileInputStream(this.cacheFile);
            nativeImage2 = this.loadTexture(fileInputStream);
         } else {
            nativeImage2 = null;
         }

         if (nativeImage2 != null) {
            this.onTextureLoaded(nativeImage2);
         } else {
            this.loader = CompletableFuture.runAsync(() -> {
               HttpURLConnection httpURLConnection = null;
               LOGGER.debug((String)"Downloading http texture from {} to {}", (Object)this.url, (Object)this.cacheFile);

               try {
                  httpURLConnection = (HttpURLConnection)(new URL(this.url)).openConnection(MinecraftClient.getInstance().getNetworkProxy());
                  httpURLConnection.setDoInput(true);
                  httpURLConnection.setDoOutput(false);
                  httpURLConnection.connect();
                  if (httpURLConnection.getResponseCode() / 100 == 2) {
                     Object inputStream2;
                     if (this.cacheFile != null) {
                        FileUtils.copyInputStreamToFile(httpURLConnection.getInputStream(), this.cacheFile);
                        inputStream2 = new FileInputStream(this.cacheFile);
                     } else {
                        inputStream2 = httpURLConnection.getInputStream();
                     }

                     MinecraftClient.getInstance().execute(() -> {
                        NativeImage nativeImage = this.loadTexture(inputStream2);
                        if (nativeImage != null) {
                           this.onTextureLoaded(nativeImage);
                        }

                     });
                     return;
                  }
               } catch (Exception var6) {
                  LOGGER.error((String)"Couldn't download http texture", (Throwable)var6);
                  return;
               } finally {
                  if (httpURLConnection != null) {
                     httpURLConnection.disconnect();
                  }

               }

            }, Util.getMainWorkerExecutor());
         }
      }
   }

   @Nullable
   private NativeImage loadTexture(InputStream stream) {
      NativeImage nativeImage = null;

      try {
         nativeImage = NativeImage.read(stream);
         if (this.convertLegacy) {
            nativeImage = this.remapTexture(nativeImage);
         }
      } catch (Exception var4) {
         LOGGER.warn((String)"Error while loading the skin texture", (Throwable)var4);
      }

      return nativeImage;
   }

   @Nullable
   private NativeImage remapTexture(NativeImage nativeImage) {
      int i = nativeImage.getHeight();
      int j = nativeImage.getWidth();
      if (j == 64 && (i == 32 || i == 64)) {
         boolean bl = i == 32;
         if (bl) {
            NativeImage nativeImage2 = new NativeImage(64, 64, true);
            nativeImage2.copyFrom(nativeImage);
            nativeImage.close();
            nativeImage = nativeImage2;
            nativeImage2.fillRect(0, 32, 64, 32, 0);
            nativeImage2.copyRect(4, 16, 16, 32, 4, 4, true, false);
            nativeImage2.copyRect(8, 16, 16, 32, 4, 4, true, false);
            nativeImage2.copyRect(0, 20, 24, 32, 4, 12, true, false);
            nativeImage2.copyRect(4, 20, 16, 32, 4, 12, true, false);
            nativeImage2.copyRect(8, 20, 8, 32, 4, 12, true, false);
            nativeImage2.copyRect(12, 20, 16, 32, 4, 12, true, false);
            nativeImage2.copyRect(44, 16, -8, 32, 4, 4, true, false);
            nativeImage2.copyRect(48, 16, -8, 32, 4, 4, true, false);
            nativeImage2.copyRect(40, 20, 0, 32, 4, 12, true, false);
            nativeImage2.copyRect(44, 20, -8, 32, 4, 12, true, false);
            nativeImage2.copyRect(48, 20, -16, 32, 4, 12, true, false);
            nativeImage2.copyRect(52, 20, -8, 32, 4, 12, true, false);
         }

         stripAlpha(nativeImage, 0, 0, 32, 16);
         if (bl) {
            stripColor(nativeImage, 32, 0, 64, 32);
         }

         stripAlpha(nativeImage, 0, 16, 64, 32);
         stripAlpha(nativeImage, 16, 48, 48, 64);
         return nativeImage;
      } else {
         nativeImage.close();
         LOGGER.warn((String)"Discarding incorrectly sized ({}x{}) skin texture from {}", (Object)j, i, this.url);
         return null;
      }
   }

   private static void stripColor(NativeImage image, int x1, int y1, int x2, int y2) {
      int l;
      int m;
      for(l = x1; l < x2; ++l) {
         for(m = y1; m < y2; ++m) {
            int k = image.getPixelColor(l, m);
            if ((k >> 24 & 255) < 128) {
               return;
            }
         }
      }

      for(l = x1; l < x2; ++l) {
         for(m = y1; m < y2; ++m) {
            image.setPixelColor(l, m, image.getPixelColor(l, m) & 16777215);
         }
      }

   }

   private static void stripAlpha(NativeImage image, int x1, int y1, int x2, int y2) {
      for(int i = x1; i < x2; ++i) {
         for(int j = y1; j < y2; ++j) {
            image.setPixelColor(i, j, image.getPixelColor(i, j) | -16777216);
         }
      }

   }
}
