package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.entity.Entity;
import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Util;

public class MeCommand {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)CommandManager.literal("me").then(CommandManager.argument("action", StringArgumentType.greedyString()).executes((context) -> {
         String string = StringArgumentType.getString(context, "action");
         Entity entity = ((ServerCommandSource)context.getSource()).getEntity();
         MinecraftServer minecraftServer = ((ServerCommandSource)context.getSource()).getMinecraftServer();
         if (entity != null) {
            if (entity instanceof ServerPlayerEntity) {
               ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
               serverPlayerEntity.getTextStream().filterText(string).thenAcceptAsync((message) -> {
                  String string = message.getFiltered();
                  Text text = string.isEmpty() ? null : getEmoteText(context, string);
                  Text text2 = getEmoteText(context, message.getRaw());
                  minecraftServer.getPlayerManager().broadcast(text2, (player) -> {
                     return serverPlayerEntity.shouldFilterMessagesSentTo(player) ? text : text2;
                  }, MessageType.CHAT, entity.getUuid());
               }, minecraftServer);
               return 1;
            }

            minecraftServer.getPlayerManager().broadcastChatMessage(getEmoteText(context, string), MessageType.CHAT, entity.getUuid());
         } else {
            minecraftServer.getPlayerManager().broadcastChatMessage(getEmoteText(context, string), MessageType.SYSTEM, Util.NIL_UUID);
         }

         return 1;
      })));
   }

   private static Text getEmoteText(CommandContext<ServerCommandSource> context, String arg) {
      return new TranslatableText("chat.type.emote", new Object[]{((ServerCommandSource)context.getSource()).getDisplayName(), arg});
   }
}
