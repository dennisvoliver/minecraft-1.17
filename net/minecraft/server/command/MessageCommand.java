package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

public class MessageCommand {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      LiteralCommandNode<ServerCommandSource> literalCommandNode = dispatcher.register((LiteralArgumentBuilder)CommandManager.literal("msg").then(CommandManager.argument("targets", EntityArgumentType.players()).then(CommandManager.argument("message", MessageArgumentType.message()).executes((context) -> {
         return execute((ServerCommandSource)context.getSource(), EntityArgumentType.getPlayers(context, "targets"), MessageArgumentType.getMessage(context, "message"));
      }))));
      dispatcher.register((LiteralArgumentBuilder)CommandManager.literal("tell").redirect(literalCommandNode));
      dispatcher.register((LiteralArgumentBuilder)CommandManager.literal("w").redirect(literalCommandNode));
   }

   private static int execute(ServerCommandSource source, Collection<ServerPlayerEntity> targets, Text message) {
      UUID uUID = source.getEntity() == null ? Util.NIL_UUID : source.getEntity().getUuid();
      Entity entity = source.getEntity();
      Consumer consumer2;
      if (entity instanceof ServerPlayerEntity) {
         ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)entity;
         consumer2 = (playerName) -> {
            serverPlayerEntity.sendSystemMessage((new TranslatableText("commands.message.display.outgoing", new Object[]{playerName, message})).formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC}), serverPlayerEntity.getUuid());
         };
      } else {
         consumer2 = (playerName) -> {
            source.sendFeedback((new TranslatableText("commands.message.display.outgoing", new Object[]{playerName, message})).formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC}), false);
         };
      }

      Iterator var8 = targets.iterator();

      while(var8.hasNext()) {
         ServerPlayerEntity serverPlayerEntity2 = (ServerPlayerEntity)var8.next();
         consumer2.accept(serverPlayerEntity2.getDisplayName());
         serverPlayerEntity2.sendSystemMessage((new TranslatableText("commands.message.display.incoming", new Object[]{source.getDisplayName(), message})).formatted(new Formatting[]{Formatting.GRAY, Formatting.ITALIC}), uUID);
      }

      return targets.size();
   }
}
