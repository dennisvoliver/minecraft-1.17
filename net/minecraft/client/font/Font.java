package net.minecraft.client.font;

import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.Closeable;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public interface Font extends Closeable {
   default void close() {
   }

   @Nullable
   default RenderableGlyph getGlyph(int codePoint) {
      return null;
   }

   /**
    * {@return the set of code points for which this font can provide glyphs}
    */
   IntSet getProvidedGlyphs();
}
