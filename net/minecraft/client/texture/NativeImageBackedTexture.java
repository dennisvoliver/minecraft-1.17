package net.minecraft.client.texture;

import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resource.ResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class NativeImageBackedTexture extends AbstractTexture {
   private static final Logger LOGGER = LogManager.getLogger();
   @Nullable
   private NativeImage image;

   public NativeImageBackedTexture(NativeImage image) {
      this.image = image;
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            TextureUtil.prepareImage(this.getGlId(), this.image.getWidth(), this.image.getHeight());
            this.upload();
         });
      } else {
         TextureUtil.prepareImage(this.getGlId(), this.image.getWidth(), this.image.getHeight());
         this.upload();
      }

   }

   public NativeImageBackedTexture(int width, int height, boolean useStb) {
      RenderSystem.assertThread(RenderSystem::isOnGameThreadOrInit);
      this.image = new NativeImage(width, height, useStb);
      TextureUtil.prepareImage(this.getGlId(), this.image.getWidth(), this.image.getHeight());
   }

   public void load(ResourceManager manager) {
   }

   public void upload() {
      if (this.image != null) {
         this.bindTexture();
         this.image.upload(0, 0, 0, false);
      } else {
         LOGGER.warn((String)"Trying to upload disposed texture {}", (Object)this.getGlId());
      }

   }

   @Nullable
   public NativeImage getImage() {
      return this.image;
   }

   public void setImage(NativeImage image) {
      if (this.image != null) {
         this.image.close();
      }

      this.image = image;
   }

   public void close() {
      if (this.image != null) {
         this.image.close();
         this.clearGlId();
         this.image = null;
      }

   }
}
