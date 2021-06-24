package net.minecraft.text;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import net.minecraft.command.EntitySelector;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class SelectorText extends BaseText implements ParsableText {
   private static final Logger LOGGER = LogManager.getLogger();
   private final String pattern;
   @Nullable
   private final EntitySelector selector;
   protected final Optional<Text> separator;

   public SelectorText(String pattern, Optional<Text> separator) {
      this.pattern = pattern;
      this.separator = separator;
      EntitySelector entitySelector = null;

      try {
         EntitySelectorReader entitySelectorReader = new EntitySelectorReader(new StringReader(pattern));
         entitySelector = entitySelectorReader.read();
      } catch (CommandSyntaxException var5) {
         LOGGER.warn((String)"Invalid selector component: {}: {}", (Object)pattern, (Object)var5.getMessage());
      }

      this.selector = entitySelector;
   }

   public String getPattern() {
      return this.pattern;
   }

   @Nullable
   public EntitySelector getSelector() {
      return this.selector;
   }

   public Optional<Text> getSeparator() {
      return this.separator;
   }

   public MutableText parse(@Nullable ServerCommandSource source, @Nullable Entity sender, int depth) throws CommandSyntaxException {
      if (source != null && this.selector != null) {
         Optional<? extends Text> optional = Texts.parse(source, this.separator, sender, depth);
         return Texts.join(this.selector.getEntities(source), (Optional)optional, Entity::getDisplayName);
      } else {
         return new LiteralText("");
      }
   }

   public String asString() {
      return this.pattern;
   }

   public SelectorText copy() {
      return new SelectorText(this.pattern, this.separator);
   }

   public boolean equals(Object object) {
      if (this == object) {
         return true;
      } else if (!(object instanceof SelectorText)) {
         return false;
      } else {
         SelectorText selectorText = (SelectorText)object;
         return this.pattern.equals(selectorText.pattern) && super.equals(object);
      }
   }

   public String toString() {
      String var10000 = this.pattern;
      return "SelectorComponent{pattern='" + var10000 + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
   }
}
