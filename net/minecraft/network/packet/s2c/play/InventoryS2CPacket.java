package net.minecraft.network.packet.s2c.play;

import java.util.Iterator;
import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.util.collection.DefaultedList;

/**
 * Represents the contents of a block or entity inventory being synchronized
 * from the server to the client.
 */
public class InventoryS2CPacket implements Packet<ClientPlayPacketListener> {
   /**
    * The {@link net.minecraft.screen.ScreenHandler#syncId} of a screen handler.
    */
   private final int syncId;
   private final List<ItemStack> contents;

   public InventoryS2CPacket(int syncId, DefaultedList<ItemStack> contents) {
      this.syncId = syncId;
      this.contents = DefaultedList.ofSize(contents.size(), ItemStack.EMPTY);

      for(int i = 0; i < this.contents.size(); ++i) {
         this.contents.set(i, ((ItemStack)contents.get(i)).copy());
      }

   }

   public InventoryS2CPacket(PacketByteBuf buf) {
      this.syncId = buf.readUnsignedByte();
      int i = buf.readShort();
      this.contents = DefaultedList.ofSize(i, ItemStack.EMPTY);

      for(int j = 0; j < i; ++j) {
         this.contents.set(j, buf.readItemStack());
      }

   }

   public void write(PacketByteBuf buf) {
      buf.writeByte(this.syncId);
      buf.writeShort(this.contents.size());
      Iterator var2 = this.contents.iterator();

      while(var2.hasNext()) {
         ItemStack itemStack = (ItemStack)var2.next();
         buf.writeItemStack(itemStack);
      }

   }

   public void apply(ClientPlayPacketListener clientPlayPacketListener) {
      clientPlayPacketListener.onInventory(this);
   }

   public int getSyncId() {
      return this.syncId;
   }

   public List<ItemStack> getContents() {
      return this.contents;
   }
}
