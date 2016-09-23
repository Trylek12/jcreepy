
package io.netty.handler.codec.serialization;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

public class CompatibleObjectEncoder
extends MessageToByteEncoder<Object> {
    private static final AttributeKey<ObjectOutputStream> OOS = new AttributeKey(CompatibleObjectEncoder.class.getName() + ".oos");
    private final int resetInterval;
    private int writtenObjects;

    public CompatibleObjectEncoder() {
        this(16);
    }

    public CompatibleObjectEncoder(int resetInterval) {
        super(Serializable.class);
        if (resetInterval < 0) {
            throw new IllegalArgumentException("resetInterval: " + resetInterval);
        }
        this.resetInterval = resetInterval;
    }

    protected ObjectOutputStream newObjectOutputStream(OutputStream out) throws Exception {
        return new ObjectOutputStream(out);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        ObjectOutputStream newOos;
        Attribute<ObjectOutputStream> oosAttr = ctx.attr(OOS);
        ObjectOutputStream oos = oosAttr.get();
        if (oos == null && (newOos = oosAttr.setIfAbsent(oos = this.newObjectOutputStream(new ByteBufOutputStream(out)))) != null) {
            oos = newOos;
        }
        newOos = oos;
        synchronized (newOos) {
            if (this.resetInterval != 0) {
                ++this.writtenObjects;
                if (this.writtenObjects % this.resetInterval == 0) {
                    oos.reset();
                    out.unsafe().discardSomeReadBytes();
                }
            }
            oos.writeObject(msg);
            oos.flush();
        }
    }
}

