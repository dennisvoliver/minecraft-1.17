package net.minecraft.client.realms.gui.screen;

import java.time.Duration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.realms.RepeatedNarrator;
import net.minecraft.client.realms.exception.RealmsDefaultUncaughtExceptionHandler;
import net.minecraft.client.realms.task.LongRunningTask;
import net.minecraft.client.realms.util.Errable;
import net.minecraft.client.util.NarratorManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class RealmsLongRunningMcoTaskScreen extends RealmsScreen implements Errable {
   private static final RepeatedNarrator field_33779 = new RepeatedNarrator(Duration.ofSeconds(5L));
   private static final Logger LOGGER = LogManager.getLogger();
   private final Screen parent;
   private volatile Text title;
   @Nullable
   private volatile Text errorMessage;
   private volatile boolean aborted;
   private int animTicks;
   private final LongRunningTask task;
   private final int buttonLength;
   private ButtonWidget field_33778;
   public static final String[] symbols = new String[]{"▃ ▄ ▅ ▆ ▇ █ ▇ ▆ ▅ ▄ ▃", "_ ▃ ▄ ▅ ▆ ▇ █ ▇ ▆ ▅ ▄", "_ _ ▃ ▄ ▅ ▆ ▇ █ ▇ ▆ ▅", "_ _ _ ▃ ▄ ▅ ▆ ▇ █ ▇ ▆", "_ _ _ _ ▃ ▄ ▅ ▆ ▇ █ ▇", "_ _ _ _ _ ▃ ▄ ▅ ▆ ▇ █", "_ _ _ _ ▃ ▄ ▅ ▆ ▇ █ ▇", "_ _ _ ▃ ▄ ▅ ▆ ▇ █ ▇ ▆", "_ _ ▃ ▄ ▅ ▆ ▇ █ ▇ ▆ ▅", "_ ▃ ▄ ▅ ▆ ▇ █ ▇ ▆ ▅ ▄", "▃ ▄ ▅ ▆ ▇ █ ▇ ▆ ▅ ▄ ▃", "▄ ▅ ▆ ▇ █ ▇ ▆ ▅ ▄ ▃ _", "▅ ▆ ▇ █ ▇ ▆ ▅ ▄ ▃ _ _", "▆ ▇ █ ▇ ▆ ▅ ▄ ▃ _ _ _", "▇ █ ▇ ▆ ▅ ▄ ▃ _ _ _ _", "█ ▇ ▆ ▅ ▄ ▃ _ _ _ _ _", "▇ █ ▇ ▆ ▅ ▄ ▃ _ _ _ _", "▆ ▇ █ ▇ ▆ ▅ ▄ ▃ _ _ _", "▅ ▆ ▇ █ ▇ ▆ ▅ ▄ ▃ _ _", "▄ ▅ ▆ ▇ █ ▇ ▆ ▅ ▄ ▃ _"};

   public RealmsLongRunningMcoTaskScreen(Screen parent, LongRunningTask task) {
      super(NarratorManager.EMPTY);
      this.title = LiteralText.EMPTY;
      this.buttonLength = 212;
      this.parent = parent;
      this.task = task;
      task.setScreen(this);
      Thread thread = new Thread(task, "Realms-long-running-task");
      thread.setUncaughtExceptionHandler(new RealmsDefaultUncaughtExceptionHandler(LOGGER));
      thread.start();
   }

   public void tick() {
      super.tick();
      field_33779.narrate(this.title);
      ++this.animTicks;
      this.task.tick();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
         this.cancelOrBackButtonClicked();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void init() {
      this.task.init();
      this.field_33778 = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.width / 2 - 106, row(12), 212, 20, ScreenTexts.CANCEL, (button) -> {
         this.cancelOrBackButtonClicked();
      }));
   }

   private void cancelOrBackButtonClicked() {
      this.aborted = true;
      this.task.abortTask();
      this.client.openScreen(this.parent);
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.renderBackground(matrices);
      drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, row(3), 16777215);
      Text text = this.errorMessage;
      if (text == null) {
         drawCenteredText(matrices, this.textRenderer, symbols[this.animTicks % symbols.length], this.width / 2, row(8), 8421504);
      } else {
         drawCenteredText(matrices, this.textRenderer, text, this.width / 2, row(8), 16711680);
      }

      super.render(matrices, mouseX, mouseY, delta);
   }

   public void error(Text errorMessage) {
      this.errorMessage = errorMessage;
      NarratorManager.INSTANCE.narrate(errorMessage);
      this.client.execute(() -> {
         this.remove(this.field_33778);
         this.field_33778 = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.width / 2 - 106, this.height / 4 + 120 + 12, 200, 20, ScreenTexts.BACK, (button) -> {
            this.cancelOrBackButtonClicked();
         }));
      });
   }

   public void setTitle(Text title) {
      this.title = title;
   }

   public boolean aborted() {
      return this.aborted;
   }
}
