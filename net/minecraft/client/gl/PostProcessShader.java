package net.minecraft.client.gl;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.math.Matrix4f;

@Environment(EnvType.CLIENT)
public class PostProcessShader implements AutoCloseable {
   private final JsonEffectGlShader program;
   public final Framebuffer input;
   public final Framebuffer output;
   private final List<IntSupplier> samplerValues = Lists.newArrayList();
   private final List<String> samplerNames = Lists.newArrayList();
   private final List<Integer> samplerWidths = Lists.newArrayList();
   private final List<Integer> samplerHeights = Lists.newArrayList();
   private Matrix4f projectionMatrix;

   public PostProcessShader(ResourceManager resourceManager, String programName, Framebuffer input, Framebuffer output) throws IOException {
      this.program = new JsonEffectGlShader(resourceManager, programName);
      this.input = input;
      this.output = output;
   }

   public void close() {
      this.program.close();
   }

   public final String getName() {
      return this.program.getName();
   }

   public void addAuxTarget(String name, IntSupplier valueSupplier, int width, int height) {
      this.samplerNames.add(this.samplerNames.size(), name);
      this.samplerValues.add(this.samplerValues.size(), valueSupplier);
      this.samplerWidths.add(this.samplerWidths.size(), width);
      this.samplerHeights.add(this.samplerHeights.size(), height);
   }

   public void setProjectionMatrix(Matrix4f projectionMatrix) {
      this.projectionMatrix = projectionMatrix;
   }

   public void render(float time) {
      this.input.endWrite();
      float f = (float)this.output.textureWidth;
      float g = (float)this.output.textureHeight;
      RenderSystem.viewport(0, 0, (int)f, (int)g);
      JsonEffectGlShader var10000 = this.program;
      Framebuffer var10002 = this.input;
      Objects.requireNonNull(var10002);
      var10000.bindSampler("DiffuseSampler", var10002::getColorAttachment);

      for(int i = 0; i < this.samplerValues.size(); ++i) {
         this.program.bindSampler((String)this.samplerNames.get(i), (IntSupplier)this.samplerValues.get(i));
         this.program.getUniformByNameOrDummy("AuxSize" + i).set((float)(Integer)this.samplerWidths.get(i), (float)(Integer)this.samplerHeights.get(i));
      }

      this.program.getUniformByNameOrDummy("ProjMat").set(this.projectionMatrix);
      this.program.getUniformByNameOrDummy("InSize").set((float)this.input.textureWidth, (float)this.input.textureHeight);
      this.program.getUniformByNameOrDummy("OutSize").set(f, g);
      this.program.getUniformByNameOrDummy("Time").set(time);
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      this.program.getUniformByNameOrDummy("ScreenSize").set((float)minecraftClient.getWindow().getFramebufferWidth(), (float)minecraftClient.getWindow().getFramebufferHeight());
      this.program.enable();
      this.output.clear(MinecraftClient.IS_SYSTEM_MAC);
      this.output.beginWrite(false);
      RenderSystem.depthFunc(519);
      BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
      bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
      bufferBuilder.vertex(0.0D, 0.0D, 500.0D).next();
      bufferBuilder.vertex((double)f, 0.0D, 500.0D).next();
      bufferBuilder.vertex((double)f, (double)g, 500.0D).next();
      bufferBuilder.vertex(0.0D, (double)g, 500.0D).next();
      bufferBuilder.end();
      BufferRenderer.postDraw(bufferBuilder);
      RenderSystem.depthFunc(515);
      this.program.disable();
      this.output.endWrite();
      this.input.endRead();
      Iterator var6 = this.samplerValues.iterator();

      while(var6.hasNext()) {
         Object object = var6.next();
         if (object instanceof Framebuffer) {
            ((Framebuffer)object).endRead();
         }
      }

   }

   public JsonEffectGlShader getProgram() {
      return this.program;
   }
}
