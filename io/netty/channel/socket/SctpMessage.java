
package io.netty.channel.socket;

import com.sun.nio.sctp.MessageInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public final class SctpMessage {
    private final int streamIdentifier;
    private final int protocolIdentifier;
    private final ByteBuf payloadBuffer;
    private MessageInfo msgInfo;

    public SctpMessage(int protocolIdentifier, int streamIdentifier, ByteBuf payloadBuffer) {
        this.protocolIdentifier = protocolIdentifier;
        this.streamIdentifier = streamIdentifier;
        this.payloadBuffer = payloadBuffer;
    }

    public SctpMessage(MessageInfo msgInfo, ByteBuf payloadBuffer) {
        this.msgInfo = msgInfo;
        this.streamIdentifier = msgInfo.streamNumber();
        this.protocolIdentifier = msgInfo.payloadProtocolID();
        this.payloadBuffer = payloadBuffer;
    }

    public int getStreamIdentifier() {
        return this.streamIdentifier;
    }

    public int getProtocolIdentifier() {
        return this.protocolIdentifier;
    }

    public ByteBuf getPayloadBuffer() {
        if (this.payloadBuffer.readable()) {
            return this.payloadBuffer.slice();
        }
        return Unpooled.EMPTY_BUFFER;
    }

    public MessageInfo getMessageInfo() {
        return this.msgInfo;
    }

    public boolean isComplete() {
        if (this.msgInfo != null) {
            return this.msgInfo.isComplete();
        }
        return true;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        SctpMessage sctpFrame = (SctpMessage)o;
        if (this.protocolIdentifier != sctpFrame.protocolIdentifier) {
            return false;
        }
        if (this.streamIdentifier != sctpFrame.streamIdentifier) {
            return false;
        }
        if (!this.payloadBuffer.equals(sctpFrame.payloadBuffer)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int result = this.streamIdentifier;
        result = 31 * result + this.protocolIdentifier;
        result = 31 * result + this.payloadBuffer.hashCode();
        return result;
    }

    public String toString() {
        return "SctpFrame{streamIdentifier=" + this.streamIdentifier + ", protocolIdentifier=" + this.protocolIdentifier + ", payloadBuffer=" + ByteBufUtil.hexDump(this.getPayloadBuffer()) + '}';
    }
}

