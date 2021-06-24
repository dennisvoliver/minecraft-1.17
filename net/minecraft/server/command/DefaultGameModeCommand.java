package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Iterator;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.TranslatableText;
import net.minecraft.world.GameMode;

public class DefaultGameModeCommand {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = (LiteralArgumentBuilder)CommandManager.literal("defaultgamemode").requires((source) -> {
         return source.hasPermissionLevel(2);
      });
      GameMode[] var2 = GameMode.values();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         GameMode gameMode = var2[var4];
         literalArgumentBuilder.then(CommandManager.literal(gameMode.getName()).executes((context) -> {
            return execute((ServerCommandSource)context.getSource(), gameMode);
         }));
      }

      dispatcher.register(literalArgumentBuilder);
   }

   private static int execute(ServerCommandSource source, GameMode defaultGameMode) {
      int i = 0;
      MinecraftServer minecraftServer = source.getMinecraftServer();
      minecraftServer.setDefaultGameMode(defaultGameMode);
      GameMode gameMode = minecraftServer.getForcedGameMode();
      if (gameMode != null) {
         Iterator var5 = minecraftServer.getPlayerManager().getPlayerList().iterator();

         while(var5.hasNext()) {
            ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var5.next();
            if (serverPlayerEntity.changeGameMode(gameMode)) {
               ++i;
            }
         }
      }

      source.sendFeedback(new TranslatableText("commands.defaultgamemode.success", new Object[]{defaultGameMode.getTranslatableName()}), true);
      return i;
   }
}
