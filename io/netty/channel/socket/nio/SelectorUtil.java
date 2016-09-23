
package io.netty.channel.socket.nio;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.util.internal.SystemPropertyUtil;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Selector;
import java.util.concurrent.TimeUnit;

final class SelectorUtil {
    private static final InternalLogger logger;
    static final long DEFAULT_SELECT_TIMEOUT = 500;
    static final long SELECT_TIMEOUT;
    static final long SELECT_TIMEOUT_NANOS;
    static final boolean EPOLL_BUG_WORKAROUND;

    static int select(Selector selector) throws IOException {
        try {
            return selector.select(SELECT_TIMEOUT);
        }
        catch (CancelledKeyException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(CancelledKeyException.class.getSimpleName() + " raised by a Selector - JDK bug?", e);
            }
            return -1;
        }
    }

    static void cleanupKeys(Selector selector) {
        try {
            selector.selectNow();
        }
        catch (Throwable t) {
            logger.warn("Failed to update SelectionKeys.", t);
        }
    }

    private SelectorUtil() {
    }

    static {
        block4 : {
            logger = InternalLoggerFactory.getInstance(SelectorUtil.class);
            SELECT_TIMEOUT = SystemPropertyUtil.getLong("io.netty.selectTimeout", 500);
            SELECT_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(SELECT_TIMEOUT);
            EPOLL_BUG_WORKAROUND = SystemPropertyUtil.getBoolean("io.netty.epollBugWorkaround", false);
            String key = "sun.nio.ch.bugLevel";
            try {
                String buglevel = System.getProperty(key);
                if (buglevel == null) {
                    System.setProperty(key, "");
                }
            }
            catch (SecurityException e) {
                if (!logger.isDebugEnabled()) break block4;
                logger.debug("Unable to get/set System Property '" + key + '\'', e);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Using select timeout of " + SELECT_TIMEOUT);
            logger.debug("Epoll-bug workaround enabled = " + EPOLL_BUG_WORKAROUND);
        }
    }
}

