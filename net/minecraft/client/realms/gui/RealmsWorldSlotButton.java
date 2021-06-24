package net.minecraft.client.realms.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.RealmsWorldOptions;
import net.minecraft.client.realms.util.RealmsTextureManager;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class RealmsWorldSlotButton extends ButtonWidget {
   public static final Identifier SLOT_FRAME = new Identifier("realms", "textures/gui/realms/slot_frame.png");
   public static final Identifier EMPTY_FRAME = new Identifier("realms", "textures/gui/realms/empty_frame.png");
   public static final Identifier PANORAMA_0 = new Identifier("minecraft", "textures/gui/title/background/panorama_0.png");
   public static final Identifier PANORAMA_2 = new Identifier("minecraft", "textures/gui/title/background/panorama_2.png");
   public static final Identifier PANORAMA_3 = new Identifier("minecraft", "textures/gui/title/background/panorama_3.png");
   private static final Text ACTIVE_TOOLTIP = new TranslatableText("mco.configure.world.slot.tooltip.active");
   private static final Text MINIGAME_TOOLTIP = new TranslatableText("mco.configure.world.slot.tooltip.minigame");
   private static final Text TOOLTIP = new TranslatableText("mco.configure.world.slot.tooltip");
   private final Supplier<RealmsServer> serverDataProvider;
   private final Consumer<Text> toolTipSetter;
   private final int slotIndex;
   private int animTick;
   @Nullable
   private RealmsWorldSlotButton.State state;

   public RealmsWorldSlotButton(int x, int y, int width, int height, Supplier<RealmsServer> serverDataProvider, Consumer<Text> toolTipSetter, int id, ButtonWidget.PressAction action) {
      super(x, y, width, height, LiteralText.EMPTY, action);
      this.serverDataProvider = serverDataProvider;
      this.slotIndex = id;
      this.toolTipSetter = toolTipSetter;
   }

   @Nullable
   public RealmsWorldSlotButton.State getState() {
      return this.state;
   }

   public void method_37007() {
      ++this.animTick;
      RealmsServer realmsServer = (RealmsServer)this.serverDataProvider.get();
      if (realmsServer != null) {
         RealmsWorldOptions realmsWorldOptions = (RealmsWorldOptions)realmsServer.slots.get(this.slotIndex);
         boolean bl = this.slotIndex == 4;
         boolean bl4;
         String string3;
         long m;
         String string4;
         boolean bl5;
         if (bl) {
            bl4 = realmsServer.worldType == RealmsServer.WorldType.MINIGAME;
            string3 = "Minigame";
            m = (long)realmsServer.minigameId;
            string4 = realmsServer.minigameImage;
            bl5 = realmsServer.minigameId == -1;
         } else {
            bl4 = realmsServer.activeSlot == this.slotIndex && realmsServer.worldType != RealmsServer.WorldType.MINIGAME;
            string3 = realmsWorldOptions.getSlotName(this.slotIndex);
            m = realmsWorldOptions.templateId;
            string4 = realmsWorldOptions.templateImage;
            bl5 = realmsWorldOptions.empty;
         }

         RealmsWorldSlotButton.Action action = getAction(realmsServer, bl4, bl);
         Pair<Text, Text> pair = this.getActionPromptAndMessage(realmsServer, string3, bl5, bl, action);
         this.state = new RealmsWorldSlotButton.State(bl4, string3, m, string4, bl5, bl, action, (Text)pair.getFirst());
         this.setMessage((Text)pair.getSecond());
      }
   }

   private static RealmsWorldSlotButton.Action getAction(RealmsServer server, boolean active, boolean minigame) {
      if (active) {
         if (!server.expired && server.state != RealmsServer.State.UNINITIALIZED) {
            return RealmsWorldSlotButton.Action.JOIN;
         }
      } else {
         if (!minigame) {
            return RealmsWorldSlotButton.Action.SWITCH_SLOT;
         }

         if (!server.expired) {
            return RealmsWorldSlotButton.Action.SWITCH_SLOT;
         }
      }

      return RealmsWorldSlotButton.Action.NOTHING;
   }

   private Pair<Text, Text> getActionPromptAndMessage(RealmsServer server, String text, boolean empty, boolean minigame, RealmsWorldSlotButton.Action action) {
      if (action == RealmsWorldSlotButton.Action.NOTHING) {
         return Pair.of((Object)null, new LiteralText(text));
      } else {
         Object text4;
         if (minigame) {
            if (empty) {
               text4 = LiteralText.EMPTY;
            } else {
               text4 = (new LiteralText(" ")).append(text).append(" ").append(server.minigameName);
            }
         } else {
            text4 = (new LiteralText(" ")).append(text);
         }

         Text text6;
         if (action == RealmsWorldSlotButton.Action.JOIN) {
            text6 = ACTIVE_TOOLTIP;
         } else {
            text6 = minigame ? MINIGAME_TOOLTIP : TOOLTIP;
         }

         Text text7 = text6.shallowCopy().append((Text)text4);
         return Pair.of(text6, text7);
      }
   }

   public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      if (this.state != null) {
         this.drawSlotFrame(matrices, this.x, this.y, mouseX, mouseY, this.state.isCurrentlyActiveSlot, this.state.slotName, this.slotIndex, this.state.imageId, this.state.image, this.state.empty, this.state.minigame, this.state.action, this.state.actionPrompt);
      }
   }

   private void drawSlotFrame(MatrixStack matrices, int x, int y, int mouseX, int mouseY, boolean active, String slotName, int slotIndex, long imageId, @Nullable String image, boolean empty, boolean minigame, RealmsWorldSlotButton.Action action, @Nullable Text actionPrompt) {
      boolean bl = this.isHovered();
      if (this.isMouseOver((double)mouseX, (double)mouseY) && actionPrompt != null) {
         this.toolTipSetter.accept(actionPrompt);
      }

      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      TextureManager textureManager = minecraftClient.getTextureManager();
      if (minigame) {
         RealmsTextureManager.bindWorldTemplate(String.valueOf(imageId), image);
      } else if (empty) {
         RenderSystem.setShaderTexture(0, EMPTY_FRAME);
      } else if (image != null && imageId != -1L) {
         RealmsTextureManager.bindWorldTemplate(String.valueOf(imageId), image);
      } else if (slotIndex == 1) {
         RenderSystem.setShaderTexture(0, PANORAMA_0);
      } else if (slotIndex == 2) {
         RenderSystem.setShaderTexture(0, PANORAMA_2);
      } else if (slotIndex == 3) {
         RenderSystem.setShaderTexture(0, PANORAMA_3);
      }

      if (active) {
         float f = 0.85F + 0.15F * MathHelper.cos((float)this.animTick * 0.2F);
         RenderSystem.setShaderColor(f, f, f, 1.0F);
      } else {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      }

      drawTexture(matrices, x + 3, y + 3, 0.0F, 0.0F, 74, 74, 74, 74);
      RenderSystem.setShaderTexture(0, SLOT_FRAME);
      boolean bl2 = bl && action != RealmsWorldSlotButton.Action.NOTHING;
      if (bl2) {
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      } else if (active) {
         RenderSystem.setShaderColor(0.8F, 0.8F, 0.8F, 1.0F);
      } else {
         RenderSystem.setShaderColor(0.56F, 0.56F, 0.56F, 1.0F);
      }

      drawTexture(matrices, x, y, 0.0F, 0.0F, 80, 80, 80, 80);
      drawCenteredText(matrices, minecraftClient.textRenderer, slotName, x + 40, y + 66, 16777215);
   }

   @Environment(EnvType.CLIENT)
   public static class State {
      final boolean isCurrentlyActiveSlot;
      final String slotName;
      final long imageId;
      final String image;
      public final boolean empty;
      public final boolean minigame;
      public final RealmsWorldSlotButton.Action action;
      @Nullable
      final Text actionPrompt;

      State(boolean isCurrentlyActiveSlot, String slotName, long imageId, @Nullable String image, boolean empty, boolean minigame, RealmsWorldSlotButton.Action action, @Nullable Text actionPrompt) {
         this.isCurrentlyActiveSlot = isCurrentlyActiveSlot;
         this.slotName = slotName;
         this.imageId = imageId;
         this.image = image;
         this.empty = empty;
         this.minigame = minigame;
         this.action = action;
         this.actionPrompt = actionPrompt;
      }
   }

   @Environment(EnvType.CLIENT)
   public static enum Action {
      NOTHING,
      SWITCH_SLOT,
      JOIN;
   }
}
