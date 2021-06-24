package net.minecraft.client.texture;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.realms.gui.screen.RealmsMainScreen;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class TextureManager implements ResourceReloader, TextureTickListener, AutoCloseable {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final Identifier MISSING_IDENTIFIER = new Identifier("");
   private final Map<Identifier, AbstractTexture> textures = Maps.newHashMap();
   private final Set<TextureTickListener> tickListeners = Sets.newHashSet();
   private final Map<String, Integer> dynamicIdCounters = Maps.newHashMap();
   private final ResourceManager resourceContainer;

   public TextureManager(ResourceManager resourceManager) {
      this.resourceContainer = resourceManager;
   }

   public void bindTexture(Identifier id) {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(() -> {
            this.bindTextureInner(id);
         });
      } else {
         this.bindTextureInner(id);
      }

   }

   private void bindTextureInner(Identifier id) {
      AbstractTexture abstractTexture = (AbstractTexture)this.textures.get(id);
      if (abstractTexture == null) {
         abstractTexture = new ResourceTexture(id);
         this.registerTexture(id, (AbstractTexture)abstractTexture);
      }

      ((AbstractTexture)abstractTexture).bindTexture();
   }

   public void registerTexture(Identifier id, AbstractTexture texture) {
      texture = this.loadTexture(id, texture);
      AbstractTexture abstractTexture = (AbstractTexture)this.textures.put(id, texture);
      if (abstractTexture != texture) {
         if (abstractTexture != null && abstractTexture != MissingSprite.getMissingSpriteTexture()) {
            this.tickListeners.remove(abstractTexture);
            this.closeTexture(id, abstractTexture);
         }

         if (texture instanceof TextureTickListener) {
            this.tickListeners.add((TextureTickListener)texture);
         }
      }

   }

   private void closeTexture(Identifier id, AbstractTexture texture) {
      if (texture != MissingSprite.getMissingSpriteTexture()) {
         try {
            texture.close();
         } catch (Exception var4) {
            LOGGER.warn((String)"Failed to close texture {}", (Object)id, (Object)var4);
         }
      }

      texture.clearGlId();
   }

   private AbstractTexture loadTexture(Identifier id, AbstractTexture texture) {
      try {
         texture.load(this.resourceContainer);
         return texture;
      } catch (IOException var6) {
         if (id != MISSING_IDENTIFIER) {
            LOGGER.warn((String)"Failed to load texture: {}", (Object)id, (Object)var6);
         }

         return MissingSprite.getMissingSpriteTexture();
      } catch (Throwable var7) {
         CrashReport crashReport = CrashReport.create(var7, "Registering texture");
         CrashReportSection crashReportSection = crashReport.addElement("Resource location being registered");
         crashReportSection.add("Resource location", (Object)id);
         crashReportSection.add("Texture object class", () -> {
            return texture.getClass().getName();
         });
         throw new CrashException(crashReport);
      }
   }

   public AbstractTexture getTexture(Identifier id) {
      AbstractTexture abstractTexture = (AbstractTexture)this.textures.get(id);
      if (abstractTexture == null) {
         abstractTexture = new ResourceTexture(id);
         this.registerTexture(id, (AbstractTexture)abstractTexture);
      }

      return (AbstractTexture)abstractTexture;
   }

   public AbstractTexture getOrDefault(Identifier id, AbstractTexture fallback) {
      return (AbstractTexture)this.textures.getOrDefault(id, fallback);
   }

   public Identifier registerDynamicTexture(String prefix, NativeImageBackedTexture texture) {
      Integer integer = (Integer)this.dynamicIdCounters.get(prefix);
      if (integer == null) {
         integer = 1;
      } else {
         integer = integer + 1;
      }

      this.dynamicIdCounters.put(prefix, integer);
      Identifier identifier = new Identifier(String.format(Locale.ROOT, "dynamic/%s_%d", prefix, integer));
      this.registerTexture(identifier, texture);
      return identifier;
   }

   public CompletableFuture<Void> loadTextureAsync(Identifier id, Executor executor) {
      if (!this.textures.containsKey(id)) {
         AsyncTexture asyncTexture = new AsyncTexture(this.resourceContainer, id, executor);
         this.textures.put(id, asyncTexture);
         return asyncTexture.getLoadCompleteFuture().thenRunAsync(() -> {
            this.registerTexture(id, asyncTexture);
         }, TextureManager::runOnRenderThread);
      } else {
         return CompletableFuture.completedFuture((Object)null);
      }
   }

   private static void runOnRenderThread(Runnable runnable) {
      MinecraftClient.getInstance().execute(() -> {
         Objects.requireNonNull(runnable);
         RenderSystem.recordRenderCall(runnable::run);
      });
   }

   public void tick() {
      Iterator var1 = this.tickListeners.iterator();

      while(var1.hasNext()) {
         TextureTickListener textureTickListener = (TextureTickListener)var1.next();
         textureTickListener.tick();
      }

   }

   public void destroyTexture(Identifier id) {
      AbstractTexture abstractTexture = this.getOrDefault(id, MissingSprite.getMissingSpriteTexture());
      if (abstractTexture != MissingSprite.getMissingSpriteTexture()) {
         TextureUtil.releaseTextureId(abstractTexture.getGlId());
      }

   }

   public void close() {
      this.textures.forEach(this::closeTexture);
      this.textures.clear();
      this.tickListeners.clear();
      this.dynamicIdCounters.clear();
   }

   public CompletableFuture<Void> reload(ResourceReloader.Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler, Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
      CompletableFuture var10000 = CompletableFuture.allOf(TitleScreen.loadTexturesAsync(this, prepareExecutor), this.loadTextureAsync(ClickableWidget.WIDGETS_TEXTURE, prepareExecutor));
      Objects.requireNonNull(synchronizer);
      return var10000.thenCompose(synchronizer::whenPrepared).thenAcceptAsync((void_) -> {
         MissingSprite.getMissingSpriteTexture();
         RealmsMainScreen.method_23765(this.resourceContainer);
         Iterator iterator = this.textures.entrySet().iterator();

         while(true) {
            while(iterator.hasNext()) {
               Entry<Identifier, AbstractTexture> entry = (Entry)iterator.next();
               Identifier identifier = (Identifier)entry.getKey();
               AbstractTexture abstractTexture = (AbstractTexture)entry.getValue();
               if (abstractTexture == MissingSprite.getMissingSpriteTexture() && !identifier.equals(MissingSprite.getMissingSpriteId())) {
                  iterator.remove();
               } else {
                  abstractTexture.registerTexture(this, manager, identifier, applyExecutor);
               }
            }

            return;
         }
      }, (runnable) -> {
         Objects.requireNonNull(runnable);
         RenderSystem.recordRenderCall(runnable::run);
      });
   }
}
