
package io.netty.channel;

import io.netty.buffer.ChannelBufType;

public final class ChannelMetadata {
    private final ChannelBufType bufferType;
    private final boolean hasDisconnect;

    public ChannelMetadata(ChannelBufType bufferType, boolean hasDisconnect) {
        if (bufferType == null) {
            throw new NullPointerException("bufferType");
        }
        this.bufferType = bufferType;
        this.hasDisconnect = hasDisconnect;
    }

    public ChannelBufType bufferType() {
        return this.bufferType;
    }

    public boolean hasDisconnect() {
        return this.hasDisconnect;
    }
}

