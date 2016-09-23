
package io.netty.logging;

import io.netty.logging.AbstractInternalLogger;
import java.util.logging.Level;
import java.util.logging.Logger;

class JdkLogger
extends AbstractInternalLogger {
    private final Logger logger;
    private final String loggerName;

    JdkLogger(Logger logger, String loggerName) {
        this.logger = logger;
        this.loggerName = loggerName;
    }

    @Override
    public void trace(String msg) {
        this.logger.logp(Level.FINEST, this.loggerName, null, msg);
    }

    @Override
    public void trace(String msg, Throwable cause) {
        this.logger.logp(Level.FINEST, this.loggerName, null, msg, cause);
    }

    @Override
    public void debug(String msg) {
        this.logger.logp(Level.FINE, this.loggerName, null, msg);
    }

    @Override
    public void debug(String msg, Throwable cause) {
        this.logger.logp(Level.FINE, this.loggerName, null, msg, cause);
    }

    @Override
    public void error(String msg) {
        this.logger.logp(Level.SEVERE, this.loggerName, null, msg);
    }

    @Override
    public void error(String msg, Throwable cause) {
        this.logger.logp(Level.SEVERE, this.loggerName, null, msg, cause);
    }

    @Override
    public void info(String msg) {
        this.logger.logp(Level.INFO, this.loggerName, null, msg);
    }

    @Override
    public void info(String msg, Throwable cause) {
        this.logger.logp(Level.INFO, this.loggerName, null, msg, cause);
    }

    @Override
    public boolean isTraceEnabled() {
        return this.logger.isLoggable(Level.FINEST);
    }

    @Override
    public boolean isDebugEnabled() {
        return this.logger.isLoggable(Level.FINE);
    }

    @Override
    public boolean isErrorEnabled() {
        return this.logger.isLoggable(Level.SEVERE);
    }

    @Override
    public boolean isInfoEnabled() {
        return this.logger.isLoggable(Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return this.logger.isLoggable(Level.WARNING);
    }

    @Override
    public void warn(String msg) {
        this.logger.logp(Level.WARNING, this.loggerName, null, msg);
    }

    @Override
    public void warn(String msg, Throwable cause) {
        this.logger.logp(Level.WARNING, this.loggerName, null, msg, cause);
    }

    public String toString() {
        return this.loggerName;
    }
}

