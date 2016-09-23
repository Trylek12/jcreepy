
package io.netty.channel;

public enum ChannelHandlerType {
    STATE(0),
    INBOUND(0),
    OPERATION(1),
    OUTBOUND(1);
    
    final int direction;

    private ChannelHandlerType(int direction) {
        if (direction != 0 && direction != 1) {
            throw new IllegalArgumentException("direction must be either 0 or 1");
        }
        this.direction = direction;
    }
}

