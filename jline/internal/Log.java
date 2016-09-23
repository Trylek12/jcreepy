
package jline.internal;

import java.io.PrintStream;
import jline.internal.Preconditions;
import jline.internal.TestAccessible;

public final class Log {
    public static final boolean TRACE = Boolean.getBoolean(Log.class.getName() + ".trace");
    public static final boolean DEBUG = TRACE || Boolean.getBoolean(Log.class.getName() + ".debug");
    private static PrintStream output = System.err;

    public static PrintStream getOutput() {
        return output;
    }

    public static void setOutput(PrintStream out) {
        output = Preconditions.checkNotNull(out);
    }

    @TestAccessible
    static void render(PrintStream out, Object message) {
        if (message.getClass().isArray()) {
            Object[] array = (Object[])message;
            out.print("[");
            for (int i = 0; i < array.length; ++i) {
                out.print(array[i]);
                if (i + 1 >= array.length) continue;
                out.print(",");
            }
            out.print("]");
        } else {
            out.print(message);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @TestAccessible
    static /* varargs */ void log(Level level, Object ... messages) {
        PrintStream printStream = output;
        synchronized (printStream) {
            output.format("[%s] ", new Object[]{level});
            for (int i = 0; i < messages.length; ++i) {
                if (i + 1 == messages.length && messages[i] instanceof Throwable) {
                    output.println();
                    ((Throwable)messages[i]).printStackTrace(output);
                    continue;
                }
                Log.render(output, messages[i]);
            }
            output.println();
            output.flush();
        }
    }

    public static /* varargs */ void trace(Object ... messages) {
        if (TRACE) {
            Log.log(Level.TRACE, messages);
        }
    }

    public static /* varargs */ void debug(Object ... messages) {
        if (TRACE || DEBUG) {
            Log.log(Level.DEBUG, messages);
        }
    }

    public static /* varargs */ void info(Object ... messages) {
        Log.log(Level.INFO, messages);
    }

    public static /* varargs */ void warn(Object ... messages) {
        Log.log(Level.WARN, messages);
    }

    public static /* varargs */ void error(Object ... messages) {
        Log.log(Level.ERROR, messages);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR;
        

        private Level() {
        }
    }

}

