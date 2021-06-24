package net.minecraft.client.gui.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ProgressListener;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class ProgressScreen extends Screen implements ProgressListener {
   @Nullable
   private Text title;
   @Nullable
   private Text task;
   private int progress;
   private boolean done;
   private final boolean field_33625;

   public ProgressScreen(boolean bl) {
      super(NarratorManager.EMPTY);
      this.field_33625 = bl;
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   public void setTitle(Text title) {
      this.setTitleAndTask(title);
   }

   public void setTitleAndTask(Text title) {
      this.title = title;
      this.setTask(new TranslatableText("progress.working"));
   }

   public void setTask(Text task) {
      this.task = task;
      this.progressStagePercentage(0);
   }

   public void progressStagePercentage(int percentage) {
      this.progress = percentage;
   }

   public void setDone() {
      this.done = true;
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      if (this.done) {
         if (this.field_33625) {
            this.client.openScreen((Screen)null);
         }

      } else {
         this.renderBackground(matrices);
         if (this.title != null) {
            drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 70, 16777215);
         }

         if (this.task != null && this.progress != 0) {
            drawCenteredText(matrices, this.textRenderer, (new LiteralText("")).append(this.task).append(" " + this.progress + "%"), this.width / 2, 90, 16777215);
         }

         super.render(matrices, mouseX, mouseY, delta);
      }
   }
}
