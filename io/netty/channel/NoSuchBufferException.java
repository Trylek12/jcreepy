
package io.netty.channel;

import io.netty.channel.ChannelPipelineException;

public class NoSuchBufferException
extends ChannelPipelineException {
    private static final long serialVersionUID = -131650785896627090L;

    public NoSuchBufferException() {
    }

    public NoSuchBufferException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchBufferException(String message) {
        super(message);
    }

    public NoSuchBufferException(Throwable cause) {
        super(cause);
    }
}

