/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  jcreepy.event.Event
 *  jcreepy.event.EventManager
 *  jcreepy.event.events.DisconnectEvent
 */
package jcreepy.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jcreepy.event.Event;
import jcreepy.event.EventManager;
import jcreepy.event.events.DisconnectEvent;
import jcreepy.network.Packet;
import jcreepy.network.Protocol;
import jcreepy.network.Session;
import jcreepy.network.handler.HandlerLookupService;
import jcreepy.network.handler.PacketHandler;

public class NetworkHandler
extends ChannelInboundMessageHandlerAdapter<Packet> {
    private final ExecutorService asyncQueue = Executors.newCachedThreadPool();
    private final ExecutorService syncQueue = Executors.newSingleThreadExecutor();
    private final HandlerLookupService handlers;
    private final EventManager em;
    private final Session session;
    private boolean useHandshake;
    private Throwable failCause;

    public NetworkHandler(HandlerLookupService handlers, EventManager em, Session session, boolean useHandshake) {
        super(new Class[0]);
        this.handlers = handlers;
        this.em = em;
        this.session = session;
        this.useHandshake = useHandshake;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, Packet packet) throws Exception {
        PacketHandler handler = this.handlers.find(packet.getClass());
        if (handler != null) {
            handler.addToQueue(packet.isAsync() ? this.asyncQueue : this.syncQueue, (Packet)packet, this.session);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        this.session.setChannel(ctx.channel());
        if (this.useHandshake) {
            ctx.channel().write(this.session.getProtocol().getHandshakePacket(this.session.getPlayerName()));
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.em.callEvent((Event)new DisconnectEvent(this.failCause));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        this.failCause = cause;
        ctx.channel().close();
    }
}

