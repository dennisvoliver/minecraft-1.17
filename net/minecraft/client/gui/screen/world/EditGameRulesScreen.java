package net.minecraft.client.gui.screen.world;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.ElementListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameRules;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class EditGameRulesScreen extends Screen {
   private final Consumer<Optional<GameRules>> ruleSaver;
   private EditGameRulesScreen.RuleListWidget ruleListWidget;
   private final Set<EditGameRulesScreen.AbstractRuleWidget> invalidRuleWidgets = Sets.newHashSet();
   private ButtonWidget doneButton;
   @Nullable
   private List<OrderedText> tooltip;
   private final GameRules gameRules;

   public EditGameRulesScreen(GameRules gameRules, Consumer<Optional<GameRules>> ruleSaveConsumer) {
      super(new TranslatableText("editGamerule.title"));
      this.gameRules = gameRules;
      this.ruleSaver = ruleSaveConsumer;
   }

   protected void init() {
      this.client.keyboard.setRepeatEvents(true);
      super.init();
      this.ruleListWidget = new EditGameRulesScreen.RuleListWidget(this.gameRules);
      this.addSelectableChild(this.ruleListWidget);
      this.addDrawableChild(new ButtonWidget(this.width / 2 - 155 + 160, this.height - 29, 150, 20, ScreenTexts.CANCEL, (button) -> {
         this.ruleSaver.accept(Optional.empty());
      }));
      this.doneButton = (ButtonWidget)this.addDrawableChild(new ButtonWidget(this.width / 2 - 155, this.height - 29, 150, 20, ScreenTexts.DONE, (button) -> {
         this.ruleSaver.accept(Optional.of(this.gameRules));
      }));
   }

   public void removed() {
      this.client.keyboard.setRepeatEvents(false);
   }

   public void onClose() {
      this.ruleSaver.accept(Optional.empty());
   }

   public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
      this.tooltip = null;
      this.ruleListWidget.render(matrices, mouseX, mouseY, delta);
      drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 20, 16777215);
      super.render(matrices, mouseX, mouseY, delta);
      if (this.tooltip != null) {
         this.renderOrderedTooltip(matrices, this.tooltip, mouseX, mouseY);
      }

   }

   void setTooltipDescription(@Nullable List<OrderedText> description) {
      this.tooltip = description;
   }

   private void updateDoneButton() {
      this.doneButton.active = this.invalidRuleWidgets.isEmpty();
   }

   void markInvalid(EditGameRulesScreen.AbstractRuleWidget ruleWidget) {
      this.invalidRuleWidgets.add(ruleWidget);
      this.updateDoneButton();
   }

   void markValid(EditGameRulesScreen.AbstractRuleWidget ruleWidget) {
      this.invalidRuleWidgets.remove(ruleWidget);
      this.updateDoneButton();
   }

   @Environment(EnvType.CLIENT)
   public class RuleListWidget extends ElementListWidget<EditGameRulesScreen.AbstractRuleWidget> {
      public RuleListWidget(final GameRules gameRules) {
         super(EditGameRulesScreen.this.client, EditGameRulesScreen.this.width, EditGameRulesScreen.this.height, 43, EditGameRulesScreen.this.height - 32, 24);
         final Map<GameRules.Category, Map<GameRules.Key<?>, EditGameRulesScreen.AbstractRuleWidget>> map = Maps.newHashMap();
         GameRules.accept(new GameRules.Visitor() {
            public void visitBoolean(GameRules.Key<GameRules.BooleanRule> key, GameRules.Type<GameRules.BooleanRule> type) {
               this.createRuleWidget(key, (text, list, string, booleanRule) -> {
                  return EditGameRulesScreen.thisx.new BooleanRuleWidget(text, list, string, booleanRule);
               });
            }

            public void visitInt(GameRules.Key<GameRules.IntRule> key, GameRules.Type<GameRules.IntRule> type) {
               this.createRuleWidget(key, (text, list, string, intRule) -> {
                  return EditGameRulesScreen.thisx.new IntRuleWidget(text, list, string, intRule);
               });
            }

            private <T extends GameRules.Rule<T>> void createRuleWidget(GameRules.Key<T> key, EditGameRulesScreen.RuleWidgetFactory<T> widgetFactory) {
               Text text = new TranslatableText(key.getTranslationKey());
               Text text2 = (new LiteralText(key.getName())).formatted(Formatting.YELLOW);
               T rule = gameRules.get(key);
               String string = rule.serialize();
               Text text3 = (new TranslatableText("editGamerule.default", new Object[]{new LiteralText(string)})).formatted(Formatting.GRAY);
               String string2 = key.getTranslationKey() + ".description";
               ImmutableList list2;
               String string4;
               if (I18n.hasTranslation(string2)) {
                  Builder<OrderedText> builder = ImmutableList.builder().add((Object)text2.asOrderedText());
                  Text text4 = new TranslatableText(string2);
                  List var10000 = EditGameRulesScreen.this.textRenderer.wrapLines(text4, 150);
                  Objects.requireNonNull(builder);
                  var10000.forEach(builder::add);
                  list2 = builder.add((Object)text3.asOrderedText()).build();
                  String var13 = text4.getString();
                  string4 = var13 + "\n" + text3.getString();
               } else {
                  list2 = ImmutableList.of(text2.asOrderedText(), text3.asOrderedText());
                  string4 = text3.getString();
               }

               ((Map)map.computeIfAbsent(key.getCategory(), (category) -> {
                  return Maps.newHashMap();
               })).put(key, widgetFactory.create(text, list2, string4, rule));
            }
         });
         map.entrySet().stream().sorted(Entry.comparingByKey()).forEach((entry) -> {
            this.addEntry(EditGameRulesScreen.this.new RuleCategoryWidget((new TranslatableText(((GameRules.Category)entry.getKey()).getCategory())).formatted(new Formatting[]{Formatting.BOLD, Formatting.YELLOW})));
            ((Map)entry.getValue()).entrySet().stream().sorted(Entry.comparingByKey(Comparator.comparing(GameRules.Key::getName))).forEach((entryx) -> {
               this.addEntry((EditGameRulesScreen.AbstractRuleWidget)entryx.getValue());
            });
         });
      }

      public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
         super.render(matrices, mouseX, mouseY, delta);
         EditGameRulesScreen.AbstractRuleWidget abstractRuleWidget = (EditGameRulesScreen.AbstractRuleWidget)this.method_37019();
         if (abstractRuleWidget != null) {
            EditGameRulesScreen.this.setTooltipDescription(abstractRuleWidget.description);
         }

      }
   }

   @Environment(EnvType.CLIENT)
   public class IntRuleWidget extends EditGameRulesScreen.NamedRuleWidget {
      private final TextFieldWidget valueWidget;

      public IntRuleWidget(Text name, List<OrderedText> description, String ruleName, GameRules.IntRule rule) {
         super(description, name);
         this.valueWidget = new TextFieldWidget(EditGameRulesScreen.this.client.textRenderer, 10, 5, 42, 20, name.shallowCopy().append("\n").append(ruleName).append("\n"));
         this.valueWidget.setText(Integer.toString(rule.get()));
         this.valueWidget.setChangedListener((value) -> {
            if (rule.validate(value)) {
               this.valueWidget.setEditableColor(14737632);
               EditGameRulesScreen.this.markValid(this);
            } else {
               this.valueWidget.setEditableColor(16711680);
               EditGameRulesScreen.this.markInvalid(this);
            }

         });
         this.children.add(this.valueWidget);
      }

      public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
         this.drawName(matrices, y, x);
         this.valueWidget.x = x + entryWidth - 44;
         this.valueWidget.y = y;
         this.valueWidget.render(matrices, mouseX, mouseY, tickDelta);
      }
   }

   @Environment(EnvType.CLIENT)
   public class BooleanRuleWidget extends EditGameRulesScreen.NamedRuleWidget {
      private final CyclingButtonWidget<Boolean> toggleButton;

      public BooleanRuleWidget(Text name, List<OrderedText> description, String ruleName, GameRules.BooleanRule rule) {
         super(description, name);
         this.toggleButton = CyclingButtonWidget.onOffBuilder(rule.get()).omitKeyText().narration((button) -> {
            return button.getGenericNarrationMessage().append("\n").append(ruleName);
         }).build(10, 5, 44, 20, name, (button, value) -> {
            rule.set(value, (MinecraftServer)null);
         });
         this.children.add(this.toggleButton);
      }

      public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
         this.drawName(matrices, y, x);
         this.toggleButton.x = x + entryWidth - 45;
         this.toggleButton.y = y;
         this.toggleButton.render(matrices, mouseX, mouseY, tickDelta);
      }
   }

   @Environment(EnvType.CLIENT)
   public abstract class NamedRuleWidget extends EditGameRulesScreen.AbstractRuleWidget {
      private final List<OrderedText> name;
      protected final List<ClickableWidget> children = Lists.newArrayList();

      public NamedRuleWidget(@Nullable List<OrderedText> description, Text name) {
         super(description);
         this.name = EditGameRulesScreen.this.client.textRenderer.wrapLines(name, 175);
      }

      public List<? extends Element> children() {
         return this.children;
      }

      public List<? extends Selectable> method_37025() {
         return this.children;
      }

      protected void drawName(MatrixStack matrices, int x, int y) {
         if (this.name.size() == 1) {
            EditGameRulesScreen.this.client.textRenderer.draw(matrices, (OrderedText)this.name.get(0), (float)y, (float)(x + 5), 16777215);
         } else if (this.name.size() >= 2) {
            EditGameRulesScreen.this.client.textRenderer.draw(matrices, (OrderedText)this.name.get(0), (float)y, (float)x, 16777215);
            EditGameRulesScreen.this.client.textRenderer.draw(matrices, (OrderedText)this.name.get(1), (float)y, (float)(x + 10), 16777215);
         }

      }
   }

   @FunctionalInterface
   @Environment(EnvType.CLIENT)
   private interface RuleWidgetFactory<T extends GameRules.Rule<T>> {
      EditGameRulesScreen.AbstractRuleWidget create(Text name, List<OrderedText> description, String ruleName, T rule);
   }

   @Environment(EnvType.CLIENT)
   public class RuleCategoryWidget extends EditGameRulesScreen.AbstractRuleWidget {
      final Text name;

      public RuleCategoryWidget(Text text) {
         super((List)null);
         this.name = text;
      }

      public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
         DrawableHelper.drawCenteredText(matrices, EditGameRulesScreen.this.client.textRenderer, this.name, x + entryWidth / 2, y + 5, 16777215);
      }

      public List<? extends Element> children() {
         return ImmutableList.of();
      }

      public List<? extends Selectable> method_37025() {
         return ImmutableList.of(new Selectable() {
            public Selectable.SelectionType getType() {
               return Selectable.SelectionType.HOVERED;
            }

            public void appendNarrations(NarrationMessageBuilder builder) {
               builder.put(NarrationPart.TITLE, RuleCategoryWidget.this.name);
            }
         });
      }
   }

   @Environment(EnvType.CLIENT)
   public abstract class AbstractRuleWidget extends ElementListWidget.Entry<EditGameRulesScreen.AbstractRuleWidget> {
      @Nullable
      final List<OrderedText> description;

      public AbstractRuleWidget(@Nullable List<OrderedText> description) {
         this.description = description;
      }
   }
}
