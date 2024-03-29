package net.minecraft.text;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class KeybindText extends BaseText {
   private static Function<String, Supplier<Text>> translator = (key) -> {
      return () -> {
         return new LiteralText(key);
      };
   };
   private final String key;
   private Supplier<Text> translated;

   public KeybindText(String key) {
      this.key = key;
   }

   public static void setTranslator(Function<String, Supplier<Text>> translator) {
      KeybindText.translator = translator;
   }

   private Text getTranslated() {
      if (this.translated == null) {
         this.translated = (Supplier)translator.apply(this.key);
      }

      return (Text)this.translated.get();
   }

   public <T> Optional<T> visitSelf(StringVisitable.Visitor<T> visitor) {
      return this.getTranslated().visit(visitor);
   }

   public <T> Optional<T> visitSelf(StringVisitable.StyledVisitor<T> visitor, Style style) {
      return this.getTranslated().visit(visitor, style);
   }

   public KeybindText copy() {
      return new KeybindText(this.key);
   }

   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (!(object instanceof KeybindText)) {
         return false;
      } else {
         KeybindText keybindText = (KeybindText)object;
         return this.key.equals(keybindText.key) && super.equals(object);
      }
   }

   public String toString() {
      String var10000 = this.key;
      return "KeybindComponent{keybind='" + var10000 + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
   }

   public String getKey() {
      return this.key;
   }
}
