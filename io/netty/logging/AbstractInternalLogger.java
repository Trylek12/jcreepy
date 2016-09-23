
package io.netty.logging;

import io.netty.logging.InternalLogLevel;
import io.netty.logging.InternalLogger;

public abstract class AbstractInternalLogger
implements InternalLogger {
    protected AbstractInternalLogger() {
    }

    @Override
    public boolean isEnabled(InternalLogLevel level) {
        switch (level) {
            case TRACE: {
                return this.isTraceEnabled();
            }
            case DEBUG: {
                return this.isDebugEnabled();
            }
            case INFO: {
                return this.isInfoEnabled();
            }
            case WARN: {
                return this.isWarnEnabled();
            }
            case ERROR: {
                return this.isErrorEnabled();
            }
        }
        throw new Error();
    }

    @Override
    public void log(InternalLogLevel level, String msg, Throwable cause) {
        switch (level) {
            case TRACE: {
                this.trace(msg, cause);
                break;
            }
            case DEBUG: {
                this.debug(msg, cause);
                break;
            }
            case INFO: {
                this.info(msg, cause);
                break;
            }
            case WARN: {
                this.warn(msg, cause);
                break;
            }
            case ERROR: {
                this.error(msg, cause);
                break;
            }
            default: {
                throw new Error();
            }
        }
    }

    @Override
    public void log(InternalLogLevel level, String msg) {
        switch (level) {
            case TRACE: {
                this.trace(msg);
                break;
            }
            case DEBUG: {
                this.debug(msg);
                break;
            }
            case INFO: {
                this.info(msg);
                break;
            }
            case WARN: {
                this.warn(msg);
                break;
            }
            case ERROR: {
                this.error(msg);
                break;
            }
            default: {
                throw new Error();
            }
        }
    }

}

