
package io.netty.channel;

public final class ChannelInputShutdownEvent {
    public static final ChannelInputShutdownEvent INSTANCE = new ChannelInputShutdownEvent();

    private ChannelInputShutdownEvent() {
    }
}

