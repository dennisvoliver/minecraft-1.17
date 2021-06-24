package net.minecraft.world.chunk;

import java.util.function.Predicate;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import org.jetbrains.annotations.Nullable;

public interface Palette<T> {
   int getIndex(T object);

   boolean accepts(Predicate<T> predicate);

   @Nullable
   T getByIndex(int index);

   void fromPacket(PacketByteBuf buf);

   void toPacket(PacketByteBuf buf);

   int getPacketSize();

   int getIndexBits();

   void readNbt(NbtList nbt);
}
