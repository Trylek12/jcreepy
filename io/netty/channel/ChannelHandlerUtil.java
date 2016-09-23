
package io.netty.channel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.NoSuchBufferException;

public final class ChannelHandlerUtil {
    private static final Class<?>[] EMPTY_TYPES = new Class[0];

    public static boolean unfoldAndAdd(ChannelHandlerContext ctx, Object msg, boolean inbound) throws Exception {
        if (msg == null) {
            return false;
        }
        if (msg instanceof Object[]) {
            Object[] array = (Object[])msg;
            if (array.length == 0) {
                return false;
            }
            boolean added = false;
            for (Object m : array) {
                if (m == null) break;
                if (!ChannelHandlerUtil.unfoldAndAdd(ctx, m, inbound)) continue;
                added = true;
            }
            return added;
        }
        if (inbound) {
            if (ctx.hasNextInboundMessageBuffer()) {
                ctx.nextInboundMessageBuffer().add(msg);
                return true;
            }
            if (msg instanceof ByteBuf && ctx.hasNextInboundByteBuffer()) {
                ByteBuf altDst = ctx.nextInboundByteBuffer();
                ByteBuf src = (ByteBuf)msg;
                altDst.writeBytes(src, src.readerIndex(), src.readableBytes());
                return true;
            }
        } else {
            if (ctx.hasNextOutboundMessageBuffer()) {
                ctx.nextOutboundMessageBuffer().add(msg);
                return true;
            }
            if (msg instanceof ByteBuf && ctx.hasNextOutboundByteBuffer()) {
                ByteBuf altDst = ctx.nextOutboundByteBuffer();
                ByteBuf src = (ByteBuf)msg;
                altDst.writeBytes(src, src.readerIndex(), src.readableBytes());
                return true;
            }
        }
        Object[] arrobject = new Object[3];
        arrobject[0] = ctx.name();
        arrobject[1] = inbound ? ChannelInboundHandler.class.getSimpleName() : ChannelOutboundHandler.class.getSimpleName();
        arrobject[2] = msg.getClass().getSimpleName();
        throw new NoSuchBufferException(String.format("the handler '%s' could not find a %s which accepts a %s.", arrobject));
    }

    public static Class<?>[] acceptedMessageTypes(Class<?>[] acceptedMsgTypes) {
        if (acceptedMsgTypes == null) {
            return EMPTY_TYPES;
        }
        int numElem = 0;
        for (Class c : acceptedMsgTypes) {
            if (c == null) break;
            ++numElem;
        }
        Class[] newAllowedMsgTypes = new Class[numElem];
        System.arraycopy(acceptedMsgTypes, 0, newAllowedMsgTypes, 0, numElem);
        return newAllowedMsgTypes;
    }

    public static boolean acceptMessage(Class<?>[] acceptedMsgTypes, Object msg) {
        if (acceptedMsgTypes.length == 0) {
            return true;
        }
        for (Class c : acceptedMsgTypes) {
            if (!c.isInstance(msg)) continue;
            return true;
        }
        return false;
    }

    public static void addToNextOutboundBuffer(ChannelHandlerContext ctx, Object msg) {
        try {
            ctx.nextOutboundMessageBuffer().add(msg);
        }
        catch (NoSuchBufferException e) {
            NoSuchBufferException newE = new NoSuchBufferException(e.getMessage() + " (msg: " + msg + ')');
            newE.setStackTrace(e.getStackTrace());
            throw newE;
        }
    }

    public static void addToNextInboundBuffer(ChannelHandlerContext ctx, Object msg) {
        try {
            ctx.nextInboundMessageBuffer().add(msg);
        }
        catch (NoSuchBufferException e) {
            NoSuchBufferException newE = new NoSuchBufferException(e.getMessage() + " (msg: " + msg + ')');
            newE.setStackTrace(e.getStackTrace());
            throw newE;
        }
    }

    private ChannelHandlerUtil() {
    }
}

