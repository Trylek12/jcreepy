
package io.netty.channel;

public interface ChannelInboundInvoker {
    public void fireChannelRegistered();

    public void fireChannelUnregistered();

    public void fireChannelActive();

    public void fireChannelInactive();

    public void fireExceptionCaught(Throwable var1);

    public void fireUserEventTriggered(Object var1);

    public void fireInboundBufferUpdated();
}

