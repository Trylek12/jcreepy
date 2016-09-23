
package io.netty.handler.logging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundByteHandler;
import io.netty.channel.ChannelOutboundByteHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.logging.InternalLogLevel;
import io.netty.logging.InternalLogger;

public class ByteLoggingHandler
extends LoggingHandler
implements ChannelInboundByteHandler,
ChannelOutboundByteHandler {
    private static final String NEWLINE;
    private static final String[] BYTE2HEX;
    private static final String[] HEXPADDING;
    private static final String[] BYTEPADDING;
    private static final char[] BYTE2CHAR;

    public ByteLoggingHandler() {
    }

    public ByteLoggingHandler(Class<?> clazz, LogLevel level) {
        super(clazz, level);
    }

    public ByteLoggingHandler(Class<?> clazz) {
        super(clazz);
    }

    public ByteLoggingHandler(LogLevel level) {
        super(level);
    }

    public ByteLoggingHandler(String name, LogLevel level) {
        super(name, level);
    }

    public ByteLoggingHandler(String name) {
        super(name);
    }

    @Override
    public ByteBuf newOutboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return ctx.alloc().buffer();
    }

    @Override
    public ByteBuf newInboundBuffer(ChannelHandlerContext ctx) throws Exception {
        return ctx.alloc().buffer();
    }

    @Override
    public void freeInboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        buf.unsafe().free();
    }

    @Override
    public void freeOutboundBuffer(ChannelHandlerContext ctx, ChannelBuf buf) throws Exception {
        buf.unsafe().free();
    }

    @Override
    public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
        ByteBuf buf = ctx.inboundByteBuffer();
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, this.formatBuffer("RECEIVED", buf)));
        }
        ctx.nextInboundByteBuffer().writeBytes(buf);
        ctx.fireInboundBufferUpdated();
    }

    @Override
    public void flush(ChannelHandlerContext ctx, ChannelFuture future) throws Exception {
        ByteBuf buf = ctx.outboundByteBuffer();
        if (this.logger.isEnabled(this.internalLevel)) {
            this.logger.log(this.internalLevel, this.format(ctx, this.formatBuffer("WRITE", buf)));
        }
        ctx.nextOutboundByteBuffer().writeBytes(buf);
        ctx.flush(future);
    }

    protected String formatBuffer(String message, ByteBuf buf) {
        int length;
        int i;
        int rows = length / 16 + ((length = buf.readableBytes()) % 15 == 0 ? 0 : 1) + 4;
        StringBuilder dump = new StringBuilder(rows * 80 + message.length() + 16);
        dump.append(message).append('(').append(length).append('B').append(')');
        dump.append(NEWLINE + "         +-------------------------------------------------+" + NEWLINE + "         |  0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f |" + NEWLINE + "+--------+-------------------------------------------------+----------------+");
        int startIndex = buf.readerIndex();
        int endIndex = buf.writerIndex();
        for (i = startIndex; i < endIndex; ++i) {
            int relIdx = i - startIndex;
            int relIdxMod16 = relIdx & 15;
            if (relIdxMod16 == 0) {
                dump.append(NEWLINE);
                dump.append(Long.toHexString((long)relIdx & 0xFFFFFFFFL | 0x100000000L));
                dump.setCharAt(dump.length() - 9, '|');
                dump.append('|');
            }
            dump.append(BYTE2HEX[buf.getUnsignedByte(i)]);
            if (relIdxMod16 != 15) continue;
            dump.append(" |");
            for (int j = i - 15; j <= i; ++j) {
                dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
            }
            dump.append('|');
        }
        if ((i - startIndex & 15) != 0) {
            int remainder = length & 15;
            dump.append(HEXPADDING[remainder]);
            dump.append(" |");
            for (int j = i - remainder; j < i; ++j) {
                dump.append(BYTE2CHAR[buf.getUnsignedByte(j)]);
            }
            dump.append(BYTEPADDING[remainder]);
            dump.append('|');
        }
        dump.append(NEWLINE + "+--------+-------------------------------------------------+----------------+");
        return dump.toString();
    }

    static {
        StringBuilder buf;
        int i;
        int j;
        StringBuilder buf2;
        NEWLINE = String.format("%n", new Object[0]);
        BYTE2HEX = new String[256];
        HEXPADDING = new String[16];
        BYTEPADDING = new String[16];
        BYTE2CHAR = new char[256];
        for (i = 0; i < 10; ++i) {
            buf2 = new StringBuilder(3);
            buf2.append(" 0");
            buf2.append(i);
            ByteLoggingHandler.BYTE2HEX[i] = buf2.toString();
        }
        while (i < 16) {
            buf2 = new StringBuilder(3);
            buf2.append(" 0");
            buf2.append((char)(97 + i - 10));
            ByteLoggingHandler.BYTE2HEX[i] = buf2.toString();
            ++i;
        }
        while (i < BYTE2HEX.length) {
            buf2 = new StringBuilder(3);
            buf2.append(' ');
            buf2.append(Integer.toHexString(i));
            ByteLoggingHandler.BYTE2HEX[i] = buf2.toString();
            ++i;
        }
        for (i = 0; i < HEXPADDING.length; ++i) {
            int padding = HEXPADDING.length - i;
            buf = new StringBuilder(padding * 3);
            for (j = 0; j < padding; ++j) {
                buf.append("   ");
            }
            ByteLoggingHandler.HEXPADDING[i] = buf.toString();
        }
        for (i = 0; i < BYTEPADDING.length; ++i) {
            int padding = BYTEPADDING.length - i;
            buf = new StringBuilder(padding);
            for (j = 0; j < padding; ++j) {
                buf.append(' ');
            }
            ByteLoggingHandler.BYTEPADDING[i] = buf.toString();
        }
        for (i = 0; i < BYTE2CHAR.length; ++i) {
            ByteLoggingHandler.BYTE2CHAR[i] = i <= 31 || i >= 127 ? 46 : (char)i;
        }
    }
}

