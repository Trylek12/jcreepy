
package io.netty.channel;

import io.netty.channel.FileRegion;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

public class DefaultFileRegion
implements FileRegion {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultFileRegion.class);
    private final FileChannel file;
    private final long position;
    private final long count;

    public DefaultFileRegion(FileChannel file, long position, long count) {
        this.file = file;
        this.position = position;
        this.count = count;
    }

    @Override
    public long position() {
        return this.position;
    }

    @Override
    public long count() {
        return this.count;
    }

    @Override
    public long transferTo(WritableByteChannel target, long position) throws IOException {
        long count = this.count - position;
        if (count < 0 || position < 0) {
            throw new IllegalArgumentException("position out of range: " + position + " (expected: 0 - " + (this.count - 1) + ")");
        }
        if (count == 0) {
            return 0;
        }
        return this.file.transferTo(this.position + position, count, target);
    }

    @Override
    public void close() {
        block2 : {
            try {
                this.file.close();
            }
            catch (IOException e) {
                if (!logger.isWarnEnabled()) break block2;
                logger.warn("Failed to close a file.", e);
            }
        }
    }
}

