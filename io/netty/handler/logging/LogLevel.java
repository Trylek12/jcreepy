
package io.netty.handler.logging;

import io.netty.logging.InternalLogLevel;

public enum LogLevel {
    TRACE(InternalLogLevel.TRACE),
    DEBUG(InternalLogLevel.DEBUG),
    INFO(InternalLogLevel.INFO),
    WARN(InternalLogLevel.WARN),
    ERROR(InternalLogLevel.ERROR);
    
    private final InternalLogLevel internalLevel;

    private LogLevel(InternalLogLevel internalLevel) {
        this.internalLevel = internalLevel;
    }

    InternalLogLevel toInternalLevel() {
        return this.internalLevel;
    }
}

