package net.minecraft.client.realms.gui.screen;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.RealmsWorldOptions;
import net.minecraft.client.realms.dto.WorldTemplate;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.gui.RealmsWorldSlotButton;
import net.minecraft.client.realms.task.CloseServerTask;
import net.minecraft.client.realms.task.OpenServerTask;
import net.minecraft.client.realms.task.SwitchMinigameTask;
import net.minecraft.client.realms.task.SwitchSlotTask;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class RealmsConfigureWorldScreen extends RealmsScreen {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Identifier ON_ICON = new Identifier("realms", "textures/gui/realms/on_icon.png");
   private static final Identifier OFF_ICON = new Identifier("realms", "textures/gui/realms/off_icon.png");
   private static final Identifier EXPIRED_ICON = new Identifier("realms", "textures/gui/realms/expired_icon.png");
   private static final Identifier EXPIRES_SOON_ICON = new Identifier("realms", "textures/gui/realms/expires_soon_icon.png");
   private static final Text WORLDS_TITLE = new TranslatableText("mco.configure.worlds.title");
   private static final Text CONFIGURE_REALM_TITLE = new TranslatableText("mco.configure.world.title");
   private static final Text CURRENT_MINIGAME_TEXT = (new TranslatableText("mco.configure.current.minigame")).append(": ");
   private static final Text EXPIRED_TEXT = new TranslatableText("mco.selectServer.expired");
   private static final Text EXPIRES_SOON_TEXT = new TranslatableText("mco.selectServer.expires.soon");
   private static final Text EXPIRES_IN_A_DAY_TEXT = new TranslatableText("mco.selectServer.expires.day");
   private static final Text OPEN_TEXT = new TranslatableText("mco.selectServer.open");
   private static final Text CLOSED_TEXT = new TranslatableText("mco.selectServer.closed");
   private static final int field_32121 = 80;
   private static final int field_32122 = 5;
   @Nullable
   private Text toolTip;
   private final RealmsMainScreen parent;
   @Nullable
   private RealmsServer server;
   private final long serverId;
   private int left_x;
   private int right_x;
   private ButtonWidget playersButton;
   private ButtonWidget settingsButton;
   private ButtonWidget subscriptionButton;
   private ButtonWidget optionsButton;
   private ButtonWidget backupButton;
   private ButtonWidget resetWorldButton;
   private ButtonWidget switchMinigameButton;
   private boolean stateChanged;
   private int animTick;
   private int clicks;
   private final List<RealmsWorldSlotButton> field_33777 = Lists.newArrayList();

   public RealmsConfigureWorldScreen(RealmsMainScreen parent, long serverId) {
      super(CONFIGURE_REALM_TITLE);
      this.parent = parent;
      this.serverId = serverId;
   }

   public void init() {
      if (this.server == null) {
         this.fetchServerData(this.serverId);
      }

      this.left_x = this.width / 2 - 187;
      this.right_x = this.width / 2 + 190;
      this.client.keyboard.setRepeatEvents(true);
      this.playersButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.buttonCenter(0, 3), row(0), 100, 20, new TranslatableText("mco.configure.world.buttons.players"), (button) -> {
         this.client.openScreen(new RealmsPlayerScreen(this, this.server));
      }));
      this.settingsButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.buttonCenter(1, 3), row(0), 100, 20, new TranslatableText("mco.configure.world.buttons.settings"), (button) -> {
         this.client.openScreen(new RealmsSettingsScreen(this, this.server.clone()));
      }));
      this.subscriptionButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.buttonCenter(2, 3), row(0), 100, 20, new TranslatableText("mco.configure.world.buttons.subscription"), (button) -> {
         this.client.openScreen(new RealmsSubscriptionInfoScreen(this, this.server.clone(), this.parent));
      }));
      this.field_33777.clear();

      for(int i = 1; i < 5; ++i) {
         this.field_33777.add(this.addSlotButton(i));
      }

      this.switchMinigameButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.buttonLeft(0), row(13) - 5, 100, 20, new TranslatableText("mco.configure.world.buttons.switchminigame"), (button) -> {
         this.client.openScreen(new RealmsSelectWorldTemplateScreen(new TranslatableText("mco.template.title.minigame"), this::method_32484, RealmsServer.WorldType.MINIGAME));
      }));
      this.optionsButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.buttonLeft(0), row(13) - 5, 90, 20, new TranslatableText("mco.configure.world.buttons.options"), (button) -> {
         this.client.openScreen(new RealmsSlotOptionsScreen(this, ((RealmsWorldOptions)this.server.slots.get(this.server.activeSlot)).clone(), this.server.worldType, this.server.activeSlot));
      }));
      this.backupButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.buttonLeft(1), row(13) - 5, 90, 20, new TranslatableText("mco.configure.world.backup"), (button) -> {
         this.client.openScreen(new RealmsBackupScreen(this, this.server.clone(), this.server.activeSlot));
      }));
      this.resetWorldButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.buttonLeft(2), row(13) - 5, 90, 20, new TranslatableText("mco.configure.world.buttons.resetworld"), (button) -> {
         this.client.openScreen(new RealmsResetWorldScreen(this, this.server.clone(), () -> {
            this.client.execute(() -> {
               this.client.openScreen(this.getNewScreen());
            });
         }, () -> {
            this.client.openScreen(this.getNewScreen());
         }));
      }));
      this.addDrawableChild(new ButtonWidget(this.right_x - 80 + 8, row(13) - 5, 70, 20, ScreenTexts.BACK, (button) -> {
         this.backButtonClicked();
      }));
      this.backupButton.active = true;
      if (this.server == null) {
         this.hideMinigameButtons();
         this.hideRegularButtons();
         this.playersButton.active = false;
         this.settingsButton.active = false;
         this.subscriptionButton.active = false;
      } else {
         this.disableButtons();
         if (this.isMinigame()) {
            this.hideRegularButtons();
         } else {
            this.hideMinigameButtons();
         }
      }

   }

   private RealmsWorldSlotButton addSlotButton(int slotIndex) {
      int i = this.frame(slotIndex);
      int j = row(5) + 5;
      RealmsWorldSlotButton realmsWorldSlotButton = new RealmsWorldSlotButton(i, j, 80, 80, () -> {
         return this.server;
      }, (text) -> {
         this.toolTip = text;
      }, slotIndex, (button) -> {
         RealmsWorldSlotButton.State state = ((RealmsWorldSlotButton)button).getState();
         if (state != null) {
            switch(state.action) {
            case NOTHING:
               break;
            case JOIN:
               this.joinRealm(this.server);
               break;
            case SWITCH_SLOT:
               if (state.minigame) {
                  this.switchToMinigame();
               } else if (state.empty) {
                  this.switchToEmptySlot(slotIndex, this.server);
               } else {
                  this.switchToFullSlot(slotIndex, this.server);
               }
               break;
            default:
               throw new IllegalStateException("Unknown action " + state.action);
            }
         }

      });
      return (RealmsWorldSlotButton)this.addDrawableChild(realmsWorldSlotButton);
   }

   private int buttonLeft(int i) {
      return this.left_x + i * 95;
   }

   private int buttonCenter(int i, int total) {
      return this.width / 2 - (total * 105 - 5) / 2 + i * 105;
   }

   public void tick() {
      super.tick();
      ++this.animTick;
      --this.clicks;
      if (this.clicks < 0) {
         this.clicks = 0;
      }

      this.field_33777.forEach(RealmsWorldSlotButton::method_37007);
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.toolTip = null;
      this.renderBackground(matrices);
      drawCenteredText(matrices, this.textRenderer, WORLDS_TITLE, this.width / 2, row(4), 16777215);
      super.render(matrices, mouseX, mouseY, delta);
      if (this.server == null) {
         drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 17, 16777215);
      } else {
         String string = this.server.getName();
         int i = this.textRenderer.getWidth(string);
         int j = this.server.state == RealmsServer.State.CLOSED ? 10526880 : 8388479;
         int k = this.textRenderer.getWidth((StringVisitable)this.title);
         drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 12, 16777215);
         drawCenteredText(matrices, this.textRenderer, string, this.width / 2, 24, j);
         int l = Math.min(this.buttonCenter(2, 3) + 80 - 11, this.width / 2 + i / 2 + k / 2 + 10);
         this.drawServerStatus(matrices, l, 7, mouseX, mouseY);
         if (this.isMinigame()) {
            this.textRenderer.draw(matrices, (Text)CURRENT_MINIGAME_TEXT.shallowCopy().append(this.server.getMinigameName()), (float)(this.left_x + 80 + 20 + 10), (float)row(13), 16777215);
         }

         if (this.toolTip != null) {
            this.renderMousehoverTooltip(matrices, this.toolTip, mouseX, mouseY);
         }

      }
   }

   private int frame(int ordinal) {
      return this.left_x + (ordinal - 1) * 98;
   }

   public void removed() {
      this.client.keyboard.setRepeatEvents(false);
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
         this.backButtonClicked();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private void backButtonClicked() {
      if (this.stateChanged) {
         this.parent.removeSelection();
      }

      this.client.openScreen(this.parent);
   }

   private void fetchServerData(long worldId) {
      (new Thread(() -> {
         RealmsClient realmsClient = RealmsClient.createRealmsClient();

         try {
            RealmsServer realmsServer = realmsClient.getOwnWorld(worldId);
            this.client.execute(() -> {
               this.server = realmsServer;
               this.disableButtons();
               if (this.isMinigame()) {
                  this.addButton(this.switchMinigameButton);
               } else {
                  this.addButton(this.optionsButton);
                  this.addButton(this.backupButton);
                  this.addButton(this.resetWorldButton);
               }

            });
         } catch (RealmsServiceException var5) {
            LOGGER.error("Couldn't get own world");
            this.client.execute(() -> {
               this.client.openScreen(new RealmsGenericErrorScreen(Text.of(var5.getMessage()), this.parent));
            });
         }

      })).start();
   }

   private void disableButtons() {
      this.playersButton.active = !this.server.expired;
      this.settingsButton.active = !this.server.expired;
      this.subscriptionButton.active = true;
      this.switchMinigameButton.active = !this.server.expired;
      this.optionsButton.active = !this.server.expired;
      this.resetWorldButton.active = !this.server.expired;
   }

   private void joinRealm(RealmsServer serverData) {
      if (this.server.state == RealmsServer.State.OPEN) {
         this.parent.play(serverData, new RealmsConfigureWorldScreen(this.parent.newScreen(), this.serverId));
      } else {
         this.openTheWorld(true, new RealmsConfigureWorldScreen(this.parent.newScreen(), this.serverId));
      }

   }

   private void switchToMinigame() {
      RealmsSelectWorldTemplateScreen realmsSelectWorldTemplateScreen = new RealmsSelectWorldTemplateScreen(new TranslatableText("mco.template.title.minigame"), this::method_32484, RealmsServer.WorldType.MINIGAME);
      realmsSelectWorldTemplateScreen.setWarning(new TranslatableText("mco.minigame.world.info.line1"), new TranslatableText("mco.minigame.world.info.line2"));
      this.client.openScreen(realmsSelectWorldTemplateScreen);
   }

   private void switchToFullSlot(int selectedSlot, RealmsServer serverData) {
      Text text = new TranslatableText("mco.configure.world.slot.switch.question.line1");
      Text text2 = new TranslatableText("mco.configure.world.slot.switch.question.line2");
      this.client.openScreen(new RealmsLongConfirmationScreen((bl) -> {
         if (bl) {
            this.client.openScreen(new RealmsLongRunningMcoTaskScreen(this.parent, new SwitchSlotTask(serverData.id, selectedSlot, () -> {
               this.client.execute(() -> {
                  this.client.openScreen(this.getNewScreen());
               });
            })));
         } else {
            this.client.openScreen(this);
         }

      }, RealmsLongConfirmationScreen.Type.Info, text, text2, true));
   }

   private void switchToEmptySlot(int selectedSlot, RealmsServer serverData) {
      Text text = new TranslatableText("mco.configure.world.slot.switch.question.line1");
      Text text2 = new TranslatableText("mco.configure.world.slot.switch.question.line2");
      this.client.openScreen(new RealmsLongConfirmationScreen((bl) -> {
         if (bl) {
            RealmsResetWorldScreen realmsResetWorldScreen = new RealmsResetWorldScreen(this, serverData, new TranslatableText("mco.configure.world.switch.slot"), new TranslatableText("mco.configure.world.switch.slot.subtitle"), 10526880, ScreenTexts.CANCEL, () -> {
               this.client.execute(() -> {
                  this.client.openScreen(this.getNewScreen());
               });
            }, () -> {
               this.client.openScreen(this.getNewScreen());
            });
            realmsResetWorldScreen.setSlot(selectedSlot);
            realmsResetWorldScreen.setResetTitle(new TranslatableText("mco.create.world.reset.title"));
            this.client.openScreen(realmsResetWorldScreen);
         } else {
            this.client.openScreen(this);
         }

      }, RealmsLongConfirmationScreen.Type.Info, text, text2, true));
   }

   protected void renderMousehoverTooltip(MatrixStack matrices, @Nullable Text text, int i, int j) {
      int k = i + 12;
      int l = j - 12;
      int m = this.textRenderer.getWidth((StringVisitable)text);
      if (k + m + 3 > this.right_x) {
         k = k - m - 20;
      }

      this.fillGradient(matrices, k - 3, l - 3, k + m + 3, l + 8 + 3, -1073741824, -1073741824);
      this.textRenderer.drawWithShadow(matrices, text, (float)k, (float)l, 16777215);
   }

   private void drawServerStatus(MatrixStack matrices, int i, int j, int k, int l) {
      if (this.server.expired) {
         this.drawExpired(matrices, i, j, k, l);
      } else if (this.server.state == RealmsServer.State.CLOSED) {
         this.drawClosed(matrices, i, j, k, l);
      } else if (this.server.state == RealmsServer.State.OPEN) {
         if (this.server.daysLeft < 7) {
            this.drawExpiring(matrices, i, j, k, l, this.server.daysLeft);
         } else {
            this.drawOpen(matrices, i, j, k, l);
         }
      }

   }

   private void drawExpired(MatrixStack matrices, int i, int j, int k, int l) {
      RenderSystem.setShaderTexture(0, EXPIRED_ICON);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      DrawableHelper.drawTexture(matrices, i, j, 0.0F, 0.0F, 10, 28, 10, 28);
      if (k >= i && k <= i + 9 && l >= j && l <= j + 27) {
         this.toolTip = EXPIRED_TEXT;
      }

   }

   private void drawExpiring(MatrixStack matrices, int i, int j, int k, int l, int m) {
      RenderSystem.setShaderTexture(0, EXPIRES_SOON_ICON);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      if (this.animTick % 20 < 10) {
         DrawableHelper.drawTexture(matrices, i, j, 0.0F, 0.0F, 10, 28, 20, 28);
      } else {
         DrawableHelper.drawTexture(matrices, i, j, 10.0F, 0.0F, 10, 28, 20, 28);
      }

      if (k >= i && k <= i + 9 && l >= j && l <= j + 27) {
         if (m <= 0) {
            this.toolTip = EXPIRES_SOON_TEXT;
         } else if (m == 1) {
            this.toolTip = EXPIRES_IN_A_DAY_TEXT;
         } else {
            this.toolTip = new TranslatableText("mco.selectServer.expires.days", new Object[]{m});
         }
      }

   }

   private void drawOpen(MatrixStack matrices, int i, int j, int k, int l) {
      RenderSystem.setShaderTexture(0, ON_ICON);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      DrawableHelper.drawTexture(matrices, i, j, 0.0F, 0.0F, 10, 28, 10, 28);
      if (k >= i && k <= i + 9 && l >= j && l <= j + 27) {
         this.toolTip = OPEN_TEXT;
      }

   }

   private void drawClosed(MatrixStack matrices, int i, int j, int k, int l) {
      RenderSystem.setShaderTexture(0, OFF_ICON);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      DrawableHelper.drawTexture(matrices, i, j, 0.0F, 0.0F, 10, 28, 10, 28);
      if (k >= i && k <= i + 9 && l >= j && l <= j + 27) {
         this.toolTip = CLOSED_TEXT;
      }

   }

   private boolean isMinigame() {
      return this.server != null && this.server.worldType == RealmsServer.WorldType.MINIGAME;
   }

   private void hideRegularButtons() {
      this.removeButton(this.optionsButton);
      this.removeButton(this.backupButton);
      this.removeButton(this.resetWorldButton);
   }

   private void removeButton(ButtonWidget button) {
      button.visible = false;
      this.remove(button);
   }

   private void addButton(ButtonWidget button) {
      button.visible = true;
      this.addDrawableChild(button);
   }

   private void hideMinigameButtons() {
      this.removeButton(this.switchMinigameButton);
   }

   public void saveSlotSettings(RealmsWorldOptions options) {
      RealmsWorldOptions realmsWorldOptions = (RealmsWorldOptions)this.server.slots.get(this.server.activeSlot);
      options.templateId = realmsWorldOptions.templateId;
      options.templateImage = realmsWorldOptions.templateImage;
      RealmsClient realmsClient = RealmsClient.createRealmsClient();

      try {
         realmsClient.updateSlot(this.server.id, this.server.activeSlot, options);
         this.server.slots.put(this.server.activeSlot, options);
      } catch (RealmsServiceException var5) {
         LOGGER.error("Couldn't save slot settings");
         this.client.openScreen(new RealmsGenericErrorScreen(var5, this));
         return;
      }

      this.client.openScreen(this);
   }

   public void saveSettings(String name, String desc) {
      String string = desc.trim().isEmpty() ? null : desc;
      RealmsClient realmsClient = RealmsClient.createRealmsClient();

      try {
         realmsClient.update(this.server.id, name, string);
         this.server.setName(name);
         this.server.setDescription(string);
      } catch (RealmsServiceException var6) {
         LOGGER.error("Couldn't save settings");
         this.client.openScreen(new RealmsGenericErrorScreen(var6, this));
         return;
      }

      this.client.openScreen(this);
   }

   public void openTheWorld(boolean join, Screen screen) {
      this.client.openScreen(new RealmsLongRunningMcoTaskScreen(screen, new OpenServerTask(this.server, this, this.parent, join, this.client)));
   }

   public void closeTheWorld(Screen screen) {
      this.client.openScreen(new RealmsLongRunningMcoTaskScreen(screen, new CloseServerTask(this.server, this)));
   }

   public void stateChanged() {
      this.stateChanged = true;
   }

   private void method_32484(@Nullable WorldTemplate worldTemplate) {
      if (worldTemplate != null && WorldTemplate.WorldTemplateType.MINIGAME == worldTemplate.type) {
         this.client.openScreen(new RealmsLongRunningMcoTaskScreen(this.parent, new SwitchMinigameTask(this.server.id, worldTemplate, this.getNewScreen())));
      } else {
         this.client.openScreen(this);
      }

   }

   public RealmsConfigureWorldScreen getNewScreen() {
      return new RealmsConfigureWorldScreen(this.parent, this.serverId);
   }
}
