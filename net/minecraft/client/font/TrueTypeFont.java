package net.minecraft.client.font;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Objects;
import java.util.stream.IntStream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@Environment(EnvType.CLIENT)
public class TrueTypeFont implements Font {
   private final ByteBuffer buffer;
   final STBTTFontinfo info;
   final float oversample;
   private final IntSet excludedCharacters = new IntArraySet();
   final float shiftX;
   final float shiftY;
   final float scaleFactor;
   final float ascent;

   public TrueTypeFont(ByteBuffer buffer, STBTTFontinfo info, float f, float oversample, float g, float h, String excludedCharacters) {
      this.buffer = buffer;
      this.info = info;
      this.oversample = oversample;
      IntStream var10000 = excludedCharacters.codePoints();
      IntSet var10001 = this.excludedCharacters;
      Objects.requireNonNull(var10001);
      var10000.forEach(var10001::add);
      this.shiftX = g * oversample;
      this.shiftY = h * oversample;
      this.scaleFactor = STBTruetype.stbtt_ScaleForPixelHeight(info, f * oversample);
      MemoryStack memoryStack = MemoryStack.stackPush();

      try {
         IntBuffer intBuffer = memoryStack.mallocInt(1);
         IntBuffer intBuffer2 = memoryStack.mallocInt(1);
         IntBuffer intBuffer3 = memoryStack.mallocInt(1);
         STBTruetype.stbtt_GetFontVMetrics(info, intBuffer, intBuffer2, intBuffer3);
         this.ascent = (float)intBuffer.get(0) * this.scaleFactor;
      } catch (Throwable var13) {
         if (memoryStack != null) {
            try {
               memoryStack.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }
         }

         throw var13;
      }

      if (memoryStack != null) {
         memoryStack.close();
      }

   }

   @Nullable
   public TrueTypeFont.TtfGlyph getGlyph(int i) {
      if (this.excludedCharacters.contains(i)) {
         return null;
      } else {
         MemoryStack memoryStack = MemoryStack.stackPush();

         Object var15;
         label61: {
            TrueTypeFont.TtfGlyph var12;
            label62: {
               IntBuffer intBuffer5;
               try {
                  IntBuffer intBuffer = memoryStack.mallocInt(1);
                  IntBuffer intBuffer2 = memoryStack.mallocInt(1);
                  IntBuffer intBuffer3 = memoryStack.mallocInt(1);
                  IntBuffer intBuffer4 = memoryStack.mallocInt(1);
                  int j = STBTruetype.stbtt_FindGlyphIndex(this.info, i);
                  if (j == 0) {
                     var15 = null;
                     break label61;
                  }

                  STBTruetype.stbtt_GetGlyphBitmapBoxSubpixel(this.info, j, this.scaleFactor, this.scaleFactor, this.shiftX, this.shiftY, intBuffer, intBuffer2, intBuffer3, intBuffer4);
                  int k = intBuffer3.get(0) - intBuffer.get(0);
                  int l = intBuffer4.get(0) - intBuffer2.get(0);
                  if (k > 0 && l > 0) {
                     intBuffer5 = memoryStack.mallocInt(1);
                     IntBuffer intBuffer6 = memoryStack.mallocInt(1);
                     STBTruetype.stbtt_GetGlyphHMetrics(this.info, j, intBuffer5, intBuffer6);
                     var12 = new TrueTypeFont.TtfGlyph(intBuffer.get(0), intBuffer3.get(0), -intBuffer2.get(0), -intBuffer4.get(0), (float)intBuffer5.get(0) * this.scaleFactor, (float)intBuffer6.get(0) * this.scaleFactor, j);
                     break label62;
                  }

                  intBuffer5 = null;
               } catch (Throwable var14) {
                  if (memoryStack != null) {
                     try {
                        memoryStack.close();
                     } catch (Throwable var13) {
                        var14.addSuppressed(var13);
                     }
                  }

                  throw var14;
               }

               if (memoryStack != null) {
                  memoryStack.close();
               }

               return intBuffer5;
            }

            if (memoryStack != null) {
               memoryStack.close();
            }

            return var12;
         }

         if (memoryStack != null) {
            memoryStack.close();
         }

         return (TrueTypeFont.TtfGlyph)var15;
      }
   }

   public void close() {
      this.info.free();
      MemoryUtil.memFree((Buffer)this.buffer);
   }

   public IntSet getProvidedGlyphs() {
      return (IntSet)IntStream.range(0, 65535).filter((codePoint) -> {
         return !this.excludedCharacters.contains(codePoint);
      }).collect(IntOpenHashSet::new, IntCollection::add, IntCollection::addAll);
   }

   @Environment(EnvType.CLIENT)
   private class TtfGlyph implements RenderableGlyph {
      private final int width;
      private final int height;
      private final float bearingX;
      private final float ascent;
      private final float advance;
      private final int glyphIndex;

      TtfGlyph(int i, int j, int k, int l, float f, float g, int m) {
         this.width = j - i;
         this.height = k - l;
         this.advance = f / TrueTypeFont.this.oversample;
         this.bearingX = (g + (float)i + TrueTypeFont.this.shiftX) / TrueTypeFont.this.oversample;
         this.ascent = (TrueTypeFont.this.ascent - (float)k + TrueTypeFont.this.shiftY) / TrueTypeFont.this.oversample;
         this.glyphIndex = m;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }

      public float getOversample() {
         return TrueTypeFont.this.oversample;
      }

      public float getAdvance() {
         return this.advance;
      }

      public float getBearingX() {
         return this.bearingX;
      }

      public float getAscent() {
         return this.ascent;
      }

      public void upload(int x, int y) {
         NativeImage nativeImage = new NativeImage(NativeImage.Format.LUMINANCE, this.width, this.height, false);
         nativeImage.makeGlyphBitmapSubpixel(TrueTypeFont.this.info, this.glyphIndex, this.width, this.height, TrueTypeFont.this.scaleFactor, TrueTypeFont.this.scaleFactor, TrueTypeFont.this.shiftX, TrueTypeFont.this.shiftY, 0, 0);
         nativeImage.upload(0, x, y, 0, 0, this.width, this.height, false, true);
      }

      public boolean hasColor() {
         return false;
      }
   }
}
