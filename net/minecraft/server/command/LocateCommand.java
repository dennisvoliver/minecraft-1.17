package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Iterator;
import java.util.Map.Entry;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.gen.feature.StructureFeature;

public class LocateCommand {
   private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.locate.failed"));

   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      LiteralArgumentBuilder<ServerCommandSource> literalArgumentBuilder = (LiteralArgumentBuilder)CommandManager.literal("locate").requires((source) -> {
         return source.hasPermissionLevel(2);
      });

      Entry entry;
      for(Iterator var2 = StructureFeature.STRUCTURES.entrySet().iterator(); var2.hasNext(); literalArgumentBuilder = (LiteralArgumentBuilder)literalArgumentBuilder.then(CommandManager.literal((String)entry.getKey()).executes((context) -> {
         return execute((ServerCommandSource)context.getSource(), (StructureFeature)entry.getValue());
      }))) {
         entry = (Entry)var2.next();
      }

      dispatcher.register(literalArgumentBuilder);
   }

   private static int execute(ServerCommandSource source, StructureFeature<?> structure) throws CommandSyntaxException {
      BlockPos blockPos = new BlockPos(source.getPosition());
      BlockPos blockPos2 = source.getWorld().locateStructure(structure, blockPos, 100, false);
      if (blockPos2 == null) {
         throw FAILED_EXCEPTION.create();
      } else {
         return sendCoordinates(source, structure.getName(), blockPos, blockPos2, "commands.locate.success");
      }
   }

   public static int sendCoordinates(ServerCommandSource source, String structure, BlockPos sourcePos, BlockPos structurePos, String successMessage) {
      int i = MathHelper.floor(getDistance(sourcePos.getX(), sourcePos.getZ(), structurePos.getX(), structurePos.getZ()));
      Text text = Texts.bracketed(new TranslatableText("chat.coordinates", new Object[]{structurePos.getX(), "~", structurePos.getZ()})).styled((style) -> {
         Style var10000 = style.withColor(Formatting.GREEN);
         ClickEvent.Action var10003 = ClickEvent.Action.SUGGEST_COMMAND;
         int var10004 = structurePos.getX();
         return var10000.withClickEvent(new ClickEvent(var10003, "/tp @s " + var10004 + " ~ " + structurePos.getZ())).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TranslatableText("chat.coordinates.tooltip")));
      });
      source.sendFeedback(new TranslatableText(successMessage, new Object[]{structure, text, i}), false);
      return i;
   }

   private static float getDistance(int x1, int y1, int x2, int y2) {
      int i = x2 - x1;
      int j = y2 - y1;
      return MathHelper.sqrt((float)(i * i + j * j));
   }
}
