/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.jboss.logging.Logger
 */
package io.netty.logging;

import io.netty.logging.AbstractInternalLogger;
import org.jboss.logging.Logger;

class JBossLogger
extends AbstractInternalLogger {
    private final Logger logger;

    JBossLogger(Logger logger) {
        this.logger = logger;
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
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isInfoEnabled();
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
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
        return String.valueOf(this.logger.getName());
    }
}

