package net.minecraft.client.font;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public interface MultilineText {
   MultilineText EMPTY = new MultilineText() {
      public int drawCenterWithShadow(MatrixStack matrices, int x, int y) {
         return y;
      }

      public int drawCenterWithShadow(MatrixStack matrices, int x, int y, int lineHeight, int color) {
         return y;
      }

      public int drawWithShadow(MatrixStack matrices, int x, int y, int lineHeight, int color) {
         return y;
      }

      public int draw(MatrixStack matrices, int x, int y, int lineHeight, int color) {
         return y;
      }

      public int count() {
         return 0;
      }
   };

   static MultilineText create(TextRenderer renderer, StringVisitable text, int width) {
      return create(renderer, (List)renderer.wrapLines(text, width).stream().map((textx) -> {
         return new MultilineText.Line(textx, renderer.getWidth(textx));
      }).collect(ImmutableList.toImmutableList()));
   }

   static MultilineText create(TextRenderer renderer, StringVisitable text, int width, int maxLines) {
      return create(renderer, (List)renderer.wrapLines(text, width).stream().limit((long)maxLines).map((textx) -> {
         return new MultilineText.Line(textx, renderer.getWidth(textx));
      }).collect(ImmutableList.toImmutableList()));
   }

   static MultilineText create(TextRenderer renderer, Text... texts) {
      return create(renderer, (List)Arrays.stream(texts).map(Text::asOrderedText).map((text) -> {
         return new MultilineText.Line(text, renderer.getWidth(text));
      }).collect(ImmutableList.toImmutableList()));
   }

   static MultilineText method_35726(TextRenderer renderer, List<Text> texts) {
      return create(renderer, (List)texts.stream().map(Text::asOrderedText).map((text) -> {
         return new MultilineText.Line(text, renderer.getWidth(text));
      }).collect(ImmutableList.toImmutableList()));
   }

   static MultilineText create(final TextRenderer renderer, final List<MultilineText.Line> lines) {
      return lines.isEmpty() ? EMPTY : new MultilineText() {
         public int drawCenterWithShadow(MatrixStack matrices, int x, int y) {
            Objects.requireNonNull(renderer);
            return this.drawCenterWithShadow(matrices, x, y, 9, 16777215);
         }

         public int drawCenterWithShadow(MatrixStack matrices, int x, int y, int lineHeight, int color) {
            int i = y;

            for(Iterator var7 = lines.iterator(); var7.hasNext(); i += lineHeight) {
               MultilineText.Line line = (MultilineText.Line)var7.next();
               renderer.drawWithShadow(matrices, line.text, (float)(x - line.width / 2), (float)i, color);
            }

            return i;
         }

         public int drawWithShadow(MatrixStack matrices, int x, int y, int lineHeight, int color) {
            int i = y;

            for(Iterator var7 = lines.iterator(); var7.hasNext(); i += lineHeight) {
               MultilineText.Line line = (MultilineText.Line)var7.next();
               renderer.drawWithShadow(matrices, line.text, (float)x, (float)i, color);
            }

            return i;
         }

         public int draw(MatrixStack matrices, int x, int y, int lineHeight, int color) {
            int i = y;

            for(Iterator var7 = lines.iterator(); var7.hasNext(); i += lineHeight) {
               MultilineText.Line line = (MultilineText.Line)var7.next();
               renderer.draw(matrices, line.text, (float)x, (float)i, color);
            }

            return i;
         }

         public int count() {
            return lines.size();
         }
      };
   }

   int drawCenterWithShadow(MatrixStack matrices, int x, int y);

   int drawCenterWithShadow(MatrixStack matrices, int x, int y, int lineHeight, int color);

   int drawWithShadow(MatrixStack matrices, int x, int y, int lineHeight, int color);

   int draw(MatrixStack matrices, int x, int y, int lineHeight, int color);

   int count();

   @Environment(EnvType.CLIENT)
   public static class Line {
      final OrderedText text;
      final int width;

      Line(OrderedText text, int width) {
         this.text = text;
         this.width = width;
      }
   }
}
