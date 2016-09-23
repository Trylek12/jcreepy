/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  jcreepy.event.Event
 *  jcreepy.event.EventManager
 *  jcreepy.event.events.DisconnectEvent
 */
package foxcraft.network;

import foxcraft.CreepyClient;
import foxcraft.network.handler.CreepyHandlerLookupService;
import io.netty.bootstrap.AbstractBootstrap;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import jcreepy.event.Event;
import jcreepy.event.EventManager;
import jcreepy.event.events.DisconnectEvent;
import jcreepy.network.NetworkHandler;
import jcreepy.network.Protocol;
import jcreepy.network.Session;
import jcreepy.network.handler.HandlerLookupService;
import jcreepy.network.stream.PacketDecoder;
import jcreepy.network.stream.PacketEncoder;
import jcreepy.protocol.MinecraftProtocol;

public class ConnectionThread
extends Thread {
    private final Bootstrap server;
    private final Session session;
    private NetworkHandler nm;
    private Channel channel;

    public ConnectionThread(Session session) {
        this.setDaemon(true);
        this.session = session;
        this.server = ((Bootstrap)((Bootstrap)new Bootstrap().handler(this.getChannelInitializer())).channel((Class)NioSocketChannel.class).group(new NioEventLoopGroup())).remoteAddress(session.getHost(), session.getPort());
    }

    public ChannelInitializer getChannelInitializer() {
        return new ChannelInitializer(){

            public void initChannel(Channel channel) throws Exception {
                NetworkHandler nh = new NetworkHandler(CreepyHandlerLookupService.INSTANCE, CreepyClient.getInstance().getEventManager(), ConnectionThread.this.session, true);
                channel.pipeline().addLast("timer", (ChannelHandler)new ReadTimeoutHandler(30)).addLast("decoder", (ChannelHandler)new PacketDecoder(new MinecraftProtocol())).addLast("encoder", (ChannelHandler)new PacketEncoder(new MinecraftProtocol())).addLast("handler", (ChannelHandler)nh);
            }
        };
    }

    public void shutdown() {
        this.server.shutdown();
    }

    @Override
    public void run() {
        this.server.connect().addListener(new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture f) {
                if (!f.isCancelled()) {
                    if (!f.isSuccess()) {
                        CreepyClient.getInstance().getEventManager().callEvent((Event)new DisconnectEvent(f.cause()));
                    } else {
                        ConnectionThread.this.session.setActive(true);
                    }
                }
            }
        });
    }

}

