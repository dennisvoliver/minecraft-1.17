package net.minecraft.client.realms.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.RealmsClient;
import net.minecraft.client.realms.RealmsObjectSelectionList;
import net.minecraft.client.realms.dto.PendingInvite;
import net.minecraft.client.realms.exception.RealmsServiceException;
import net.minecraft.client.realms.util.RealmsTextureManager;
import net.minecraft.client.realms.util.RealmsUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class RealmsPendingInvitesScreen extends RealmsScreen {
   static final Logger LOGGER = LogManager.getLogger();
   static final Identifier ACCEPT_ICON = new Identifier("realms", "textures/gui/realms/accept_icon.png");
   static final Identifier REJECT_ICON = new Identifier("realms", "textures/gui/realms/reject_icon.png");
   private static final Text NO_PENDING_TEXT = new TranslatableText("mco.invites.nopending");
   static final Text ACCEPT_TEXT = new TranslatableText("mco.invites.button.accept");
   static final Text REJECT_TEXT = new TranslatableText("mco.invites.button.reject");
   private final Screen parent;
   @Nullable
   Text toolTip;
   boolean loaded;
   RealmsPendingInvitesScreen.PendingInvitationSelectionList pendingInvitationSelectionList;
   int selectedInvite = -1;
   private ButtonWidget acceptButton;
   private ButtonWidget rejectButton;

   public RealmsPendingInvitesScreen(Screen parent) {
      super(new TranslatableText("mco.invites.title"));
      this.parent = parent;
   }

   public void init() {
      this.client.keyboard.setRepeatEvents(true);
      this.pendingInvitationSelectionList = new RealmsPendingInvitesScreen.PendingInvitationSelectionList();
      (new Thread("Realms-pending-invitations-fetcher") {
         public void run() {
            RealmsClient realmsClient = RealmsClient.createRealmsClient();

            try {
               List<PendingInvite> list = realmsClient.pendingInvites().pendingInvites;
               List<RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry> list2 = (List)list.stream().map((pendingInvite) -> {
                  return RealmsPendingInvitesScreen.this.new PendingInvitationSelectionListEntry(pendingInvite);
               }).collect(Collectors.toList());
               RealmsPendingInvitesScreen.this.client.execute(() -> {
                  RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.replaceEntries(list2);
               });
            } catch (RealmsServiceException var7) {
               RealmsPendingInvitesScreen.LOGGER.error("Couldn't list invites");
            } finally {
               RealmsPendingInvitesScreen.this.loaded = true;
            }

         }
      }).start();
      this.addSelectableChild(this.pendingInvitationSelectionList);
      this.acceptButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.width / 2 - 174, this.height - 32, 100, 20, new TranslatableText("mco.invites.button.accept"), (button) -> {
         this.accept(this.selectedInvite);
         this.selectedInvite = -1;
         this.updateButtonStates();
      }));
      this.addDrawableChild(new ButtonWidget(this.width / 2 - 50, this.height - 32, 100, 20, ScreenTexts.DONE, (button) -> {
         this.client.openScreen(new RealmsMainScreen(this.parent));
      }));
      this.rejectButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.width / 2 + 74, this.height - 32, 100, 20, new TranslatableText("mco.invites.button.reject"), (button) -> {
         this.reject(this.selectedInvite);
         this.selectedInvite = -1;
         this.updateButtonStates();
      }));
      this.updateButtonStates();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
         this.client.openScreen(new RealmsMainScreen(this.parent));
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   void updateList(int slot) {
      this.pendingInvitationSelectionList.removeAtIndex(slot);
   }

   void reject(final int slot) {
      if (slot < this.pendingInvitationSelectionList.getEntryCount()) {
         (new Thread("Realms-reject-invitation") {
            public void run() {
               try {
                  RealmsClient realmsClient = RealmsClient.createRealmsClient();
                  realmsClient.rejectInvitation(((RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry)RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.children().get(slot)).mPendingInvite.invitationId);
                  RealmsPendingInvitesScreen.this.client.execute(() -> {
                     RealmsPendingInvitesScreen.this.updateList(slot);
                  });
               } catch (RealmsServiceException var2) {
                  RealmsPendingInvitesScreen.LOGGER.error("Couldn't reject invite");
               }

            }
         }).start();
      }

   }

   void accept(final int slot) {
      if (slot < this.pendingInvitationSelectionList.getEntryCount()) {
         (new Thread("Realms-accept-invitation") {
            public void run() {
               try {
                  RealmsClient realmsClient = RealmsClient.createRealmsClient();
                  realmsClient.acceptInvitation(((RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry)RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.children().get(slot)).mPendingInvite.invitationId);
                  RealmsPendingInvitesScreen.this.client.execute(() -> {
                     RealmsPendingInvitesScreen.this.updateList(slot);
                  });
               } catch (RealmsServiceException var2) {
                  RealmsPendingInvitesScreen.LOGGER.error("Couldn't accept invite");
               }

            }
         }).start();
      }

   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.toolTip = null;
      this.renderBackground(matrices);
      this.pendingInvitationSelectionList.render(matrices, mouseX, mouseY, delta);
      drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 12, 16777215);
      if (this.toolTip != null) {
         this.renderMousehoverTooltip(matrices, this.toolTip, mouseX, mouseY);
      }

      if (this.pendingInvitationSelectionList.getEntryCount() == 0 && this.loaded) {
         drawCenteredText(matrices, this.textRenderer, NO_PENDING_TEXT, this.width / 2, this.height / 2 - 20, 16777215);
      }

      super.render(matrices, mouseX, mouseY, delta);
   }

   protected void renderMousehoverTooltip(MatrixStack matrices, @Nullable Text text, int i, int j) {
      if (text != null) {
         int k = i + 12;
         int l = j - 12;
         int m = this.textRenderer.getWidth((StringVisitable)text);
         this.fillGradient(matrices, k - 3, l - 3, k + m + 3, l + 8 + 3, -1073741824, -1073741824);
         this.textRenderer.drawWithShadow(matrices, text, (float)k, (float)l, 16777215);
      }
   }

   void updateButtonStates() {
      this.acceptButton.visible = this.shouldAcceptAndRejectButtonBeVisible(this.selectedInvite);
      this.rejectButton.visible = this.shouldAcceptAndRejectButtonBeVisible(this.selectedInvite);
   }

   private boolean shouldAcceptAndRejectButtonBeVisible(int invite) {
      return invite != -1;
   }

   @Environment(EnvType.CLIENT)
   private class PendingInvitationSelectionList extends RealmsObjectSelectionList<RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry> {
      public PendingInvitationSelectionList() {
         super(RealmsPendingInvitesScreen.this.width, RealmsPendingInvitesScreen.this.height, 32, RealmsPendingInvitesScreen.this.height - 40, 36);
      }

      public void removeAtIndex(int index) {
         this.remove(index);
      }

      public int getMaxPosition() {
         return this.getEntryCount() * 36;
      }

      public int getRowWidth() {
         return 260;
      }

      public boolean isFocused() {
         return RealmsPendingInvitesScreen.this.getFocused() == this;
      }

      public void renderBackground(MatrixStack matrices) {
         RealmsPendingInvitesScreen.this.renderBackground(matrices);
      }

      public void setSelected(int index) {
         super.setSelected(index);
         this.selectInviteListItem(index);
      }

      public void selectInviteListItem(int item) {
         RealmsPendingInvitesScreen.this.selectedInvite = item;
         RealmsPendingInvitesScreen.this.updateButtonStates();
      }

      public void setSelected(@Nullable RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry pendingInvitationSelectionListEntry) {
         super.setSelected(pendingInvitationSelectionListEntry);
         RealmsPendingInvitesScreen.this.selectedInvite = this.children().indexOf(pendingInvitationSelectionListEntry);
         RealmsPendingInvitesScreen.this.updateButtonStates();
      }
   }

   @Environment(EnvType.CLIENT)
   class PendingInvitationSelectionListEntry extends AlwaysSelectedEntryListWidget.Entry<RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry> {
      private static final int field_32123 = 38;
      final PendingInvite mPendingInvite;
      private final List<RealmsAcceptRejectButton> buttons;

      PendingInvitationSelectionListEntry(PendingInvite pendingInvite) {
         this.mPendingInvite = pendingInvite;
         this.buttons = Arrays.asList(new RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry.AcceptButton(), new RealmsPendingInvitesScreen.PendingInvitationSelectionListEntry.RejectButton());
      }

      public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
         this.renderPendingInvitationItem(matrices, this.mPendingInvite, x, y, mouseX, mouseY);
      }

      public boolean mouseClicked(double mouseX, double mouseY, int button) {
         RealmsAcceptRejectButton.handleClick(RealmsPendingInvitesScreen.this.pendingInvitationSelectionList, this, this.buttons, button, mouseX, mouseY);
         return true;
      }

      private void renderPendingInvitationItem(MatrixStack matrices, PendingInvite pendingInvite, int i, int j, int k, int l) {
         RealmsPendingInvitesScreen.this.textRenderer.draw(matrices, pendingInvite.worldName, (float)(i + 38), (float)(j + 1), 16777215);
         RealmsPendingInvitesScreen.this.textRenderer.draw(matrices, pendingInvite.worldOwnerName, (float)(i + 38), (float)(j + 12), 7105644);
         RealmsPendingInvitesScreen.this.textRenderer.draw(matrices, RealmsUtil.convertToAgePresentation(pendingInvite.date), (float)(i + 38), (float)(j + 24), 7105644);
         RealmsAcceptRejectButton.render(matrices, this.buttons, RealmsPendingInvitesScreen.this.pendingInvitationSelectionList, i, j, k, l);
         RealmsTextureManager.withBoundFace(pendingInvite.worldOwnerUuid, () -> {
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            DrawableHelper.drawTexture(matrices, i, j, 32, 32, 8.0F, 8.0F, 8, 8, 64, 64);
            DrawableHelper.drawTexture(matrices, i, j, 32, 32, 40.0F, 8.0F, 8, 8, 64, 64);
         });
      }

      public Text method_37006() {
         Text text = ScreenTexts.joinLines(new LiteralText(this.mPendingInvite.worldName), new LiteralText(this.mPendingInvite.worldOwnerName), new LiteralText(RealmsUtil.convertToAgePresentation(this.mPendingInvite.date)));
         return new TranslatableText("narrator.select", new Object[]{text});
      }

      @Environment(EnvType.CLIENT)
      class AcceptButton extends RealmsAcceptRejectButton {
         AcceptButton() {
            super(15, 15, 215, 5);
         }

         protected void render(MatrixStack matrices, int x, int y, boolean bl) {
            RenderSystem.setShaderTexture(0, RealmsPendingInvitesScreen.ACCEPT_ICON);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            float f = bl ? 19.0F : 0.0F;
            DrawableHelper.drawTexture(matrices, x, y, f, 0.0F, 18, 18, 37, 18);
            if (bl) {
               RealmsPendingInvitesScreen.this.toolTip = RealmsPendingInvitesScreen.ACCEPT_TEXT;
            }

         }

         public void handleClick(int index) {
            RealmsPendingInvitesScreen.this.accept(index);
         }
      }

      @Environment(EnvType.CLIENT)
      class RejectButton extends RealmsAcceptRejectButton {
         RejectButton() {
            super(15, 15, 235, 5);
         }

         protected void render(MatrixStack matrices, int x, int y, boolean bl) {
            RenderSystem.setShaderTexture(0, RealmsPendingInvitesScreen.REJECT_ICON);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            float f = bl ? 19.0F : 0.0F;
            DrawableHelper.drawTexture(matrices, x, y, f, 0.0F, 18, 18, 37, 18);
            if (bl) {
               RealmsPendingInvitesScreen.this.toolTip = RealmsPendingInvitesScreen.REJECT_TEXT;
            }

         }

         public void handleClick(int index) {
            RealmsPendingInvitesScreen.this.reject(index);
         }
      }
   }
}
