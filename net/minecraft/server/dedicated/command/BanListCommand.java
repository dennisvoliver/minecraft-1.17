package net.minecraft.server.dedicated.command;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Collection;
import java.util.Iterator;
import net.minecraft.server.BanEntry;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;

public class BanListCommand {
   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("banlist").requires((source) -> {
         return source.hasPermissionLevel(3);
      })).executes((context) -> {
         PlayerManager playerManager = ((ServerCommandSource)context.getSource()).getMinecraftServer().getPlayerManager();
         return execute((ServerCommandSource)context.getSource(), Lists.newArrayList(Iterables.concat(playerManager.getUserBanList().values(), playerManager.getIpBanList().values())));
      })).then(CommandManager.literal("ips").executes((context) -> {
         return execute((ServerCommandSource)context.getSource(), ((ServerCommandSource)context.getSource()).getMinecraftServer().getPlayerManager().getIpBanList().values());
      }))).then(CommandManager.literal("players").executes((context) -> {
         return execute((ServerCommandSource)context.getSource(), ((ServerCommandSource)context.getSource()).getMinecraftServer().getPlayerManager().getUserBanList().values());
      })));
   }

   private static int execute(ServerCommandSource source, Collection<? extends BanEntry<?>> targets) {
      if (targets.isEmpty()) {
         source.sendFeedback(new TranslatableText("commands.banlist.none"), false);
      } else {
         source.sendFeedback(new TranslatableText("commands.banlist.list", new Object[]{targets.size()}), false);
         Iterator var2 = targets.iterator();

         while(var2.hasNext()) {
            BanEntry<?> banEntry = (BanEntry)var2.next();
            source.sendFeedback(new TranslatableText("commands.banlist.entry", new Object[]{banEntry.toText(), banEntry.getSource(), banEntry.getReason()}), false);
         }
      }

      return targets.size();
   }
}
