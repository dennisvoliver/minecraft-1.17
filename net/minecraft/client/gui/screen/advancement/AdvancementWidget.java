package net.minecraft.client.gui.screen.advancement;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class AdvancementWidget extends DrawableHelper {
   private static final Identifier WIDGETS_TEXTURE = new Identifier("textures/gui/advancements/widgets.png");
   private static final int field_32286 = 26;
   private static final int field_32287 = 0;
   private static final int field_32288 = 200;
   private static final int field_32289 = 26;
   private static final int field_32290 = 8;
   private static final int field_32291 = 5;
   private static final int field_32292 = 26;
   private static final int field_32293 = 3;
   private static final int field_32294 = 5;
   private static final int field_32295 = 32;
   private static final int field_32296 = 9;
   private static final int field_32297 = 163;
   private static final int[] SPLIT_OFFSET_CANDIDATES = new int[]{0, 10, -10, 25, -25};
   private final AdvancementTab tab;
   private final Advancement advancement;
   private final AdvancementDisplay display;
   private final OrderedText title;
   private final int width;
   private final List<OrderedText> description;
   private final MinecraftClient client;
   private AdvancementWidget parent;
   private final List<AdvancementWidget> children = Lists.newArrayList();
   private AdvancementProgress progress;
   private final int x;
   private final int y;

   public AdvancementWidget(AdvancementTab tab, MinecraftClient client, Advancement advancement, AdvancementDisplay display) {
      this.tab = tab;
      this.advancement = advancement;
      this.display = display;
      this.client = client;
      this.title = Language.getInstance().reorder(client.textRenderer.trimToWidth((StringVisitable)display.getTitle(), 163));
      this.x = MathHelper.floor(display.getX() * 28.0F);
      this.y = MathHelper.floor(display.getY() * 27.0F);
      int i = advancement.getRequirementCount();
      int j = String.valueOf(i).length();
      int k = i > 1 ? client.textRenderer.getWidth("  ") + client.textRenderer.getWidth("0") * j * 2 + client.textRenderer.getWidth("/") : 0;
      int l = 29 + client.textRenderer.getWidth(this.title) + k;
      this.description = Language.getInstance().reorder(this.wrapDescription(Texts.setStyleIfAbsent(display.getDescription().shallowCopy(), Style.EMPTY.withColor(display.getFrame().getTitleFormat())), l));

      OrderedText orderedText;
      for(Iterator var9 = this.description.iterator(); var9.hasNext(); l = Math.max(l, client.textRenderer.getWidth(orderedText))) {
         orderedText = (OrderedText)var9.next();
      }

      this.width = l + 3 + 5;
   }

   private static float getMaxWidth(TextHandler textHandler, List<StringVisitable> lines) {
      Stream var10000 = lines.stream();
      Objects.requireNonNull(textHandler);
      return (float)var10000.mapToDouble(textHandler::getWidth).max().orElse(0.0D);
   }

   private List<StringVisitable> wrapDescription(Text text, int width) {
      TextHandler textHandler = this.client.textRenderer.getTextHandler();
      List<StringVisitable> list = null;
      float f = Float.MAX_VALUE;
      int[] var6 = SPLIT_OFFSET_CANDIDATES;
      int var7 = var6.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         int i = var6[var8];
         List<StringVisitable> list2 = textHandler.wrapLines((StringVisitable)text, width - i, Style.EMPTY);
         float g = Math.abs(getMaxWidth(textHandler, list2) - (float)width);
         if (g <= 10.0F) {
            return list2;
         }

         if (g < f) {
            f = g;
            list = list2;
         }
      }

      return list;
   }

   @Nullable
   private AdvancementWidget getParent(Advancement advancement) {
      do {
         advancement = advancement.getParent();
      } while(advancement != null && advancement.getDisplay() == null);

      if (advancement != null && advancement.getDisplay() != null) {
         return this.tab.getWidget(advancement);
      } else {
         return null;
      }
   }

   public void renderLines(MatrixStack matrices, int x, int y, boolean bl) {
      if (this.parent != null) {
         int i = x + this.parent.x + 13;
         int j = x + this.parent.x + 26 + 4;
         int k = y + this.parent.y + 13;
         int l = x + this.x + 13;
         int m = y + this.y + 13;
         int n = bl ? -16777216 : -1;
         if (bl) {
            this.drawHorizontalLine(matrices, j, i, k - 1, n);
            this.drawHorizontalLine(matrices, j + 1, i, k, n);
            this.drawHorizontalLine(matrices, j, i, k + 1, n);
            this.drawHorizontalLine(matrices, l, j - 1, m - 1, n);
            this.drawHorizontalLine(matrices, l, j - 1, m, n);
            this.drawHorizontalLine(matrices, l, j - 1, m + 1, n);
            this.drawVerticalLine(matrices, j - 1, m, k, n);
            this.drawVerticalLine(matrices, j + 1, m, k, n);
         } else {
            this.drawHorizontalLine(matrices, j, i, k, n);
            this.drawHorizontalLine(matrices, l, j, m, n);
            this.drawVerticalLine(matrices, j, m, k, n);
         }
      }

      Iterator var11 = this.children.iterator();

      while(var11.hasNext()) {
         AdvancementWidget advancementWidget = (AdvancementWidget)var11.next();
         advancementWidget.renderLines(matrices, x, y, bl);
      }

   }

   public void renderWidgets(MatrixStack matrices, int x, int y) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         float f = this.progress == null ? 0.0F : this.progress.getProgressBarPercentage();
         AdvancementObtainedStatus advancementObtainedStatus2;
         if (f >= 1.0F) {
            advancementObtainedStatus2 = AdvancementObtainedStatus.OBTAINED;
         } else {
            advancementObtainedStatus2 = AdvancementObtainedStatus.UNOBTAINED;
         }

         RenderSystem.setShader(GameRenderer::getPositionTexShader);
         RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
         this.drawTexture(matrices, x + this.x + 3, y + this.y, this.display.getFrame().getTextureV(), 128 + advancementObtainedStatus2.getSpriteIndex() * 26, 26, 26);
         this.client.getItemRenderer().renderInGui(this.display.getIcon(), x + this.x + 8, y + this.y + 5);
      }

      Iterator var6 = this.children.iterator();

      while(var6.hasNext()) {
         AdvancementWidget advancementWidget = (AdvancementWidget)var6.next();
         advancementWidget.renderWidgets(matrices, x, y);
      }

   }

   public int getWidth() {
      return this.width;
   }

   public void setProgress(AdvancementProgress progress) {
      this.progress = progress;
   }

   public void addChild(AdvancementWidget widget) {
      this.children.add(widget);
   }

   public void drawTooltip(MatrixStack matrices, int x, int y, float alpha, int i, int j) {
      boolean bl = i + x + this.x + this.width + 26 >= this.tab.getScreen().width;
      String string = this.progress == null ? null : this.progress.getProgressBarFraction();
      int k = string == null ? 0 : this.client.textRenderer.getWidth(string);
      int var10000 = 113 - y - this.y - 26;
      int var10002 = this.description.size();
      Objects.requireNonNull(this.client.textRenderer);
      boolean bl2 = var10000 <= 6 + var10002 * 9;
      float f = this.progress == null ? 0.0F : this.progress.getProgressBarPercentage();
      int l = MathHelper.floor(f * (float)this.width);
      AdvancementObtainedStatus advancementObtainedStatus10;
      AdvancementObtainedStatus advancementObtainedStatus11;
      AdvancementObtainedStatus advancementObtainedStatus12;
      if (f >= 1.0F) {
         l = this.width / 2;
         advancementObtainedStatus10 = AdvancementObtainedStatus.OBTAINED;
         advancementObtainedStatus11 = AdvancementObtainedStatus.OBTAINED;
         advancementObtainedStatus12 = AdvancementObtainedStatus.OBTAINED;
      } else if (l < 2) {
         l = this.width / 2;
         advancementObtainedStatus10 = AdvancementObtainedStatus.UNOBTAINED;
         advancementObtainedStatus11 = AdvancementObtainedStatus.UNOBTAINED;
         advancementObtainedStatus12 = AdvancementObtainedStatus.UNOBTAINED;
      } else if (l > this.width - 2) {
         l = this.width / 2;
         advancementObtainedStatus10 = AdvancementObtainedStatus.OBTAINED;
         advancementObtainedStatus11 = AdvancementObtainedStatus.OBTAINED;
         advancementObtainedStatus12 = AdvancementObtainedStatus.UNOBTAINED;
      } else {
         advancementObtainedStatus10 = AdvancementObtainedStatus.OBTAINED;
         advancementObtainedStatus11 = AdvancementObtainedStatus.UNOBTAINED;
         advancementObtainedStatus12 = AdvancementObtainedStatus.UNOBTAINED;
      }

      int m = this.width - l;
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, WIDGETS_TEXTURE);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
      RenderSystem.enableBlend();
      int n = y + this.y;
      int p;
      if (bl) {
         p = x + this.x - this.width + 26 + 6;
      } else {
         p = x + this.x;
      }

      int var10001 = this.description.size();
      Objects.requireNonNull(this.client.textRenderer);
      int q = 32 + var10001 * 9;
      if (!this.description.isEmpty()) {
         if (bl2) {
            this.method_2324(matrices, p, n + 26 - q, this.width, q, 10, 200, 26, 0, 52);
         } else {
            this.method_2324(matrices, p, n, this.width, q, 10, 200, 26, 0, 52);
         }
      }

      this.drawTexture(matrices, p, n, 0, advancementObtainedStatus10.getSpriteIndex() * 26, l, 26);
      this.drawTexture(matrices, p + l, n, 200 - m, advancementObtainedStatus11.getSpriteIndex() * 26, m, 26);
      this.drawTexture(matrices, x + this.x + 3, y + this.y, this.display.getFrame().getTextureV(), 128 + advancementObtainedStatus12.getSpriteIndex() * 26, 26, 26);
      if (bl) {
         this.client.textRenderer.drawWithShadow(matrices, (OrderedText)this.title, (float)(p + 5), (float)(y + this.y + 9), -1);
         if (string != null) {
            this.client.textRenderer.drawWithShadow(matrices, (String)string, (float)(x + this.x - k), (float)(y + this.y + 9), -1);
         }
      } else {
         this.client.textRenderer.drawWithShadow(matrices, (OrderedText)this.title, (float)(x + this.x + 32), (float)(y + this.y + 9), -1);
         if (string != null) {
            this.client.textRenderer.drawWithShadow(matrices, (String)string, (float)(x + this.x + this.width - k - 5), (float)(y + this.y + 9), -1);
         }
      }

      float var10003;
      int r;
      int var10004;
      TextRenderer var21;
      OrderedText var22;
      if (bl2) {
         for(r = 0; r < this.description.size(); ++r) {
            var21 = this.client.textRenderer;
            var22 = (OrderedText)this.description.get(r);
            var10003 = (float)(p + 5);
            var10004 = n + 26 - q + 7;
            Objects.requireNonNull(this.client.textRenderer);
            var21.draw(matrices, var22, var10003, (float)(var10004 + r * 9), -5592406);
         }
      } else {
         for(r = 0; r < this.description.size(); ++r) {
            var21 = this.client.textRenderer;
            var22 = (OrderedText)this.description.get(r);
            var10003 = (float)(p + 5);
            var10004 = y + this.y + 9 + 17;
            Objects.requireNonNull(this.client.textRenderer);
            var21.draw(matrices, var22, var10003, (float)(var10004 + r * 9), -5592406);
         }
      }

      this.client.getItemRenderer().renderInGui(this.display.getIcon(), x + this.x + 8, y + this.y + 5);
   }

   protected void method_2324(MatrixStack matrices, int x, int y, int i, int j, int k, int l, int m, int n, int o) {
      this.drawTexture(matrices, x, y, n, o, k, k);
      this.method_2321(matrices, x + k, y, i - k - k, k, n + k, o, l - k - k, m);
      this.drawTexture(matrices, x + i - k, y, n + l - k, o, k, k);
      this.drawTexture(matrices, x, y + j - k, n, o + m - k, k, k);
      this.method_2321(matrices, x + k, y + j - k, i - k - k, k, n + k, o + m - k, l - k - k, m);
      this.drawTexture(matrices, x + i - k, y + j - k, n + l - k, o + m - k, k, k);
      this.method_2321(matrices, x, y + k, k, j - k - k, n, o + k, l, m - k - k);
      this.method_2321(matrices, x + k, y + k, i - k - k, j - k - k, n + k, o + k, l - k - k, m - k - k);
      this.method_2321(matrices, x + i - k, y + k, k, j - k - k, n + l - k, o + k, l, m - k - k);
   }

   protected void method_2321(MatrixStack matrices, int x, int y, int i, int j, int k, int l, int m, int n) {
      for(int o = 0; o < i; o += m) {
         int p = x + o;
         int q = Math.min(m, i - o);

         for(int r = 0; r < j; r += n) {
            int s = y + r;
            int t = Math.min(n, j - r);
            this.drawTexture(matrices, p, s, k, l, q, t);
         }
      }

   }

   public boolean shouldRender(int originX, int originY, int mouseX, int mouseY) {
      if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
         int i = originX + this.x;
         int j = i + 26;
         int k = originY + this.y;
         int l = k + 26;
         return mouseX >= i && mouseX <= j && mouseY >= k && mouseY <= l;
      } else {
         return false;
      }
   }

   public void addToTree() {
      if (this.parent == null && this.advancement.getParent() != null) {
         this.parent = this.getParent(this.advancement);
         if (this.parent != null) {
            this.parent.addChild(this);
         }
      }

   }

   public int getY() {
      return this.y;
   }

   public int getX() {
      return this.x;
   }
}
