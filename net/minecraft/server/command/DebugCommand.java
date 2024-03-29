package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.command.argument.CommandFunctionArgumentType;
import net.minecraft.entity.ai.Durations;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.function.CommandFunctionManager;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.profiler.ProfileResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DebugCommand {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final SimpleCommandExceptionType NOT_RUNNING_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.debug.notRunning"));
   private static final SimpleCommandExceptionType ALREADY_RUNNING_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.debug.alreadyRunning"));

   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("debug").requires((source) -> {
         return source.hasPermissionLevel(3);
      })).then(CommandManager.literal("start").executes((context) -> {
         return executeStart((ServerCommandSource)context.getSource());
      }))).then(CommandManager.literal("stop").executes((context) -> {
         return executeStop((ServerCommandSource)context.getSource());
      }))).then(((LiteralArgumentBuilder)CommandManager.literal("function").requires((serverCommandSource) -> {
         return serverCommandSource.hasPermissionLevel(3);
      })).then(CommandManager.argument("name", CommandFunctionArgumentType.commandFunction()).suggests(FunctionCommand.SUGGESTION_PROVIDER).executes((context) -> {
         return executeFunction((ServerCommandSource)context.getSource(), CommandFunctionArgumentType.getFunctions(context, "name"));
      }))));
   }

   private static int executeStart(ServerCommandSource source) throws CommandSyntaxException {
      MinecraftServer minecraftServer = source.getMinecraftServer();
      if (minecraftServer.isDebugRunning()) {
         throw ALREADY_RUNNING_EXCEPTION.create();
      } else {
         minecraftServer.enableProfiler();
         source.sendFeedback(new TranslatableText("commands.debug.started"), true);
         return 0;
      }
   }

   private static int executeStop(ServerCommandSource source) throws CommandSyntaxException {
      MinecraftServer minecraftServer = source.getMinecraftServer();
      if (!minecraftServer.isDebugRunning()) {
         throw NOT_RUNNING_EXCEPTION.create();
      } else {
         ProfileResult profileResult = minecraftServer.stopDebug();
         double d = (double)profileResult.getTimeSpan() / (double)Durations.field_33868;
         double e = (double)profileResult.getTickSpan() / d;
         source.sendFeedback(new TranslatableText("commands.debug.stopped", new Object[]{String.format(Locale.ROOT, "%.2f", d), profileResult.getTickSpan(), String.format("%.2f", e)}), true);
         return (int)e;
      }
   }

   private static int executeFunction(ServerCommandSource source, Collection<CommandFunction> functions) {
      int i = 0;
      MinecraftServer minecraftServer = source.getMinecraftServer();
      SimpleDateFormat var10000 = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
      Date var10001 = new Date();
      String string = "debug-trace-" + var10000.format(var10001) + ".txt";

      try {
         Path path = minecraftServer.getFile("debug").toPath();
         Files.createDirectories(path);
         BufferedWriter writer = Files.newBufferedWriter(path.resolve(string), StandardCharsets.UTF_8);

         try {
            PrintWriter printWriter = new PrintWriter(writer);

            CommandFunction commandFunction;
            DebugCommand.Tracer tracer;
            for(Iterator var8 = functions.iterator(); var8.hasNext(); i += source.getMinecraftServer().getCommandFunctionManager().execute(commandFunction, source.withOutput(tracer).withMaxLevel(2), tracer)) {
               commandFunction = (CommandFunction)var8.next();
               printWriter.println(commandFunction.getId());
               tracer = new DebugCommand.Tracer(printWriter);
            }
         } catch (Throwable var12) {
            if (writer != null) {
               try {
                  writer.close();
               } catch (Throwable var11) {
                  var12.addSuppressed(var11);
               }
            }

            throw var12;
         }

         if (writer != null) {
            writer.close();
         }
      } catch (IOException | UncheckedIOException var13) {
         LOGGER.warn((String)"Tracing failed", (Throwable)var13);
         source.sendError(new TranslatableText("commands.debug.function.traceFailed"));
      }

      if (functions.size() == 1) {
         source.sendFeedback(new TranslatableText("commands.debug.function.success.single", new Object[]{i, ((CommandFunction)functions.iterator().next()).getId(), string}), true);
      } else {
         source.sendFeedback(new TranslatableText("commands.debug.function.success.multiple", new Object[]{i, functions.size(), string}), true);
      }

      return i;
   }

   private static class Tracer implements CommandOutput, CommandFunctionManager.Tracer {
      public static final int MARGIN = 1;
      private final PrintWriter writer;
      private int lastIndentWidth;
      private boolean expectsCommandResult;

      Tracer(PrintWriter writer) {
         this.writer = writer;
      }

      private void writeIndent(int width) {
         this.writeIndentWithoutRememberingWidth(width);
         this.lastIndentWidth = width;
      }

      private void writeIndentWithoutRememberingWidth(int width) {
         for(int i = 0; i < width + 1; ++i) {
            this.writer.write("    ");
         }

      }

      private void writeNewLine() {
         if (this.expectsCommandResult) {
            this.writer.println();
            this.expectsCommandResult = false;
         }

      }

      public void traceCommandStart(int depth, String command) {
         this.writeNewLine();
         this.writeIndent(depth);
         this.writer.print("[C] ");
         this.writer.print(command);
         this.expectsCommandResult = true;
      }

      public void traceCommandEnd(int depth, String command, int result) {
         if (this.expectsCommandResult) {
            this.writer.print(" -> ");
            this.writer.println(result);
            this.expectsCommandResult = false;
         } else {
            this.writeIndent(depth);
            this.writer.print("[R = ");
            this.writer.print(result);
            this.writer.print("] ");
            this.writer.println(command);
         }

      }

      public void traceFunctionCall(int depth, Identifier function, int size) {
         this.writeNewLine();
         this.writeIndent(depth);
         this.writer.print("[F] ");
         this.writer.print(function);
         this.writer.print(" size=");
         this.writer.println(size);
      }

      public void traceError(int depth, String message) {
         this.writeNewLine();
         this.writeIndent(depth + 1);
         this.writer.print("[E] ");
         this.writer.print(message);
      }

      public void sendSystemMessage(Text message, UUID sender) {
         this.writeNewLine();
         this.writeIndentWithoutRememberingWidth(this.lastIndentWidth + 1);
         this.writer.print("[M] ");
         if (sender != Util.NIL_UUID) {
            this.writer.print(sender);
            this.writer.print(": ");
         }

         this.writer.println(message.getString());
      }

      public boolean shouldReceiveFeedback() {
         return true;
      }

      public boolean shouldTrackOutput() {
         return true;
      }

      public boolean shouldBroadcastConsoleToOps() {
         return false;
      }

      public boolean cannotBeSilenced() {
         return true;
      }
   }
}
