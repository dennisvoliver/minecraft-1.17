package net.minecraft.client.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.Closeable;
import java.io.IOException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resource.metadata.TextureResourceMetadata;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ResourceTexture extends AbstractTexture {
   static final Logger LOGGER = LogManager.getLogger();
   protected final Identifier location;

   public ResourceTexture(Identifier location) {
      this.location = location;
   }

   public void load(ResourceManager manager) throws IOException {
      ResourceTexture.TextureData textureData = this.loadTextureData(manager);
      textureData.checkException();
      TextureResourceMetadata textureResourceMetadata = textureData.getMetadata();
      boolean bl3;
      boolean bl4;
      if (textureResourceMetadata != null) {
         bl3 = textureResourceMetadata.shouldBlur();
         bl4 = textureResourceMetadata.shouldClamp();
      } else {
         bl3 = false;
         bl4 = false;
      }

      NativeImage nativeImage = textureData.getImage();
      if (!RenderSystem.isOnRenderThreadOrInit()) {
         RenderSystem.recordRenderCall(() -> {
            this.upload(nativeImage, bl3, bl4);
         });
      } else {
         this.upload(nativeImage, bl3, bl4);
      }

   }

   private void upload(NativeImage nativeImage, boolean blur, boolean clamp) {
      TextureUtil.prepareImage(this.getGlId(), 0, nativeImage.getWidth(), nativeImage.getHeight());
      nativeImage.upload(0, 0, 0, 0, 0, nativeImage.getWidth(), nativeImage.getHeight(), blur, clamp, false, true);
   }

   protected ResourceTexture.TextureData loadTextureData(ResourceManager resourceManager) {
      return ResourceTexture.TextureData.load(resourceManager, this.location);
   }

   @Environment(EnvType.CLIENT)
   protected static class TextureData implements Closeable {
      @Nullable
      private final TextureResourceMetadata metadata;
      @Nullable
      private final NativeImage image;
      @Nullable
      private final IOException exception;

      public TextureData(IOException exception) {
         this.exception = exception;
         this.metadata = null;
         this.image = null;
      }

      public TextureData(@Nullable TextureResourceMetadata metadata, NativeImage image) {
         this.exception = null;
         this.metadata = metadata;
         this.image = image;
      }

      public static ResourceTexture.TextureData load(ResourceManager resourceManager, Identifier identifier) {
         try {
            Resource resource = resourceManager.getResource(identifier);

            ResourceTexture.TextureData var5;
            try {
               NativeImage nativeImage = NativeImage.read(resource.getInputStream());
               TextureResourceMetadata textureResourceMetadata = null;

               try {
                  textureResourceMetadata = (TextureResourceMetadata)resource.getMetadata(TextureResourceMetadata.READER);
               } catch (RuntimeException var7) {
                  ResourceTexture.LOGGER.warn((String)"Failed reading metadata of: {}", (Object)identifier, (Object)var7);
               }

               var5 = new ResourceTexture.TextureData(textureResourceMetadata, nativeImage);
            } catch (Throwable var8) {
               if (resource != null) {
                  try {
                     resource.close();
                  } catch (Throwable var6) {
                     var8.addSuppressed(var6);
                  }
               }

               throw var8;
            }

            if (resource != null) {
               resource.close();
            }

            return var5;
         } catch (IOException var9) {
            return new ResourceTexture.TextureData(var9);
         }
      }

      @Nullable
      public TextureResourceMetadata getMetadata() {
         return this.metadata;
      }

      public NativeImage getImage() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         } else {
            return this.image;
         }
      }

      public void close() {
         if (this.image != null) {
            this.image.close();
         }

      }

      public void checkException() throws IOException {
         if (this.exception != null) {
            throw this.exception;
         }
      }
   }
}
