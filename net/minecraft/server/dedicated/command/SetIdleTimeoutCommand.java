package net.minecraft.server.dedicated.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;

public class SetIdleTimeoutCommand {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("setidletimeout").requires((source) -> {
         return source.hasPermissionLevel(3);
      })).then(CommandManager.argument("minutes", IntegerArgumentType.integer(0)).executes((context) -> {
         return execute((ServerCommandSource)context.getSource(), IntegerArgumentType.getInteger(context, "minutes"));
      })));
   }

   private static int execute(ServerCommandSource source, int minutes) {
      source.getMinecraftServer().setPlayerIdleTimeout(minutes);
      source.sendFeedback(new TranslatableText("commands.setidletimeout.success", new Object[]{minutes}), true);
      return minutes;
   }
}
