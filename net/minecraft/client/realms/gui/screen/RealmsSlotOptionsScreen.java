package net.minecraft.client.realms.gui.screen;

import com.google.common.collect.ImmutableList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.realms.RealmsLabel;
import net.minecraft.client.realms.dto.RealmsServer;
import net.minecraft.client.realms.dto.RealmsWorldOptions;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.LiteralText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameMode;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public class RealmsSlotOptionsScreen extends RealmsScreen {
   private static final int field_32125 = 2;
   public static final List<Difficulty> DIFFICULTIES;
   private static final int field_32126 = 0;
   public static final List<GameMode> GAME_MODES;
   private static final Text EDIT_SLOT_NAME;
   static final Text SPAWN_PROTECTION;
   private TextFieldWidget nameEdit;
   protected final RealmsConfigureWorldScreen parent;
   private int column1_x;
   private int column2_x;
   private final RealmsWorldOptions options;
   private final RealmsServer.WorldType worldType;
   private final int activeSlot;
   private Difficulty field_27943;
   private GameMode gameModeIndex;
   private boolean pvp;
   private boolean spawnNpcs;
   private boolean spawnAnimals;
   private boolean spawnMonsters;
   int difficultyIndex;
   private boolean commandBlocks;
   private boolean forceGameMode;
   RealmsSlotOptionsScreen.SettingsSlider spawnProtectionButton;

   public RealmsSlotOptionsScreen(RealmsConfigureWorldScreen parent, RealmsWorldOptions options, RealmsServer.WorldType worldType, int activeSlot) {
      super(new TranslatableText("mco.configure.world.buttons.options"));
      this.parent = parent;
      this.options = options;
      this.worldType = worldType;
      this.activeSlot = activeSlot;
   }

   public void removed() {
      this.client.keyboard.setRepeatEvents(false);
   }

   public void tick() {
      this.nameEdit.tick();
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
         this.client.openScreen(this.parent);
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   private static <T> T method_32498(List<T> list, int i, int j) {
      try {
         return list.get(i);
      } catch (IndexOutOfBoundsException var4) {
         return list.get(j);
      }
   }

   private static <T> int method_32499(List<T> list, T object, int i) {
      int j = list.indexOf(object);
      return j == -1 ? i : j;
   }

   public void init() {
      this.column2_x = 170;
      this.column1_x = this.width / 2 - this.column2_x;
      int i = this.width / 2 + 10;
      this.field_27943 = (Difficulty)method_32498(DIFFICULTIES, this.options.difficulty, 2);
      this.gameModeIndex = (GameMode)method_32498(GAME_MODES, this.options.gameMode, 0);
      if (this.worldType == RealmsServer.WorldType.NORMAL) {
         this.pvp = this.options.pvp;
         this.difficultyIndex = this.options.spawnProtection;
         this.forceGameMode = this.options.forceGameMode;
         this.spawnAnimals = this.options.spawnAnimals;
         this.spawnMonsters = this.options.spawnMonsters;
         this.spawnNpcs = this.options.spawnNpcs;
         this.commandBlocks = this.options.commandBlocks;
      } else {
         TranslatableText text3;
         if (this.worldType == RealmsServer.WorldType.ADVENTUREMAP) {
            text3 = new TranslatableText("mco.configure.world.edit.subscreen.adventuremap");
         } else if (this.worldType == RealmsServer.WorldType.INSPIRATION) {
            text3 = new TranslatableText("mco.configure.world.edit.subscreen.inspiration");
         } else {
            text3 = new TranslatableText("mco.configure.world.edit.subscreen.experience");
         }

         this.method_37107(new RealmsLabel(text3, this.width / 2, 26, 16711680));
         this.pvp = true;
         this.difficultyIndex = 0;
         this.forceGameMode = false;
         this.spawnAnimals = true;
         this.spawnMonsters = true;
         this.spawnNpcs = true;
         this.commandBlocks = true;
      }

      this.nameEdit = new TextFieldWidget(this.client.textRenderer, this.column1_x + 2, row(1), this.column2_x - 4, 20, (TextFieldWidget)null, new TranslatableText("mco.configure.world.edit.slot.name"));
      this.nameEdit.setMaxLength(10);
      this.nameEdit.setText(this.options.getSlotName(this.activeSlot));
      this.focusOn(this.nameEdit);
      CyclingButtonWidget<Boolean> cyclingButtonWidget = (CyclingButtonWidget)this.addDrawableChild(CyclingButtonWidget.onOffBuilder(this.pvp).build(i, row(1), this.column2_x, 20, new TranslatableText("mco.configure.world.pvp"), (button, pvp) -> {
         this.pvp = pvp;
      }));
      this.addDrawableChild(CyclingButtonWidget.builder(GameMode::getSimpleTranslatableName).values(GAME_MODES).initially(this.gameModeIndex).build(this.column1_x, row(3), this.column2_x, 20, new TranslatableText("selectWorld.gameMode"), (button, gameModeIndex) -> {
         this.gameModeIndex = gameModeIndex;
      }));
      CyclingButtonWidget<Boolean> cyclingButtonWidget2 = (CyclingButtonWidget)this.addDrawableChild(CyclingButtonWidget.onOffBuilder(this.spawnAnimals).build(i, row(3), this.column2_x, 20, new TranslatableText("mco.configure.world.spawnAnimals"), (button, spawnAnimals) -> {
         this.spawnAnimals = spawnAnimals;
      }));
      CyclingButtonWidget<Boolean> cyclingButtonWidget3 = CyclingButtonWidget.onOffBuilder(this.field_27943 != Difficulty.PEACEFUL && this.spawnMonsters).build(i, row(5), this.column2_x, 20, new TranslatableText("mco.configure.world.spawnMonsters"), (button, spawnMonsters) -> {
         this.spawnMonsters = spawnMonsters;
      });
      this.addDrawableChild(CyclingButtonWidget.builder(Difficulty::getTranslatableName).values(DIFFICULTIES).initially(this.field_27943).build(this.column1_x, row(5), this.column2_x, 20, new TranslatableText("options.difficulty"), (button, difficulty) -> {
         this.field_27943 = difficulty;
         if (this.worldType == RealmsServer.WorldType.NORMAL) {
            boolean bl = this.field_27943 != Difficulty.PEACEFUL;
            cyclingButtonWidget3.active = bl;
            cyclingButtonWidget3.setValue(bl && this.spawnMonsters);
         }

      }));
      this.addDrawableChild(cyclingButtonWidget3);
      this.spawnProtectionButton = (RealmsSlotOptionsScreen.SettingsSlider)this.addDrawableChild(new RealmsSlotOptionsScreen.SettingsSlider(this.column1_x, row(7), this.column2_x, this.difficultyIndex, 0.0F, 16.0F));
      CyclingButtonWidget<Boolean> cyclingButtonWidget4 = (CyclingButtonWidget)this.addDrawableChild(CyclingButtonWidget.onOffBuilder(this.spawnNpcs).build(i, row(7), this.column2_x, 20, new TranslatableText("mco.configure.world.spawnNPCs"), (button, spawnNpcs) -> {
         this.spawnNpcs = spawnNpcs;
      }));
      CyclingButtonWidget<Boolean> cyclingButtonWidget5 = (CyclingButtonWidget)this.addDrawableChild(CyclingButtonWidget.onOffBuilder(this.forceGameMode).build(this.column1_x, row(9), this.column2_x, 20, new TranslatableText("mco.configure.world.forceGameMode"), (button, forceGameMode) -> {
         this.forceGameMode = forceGameMode;
      }));
      CyclingButtonWidget<Boolean> cyclingButtonWidget6 = (CyclingButtonWidget)this.addDrawableChild(CyclingButtonWidget.onOffBuilder(this.commandBlocks).build(i, row(9), this.column2_x, 20, new TranslatableText("mco.configure.world.commandBlocks"), (button, commandBlocks) -> {
         this.commandBlocks = commandBlocks;
      }));
      if (this.worldType != RealmsServer.WorldType.NORMAL) {
         cyclingButtonWidget.active = false;
         cyclingButtonWidget2.active = false;
         cyclingButtonWidget4.active = false;
         cyclingButtonWidget3.active = false;
         this.spawnProtectionButton.active = false;
         cyclingButtonWidget6.active = false;
         cyclingButtonWidget5.active = false;
      }

      if (this.field_27943 == Difficulty.PEACEFUL) {
         cyclingButtonWidget3.active = false;
      }

      this.addDrawableChild(new ButtonWidget(this.column1_x, row(13), this.column2_x, 20, new TranslatableText("mco.configure.world.buttons.done"), (button) -> {
         this.saveSettings();
      }));
      this.addDrawableChild(new ButtonWidget(i, row(13), this.column2_x, 20, ScreenTexts.CANCEL, (button) -> {
         this.client.openScreen(this.parent);
      }));
      this.addSelectableChild(this.nameEdit);
   }

   public Text getNarratedTitle() {
      return ScreenTexts.joinSentences(this.getTitle(), this.narrateLabels());
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.renderBackground(matrices);
      drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 17, 16777215);
      this.textRenderer.draw(matrices, EDIT_SLOT_NAME, (float)(this.column1_x + this.column2_x / 2 - this.textRenderer.getWidth((StringVisitable)EDIT_SLOT_NAME) / 2), (float)(row(0) - 5), 16777215);
      this.nameEdit.render(matrices, mouseX, mouseY, delta);
      super.render(matrices, mouseX, mouseY, delta);
   }

   private String getSlotName() {
      return this.nameEdit.getText().equals(this.options.getDefaultSlotName(this.activeSlot)) ? "" : this.nameEdit.getText();
   }

   private void saveSettings() {
      int i = method_32499(DIFFICULTIES, this.field_27943, 2);
      int j = method_32499(GAME_MODES, this.gameModeIndex, 0);
      if (this.worldType != RealmsServer.WorldType.ADVENTUREMAP && this.worldType != RealmsServer.WorldType.EXPERIENCE && this.worldType != RealmsServer.WorldType.INSPIRATION) {
         this.parent.saveSlotSettings(new RealmsWorldOptions(this.pvp, this.spawnAnimals, this.spawnMonsters, this.spawnNpcs, this.difficultyIndex, this.commandBlocks, i, j, this.forceGameMode, this.getSlotName()));
      } else {
         this.parent.saveSlotSettings(new RealmsWorldOptions(this.options.pvp, this.options.spawnAnimals, this.options.spawnMonsters, this.options.spawnNpcs, this.options.spawnProtection, this.options.commandBlocks, i, j, this.options.forceGameMode, this.getSlotName()));
      }

   }

   static {
      DIFFICULTIES = ImmutableList.of(Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD);
      GAME_MODES = ImmutableList.of(GameMode.SURVIVAL, GameMode.CREATIVE, GameMode.ADVENTURE);
      EDIT_SLOT_NAME = new TranslatableText("mco.configure.world.edit.slot.name");
      SPAWN_PROTECTION = new TranslatableText("mco.configure.world.spawnProtection");
   }

   @Environment(EnvType.CLIENT)
   private class SettingsSlider extends SliderWidget {
      private final double min;
      private final double max;

      public SettingsSlider(int x, int y, int width, int value, float min, float max) {
         super(x, y, width, 20, LiteralText.EMPTY, 0.0D);
         this.min = (double)min;
         this.max = (double)max;
         this.value = (double)((MathHelper.clamp((float)value, min, max) - min) / (max - min));
         this.updateMessage();
      }

      public void applyValue() {
         if (RealmsSlotOptionsScreen.this.spawnProtectionButton.active) {
            RealmsSlotOptionsScreen.this.difficultyIndex = (int)MathHelper.lerp(MathHelper.clamp(this.value, 0.0D, 1.0D), this.min, this.max);
         }
      }

      protected void updateMessage() {
         this.setMessage(ScreenTexts.composeGenericOptionText(RealmsSlotOptionsScreen.SPAWN_PROTECTION, (Text)(RealmsSlotOptionsScreen.this.difficultyIndex == 0 ? ScreenTexts.OFF : new LiteralText(String.valueOf(RealmsSlotOptionsScreen.this.difficultyIndex)))));
      }

      public void onClick(double mouseX, double mouseY) {
      }

      public void onRelease(double mouseX, double mouseY) {
      }
   }
}
