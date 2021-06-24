package net.minecraft.client.gui.screen.ingame;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateBeaconC2SPacket;
import net.minecraft.screen.BeaconScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerListener;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class BeaconScreen extends HandledScreen<BeaconScreenHandler> {
   static final Identifier TEXTURE = new Identifier("textures/gui/container/beacon.png");
   private static final Text PRIMARY_POWER_TEXT = new TranslatableText("block.minecraft.beacon.primary");
   private static final Text SECONDARY_POWER_TEXT = new TranslatableText("block.minecraft.beacon.secondary");
   private final List<BeaconScreen.class_6392> field_33832 = Lists.newArrayList();
   @Nullable
   StatusEffect primaryEffect;
   @Nullable
   StatusEffect secondaryEffect;

   public BeaconScreen(final BeaconScreenHandler handler, PlayerInventory inventory, Text title) {
      super(handler, inventory, title);
      this.backgroundWidth = 230;
      this.backgroundHeight = 219;
      handler.addListener(new ScreenHandlerListener() {
         public void onSlotUpdate(ScreenHandler handlerx, int slotId, ItemStack stack) {
         }

         public void onPropertyUpdate(ScreenHandler handlerx, int property, int value) {
            BeaconScreen.this.primaryEffect = handler.getPrimaryEffect();
            BeaconScreen.this.secondaryEffect = handler.getSecondaryEffect();
         }
      });
   }

   private <T extends ClickableWidget & BeaconScreen.class_6392> void method_37076(T clickableWidget) {
      this.addDrawableChild(clickableWidget);
      this.field_33832.add((BeaconScreen.class_6392)clickableWidget);
   }

   protected void init() {
      super.init();
      this.field_33832.clear();
      this.method_37076(new BeaconScreen.DoneButtonWidget(this.x + 164, this.y + 107));
      this.method_37076(new BeaconScreen.CancelButtonWidget(this.x + 190, this.y + 107));

      int n;
      int o;
      int p;
      StatusEffect statusEffect2;
      BeaconScreen.EffectButtonWidget effectButtonWidget2;
      for(int i = 0; i <= 2; ++i) {
         n = BeaconBlockEntity.EFFECTS_BY_LEVEL[i].length;
         o = n * 22 + (n - 1) * 2;

         for(p = 0; p < n; ++p) {
            statusEffect2 = BeaconBlockEntity.EFFECTS_BY_LEVEL[i][p];
            effectButtonWidget2 = new BeaconScreen.EffectButtonWidget(this.x + 76 + p * 24 - o / 2, this.y + 22 + i * 25, statusEffect2, true, i);
            effectButtonWidget2.active = false;
            this.method_37076(effectButtonWidget2);
         }
      }

      int m = true;
      n = BeaconBlockEntity.EFFECTS_BY_LEVEL[3].length + 1;
      o = n * 22 + (n - 1) * 2;

      for(p = 0; p < n - 1; ++p) {
         statusEffect2 = BeaconBlockEntity.EFFECTS_BY_LEVEL[3][p];
         effectButtonWidget2 = new BeaconScreen.EffectButtonWidget(this.x + 167 + p * 24 - o / 2, this.y + 47, statusEffect2, false, 3);
         effectButtonWidget2.active = false;
         this.method_37076(effectButtonWidget2);
      }

      BeaconScreen.EffectButtonWidget effectButtonWidget3 = new BeaconScreen.LevelTwoEffectButtonWidget(this.x + 167 + (n - 1) * 24 - o / 2, this.y + 47, BeaconBlockEntity.EFFECTS_BY_LEVEL[0][0]);
      effectButtonWidget3.visible = false;
      this.method_37076(effectButtonWidget3);
   }

   public void tick() {
      super.tick();
      this.method_37078();
   }

   void method_37078() {
      int i = ((BeaconScreenHandler)this.handler).getProperties();
      this.field_33832.forEach((arg) -> {
         arg.method_37080(i);
      });
   }

   protected void drawForeground(MatrixStack matrices, int mouseX, int mouseY) {
      drawCenteredText(matrices, this.textRenderer, PRIMARY_POWER_TEXT, 62, 10, 14737632);
      drawCenteredText(matrices, this.textRenderer, SECONDARY_POWER_TEXT, 169, 10, 14737632);
      Iterator var4 = this.field_33832.iterator();

      while(var4.hasNext()) {
         BeaconScreen.class_6392 lv = (BeaconScreen.class_6392)var4.next();
         if (lv.method_37079()) {
            lv.renderToolTip(matrices, mouseX - this.x, mouseY - this.y);
            break;
         }
      }

   }

   protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.setShaderTexture(0, TEXTURE);
      int i = (this.width - this.backgroundWidth) / 2;
      int j = (this.height - this.backgroundHeight) / 2;
      this.drawTexture(matrices, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight);
      this.itemRenderer.zOffset = 100.0F;
      this.itemRenderer.renderInGuiWithOverrides(new ItemStack(Items.NETHERITE_INGOT), i + 20, j + 109);
      this.itemRenderer.renderInGuiWithOverrides(new ItemStack(Items.EMERALD), i + 41, j + 109);
      this.itemRenderer.renderInGuiWithOverrides(new ItemStack(Items.DIAMOND), i + 41 + 22, j + 109);
      this.itemRenderer.renderInGuiWithOverrides(new ItemStack(Items.GOLD_INGOT), i + 42 + 44, j + 109);
      this.itemRenderer.renderInGuiWithOverrides(new ItemStack(Items.IRON_INGOT), i + 42 + 66, j + 109);
      this.itemRenderer.zOffset = 0.0F;
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.renderBackground(matrices);
      super.render(matrices, mouseX, mouseY, delta);
      this.drawMouseoverTooltip(matrices, mouseX, mouseY);
   }

   @Environment(EnvType.CLIENT)
   private interface class_6392 {
      boolean method_37079();

      void renderToolTip(MatrixStack matrices, int mouseX, int mouseY);

      void method_37080(int i);
   }

   @Environment(EnvType.CLIENT)
   private class DoneButtonWidget extends BeaconScreen.IconButtonWidget {
      public DoneButtonWidget(int x, int y) {
         super(x, y, 90, 220, ScreenTexts.DONE);
      }

      public void onPress() {
         BeaconScreen.this.client.getNetworkHandler().sendPacket(new UpdateBeaconC2SPacket(StatusEffect.getRawId(BeaconScreen.this.primaryEffect), StatusEffect.getRawId(BeaconScreen.this.secondaryEffect)));
         BeaconScreen.this.client.player.closeHandledScreen();
      }

      public void method_37080(int i) {
         this.active = ((BeaconScreenHandler)BeaconScreen.this.handler).hasPayment() && BeaconScreen.this.primaryEffect != null;
      }
   }

   @Environment(EnvType.CLIENT)
   private class CancelButtonWidget extends BeaconScreen.IconButtonWidget {
      public CancelButtonWidget(int x, int y) {
         super(x, y, 112, 220, ScreenTexts.CANCEL);
      }

      public void onPress() {
         BeaconScreen.this.client.player.closeHandledScreen();
      }

      public void method_37080(int i) {
      }
   }

   @Environment(EnvType.CLIENT)
   private class EffectButtonWidget extends BeaconScreen.BaseButtonWidget {
      private final boolean primary;
      protected final int field_33833;
      private StatusEffect effect;
      private Sprite sprite;
      private Text tooltip;

      public EffectButtonWidget(int x, int y, StatusEffect statusEffect, boolean primary, int i) {
         super(x, y);
         this.primary = primary;
         this.field_33833 = i;
         this.init(statusEffect);
      }

      protected void init(StatusEffect statusEffect) {
         this.effect = statusEffect;
         this.sprite = MinecraftClient.getInstance().getStatusEffectSpriteManager().getSprite(statusEffect);
         this.tooltip = this.getEffectName(statusEffect);
      }

      protected MutableText getEffectName(StatusEffect statusEffect) {
         return new TranslatableText(statusEffect.getTranslationKey());
      }

      public void onPress() {
         if (!this.isDisabled()) {
            if (this.primary) {
               BeaconScreen.this.primaryEffect = this.effect;
            } else {
               BeaconScreen.this.secondaryEffect = this.effect;
            }

            BeaconScreen.this.method_37078();
         }
      }

      public void renderToolTip(MatrixStack matrices, int mouseX, int mouseY) {
         BeaconScreen.this.renderTooltip(matrices, this.tooltip, mouseX, mouseY);
      }

      protected void renderExtra(MatrixStack matrices) {
         RenderSystem.setShaderTexture(0, this.sprite.getAtlas().getId());
         drawSprite(matrices, this.x + 2, this.y + 2, this.getZOffset(), 18, 18, this.sprite);
      }

      public void method_37080(int i) {
         this.active = this.field_33833 < i;
         this.setDisabled(this.effect == (this.primary ? BeaconScreen.this.primaryEffect : BeaconScreen.this.secondaryEffect));
      }

      protected MutableText getNarrationMessage() {
         return this.getEffectName(this.effect);
      }
   }

   @Environment(EnvType.CLIENT)
   private class LevelTwoEffectButtonWidget extends BeaconScreen.EffectButtonWidget {
      public LevelTwoEffectButtonWidget(int x, int y, StatusEffect statusEffect) {
         super(x, y, statusEffect, false, 3);
      }

      protected MutableText getEffectName(StatusEffect statusEffect) {
         return (new TranslatableText(statusEffect.getTranslationKey())).append(" II");
      }

      public void method_37080(int i) {
         if (BeaconScreen.this.primaryEffect != null) {
            this.visible = true;
            this.init(BeaconScreen.this.primaryEffect);
            super.method_37080(i);
         } else {
            this.visible = false;
         }

      }
   }

   @Environment(EnvType.CLIENT)
   private abstract class IconButtonWidget extends BeaconScreen.BaseButtonWidget {
      private final int u;
      private final int v;

      protected IconButtonWidget(int i, int j, int k, int l, Text text) {
         super(i, j, text);
         this.u = k;
         this.v = l;
      }

      protected void renderExtra(MatrixStack matrices) {
         this.drawTexture(matrices, this.x + 2, this.y + 2, this.u, this.v, 18, 18);
      }

      public void renderToolTip(MatrixStack matrices, int mouseX, int mouseY) {
         BeaconScreen.this.renderTooltip(matrices, BeaconScreen.this.title, mouseX, mouseY);
      }
   }

   @Environment(EnvType.CLIENT)
   private abstract static class BaseButtonWidget extends PressableWidget implements BeaconScreen.class_6392 {
      private boolean disabled;

      protected BaseButtonWidget(int x, int y) {
         super(x, y, 22, 22, LiteralText.EMPTY);
      }

      protected BaseButtonWidget(int i, int j, Text text) {
         super(i, j, 22, 22, text);
      }

      public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, BeaconScreen.TEXTURE);
         RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
         int i = true;
         int j = 0;
         if (!this.active) {
            j += this.width * 2;
         } else if (this.disabled) {
            j += this.width * 1;
         } else if (this.isHovered()) {
            j += this.width * 3;
         }

         this.drawTexture(matrices, this.x, this.y, j, 219, this.width, this.height);
         this.renderExtra(matrices);
      }

      protected abstract void renderExtra(MatrixStack matrices);

      public boolean isDisabled() {
         return this.disabled;
      }

      public void setDisabled(boolean disabled) {
         this.disabled = disabled;
      }

      public boolean method_37079() {
         return this.hovered;
      }

      public void appendNarrations(NarrationMessageBuilder builder) {
         this.method_37021(builder);
      }
   }
}
