package net.minecraft.client.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

@Environment(EnvType.CLIENT)
public class FontStorage implements AutoCloseable {
   private static final EmptyGlyphRenderer EMPTY_GLYPH_RENDERER = new EmptyGlyphRenderer();
   private static final Glyph SPACE = () -> {
      return 4.0F;
   };
   private static final Random RANDOM = new Random();
   private final TextureManager textureManager;
   private final Identifier id;
   private GlyphRenderer blankGlyphRenderer;
   private GlyphRenderer whiteRectangleGlyphRenderer;
   private final List<Font> fonts = Lists.newArrayList();
   private final Int2ObjectMap<GlyphRenderer> glyphRendererCache = new Int2ObjectOpenHashMap();
   private final Int2ObjectMap<Glyph> glyphCache = new Int2ObjectOpenHashMap();
   private final Int2ObjectMap<IntList> charactersByWidth = new Int2ObjectOpenHashMap();
   private final List<GlyphAtlasTexture> glyphAtlases = Lists.newArrayList();

   public FontStorage(TextureManager textureManager, Identifier id) {
      this.textureManager = textureManager;
      this.id = id;
   }

   public void setFonts(List<Font> fonts) {
      this.closeFonts();
      this.closeGlyphAtlases();
      this.glyphRendererCache.clear();
      this.glyphCache.clear();
      this.charactersByWidth.clear();
      this.blankGlyphRenderer = this.getGlyphRenderer(BlankGlyph.INSTANCE);
      this.whiteRectangleGlyphRenderer = this.getGlyphRenderer(WhiteRectangleGlyph.INSTANCE);
      IntSet intSet = new IntOpenHashSet();
      Iterator var3 = fonts.iterator();

      while(var3.hasNext()) {
         Font font = (Font)var3.next();
         intSet.addAll(font.getProvidedGlyphs());
      }

      Set<Font> set = Sets.newHashSet();
      intSet.forEach((codePoint) -> {
         Iterator var4 = fonts.iterator();

         while(var4.hasNext()) {
            Font font = (Font)var4.next();
            Glyph glyph = codePoint == 32 ? SPACE : font.getGlyph(codePoint);
            if (glyph != null) {
               set.add(font);
               if (glyph != BlankGlyph.INSTANCE) {
                  ((IntList)this.charactersByWidth.computeIfAbsent(MathHelper.ceil(((Glyph)glyph).getAdvance(false)), (i) -> {
                     return new IntArrayList();
                  })).add(codePoint);
               }
               break;
            }
         }

      });
      Stream var10000 = fonts.stream();
      Objects.requireNonNull(set);
      var10000 = var10000.filter(set::contains);
      List var10001 = this.fonts;
      Objects.requireNonNull(var10001);
      var10000.forEach(var10001::add);
   }

   public void close() {
      this.closeFonts();
      this.closeGlyphAtlases();
   }

   private void closeFonts() {
      Iterator var1 = this.fonts.iterator();

      while(var1.hasNext()) {
         Font font = (Font)var1.next();
         font.close();
      }

      this.fonts.clear();
   }

   private void closeGlyphAtlases() {
      Iterator var1 = this.glyphAtlases.iterator();

      while(var1.hasNext()) {
         GlyphAtlasTexture glyphAtlasTexture = (GlyphAtlasTexture)var1.next();
         glyphAtlasTexture.close();
      }

      this.glyphAtlases.clear();
   }

   public Glyph getGlyph(int codePoint) {
      return (Glyph)this.glyphCache.computeIfAbsent(codePoint, (codePointx) -> {
         return (Glyph)(codePointx == 32 ? SPACE : this.getRenderableGlyph(codePointx));
      });
   }

   private RenderableGlyph getRenderableGlyph(int codePoint) {
      Iterator var2 = this.fonts.iterator();

      RenderableGlyph renderableGlyph;
      do {
         if (!var2.hasNext()) {
            return BlankGlyph.INSTANCE;
         }

         Font font = (Font)var2.next();
         renderableGlyph = font.getGlyph(codePoint);
      } while(renderableGlyph == null);

      return renderableGlyph;
   }

   public GlyphRenderer getGlyphRenderer(int i) {
      return (GlyphRenderer)this.glyphRendererCache.computeIfAbsent(i, (ix) -> {
         return (GlyphRenderer)(ix == 32 ? EMPTY_GLYPH_RENDERER : this.getGlyphRenderer(this.getRenderableGlyph(ix)));
      });
   }

   private GlyphRenderer getGlyphRenderer(RenderableGlyph c) {
      Iterator var2 = this.glyphAtlases.iterator();

      GlyphRenderer glyphRenderer;
      do {
         if (!var2.hasNext()) {
            String var10004 = this.id.getNamespace();
            String var10005 = this.id.getPath();
            GlyphAtlasTexture glyphAtlasTexture2 = new GlyphAtlasTexture(new Identifier(var10004, var10005 + "/" + this.glyphAtlases.size()), c.hasColor());
            this.glyphAtlases.add(glyphAtlasTexture2);
            this.textureManager.registerTexture(glyphAtlasTexture2.getId(), glyphAtlasTexture2);
            GlyphRenderer glyphRenderer2 = glyphAtlasTexture2.getGlyphRenderer(c);
            return glyphRenderer2 == null ? this.blankGlyphRenderer : glyphRenderer2;
         }

         GlyphAtlasTexture glyphAtlasTexture = (GlyphAtlasTexture)var2.next();
         glyphRenderer = glyphAtlasTexture.getGlyphRenderer(c);
      } while(glyphRenderer == null);

      return glyphRenderer;
   }

   public GlyphRenderer getObfuscatedGlyphRenderer(Glyph glyph) {
      IntList intList = (IntList)this.charactersByWidth.get(MathHelper.ceil(glyph.getAdvance(false)));
      return intList != null && !intList.isEmpty() ? this.getGlyphRenderer(intList.getInt(RANDOM.nextInt(intList.size()))) : this.blankGlyphRenderer;
   }

   public GlyphRenderer getRectangleRenderer() {
      return this.whiteRectangleGlyphRenderer;
   }
}
