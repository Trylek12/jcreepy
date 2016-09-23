
package io.netty.channel;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

public interface FileRegion {
    public long position();

    public long count();

    public long transferTo(WritableByteChannel var1, long var2) throws IOException;

    public void close();
}

