
package io.netty.logging;

import io.netty.logging.InternalLogLevel;
import io.netty.logging.InternalLogger;
import io.netty.logging.JdkLoggerFactory;

public abstract class InternalLoggerFactory {
    private static volatile InternalLoggerFactory defaultFactory = new JdkLoggerFactory();

    public static InternalLoggerFactory getDefaultFactory() {
        return defaultFactory;
    }

    public static void setDefaultFactory(InternalLoggerFactory defaultFactory) {
        if (defaultFactory == null) {
            throw new NullPointerException("defaultFactory");
        }
        InternalLoggerFactory.defaultFactory = defaultFactory;
    }

    public static InternalLogger getInstance(Class<?> clazz) {
        return InternalLoggerFactory.getInstance(clazz.getName());
    }

    public static InternalLogger getInstance(String name) {
        final InternalLogger logger = InternalLoggerFactory.getDefaultFactory().newInstance(name);
        return new InternalLogger(){

            @Override
            public void trace(String msg) {
                logger.trace(msg);
            }

            @Override
            public void trace(String msg, Throwable cause) {
                logger.trace(msg, cause);
            }

            @Override
            public void debug(String msg) {
                logger.debug(msg);
            }

            @Override
            public void debug(String msg, Throwable cause) {
                logger.debug(msg, cause);
            }

            @Override
            public void error(String msg) {
                logger.error(msg);
            }

            @Override
            public void error(String msg, Throwable cause) {
                logger.error(msg, cause);
            }

            @Override
            public void info(String msg) {
                logger.info(msg);
            }

            @Override
            public void info(String msg, Throwable cause) {
                logger.info(msg, cause);
            }

            @Override
            public boolean isTraceEnabled() {
                return logger.isTraceEnabled();
            }

            @Override
            public boolean isDebugEnabled() {
                return logger.isDebugEnabled();
            }

            @Override
            public boolean isErrorEnabled() {
                return logger.isErrorEnabled();
            }

            @Override
            public boolean isInfoEnabled() {
                return logger.isInfoEnabled();
            }

            @Override
            public boolean isWarnEnabled() {
                return logger.isWarnEnabled();
            }

            @Override
            public void warn(String msg) {
                logger.warn(msg);
            }

            @Override
            public void warn(String msg, Throwable cause) {
                logger.warn(msg, cause);
            }

            @Override
            public boolean isEnabled(InternalLogLevel level) {
                return logger.isEnabled(level);
            }

            @Override
            public void log(InternalLogLevel level, String msg) {
                logger.log(level, msg);
            }

            @Override
            public void log(InternalLogLevel level, String msg, Throwable cause) {
                logger.log(level, msg, cause);
            }
        };
    }

    public abstract InternalLogger newInstance(String var1);

}

