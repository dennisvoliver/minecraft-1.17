package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.annotation.DeobfuscateClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
@DeobfuscateClass
public class TextureUtil {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final int MIN_MIPMAP_LEVEL = 0;
   private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;

   public static int generateTextureId() {
      RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
      if (SharedConstants.isDevelopment) {
         int[] is = new int[ThreadLocalRandom.current().nextInt(15) + 1];
         GlStateManager._genTextures(is);
         int i = GlStateManager._genTexture();
         GlStateManager._deleteTextures(is);
         return i;
      } else {
         return GlStateManager._genTexture();
      }
   }

   public static void releaseTextureId(int id) {
      RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
      GlStateManager._deleteTexture(id);
   }

   public static void prepareImage(int id, int width, int height) {
      prepareImage(NativeImage.GLFormat.ABGR, id, 0, width, height);
   }

   public static void prepareImage(NativeImage.GLFormat internalFormat, int id, int width, int height) {
      prepareImage(internalFormat, id, 0, width, height);
   }

   public static void prepareImage(int id, int maxLevel, int width, int height) {
      prepareImage(NativeImage.GLFormat.ABGR, id, maxLevel, width, height);
   }

   public static void prepareImage(NativeImage.GLFormat internalFormat, int id, int maxLevel, int width, int height) {
      RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
      bind(id);
      if (maxLevel >= 0) {
         GlStateManager._texParameter(3553, 33085, maxLevel);
         GlStateManager._texParameter(3553, 33082, 0);
         GlStateManager._texParameter(3553, 33083, maxLevel);
         GlStateManager._texParameter(3553, 34049, 0.0F);
      }

      for(int i = 0; i <= maxLevel; ++i) {
         GlStateManager._texImage2D(3553, i, internalFormat.getGlConstant(), width >> i, height >> i, 0, 6408, 5121, (IntBuffer)null);
      }

   }

   private static void bind(int id) {
      RenderSystem.assertThread(RenderSystem::isOnRenderThreadOrInit);
      GlStateManager._bindTexture(id);
   }

   public static ByteBuffer readResource(InputStream inputStream) throws IOException {
      ByteBuffer byteBuffer2;
      if (inputStream instanceof FileInputStream) {
         FileInputStream fileInputStream = (FileInputStream)inputStream;
         FileChannel fileChannel = fileInputStream.getChannel();
         byteBuffer2 = MemoryUtil.memAlloc((int)fileChannel.size() + 1);

         while(true) {
            if (fileChannel.read(byteBuffer2) != -1) {
               continue;
            }
         }
      } else {
         byteBuffer2 = MemoryUtil.memAlloc(8192);
         ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);

         while(readableByteChannel.read(byteBuffer2) != -1) {
            if (byteBuffer2.remaining() == 0) {
               byteBuffer2 = MemoryUtil.memRealloc(byteBuffer2, byteBuffer2.capacity() * 2);
            }
         }
      }

      return byteBuffer2;
   }

   @Nullable
   public static String readResourceAsString(InputStream inputStream) {
      RenderSystem.assertThread(RenderSystem::isOnRenderThread);
      ByteBuffer byteBuffer = null;

      try {
         byteBuffer = readResource(inputStream);
         int i = byteBuffer.position();
         byteBuffer.rewind();
         String var3 = MemoryUtil.memASCII(byteBuffer, i);
         return var3;
      } catch (IOException var7) {
      } finally {
         if (byteBuffer != null) {
            MemoryUtil.memFree((Buffer)byteBuffer);
         }

      }

      return null;
   }

   public static void writeAsPNG(String string, int i, int j, int k, int l) {
      RenderSystem.assertThread(RenderSystem::isOnRenderThread);
      bind(i);

      for(int m = 0; m <= j; ++m) {
         String string2 = string + "_" + m + ".png";
         int n = k >> m;
         int o = l >> m;

         try {
            NativeImage nativeImage = new NativeImage(n, o, false);

            try {
               nativeImage.loadFromTextureImage(m, false);
               nativeImage.method_35622(string2);
               LOGGER.debug((String)"Exported png to: {}", (Object)(new File(string2)).getAbsolutePath());
            } catch (Throwable var13) {
               try {
                  nativeImage.close();
               } catch (Throwable var12) {
                  var13.addSuppressed(var12);
               }

               throw var13;
            }

            nativeImage.close();
         } catch (IOException var14) {
            LOGGER.debug((String)"Unable to write: ", (Throwable)var14);
         }
      }

   }

   public static void initTexture(IntBuffer imageData, int width, int height) {
      RenderSystem.assertThread(RenderSystem::isOnRenderThread);
      GL11.glPixelStorei(3312, 0);
      GL11.glPixelStorei(3313, 0);
      GL11.glPixelStorei(3314, 0);
      GL11.glPixelStorei(3315, 0);
      GL11.glPixelStorei(3316, 0);
      GL11.glPixelStorei(3317, 4);
      GL11.glTexImage2D(3553, 0, 6408, width, height, 0, 32993, 33639, (IntBuffer)imageData);
      GL11.glTexParameteri(3553, 10240, 9728);
      GL11.glTexParameteri(3553, 10241, 9729);
   }
}
