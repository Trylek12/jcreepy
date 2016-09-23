
package io.netty.buffer;

import io.netty.buffer.ChannelBufType;

public interface ChannelBuf {
    public ChannelBufType type();

    public Unsafe unsafe();

    public static interface Unsafe {
        public boolean isFreed();

        public void free();
    }

}

