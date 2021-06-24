package net.minecraft.world.chunk;

import java.util.function.Predicate;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.collection.IdList;

public class IdListPalette<T> implements Palette<T> {
   private final IdList<T> idList;
   private final T defaultValue;

   public IdListPalette(IdList<T> idList, T defaultValue) {
      this.idList = idList;
      this.defaultValue = defaultValue;
   }

   public int getIndex(T object) {
      int i = this.idList.getRawId(object);
      return i == -1 ? 0 : i;
   }

   public boolean accepts(Predicate<T> predicate) {
      return true;
   }

   public T getByIndex(int index) {
      T object = this.idList.get(index);
      return object == null ? this.defaultValue : object;
   }

   public void fromPacket(PacketByteBuf buf) {
   }

   public void toPacket(PacketByteBuf buf) {
   }

   public int getPacketSize() {
      return PacketByteBuf.getVarIntLength(0);
   }

   public int getIndexBits() {
      return this.idList.size();
   }

   public void readNbt(NbtList nbt) {
   }
}
