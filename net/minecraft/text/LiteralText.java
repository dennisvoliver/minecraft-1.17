package net.minecraft.text;

public class LiteralText extends BaseText {
   public static final Text EMPTY = new LiteralText("");
   private final String string;

   public LiteralText(String string) {
      this.string = string;
   }

   public String getRawString() {
      return this.string;
   }

   public String asString() {
      return this.string;
   }

   public LiteralText copy() {
      return new LiteralText(this.string);
   }

   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (!(object instanceof LiteralText)) {
         return false;
      } else {
         LiteralText literalText = (LiteralText)object;
         return this.string.equals(literalText.getRawString()) && super.equals(object);
      }
   }

   public String toString() {
      String var10000 = this.string;
      return "TextComponent{text='" + var10000 + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
   }
}
