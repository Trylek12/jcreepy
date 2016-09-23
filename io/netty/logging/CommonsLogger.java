/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.apache.commons.logging.Log
 */
package io.netty.logging;

import io.netty.logging.AbstractInternalLogger;
import org.apache.commons.logging.Log;

class CommonsLogger
extends AbstractInternalLogger {
    private final Log logger;
    private final String loggerName;

    CommonsLogger(Log logger, String loggerName) {
        this.logger = logger;
        this.loggerName = loggerName;
    }

    @Override
    public void trace(String msg) {
        this.logger.trace((Object)msg);
    }

    @Override
    public void trace(String msg, Throwable cause) {
        this.logger.trace((Object)msg, cause);
    }

    @Override
    public void debug(String msg) {
        this.logger.debug((Object)msg);
    }

    @Override
    public void debug(String msg, Throwable cause) {
        this.logger.debug((Object)msg, cause);
    }

    @Override
    public void error(String msg) {
        this.logger.error((Object)msg);
    }

    @Override
    public void error(String msg, Throwable cause) {
        this.logger.error((Object)msg, cause);
    }

    @Override
    public void info(String msg) {
        this.logger.info((Object)msg);
    }

    @Override
    public void info(String msg, Throwable cause) {
        this.logger.info((Object)msg, cause);
    }

    @Override
    public boolean isTraceEnabled() {
        return this.logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isDebugEnabled();
    }

    @Override
    public boolean isErrorEnabled() {
        return this.logger.isErrorEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return this.logger.isWarnEnabled();
    }

    @Override
    public void warn(String msg) {
        this.logger.warn((Object)msg);
    }

    @Override
    public void warn(String msg, Throwable cause) {
        this.logger.warn((Object)msg, cause);
    }

    public String toString() {
        return this.loggerName;
    }
}

