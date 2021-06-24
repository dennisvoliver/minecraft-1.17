package net.minecraft.server.command;

import com.google.common.collect.Iterables;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

public class HelpCommand {
   private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.help.failed"));

   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("help").executes((context) -> {
         Map<CommandNode<ServerCommandSource>, String> map = dispatcher.getSmartUsage(dispatcher.getRoot(), (ServerCommandSource)context.getSource());
         Iterator var3 = map.values().iterator();

         while(var3.hasNext()) {
            String string = (String)var3.next();
            ((ServerCommandSource)context.getSource()).sendFeedback(new LiteralText("/" + string), false);
         }

         return map.size();
      })).then(CommandManager.argument("command", StringArgumentType.greedyString()).executes((context) -> {
         ParseResults<ServerCommandSource> parseResults = dispatcher.parse((String)StringArgumentType.getString(context, "command"), (ServerCommandSource)context.getSource());
         if (parseResults.getContext().getNodes().isEmpty()) {
            throw FAILED_EXCEPTION.create();
         } else {
            Map<CommandNode<ServerCommandSource>, String> map = dispatcher.getSmartUsage(((ParsedCommandNode)Iterables.getLast(parseResults.getContext().getNodes())).getNode(), (ServerCommandSource)context.getSource());
            Iterator var4 = map.values().iterator();

            while(var4.hasNext()) {
               String string = (String)var4.next();
               ServerCommandSource var10000 = (ServerCommandSource)context.getSource();
               String var10003 = parseResults.getReader().getString();
               var10000.sendFeedback(new LiteralText("/" + var10003 + " " + string), false);
            }

            return map.size();
         }
      })));
   }
}
