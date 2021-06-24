package net.minecraft.client.gui.screen.world;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.BackupPromptScreen;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.FatalErrorScreen;
import net.minecraft.client.gui.screen.NoticeScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.SaveLevelScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.resource.DataPackSettings;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.level.LevelInfo;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelStorageException;
import net.minecraft.world.level.storage.LevelSummary;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WorldListWidget extends AlwaysSelectedEntryListWidget<WorldListWidget.Entry> {
   static final Logger LOGGER = LogManager.getLogger();
   static final DateFormat DATE_FORMAT = new SimpleDateFormat();
   static final Identifier UNKNOWN_SERVER_LOCATION = new Identifier("textures/misc/unknown_server.png");
   static final Identifier WORLD_SELECTION_LOCATION = new Identifier("textures/gui/world_selection.png");
   static final Text FROM_NEWER_VERSION_FIRST_LINE;
   static final Text FROM_NEWER_VERSION_SECOND_LINE;
   static final Text SNAPSHOT_FIRST_LINE;
   static final Text SNAPSHOT_SECOND_LINE;
   static final Text LOCKED_TEXT;
   static final Text PRE_WORLDHEIGHT_TEXT;
   private final SelectWorldScreen parent;
   @Nullable
   private List<LevelSummary> levels;

   public WorldListWidget(SelectWorldScreen parent, MinecraftClient client, int width, int height, int top, int bottom, int itemHeight, Supplier<String> searchFilter, @Nullable WorldListWidget list) {
      super(client, width, height, top, bottom, itemHeight);
      this.parent = parent;
      if (list != null) {
         this.levels = list.levels;
      }

      this.filter(searchFilter, false);
   }

   public void filter(Supplier<String> searchTextSupplier, boolean load) {
      this.clearEntries();
      LevelStorage levelStorage = this.client.getLevelStorage();
      if (this.levels == null || load) {
         try {
            this.levels = levelStorage.getLevelList();
         } catch (LevelStorageException var7) {
            LOGGER.error((String)"Couldn't load level list", (Throwable)var7);
            this.client.openScreen(new FatalErrorScreen(new TranslatableText("selectWorld.unable_to_load"), new LiteralText(var7.getMessage())));
            return;
         }

         Collections.sort(this.levels);
      }

      if (this.levels.isEmpty()) {
         this.client.openScreen(CreateWorldScreen.create((Screen)null));
      } else {
         String string = ((String)searchTextSupplier.get()).toLowerCase(Locale.ROOT);
         Iterator var5 = this.levels.iterator();

         while(true) {
            LevelSummary levelSummary;
            do {
               if (!var5.hasNext()) {
                  return;
               }

               levelSummary = (LevelSummary)var5.next();
            } while(!levelSummary.getDisplayName().toLowerCase(Locale.ROOT).contains(string) && !levelSummary.getName().toLowerCase(Locale.ROOT).contains(string));

            this.addEntry(new WorldListWidget.Entry(this, levelSummary));
         }
      }
   }

   protected int getScrollbarPositionX() {
      return super.getScrollbarPositionX() + 20;
   }

   public int getRowWidth() {
      return super.getRowWidth() + 50;
   }

   protected boolean isFocused() {
      return this.parent.getFocused() == this;
   }

   public void setSelected(@Nullable WorldListWidget.Entry entry) {
      super.setSelected(entry);
      this.parent.worldSelected(entry != null && !entry.level.isUnavailable());
   }

   protected void moveSelection(EntryListWidget.MoveDirection direction) {
      this.moveSelectionIf(direction, (entry) -> {
         return !entry.level.isUnavailable();
      });
   }

   public Optional<WorldListWidget.Entry> getSelectedAsOptional() {
      return Optional.ofNullable((WorldListWidget.Entry)this.getSelected());
   }

   public SelectWorldScreen getParent() {
      return this.parent;
   }

   static {
      FROM_NEWER_VERSION_FIRST_LINE = (new TranslatableText("selectWorld.tooltip.fromNewerVersion1")).formatted(Formatting.RED);
      FROM_NEWER_VERSION_SECOND_LINE = (new TranslatableText("selectWorld.tooltip.fromNewerVersion2")).formatted(Formatting.RED);
      SNAPSHOT_FIRST_LINE = (new TranslatableText("selectWorld.tooltip.snapshot1")).formatted(Formatting.GOLD);
      SNAPSHOT_SECOND_LINE = (new TranslatableText("selectWorld.tooltip.snapshot2")).formatted(Formatting.GOLD);
      LOCKED_TEXT = (new TranslatableText("selectWorld.locked")).formatted(Formatting.RED);
      PRE_WORLDHEIGHT_TEXT = (new TranslatableText("selectWorld.pre_worldheight")).formatted(Formatting.RED);
   }

   @Environment(EnvType.CLIENT)
   public final class Entry extends AlwaysSelectedEntryListWidget.Entry<WorldListWidget.Entry> implements AutoCloseable {
      private static final int field_32435 = 32;
      private static final int field_32436 = 32;
      private static final int field_32437 = 0;
      private static final int field_32438 = 32;
      private static final int field_32439 = 64;
      private static final int field_32440 = 96;
      private static final int field_32441 = 0;
      private static final int field_32442 = 32;
      private final MinecraftClient client;
      private final SelectWorldScreen screen;
      final LevelSummary level;
      private final Identifier iconLocation;
      private File iconFile;
      @Nullable
      private final NativeImageBackedTexture icon;
      private long time;

      public Entry(WorldListWidget levelList, LevelSummary level) {
         this.screen = levelList.getParent();
         this.level = level;
         this.client = MinecraftClient.getInstance();
         String string = level.getName();
         String var10004 = Util.replaceInvalidChars(string, Identifier::isPathCharacterValid);
         this.iconLocation = new Identifier("minecraft", "worlds/" + var10004 + "/" + Hashing.sha1().hashUnencodedChars(string) + "/icon");
         this.iconFile = level.getFile();
         if (!this.iconFile.isFile()) {
            this.iconFile = null;
         }

         this.icon = this.getIconTexture();
      }

      public Text method_37006() {
         TranslatableText translatableText = new TranslatableText("narrator.select.world", new Object[]{this.level.getDisplayName(), new Date(this.level.getLastPlayed()), this.level.isHardcore() ? new TranslatableText("gameMode.hardcore") : new TranslatableText("gameMode." + this.level.getGameMode().getName()), this.level.hasCheats() ? new TranslatableText("selectWorld.cheats") : LiteralText.EMPTY, this.level.getVersion()});
         Object text3;
         if (this.level.isLocked()) {
            text3 = ScreenTexts.joinSentences(translatableText, WorldListWidget.LOCKED_TEXT);
         } else if (this.level.isPreWorldHeightChangeVersion()) {
            text3 = ScreenTexts.joinSentences(translatableText, WorldListWidget.PRE_WORLDHEIGHT_TEXT);
         } else {
            text3 = translatableText;
         }

         return new TranslatableText("narrator.select", new Object[]{text3});
      }

      public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
         String string = this.level.getDisplayName();
         String var10000 = this.level.getName();
         String string2 = var10000 + " (" + WorldListWidget.DATE_FORMAT.format(new Date(this.level.getLastPlayed())) + ")";
         if (StringUtils.isEmpty(string)) {
            var10000 = I18n.translate("selectWorld.world");
            string = var10000 + " " + (index + 1);
         }

         Text text = this.level.getDetails();
         this.client.textRenderer.draw(matrices, string, (float)(x + 32 + 3), (float)(y + 1), 16777215);
         TextRenderer var17 = this.client.textRenderer;
         float var10003 = (float)(x + 32 + 3);
         Objects.requireNonNull(this.client.textRenderer);
         var17.draw(matrices, string2, var10003, (float)(y + 9 + 3), 8421504);
         var17 = this.client.textRenderer;
         var10003 = (float)(x + 32 + 3);
         Objects.requireNonNull(this.client.textRenderer);
         int var10004 = y + 9;
         Objects.requireNonNull(this.client.textRenderer);
         var17.draw(matrices, text, var10003, (float)(var10004 + 9 + 3), 8421504);
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         RenderSystem.setShaderTexture(0, this.icon != null ? this.iconLocation : WorldListWidget.UNKNOWN_SERVER_LOCATION);
         RenderSystem.enableBlend();
         DrawableHelper.drawTexture(matrices, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
         RenderSystem.disableBlend();
         if (this.client.options.touchscreen || hovered) {
            RenderSystem.setShaderTexture(0, WorldListWidget.WORLD_SELECTION_LOCATION);
            DrawableHelper.fill(matrices, x, y, x + 32, y + 32, -1601138544);
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            int i = mouseX - x;
            boolean bl = i < 32;
            int j = bl ? 32 : 0;
            if (this.level.isLocked()) {
               DrawableHelper.drawTexture(matrices, x, y, 96.0F, (float)j, 32, 32, 256, 256);
               if (bl) {
                  this.screen.setTooltip(this.client.textRenderer.wrapLines(WorldListWidget.LOCKED_TEXT, 175));
               }
            } else if (this.level.isPreWorldHeightChangeVersion()) {
               DrawableHelper.drawTexture(matrices, x, y, 96.0F, 32.0F, 32, 32, 256, 256);
               if (bl) {
                  this.screen.setTooltip(this.client.textRenderer.wrapLines(WorldListWidget.PRE_WORLDHEIGHT_TEXT, 175));
               }
            } else if (this.level.isDifferentVersion()) {
               DrawableHelper.drawTexture(matrices, x, y, 32.0F, (float)j, 32, 32, 256, 256);
               if (this.level.isFutureLevel()) {
                  DrawableHelper.drawTexture(matrices, x, y, 96.0F, (float)j, 32, 32, 256, 256);
                  if (bl) {
                     this.screen.setTooltip(ImmutableList.of(WorldListWidget.FROM_NEWER_VERSION_FIRST_LINE.asOrderedText(), WorldListWidget.FROM_NEWER_VERSION_SECOND_LINE.asOrderedText()));
                  }
               } else if (!SharedConstants.getGameVersion().isStable()) {
                  DrawableHelper.drawTexture(matrices, x, y, 64.0F, (float)j, 32, 32, 256, 256);
                  if (bl) {
                     this.screen.setTooltip(ImmutableList.of(WorldListWidget.SNAPSHOT_FIRST_LINE.asOrderedText(), WorldListWidget.SNAPSHOT_SECOND_LINE.asOrderedText()));
                  }
               }
            } else {
               DrawableHelper.drawTexture(matrices, x, y, 0.0F, (float)j, 32, 32, 256, 256);
            }
         }

      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         if (this.level.isUnavailable()) {
            return true;
         } else {
            WorldListWidget.this.setSelected(this);
            this.screen.worldSelected(WorldListWidget.this.getSelectedAsOptional().isPresent());
            if (mouseX - (double)WorldListWidget.this.getRowLeft() <= 32.0D) {
               this.play();
               return true;
            } else if (Util.getMeasuringTimeMs() - this.time < 250L) {
               this.play();
               return true;
            } else {
               this.time = Util.getMeasuringTimeMs();
               return false;
            }
         }
      }

      public void play() {
         if (!this.level.isUnavailable()) {
            LevelSummary.ConversionWarning conversionWarning = this.level.getConversionWarning();
            if (conversionWarning.promptsBackup()) {
               String string = "selectWorld.backupQuestion." + conversionWarning.getTranslationKeySuffix();
               String string2 = "selectWorld.backupWarning." + conversionWarning.getTranslationKeySuffix();
               MutableText mutableText = new TranslatableText(string);
               if (conversionWarning.needsBoldRedFormatting()) {
                  mutableText.formatted(Formatting.BOLD, Formatting.RED);
               }

               Text text = new TranslatableText(string2, new Object[]{this.level.getVersion(), SharedConstants.getGameVersion().getName()});
               this.client.openScreen(new BackupPromptScreen(this.screen, (bl, bl2) -> {
                  if (bl) {
                     String string = this.level.getName();

                     try {
                        LevelStorage.Session session = this.client.getLevelStorage().createSession(string);

                        try {
                           EditWorldScreen.backupLevel(session);
                        } catch (Throwable var8) {
                           if (session != null) {
                              try {
                                 session.close();
                              } catch (Throwable var7) {
                                 var8.addSuppressed(var7);
                              }
                           }

                           throw var8;
                        }

                        if (session != null) {
                           session.close();
                        }
                     } catch (IOException var9) {
                        SystemToast.addWorldAccessFailureToast(this.client, string);
                        WorldListWidget.LOGGER.error((String)"Failed to backup level {}", (Object)string, (Object)var9);
                     }
                  }

                  this.start();
               }, mutableText, text, false));
            } else if (this.level.isFutureLevel()) {
               this.client.openScreen(new ConfirmScreen((bl) -> {
                  if (bl) {
                     try {
                        this.start();
                     } catch (Exception var3) {
                        WorldListWidget.LOGGER.error((String)"Failure to open 'future world'", (Throwable)var3);
                        this.client.openScreen(new NoticeScreen(() -> {
                           this.client.openScreen(this.screen);
                        }, new TranslatableText("selectWorld.futureworld.error.title"), new TranslatableText("selectWorld.futureworld.error.text")));
                     }
                  } else {
                     this.client.openScreen(this.screen);
                  }

               }, new TranslatableText("selectWorld.versionQuestion"), new TranslatableText("selectWorld.versionWarning", new Object[]{this.level.getVersion()}), new TranslatableText("selectWorld.versionJoinButton"), ScreenTexts.CANCEL));
            } else {
               this.start();
            }

         }
      }

      public void deleteIfConfirmed() {
         this.client.openScreen(new ConfirmScreen((confirmed) -> {
            if (confirmed) {
               this.client.openScreen(new ProgressScreen(true));
               this.delete();
            }

            this.client.openScreen(this.screen);
         }, new TranslatableText("selectWorld.deleteQuestion"), new TranslatableText("selectWorld.deleteWarning", new Object[]{this.level.getDisplayName()}), new TranslatableText("selectWorld.deleteButton"), ScreenTexts.CANCEL));
      }

      public void delete() {
         LevelStorage levelStorage = this.client.getLevelStorage();
         String string = this.level.getName();

         try {
            LevelStorage.Session session = levelStorage.createSession(string);

            try {
               session.deleteSessionLock();
            } catch (Throwable var7) {
               if (session != null) {
                  try {
                     session.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }
               }

               throw var7;
            }

            if (session != null) {
               session.close();
            }
         } catch (IOException var8) {
            SystemToast.addWorldDeleteFailureToast(this.client, string);
            WorldListWidget.LOGGER.error((String)"Failed to delete world {}", (Object)string, (Object)var8);
         }

         WorldListWidget.this.filter(() -> {
            return this.screen.searchBox.getText();
         }, true);
      }

      public void edit() {
         String string = this.level.getName();

         try {
            LevelStorage.Session session = this.client.getLevelStorage().createSession(string);
            this.client.openScreen(new EditWorldScreen((bl) -> {
               try {
                  session.close();
               } catch (IOException var5) {
                  WorldListWidget.LOGGER.error((String)"Failed to unlock level {}", (Object)string, (Object)var5);
               }

               if (bl) {
                  WorldListWidget.this.filter(() -> {
                     return this.screen.searchBox.getText();
                  }, true);
               }

               this.client.openScreen(this.screen);
            }, session));
         } catch (IOException var3) {
            SystemToast.addWorldAccessFailureToast(this.client, string);
            WorldListWidget.LOGGER.error((String)"Failed to access level {}", (Object)string, (Object)var3);
            WorldListWidget.this.filter(() -> {
               return this.screen.searchBox.getText();
            }, true);
         }

      }

      public void recreate() {
         this.openReadingWorldScreen();
         DynamicRegistryManager.Impl impl = DynamicRegistryManager.create();

         try {
            LevelStorage.Session session = this.client.getLevelStorage().createSession(this.level.getName());

            try {
               MinecraftClient.IntegratedResourceManager integratedResourceManager = this.client.createIntegratedResourceManager(impl, MinecraftClient::loadDataPackSettings, MinecraftClient::createSaveProperties, false, session);

               try {
                  LevelInfo levelInfo = integratedResourceManager.getSaveProperties().getLevelInfo();
                  DataPackSettings dataPackSettings = levelInfo.getDataPackSettings();
                  GeneratorOptions generatorOptions = integratedResourceManager.getSaveProperties().getGeneratorOptions();
                  Path path = CreateWorldScreen.copyDataPack(session.getDirectory(WorldSavePath.DATAPACKS), this.client);
                  if (generatorOptions.isLegacyCustomizedType()) {
                     this.client.openScreen(new ConfirmScreen((bl) -> {
                        this.client.openScreen((Screen)(bl ? new CreateWorldScreen(this.screen, levelInfo, generatorOptions, path, dataPackSettings, impl) : this.screen));
                     }, new TranslatableText("selectWorld.recreate.customized.title"), new TranslatableText("selectWorld.recreate.customized.text"), ScreenTexts.PROCEED, ScreenTexts.CANCEL));
                  } else {
                     this.client.openScreen(new CreateWorldScreen(this.screen, levelInfo, generatorOptions, path, dataPackSettings, impl));
                  }
               } catch (Throwable var10) {
                  if (integratedResourceManager != null) {
                     try {
                        integratedResourceManager.close();
                     } catch (Throwable var9) {
                        var10.addSuppressed(var9);
                     }
                  }

                  throw var10;
               }

               if (integratedResourceManager != null) {
                  integratedResourceManager.close();
               }
            } catch (Throwable var11) {
               if (session != null) {
                  try {
                     session.close();
                  } catch (Throwable var8) {
                     var11.addSuppressed(var8);
                  }
               }

               throw var11;
            }

            if (session != null) {
               session.close();
            }
         } catch (Exception var12) {
            WorldListWidget.LOGGER.error((String)"Unable to recreate world", (Throwable)var12);
            this.client.openScreen(new NoticeScreen(() -> {
               this.client.openScreen(this.screen);
            }, new TranslatableText("selectWorld.recreate.error.title"), new TranslatableText("selectWorld.recreate.error.text")));
         }

      }

      private void start() {
         this.client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
         if (this.client.getLevelStorage().levelExists(this.level.getName())) {
            this.openReadingWorldScreen();
            this.client.startIntegratedServer(this.level.getName());
         }

      }

      private void openReadingWorldScreen() {
         this.client.method_29970(new SaveLevelScreen(new TranslatableText("selectWorld.data_read")));
      }

      @Nullable
      private NativeImageBackedTexture getIconTexture() {
         boolean bl = this.iconFile != null && this.iconFile.isFile();
         if (bl) {
            try {
               FileInputStream inputStream = new FileInputStream(this.iconFile);

               NativeImageBackedTexture var5;
               try {
                  NativeImage nativeImage = NativeImage.read((InputStream)inputStream);
                  Validate.validState(nativeImage.getWidth() == 64, "Must be 64 pixels wide");
                  Validate.validState(nativeImage.getHeight() == 64, "Must be 64 pixels high");
                  NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(nativeImage);
                  this.client.getTextureManager().registerTexture(this.iconLocation, nativeImageBackedTexture);
                  var5 = nativeImageBackedTexture;
               } catch (Throwable var7) {
                  try {
                     inputStream.close();
                  } catch (Throwable var6) {
                     var7.addSuppressed(var6);
                  }

                  throw var7;
               }

               inputStream.close();
               return var5;
            } catch (Throwable var8) {
               WorldListWidget.LOGGER.error((String)"Invalid icon for world {}", (Object)this.level.getName(), (Object)var8);
               this.iconFile = null;
               return null;
            }
         } else {
            this.client.getTextureManager().destroyTexture(this.iconLocation);
            return null;
         }
      }

      public void close() {
         if (this.icon != null) {
            this.icon.close();
         }

      }

      public String getLevelDisplayName() {
         return this.level.getDisplayName();
      }
   }
}
