package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.DecoderHandler;
import net.minecraft.network.LegacyQueryHandler;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.PacketEncoder;
import net.minecraft.network.RateLimitedConnection;
import net.minecraft.network.SizePrepender;
import net.minecraft.network.SplitterHandler;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.server.network.LocalServerHandshakeNetworkHandler;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Lazy;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class ServerNetworkIo {
   private static final Logger LOGGER = LogManager.getLogger();
   public static final Lazy<NioEventLoopGroup> DEFAULT_CHANNEL = new Lazy(() -> {
      return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Server IO #%d").setDaemon(true).build());
   });
   public static final Lazy<EpollEventLoopGroup> EPOLL_CHANNEL = new Lazy(() -> {
      return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Server IO #%d").setDaemon(true).build());
   });
   final MinecraftServer server;
   public volatile boolean active;
   private final List<ChannelFuture> channels = Collections.synchronizedList(Lists.newArrayList());
   final List<ClientConnection> connections = Collections.synchronizedList(Lists.newArrayList());

   public ServerNetworkIo(MinecraftServer server) {
      this.server = server;
      this.active = true;
   }

   public void bind(@Nullable InetAddress address, int port) throws IOException {
      synchronized(this.channels) {
         Class class2;
         Lazy lazy2;
         if (Epoll.isAvailable() && this.server.isUsingNativeTransport()) {
            class2 = EpollServerSocketChannel.class;
            lazy2 = EPOLL_CHANNEL;
            LOGGER.info("Using epoll channel type");
         } else {
            class2 = NioServerSocketChannel.class;
            lazy2 = DEFAULT_CHANNEL;
            LOGGER.info("Using default channel type");
         }

         this.channels.add(((ServerBootstrap)((ServerBootstrap)(new ServerBootstrap()).channel(class2)).childHandler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
               try {
                  channel.config().setOption(ChannelOption.TCP_NODELAY, true);
               } catch (ChannelException var4) {
               }

               channel.pipeline().addLast((String)"timeout", (ChannelHandler)(new ReadTimeoutHandler(30))).addLast((String)"legacy_query", (ChannelHandler)(new LegacyQueryHandler(ServerNetworkIo.this))).addLast((String)"splitter", (ChannelHandler)(new SplitterHandler())).addLast((String)"decoder", (ChannelHandler)(new DecoderHandler(NetworkSide.SERVERBOUND))).addLast((String)"prepender", (ChannelHandler)(new SizePrepender())).addLast((String)"encoder", (ChannelHandler)(new PacketEncoder(NetworkSide.CLIENTBOUND)));
               int i = ServerNetworkIo.this.server.getRateLimit();
               ClientConnection clientConnection = i > 0 ? new RateLimitedConnection(i) : new ClientConnection(NetworkSide.SERVERBOUND);
               ServerNetworkIo.this.connections.add(clientConnection);
               channel.pipeline().addLast((String)"packet_handler", (ChannelHandler)clientConnection);
               ((ClientConnection)clientConnection).setPacketListener(new ServerHandshakeNetworkHandler(ServerNetworkIo.this.server, (ClientConnection)clientConnection));
            }
         }).group((EventLoopGroup)lazy2.get()).localAddress(address, port)).bind().syncUninterruptibly());
      }
   }

   public SocketAddress bindLocal() {
      ChannelFuture channelFuture2;
      synchronized(this.channels) {
         channelFuture2 = ((ServerBootstrap)((ServerBootstrap)(new ServerBootstrap()).channel(LocalServerChannel.class)).childHandler(new ChannelInitializer<Channel>() {
            protected void initChannel(Channel channel) {
               ClientConnection clientConnection = new ClientConnection(NetworkSide.SERVERBOUND);
               clientConnection.setPacketListener(new LocalServerHandshakeNetworkHandler(ServerNetworkIo.this.server, clientConnection));
               ServerNetworkIo.this.connections.add(clientConnection);
               channel.pipeline().addLast((String)"packet_handler", (ChannelHandler)clientConnection);
            }
         }).group((EventLoopGroup)DEFAULT_CHANNEL.get()).localAddress(LocalAddress.ANY)).bind().syncUninterruptibly();
         this.channels.add(channelFuture2);
      }

      return channelFuture2.channel().localAddress();
   }

   public void stop() {
      this.active = false;
      Iterator var1 = this.channels.iterator();

      while(var1.hasNext()) {
         ChannelFuture channelFuture = (ChannelFuture)var1.next();

         try {
            channelFuture.channel().close().sync();
         } catch (InterruptedException var4) {
            LOGGER.error("Interrupted whilst closing channel");
         }
      }

   }

   public void tick() {
      synchronized(this.connections) {
         Iterator iterator = this.connections.iterator();

         while(true) {
            while(true) {
               ClientConnection clientConnection;
               do {
                  if (!iterator.hasNext()) {
                     return;
                  }

                  clientConnection = (ClientConnection)iterator.next();
               } while(clientConnection.hasChannel());

               if (clientConnection.isOpen()) {
                  try {
                     clientConnection.tick();
                  } catch (Exception var7) {
                     if (clientConnection.isLocal()) {
                        throw new CrashException(CrashReport.create(var7, "Ticking memory connection"));
                     }

                     LOGGER.warn((String)"Failed to handle packet for {}", (Object)clientConnection.getAddress(), (Object)var7);
                     Text text = new LiteralText("Internal server error");
                     clientConnection.send(new DisconnectS2CPacket(text), (future) -> {
                        clientConnection.disconnect(text);
                     });
                     clientConnection.disableAutoRead();
                  }
               } else {
                  iterator.remove();
                  clientConnection.handleDisconnection();
               }
            }
         }
      }
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   /**
    * A channel in-bound handler that only forwards received messages to the next
    * channel in-bound handler in the channel pipeline after a random delay between
    * {@link #baseDelay} and {@code baseDelay + }{@link #extraDelay} milliseconds.
    * 
    * @apiNote This may be used to simulate a laggy network enviroment.
    */
   private static class DelayingChannelInboundHandler extends ChannelInboundHandlerAdapter {
      private static final Timer TIMER = new HashedWheelTimer();
      private final int baseDelay;
      private final int extraDelay;
      private final List<ServerNetworkIo.DelayingChannelInboundHandler.Packet> packets = Lists.newArrayList();

      public DelayingChannelInboundHandler(int baseDelay, int extraDelay) {
         this.baseDelay = baseDelay;
         this.extraDelay = extraDelay;
      }

      public void channelRead(ChannelHandlerContext ctx, Object msg) {
         this.delay(ctx, msg);
      }

      private void delay(ChannelHandlerContext ctx, Object msg) {
         int i = this.baseDelay + (int)(Math.random() * (double)this.extraDelay);
         this.packets.add(new ServerNetworkIo.DelayingChannelInboundHandler.Packet(ctx, msg));
         TIMER.newTimeout(this::forward, (long)i, TimeUnit.MILLISECONDS);
      }

      private void forward(Timeout timeout) {
         ServerNetworkIo.DelayingChannelInboundHandler.Packet packet = (ServerNetworkIo.DelayingChannelInboundHandler.Packet)this.packets.remove(0);
         packet.context.fireChannelRead(packet.message);
      }

      static class Packet {
         public final ChannelHandlerContext context;
         public final Object message;

         public Packet(ChannelHandlerContext context, Object message) {
            this.context = context;
            this.message = message;
         }
      }
   }
}
