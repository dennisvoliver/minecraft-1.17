package net.minecraft.client.gui.widget;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Narratable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

@Environment(EnvType.CLIENT)
public abstract class AlwaysSelectedEntryListWidget<E extends AlwaysSelectedEntryListWidget.Entry<E>> extends EntryListWidget<E> {
   private static final Text SELECTION_USAGE_TEXT = new TranslatableText("narration.selection.usage");
   private boolean inFocus;

   public AlwaysSelectedEntryListWidget(MinecraftClient minecraftClient, int i, int j, int k, int l, int m) {
      super(minecraftClient, i, j, k, l, m);
   }

   public boolean changeFocus(boolean lookForwards) {
      if (!this.inFocus && this.getEntryCount() == 0) {
         return false;
      } else {
         this.inFocus = !this.inFocus;
         if (this.inFocus && this.getSelected() == null && this.getEntryCount() > 0) {
            this.moveSelection(EntryListWidget.MoveDirection.DOWN);
         } else if (this.inFocus && this.getSelected() != null) {
            this.ensureSelectedEntryVisible();
         }

         return this.inFocus;
      }
   }

   public void appendNarrations(NarrationMessageBuilder builder) {
      E entry = (AlwaysSelectedEntryListWidget.Entry)this.method_37019();
      if (entry != null) {
         this.method_37017(builder.nextMessage(), entry);
         entry.appendNarrations(builder);
      } else {
         E entry2 = (AlwaysSelectedEntryListWidget.Entry)this.getSelected();
         if (entry2 != null) {
            this.method_37017(builder.nextMessage(), entry2);
            entry2.appendNarrations(builder);
         }
      }

      if (this.isFocused()) {
         builder.put(NarrationPart.USAGE, SELECTION_USAGE_TEXT);
      }

   }

   @Environment(EnvType.CLIENT)
   public abstract static class Entry<E extends AlwaysSelectedEntryListWidget.Entry<E>> extends EntryListWidget.Entry<E> implements Narratable {
      public boolean changeFocus(boolean lookForwards) {
         return false;
      }

      public abstract Text method_37006();

      public void appendNarrations(NarrationMessageBuilder builder) {
         builder.put(NarrationPart.TITLE, this.method_37006());
      }
   }
}
