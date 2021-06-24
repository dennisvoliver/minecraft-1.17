package net.minecraft.network.packet.s2c.play;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.util.Hand;

public class OpenWrittenBookS2CPacket implements Packet<ClientPlayPacketListener> {
   private final Hand hand;

   public OpenWrittenBookS2CPacket(Hand hand) {
      this.hand = hand;
   }

   public OpenWrittenBookS2CPacket(PacketByteBuf buf) {
      this.hand = (Hand)buf.readEnumConstant(Hand.class);
   }

   public void write(PacketByteBuf buf) {
      buf.writeEnumConstant(this.hand);
   }

   public void apply(ClientPlayPacketListener clientPlayPacketListener) {
      clientPlayPacketListener.onOpenWrittenBook(this);
   }

   public Hand getHand() {
      return this.hand;
   }
}
