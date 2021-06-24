package net.minecraft.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColumnPos;

public class ColumnPosArgumentType implements ArgumentType<PosArgument> {
   private static final Collection<String> EXAMPLES = Arrays.asList("0 0", "~ ~", "~1 ~-2", "^ ^", "^-1 ^0");
   public static final SimpleCommandExceptionType INCOMPLETE_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("argument.pos2d.incomplete"));

   public static ColumnPosArgumentType columnPos() {
      return new ColumnPosArgumentType();
   }

   public static ColumnPos getColumnPos(CommandContext<ServerCommandSource> context, String name) {
      BlockPos blockPos = ((PosArgument)context.getArgument(name, PosArgument.class)).toAbsoluteBlockPos((ServerCommandSource)context.getSource());
      return new ColumnPos(blockPos.getX(), blockPos.getZ());
   }

   public PosArgument parse(StringReader stringReader) throws CommandSyntaxException {
      int i = stringReader.getCursor();
      if (!stringReader.canRead()) {
         throw INCOMPLETE_EXCEPTION.createWithContext(stringReader);
      } else {
         CoordinateArgument coordinateArgument = CoordinateArgument.parse(stringReader);
         if (stringReader.canRead() && stringReader.peek() == ' ') {
            stringReader.skip();
            CoordinateArgument coordinateArgument2 = CoordinateArgument.parse(stringReader);
            return new DefaultPosArgument(coordinateArgument, new CoordinateArgument(true, 0.0D), coordinateArgument2);
         } else {
            stringReader.setCursor(i);
            throw INCOMPLETE_EXCEPTION.createWithContext(stringReader);
         }
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
      if (!(context.getSource() instanceof CommandSource)) {
         return Suggestions.empty();
      } else {
         String string = builder.getRemaining();
         Object collection2;
         if (!string.isEmpty() && string.charAt(0) == '^') {
            collection2 = Collections.singleton(CommandSource.RelativePosition.ZERO_LOCAL);
         } else {
            collection2 = ((CommandSource)context.getSource()).getBlockPositionSuggestions();
         }

         return CommandSource.suggestColumnPositions(string, (Collection)collection2, builder, CommandManager.getCommandValidator(this::parse));
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}