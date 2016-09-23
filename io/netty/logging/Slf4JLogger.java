/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 */
package io.netty.logging;

import io.netty.logging.AbstractInternalLogger;
import org.slf4j.Logger;

class Slf4JLogger
extends AbstractInternalLogger {
    private final Logger logger;

    Slf4JLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void trace(String msg) {
        this.logger.trace(msg);
    }

    @Override
    public void trace(String msg, Throwable cause) {
        this.logger.trace(msg, cause);
    }

    @Override
    public void debug(String msg) {
        this.logger.debug(msg);
    }

    @Override
    public void debug(String msg, Throwable cause) {
        this.logger.debug(msg, cause);
    }

    @Override
    public void error(String msg) {
        this.logger.error(msg);
    }

    @Override
    public void error(String msg, Throwable cause) {
        this.logger.error(msg, cause);
    }

    @Override
    public void info(String msg) {
        this.logger.info(msg);
    }

    @Override
    public void info(String msg, Throwable cause) {
        this.logger.info(msg, cause);
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
        this.logger.warn(msg);
    }

    @Override
    public void warn(String msg, Throwable cause) {
        this.logger.warn(msg, cause);
    }

    public String toString() {
        return String.valueOf(this.logger.getName());
    }
}

