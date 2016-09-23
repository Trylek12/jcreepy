
package jline;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import jline.AnsiWindowsTerminal;
import jline.Terminal;
import jline.UnixTerminal;
import jline.UnsupportedTerminal;
import jline.internal.Configuration;
import jline.internal.Log;
import jline.internal.Preconditions;


public class TerminalFactory {
    public static final String JLINE_TERMINAL = "jline.terminal";
    public static final String AUTO = "auto";
    public static final String UNIX = "unix";
    public static final String WIN = "win";
    public static final String WINDOWS = "windows";
    public static final String NONE = "none";
    public static final String OFF = "off";
    public static final String FALSE = "false";
    private static final InheritableThreadLocal<Terminal> holder = new InheritableThreadLocal();
    private static final Map<Flavor, Class<? extends Terminal>> FLAVORS = new HashMap<Flavor, Class<? extends Terminal>>();

    public static synchronized Terminal create() {
        Terminal t;
        block13 : {
            if (Log.TRACE) {
                Log.trace(new Throwable("CREATE MARKER"));
            }
            String type = Configuration.getString("jline.terminal", "auto");
            if (System.getenv("TERM") == "dumb") {
                type = "none";
                Log.debug("$TERM=dumb; setting type=", type);
            }
            Log.debug("Creating terminal; type=", type);
            try {
                String tmp = type.toLowerCase();
                if (tmp.equals("unix")) {
                    t = TerminalFactory.getFlavor(Flavor.UNIX);
                    break block13;
                }
                if (tmp.equals("win") | tmp.equals("windows")) {
                    t = TerminalFactory.getFlavor(Flavor.WINDOWS);
                    break block13;
                }
                if (tmp.equals("none") || tmp.equals("off") || tmp.equals("false")) {
                    t = new UnsupportedTerminal();
                    break block13;
                }
                if (tmp.equals("auto")) {
                    String os = Configuration.getOsName();
                    Flavor flavor = Flavor.UNIX;
                    if (os.contains("windows")) {
                        flavor = Flavor.WINDOWS;
                    }
                    t = TerminalFactory.getFlavor(flavor);
                    break block13;
                }
                try {
                    t = (Terminal)Thread.currentThread().getContextClassLoader().loadClass(type).newInstance();
                }
                catch (Exception e) {
                    throw new IllegalArgumentException(MessageFormat.format("Invalid terminal type: {0}", type), e);
                }
            }
            catch (Exception e) {
                Log.error("Failed to construct terminal; falling back to unsupported", e);
                t = new UnsupportedTerminal();
            }
        }
        Log.debug("Created Terminal: ", t);
        try {
            t.init();
        }
        catch (Throwable e) {
            Log.error("Terminal initialization failed; falling back to unsupported", e);
            return new UnsupportedTerminal();
        }
        return t;
    }

    public static synchronized void reset() {
        holder.remove();
    }

    public static synchronized void resetIf(Terminal t) {
        if (holder.get() == t) {
            TerminalFactory.reset();
        }
    }

    public static synchronized void configure(String type) {
        Preconditions.checkNotNull(type);
        System.setProperty("jline.terminal", type);
    }

    public static synchronized void configure(Type type) {
        Preconditions.checkNotNull(type);
        TerminalFactory.configure(type.name().toLowerCase());
    }

    public static synchronized Terminal get() {
        Terminal t = holder.get();
        if (t == null) {
            t = TerminalFactory.create();
            holder.set(t);
        }
        return t;
    }

    public static Terminal getFlavor(Flavor flavor) throws Exception {
        Class<? extends Terminal> type = FLAVORS.get((Object)flavor);
        if (type != null) {
            return type.newInstance();
        }
        throw new InternalError();
    }

    public static void registerFlavor(Flavor flavor, Class<? extends Terminal> type) {
        FLAVORS.put(flavor, type);
    }

    static {
        TerminalFactory.registerFlavor(Flavor.WINDOWS, AnsiWindowsTerminal.class);
        TerminalFactory.registerFlavor(Flavor.UNIX, UnixTerminal.class);
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum Flavor {
        WINDOWS,
        UNIX;
        

        private Flavor() {
        }
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum Type {
        AUTO,
        WINDOWS,
        UNIX,
        NONE;
        

        private Type() {
        }
    }

}

