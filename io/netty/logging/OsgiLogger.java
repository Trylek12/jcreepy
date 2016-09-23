/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.osgi.service.log.LogService
 */
package io.netty.logging;

import io.netty.logging.AbstractInternalLogger;
import io.netty.logging.InternalLogger;
import io.netty.logging.OsgiLoggerFactory;
import org.osgi.service.log.LogService;

class OsgiLogger
extends AbstractInternalLogger {
    private final OsgiLoggerFactory parent;
    private final InternalLogger fallback;
    private final String name;
    private final String prefix;

    OsgiLogger(OsgiLoggerFactory parent, String name, InternalLogger fallback) {
        this.parent = parent;
        this.name = name;
        this.fallback = fallback;
        this.prefix = "" + '[' + name + "] ";
    }

    @Override
    public void trace(String msg) {
    }

    @Override
    public void trace(String msg, Throwable cause) {
    }

    @Override
    public void debug(String msg) {
        LogService logService = this.parent.getLogService();
        if (logService != null) {
            logService.log(4, this.prefix + msg);
        } else {
            this.fallback.debug(msg);
        }
    }

    @Override
    public void debug(String msg, Throwable cause) {
        LogService logService = this.parent.getLogService();
        if (logService != null) {
            logService.log(4, this.prefix + msg, cause);
        } else {
            this.fallback.debug(msg, cause);
        }
    }

    @Override
    public void error(String msg) {
        LogService logService = this.parent.getLogService();
        if (logService != null) {
            logService.log(1, this.prefix + msg);
        } else {
            this.fallback.error(msg);
        }
    }

    @Override
    public void error(String msg, Throwable cause) {
        LogService logService = this.parent.getLogService();
        if (logService != null) {
            logService.log(1, this.prefix + msg, cause);
        } else {
            this.fallback.error(msg, cause);
        }
    }

    @Override
    public void info(String msg) {
        LogService logService = this.parent.getLogService();
        if (logService != null) {
            logService.log(3, this.prefix + msg);
        } else {
            this.fallback.info(msg);
        }
    }

    @Override
    public void info(String msg, Throwable cause) {
        LogService logService = this.parent.getLogService();
        if (logService != null) {
            logService.log(3, this.prefix + msg, cause);
        } else {
            this.fallback.info(msg, cause);
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public void warn(String msg) {
        LogService logService = this.parent.getLogService();
        if (logService != null) {
            logService.log(2, this.prefix + msg);
        } else {
            this.fallback.warn(msg);
        }
    }

    @Override
    public void warn(String msg, Throwable cause) {
        LogService logService = this.parent.getLogService();
        if (logService != null) {
            logService.log(2, this.prefix + msg, cause);
        } else {
            this.fallback.warn(msg, cause);
        }
    }

    public String toString() {
        return this.name;
    }
}

