package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class DecoderHandler extends ByteToMessageDecoder {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final Marker MARKER;
   private final NetworkSide side;

   public DecoderHandler(NetworkSide side) {
      this.side = side;
   }

   protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf, List<Object> list) throws Exception {
      if (byteBuf.readableBytes() != 0) {
         PacketByteBuf packetByteBuf = new PacketByteBuf(byteBuf);
         int i = packetByteBuf.readVarInt();
         Packet<?> packet = ((NetworkState)channelHandlerContext.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get()).getPacketHandler(this.side, i, packetByteBuf);
         if (packet == null) {
            throw new IOException("Bad packet id " + i);
         } else if (packetByteBuf.readableBytes() > 0) {
            int var10002 = ((NetworkState)channelHandlerContext.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get()).getId();
            throw new IOException("Packet " + var10002 + "/" + i + " (" + packet.getClass().getSimpleName() + ") was larger than I expected, found " + packetByteBuf.readableBytes() + " bytes extra whilst reading packet " + i);
         } else {
            list.add(packet);
            if (LOGGER.isDebugEnabled()) {
               LOGGER.debug((Marker)MARKER, (String)" IN: [{}:{}] {}", channelHandlerContext.channel().attr(ClientConnection.PROTOCOL_ATTRIBUTE_KEY).get(), i, packet.getClass().getName());
            }

         }
      }
   }

   static {
      MARKER = MarkerManager.getMarker("PACKET_RECEIVED", ClientConnection.NETWORK_PACKETS_MARKER);
   }
}
