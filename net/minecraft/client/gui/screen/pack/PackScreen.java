package net.minecraft.client.gui.screen.pack;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class PackScreen extends Screen {
   static final Logger LOGGER = LogManager.getLogger();
   private static final int field_32395 = 200;
   private static final Text DROP_INFO;
   static final Text FOLDER_INFO;
   private static final int field_32396 = 20;
   private static final Identifier UNKNOWN_PACK;
   private final ResourcePackOrganizer organizer;
   private final Screen parent;
   @Nullable
   private PackScreen.DirectoryWatcher directoryWatcher;
   private long refreshTimeout;
   private PackListWidget availablePackList;
   private PackListWidget selectedPackList;
   private final File file;
   private ButtonWidget doneButton;
   private final Map<String, Identifier> iconTextures = Maps.newHashMap();

   public PackScreen(Screen parent, ResourcePackManager packManager, Consumer<ResourcePackManager> consumer, File file, Text title) {
      super(title);
      this.parent = parent;
      this.organizer = new ResourcePackOrganizer(this::updatePackLists, this::getPackIconTexture, packManager, consumer);
      this.file = file;
      this.directoryWatcher = PackScreen.DirectoryWatcher.create(file);
   }

   public void onClose() {
      this.organizer.apply();
      this.client.openScreen(this.parent);
      this.closeDirectoryWatcher();
   }

   private void closeDirectoryWatcher() {
      if (this.directoryWatcher != null) {
         try {
            this.directoryWatcher.close();
            this.directoryWatcher = null;
         } catch (Exception var2) {
         }
      }

   }

   protected void init() {
      this.doneButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.width / 2 + 4, this.height - 48, 150, 20, ScreenTexts.DONE, (button) -> {
         this.onClose();
      }));
      this.addDrawableChild(new ButtonWidget(this.width / 2 - 154, this.height - 48, 150, 20, new TranslatableText("pack.openFolder"), (button) -> {
         Util.getOperatingSystem().open(this.file);
      }, new ButtonWidget.TooltipSupplier() {
         public void onTooltip(ButtonWidget buttonWidget, MatrixStack matrixStack, int i, int j) {
            PackScreen.this.renderTooltip(matrixStack, PackScreen.FOLDER_INFO, i, j);
         }

         public void method_37023(Consumer<Text> consumer) {
            consumer.accept(PackScreen.FOLDER_INFO);
         }
      }));
      this.availablePackList = new PackListWidget(this.client, 200, this.height, new TranslatableText("pack.available.title"));
      this.availablePackList.setLeftPos(this.width / 2 - 4 - 200);
      this.addSelectableChild(this.availablePackList);
      this.selectedPackList = new PackListWidget(this.client, 200, this.height, new TranslatableText("pack.selected.title"));
      this.selectedPackList.setLeftPos(this.width / 2 + 4);
      this.addSelectableChild(this.selectedPackList);
      this.refresh();
   }

   public void tick() {
      if (this.directoryWatcher != null) {
         try {
            if (this.directoryWatcher.pollForChange()) {
               this.refreshTimeout = 20L;
            }
         } catch (IOException var2) {
            LOGGER.warn((String)"Failed to poll for directory {} changes, stopping", (Object)this.file);
            this.closeDirectoryWatcher();
         }
      }

      if (this.refreshTimeout > 0L && --this.refreshTimeout == 0L) {
         this.refresh();
      }

   }

   private void updatePackLists() {
      this.updatePackList(this.selectedPackList, this.organizer.getEnabledPacks());
      this.updatePackList(this.availablePackList, this.organizer.getDisabledPacks());
      this.doneButton.active = !this.selectedPackList.children().isEmpty();
   }

   private void updatePackList(PackListWidget widget, Stream<ResourcePackOrganizer.Pack> packs) {
      widget.children().clear();
      packs.forEach((pack) -> {
         widget.children().add(new PackListWidget.ResourcePackEntry(this.client, widget, this, pack));
      });
   }

   private void refresh() {
      this.organizer.refresh();
      this.updatePackLists();
      this.refreshTimeout = 0L;
      this.iconTextures.clear();
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.renderBackgroundTexture(0);
      this.availablePackList.render(matrices, mouseX, mouseY, delta);
      this.selectedPackList.render(matrices, mouseX, mouseY, delta);
      drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 8, 16777215);
      drawCenteredText(matrices, this.textRenderer, DROP_INFO, this.width / 2, 20, 16777215);
      super.render(matrices, mouseX, mouseY, delta);
   }

   protected static void copyPacks(MinecraftClient client, List<Path> srcPaths, Path destPath) {
      MutableBoolean mutableBoolean = new MutableBoolean();
      srcPaths.forEach((src) -> {
         try {
            Stream stream = Files.walk(src);

            try {
               stream.forEach((toCopy) -> {
                  try {
                     Util.relativeCopy(src.getParent(), destPath, toCopy);
                  } catch (IOException var5) {
                     LOGGER.warn((String)"Failed to copy datapack file  from {} to {}", (Object)toCopy, destPath, var5);
                     mutableBoolean.setTrue();
                  }

               });
            } catch (Throwable var7) {
               if (stream != null) {
                  try {
                     stream.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (stream != null) {
               stream.close();
            }
         } catch (IOException var8) {
            LOGGER.warn((String)"Failed to copy datapack file from {} to {}", (Object)src, (Object)destPath);
            mutableBoolean.setTrue();
         }

      });
      if (mutableBoolean.isTrue()) {
         SystemToast.addPackCopyFailure(client, destPath.toString());
      }

   }

   public void filesDragged(List<Path> paths) {
      String string = (String)paths.stream().map(Path::getFileName).map(Path::toString).collect(Collectors.joining(", "));
      this.client.openScreen(new ConfirmScreen((bl) -> {
         if (bl) {
            copyPacks(this.client, paths, this.file.toPath());
            this.refresh();
         }

         this.client.openScreen(this);
      }, new TranslatableText("pack.dropConfirm"), new LiteralText(string)));
   }

   private Identifier loadPackIcon(TextureManager textureManager, ResourcePackProfile resourcePackProfile) {
      try {
         ResourcePack resourcePack = resourcePackProfile.createResourcePack();

         Identifier var5;
         label84: {
            Identifier var8;
            try {
               InputStream inputStream = resourcePack.openRoot("pack.png");

               label86: {
                  try {
                     if (inputStream != null) {
                        String string = resourcePackProfile.getName();
                        String var10003 = Util.replaceInvalidChars(string, Identifier::isPathCharacterValid);
                        Identifier identifier = new Identifier("minecraft", "pack/" + var10003 + "/" + Hashing.sha1().hashUnencodedChars(string) + "/icon");
                        NativeImage nativeImage = NativeImage.read(inputStream);
                        textureManager.registerTexture(identifier, new NativeImageBackedTexture(nativeImage));
                        var8 = identifier;
                        break label86;
                     }

                     var5 = UNKNOWN_PACK;
                  } catch (Throwable var11) {
                     if (inputStream != null) {
                        try {
                           inputStream.close();
                        } catch (Throwable var10) {
                           var11.addSuppressed(var10);
                        }
                     }

                     throw var11;
                  }

                  if (inputStream != null) {
                     inputStream.close();
                  }
                  break label84;
               }

               if (inputStream != null) {
                  inputStream.close();
               }
            } catch (Throwable var12) {
               if (resourcePack != null) {
                  try {
                     resourcePack.close();
                  } catch (Throwable var9) {
                     var12.addSuppressed(var9);
                  }
               }

               throw var12;
            }

            if (resourcePack != null) {
               resourcePack.close();
            }

            return var8;
         }

         if (resourcePack != null) {
            resourcePack.close();
         }

         return var5;
      } catch (FileNotFoundException var13) {
      } catch (Exception var14) {
         LOGGER.warn((String)"Failed to load icon from pack {}", (Object)resourcePackProfile.getName(), (Object)var14);
      }

      return UNKNOWN_PACK;
   }

   private Identifier getPackIconTexture(ResourcePackProfile resourcePackProfile) {
      return (Identifier)this.iconTextures.computeIfAbsent(resourcePackProfile.getName(), (profileName) -> {
         return this.loadPackIcon(this.client.getTextureManager(), resourcePackProfile);
      });
   }

   static {
      DROP_INFO = (new TranslatableText("pack.dropInfo")).formatted(Formatting.GRAY);
      FOLDER_INFO = new TranslatableText("pack.folderInfo");
      UNKNOWN_PACK = new Identifier("textures/misc/unknown_pack.png");
   }

   @Environment(EnvType.CLIENT)
   private static class DirectoryWatcher implements AutoCloseable {
      private final WatchService watchService;
      private final Path path;

      public DirectoryWatcher(File file) throws IOException {
         this.path = file.toPath();
         this.watchService = this.path.getFileSystem().newWatchService();

         try {
            this.watchDirectory(this.path);
            DirectoryStream directoryStream = Files.newDirectoryStream(this.path);

            try {
               Iterator var3 = directoryStream.iterator();

               while(var3.hasNext()) {
                  Path path = (Path)var3.next();
                  if (Files.isDirectory(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
                     this.watchDirectory(path);
                  }
               }
            } catch (Throwable var6) {
               if (directoryStream != null) {
                  try {
                     directoryStream.close();
                  } catch (Throwable var5) {
                     var6.addSuppressed(var5);
                  }
               }

               throw var6;
            }

            if (directoryStream != null) {
               directoryStream.close();
            }

         } catch (Exception var7) {
            this.watchService.close();
            throw var7;
         }
      }

      @Nullable
      public static PackScreen.DirectoryWatcher create(File file) {
         try {
            return new PackScreen.DirectoryWatcher(file);
         } catch (IOException var2) {
            PackScreen.LOGGER.warn((String)"Failed to initialize pack directory {} monitoring", (Object)file, (Object)var2);
            return null;
         }
      }

      private void watchDirectory(Path path) throws IOException {
         path.register(this.watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
      }

      public boolean pollForChange() throws IOException {
         boolean bl = false;

         WatchKey watchKey;
         while((watchKey = this.watchService.poll()) != null) {
            List<WatchEvent<?>> list = watchKey.pollEvents();
            Iterator var4 = list.iterator();

            while(var4.hasNext()) {
               WatchEvent<?> watchEvent = (WatchEvent)var4.next();
               bl = true;
               if (watchKey.watchable() == this.path && watchEvent.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                  Path path = this.path.resolve((Path)watchEvent.context());
                  if (Files.isDirectory(path, new LinkOption[]{LinkOption.NOFOLLOW_LINKS})) {
                     this.watchDirectory(path);
                  }
               }
            }

            watchKey.reset();
         }

         return bl;
      }

      public void close() throws IOException {
         this.watchService.close();
      }
   }
}
