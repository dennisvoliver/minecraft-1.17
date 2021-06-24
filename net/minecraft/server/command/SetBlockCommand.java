package net.minecraft.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.pattern.CachedBlockPosition;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Clearable;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class SetBlockCommand {
   private static final SimpleCommandExceptionType FAILED_EXCEPTION = new SimpleCommandExceptionType(new TranslatableText("commands.setblock.failed"));

   public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
      dispatcher.register((LiteralArgumentBuilder)((LiteralArgumentBuilder)CommandManager.literal("setblock").requires((source) -> {
         return source.hasPermissionLevel(2);
      })).then(CommandManager.argument("pos", BlockPosArgumentType.blockPos()).then(((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)CommandManager.argument("block", BlockStateArgumentType.blockState()).executes((context) -> {
         return execute((ServerCommandSource)context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), SetBlockCommand.Mode.REPLACE, (Predicate)null);
      })).then(CommandManager.literal("destroy").executes((context) -> {
         return execute((ServerCommandSource)context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), SetBlockCommand.Mode.DESTROY, (Predicate)null);
      }))).then(CommandManager.literal("keep").executes((context) -> {
         return execute((ServerCommandSource)context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), SetBlockCommand.Mode.REPLACE, (pos) -> {
            return pos.getWorld().isAir(pos.getBlockPos());
         });
      }))).then(CommandManager.literal("replace").executes((context) -> {
         return execute((ServerCommandSource)context.getSource(), BlockPosArgumentType.getLoadedBlockPos(context, "pos"), BlockStateArgumentType.getBlockState(context, "block"), SetBlockCommand.Mode.REPLACE, (Predicate)null);
      })))));
   }

   private static int execute(ServerCommandSource source, BlockPos pos, BlockStateArgument block, SetBlockCommand.Mode mode, @Nullable Predicate<CachedBlockPosition> condition) throws CommandSyntaxException {
      ServerWorld serverWorld = source.getWorld();
      if (condition != null && !condition.test(new CachedBlockPosition(serverWorld, pos, true))) {
         throw FAILED_EXCEPTION.create();
      } else {
         boolean bl2;
         if (mode == SetBlockCommand.Mode.DESTROY) {
            serverWorld.breakBlock(pos, true);
            bl2 = !block.getBlockState().isAir() || !serverWorld.getBlockState(pos).isAir();
         } else {
            BlockEntity blockEntity = serverWorld.getBlockEntity(pos);
            Clearable.clear(blockEntity);
            bl2 = true;
         }

         if (bl2 && !block.setBlockState(serverWorld, pos, Block.NOTIFY_LISTENERS)) {
            throw FAILED_EXCEPTION.create();
         } else {
            serverWorld.updateNeighbors(pos, block.getBlockState().getBlock());
            source.sendFeedback(new TranslatableText("commands.setblock.success", new Object[]{pos.getX(), pos.getY(), pos.getZ()}), true);
            return 1;
         }
      }
   }

   public static enum Mode {
      REPLACE,
      DESTROY;
   }

   public interface Filter {
      @Nullable
      BlockStateArgument filter(BlockBox box, BlockPos pos, BlockStateArgument block, ServerWorld world);
   }
}
