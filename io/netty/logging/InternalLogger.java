
package io.netty.logging;

import io.netty.logging.InternalLogLevel;

public interface InternalLogger {
    public boolean isTraceEnabled();

    public boolean isDebugEnabled();

    public boolean isInfoEnabled();

    public boolean isWarnEnabled();

    public boolean isErrorEnabled();

    public boolean isEnabled(InternalLogLevel var1);

    public void trace(String var1);

    public void trace(String var1, Throwable var2);

    public void debug(String var1);

    public void debug(String var1, Throwable var2);

    public void info(String var1);

    public void info(String var1, Throwable var2);

    public void warn(String var1);

    public void warn(String var1, Throwable var2);

    public void error(String var1);

    public void error(String var1, Throwable var2);

    public void log(InternalLogLevel var1, String var2);

    public void log(InternalLogLevel var1, String var2, Throwable var3);
}

