package net.minecraft.client.gui.screen;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.font.MultilineText;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class ConfirmScreen extends Screen {
   private static final int field_33535 = 90;
   private final Text message;
   private MultilineText messageSplit;
   protected Text yesTranslated;
   protected Text noTranslated;
   private int buttonEnableTimer;
   protected final BooleanConsumer callback;
   private final List<ButtonWidget> field_33808;

   public ConfirmScreen(BooleanConsumer callback, Text title, Text message) {
      this(callback, title, message, ScreenTexts.YES, ScreenTexts.NO);
   }

   public ConfirmScreen(BooleanConsumer callback, Text title, Text message, Text yesTranslated, Text noTranslated) {
      super(title);
      this.messageSplit = MultilineText.EMPTY;
      this.field_33808 = Lists.newArrayList();
      this.callback = callback;
      this.message = message;
      this.yesTranslated = yesTranslated;
      this.noTranslated = noTranslated;
   }

   public Text getNarratedTitle() {
      return ScreenTexts.joinSentences(super.getNarratedTitle(), this.message);
   }

   protected void init() {
      super.init();
      this.messageSplit = MultilineText.create(this.textRenderer, this.message, this.width - 50);
      int var10000 = this.messageSplit.count();
      Objects.requireNonNull(this.textRenderer);
      int i = var10000 * 9;
      int j = MathHelper.clamp(90 + i + 12, this.height / 6 + 96, this.height - 24);
      this.field_33808.clear();
      this.method_37051(j);
   }

   protected void method_37051(int i) {
      this.method_37052(new ButtonWidget(this.width / 2 - 155, i, 150, 20, this.yesTranslated, (button) -> {
         this.callback.accept(true);
      }));
      this.method_37052(new ButtonWidget(this.width / 2 - 155 + 160, i, 150, 20, this.noTranslated, (button) -> {
         this.callback.accept(false);
      }));
   }

   protected void method_37052(ButtonWidget buttonWidget) {
      this.field_33808.add((ButtonWidget)this.addDrawableChild(buttonWidget));
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.renderBackground(matrices);
      drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 70, 16777215);
      this.messageSplit.drawCenterWithShadow(matrices, this.width / 2, 90);
      super.render(matrices, mouseX, mouseY, delta);
   }

   public void disableButtons(int ticks) {
      this.buttonEnableTimer = ticks;

      ButtonWidget buttonWidget;
      for(Iterator var2 = this.field_33808.iterator(); var2.hasNext(); buttonWidget.active = false) {
         buttonWidget = (ButtonWidget)var2.next();
      }

   }

   public void tick() {
      super.tick();
      ButtonWidget buttonWidget;
      if (--this.buttonEnableTimer == 0) {
         for(Iterator var1 = this.field_33808.iterator(); var1.hasNext(); buttonWidget.active = true) {
            buttonWidget = (ButtonWidget)var1.next();
         }
      }

   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
         this.callback.accept(false);
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }
}
