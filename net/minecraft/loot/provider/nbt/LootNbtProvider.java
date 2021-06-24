package net.minecraft.loot.provider.nbt;

import java.util.Set;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.nbt.NbtElement;
import org.jetbrains.annotations.Nullable;

public interface LootNbtProvider {
   @Nullable
   NbtElement getNbtTag(LootContext context);

   Set<LootContextParameter<?>> getRequiredParameters();

   LootNbtProviderType getType();
}