package net.minecraft.client.realms.gui.screen;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class RealmsConfirmScreen extends RealmsScreen {
   protected BooleanConsumer field_22692;
   private final Text title1;
   private final Text title2;

   public RealmsConfirmScreen(BooleanConsumer booleanConsumer, Text title1, Text title2) {
      super(NarratorManager.EMPTY);
      this.field_22692 = booleanConsumer;
      this.title1 = title1;
      this.title2 = title2;
   }

   public void init() {
      this.addDrawableChild(new ButtonWidget(this.width / 2 - 105, row(9), 100, 20, ScreenTexts.YES, (button) -> {
         this.field_22692.accept(true);
      }));
      this.addDrawableChild(new ButtonWidget(this.width / 2 + 5, row(9), 100, 20, ScreenTexts.NO, (button) -> {
         this.field_22692.accept(false);
      }));
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.renderBackground(matrices);
      drawCenteredText(matrices, this.textRenderer, this.title1, this.width / 2, row(3), 16777215);
      drawCenteredText(matrices, this.textRenderer, this.title2, this.width / 2, row(5), 16777215);
      super.render(matrices, mouseX, mouseY, delta);
   }
}
