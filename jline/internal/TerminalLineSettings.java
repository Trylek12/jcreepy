
package jline.internal;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jline.internal.Configuration;
import jline.internal.Log;
import jline.internal.Preconditions;

public final class TerminalLineSettings {
    public static final String JLINE_STTY = "jline.stty";
    public static final String DEFAULT_STTY = "stty";
    public static final String JLINE_SH = "jline.sh";
    public static final String DEFAULT_SH = "sh";
    private String sttyCommand = Configuration.getString("jline.stty", "stty");
    private String shCommand = Configuration.getString("jline.sh", "sh");
    private String config;
    private long configLastFetched;

    public TerminalLineSettings() throws IOException, InterruptedException {
        this.config = this.get("-a");
        this.configLastFetched = System.currentTimeMillis();
        Log.debug("Config: ", this.config);
        if (this.config.length() == 0) {
            throw new IOException(MessageFormat.format("Unrecognized stty code: {0}", this.config));
        }
    }

    public String getConfig() {
        return this.config;
    }

    public void restore() throws IOException, InterruptedException {
        this.set("sane");
    }

    public String get(String args) throws IOException, InterruptedException {
        return this.stty(args);
    }

    public void set(String args) throws IOException, InterruptedException {
        this.stty(args);
    }

    public int getProperty(String name) {
        Preconditions.checkNotNull(name);
        try {
            if (this.config == null || System.currentTimeMillis() - this.configLastFetched > 1000) {
                this.config = this.get("-a");
                this.configLastFetched = System.currentTimeMillis();
            }
            return TerminalLineSettings.getProperty(name, this.config);
        }
        catch (Exception e) {
            Log.warn("Failed to query stty ", name, e);
            return -1;
        }
    }

    protected static int getProperty(String name, String stty) {
        Pattern pattern = Pattern.compile(name + "\\s+=\\s+([^;]*)[;\\n\\r]");
        Matcher matcher = pattern.matcher(stty);
        if (!(matcher.find() || (matcher = (pattern = Pattern.compile(name + "\\s+([^;]*)[;\\n\\r]")).matcher(stty)).find() || (matcher = (pattern = Pattern.compile("(\\S*)\\s+" + name)).matcher(stty)).find())) {
            return -1;
        }
        return TerminalLineSettings.parseControlChar(matcher.group(1));
    }

    private static int parseControlChar(String str) {
        if ("<undef>".equals(str)) {
            return -1;
        }
        if (str.charAt(0) == '0') {
            return Integer.parseInt(str, 8);
        }
        if (str.charAt(0) >= '1' && str.charAt(0) <= '9') {
            return Integer.parseInt(str, 10);
        }
        if (str.charAt(0) == '^') {
            if (str.charAt(1) == '?') {
                return 127;
            }
            return str.charAt(1) - 64;
        }
        if (str.charAt(0) == 'M' && str.charAt(1) == '-') {
            if (str.charAt(2) == '^') {
                if (str.charAt(3) == '?') {
                    return 255;
                }
                return str.charAt(3) - 64 + 128;
            }
            return str.charAt(2) + 128;
        }
        return str.charAt(0);
    }

    private String stty(String args) throws IOException, InterruptedException {
        Preconditions.checkNotNull(args);
        return this.exec(String.format("%s %s < /dev/tty", this.sttyCommand, args));
    }

    private String exec(String cmd) throws IOException, InterruptedException {
        Preconditions.checkNotNull(cmd);
        return this.exec(this.shCommand, "-c", cmd);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private /* varargs */ String exec(String ... cmd) throws IOException, InterruptedException {
        Preconditions.checkNotNull(cmd);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        Log.trace("Running: ", cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        InputStream in = null;
        InputStream err = null;
        OutputStream out = null;
        try {
            int c;
            in = p.getInputStream();
            while ((c = in.read()) != -1) {
                bout.write(c);
            }
            err = p.getErrorStream();
            while ((c = err.read()) != -1) {
                bout.write(c);
            }
        }
        catch (Throwable var8_9) {
            TerminalLineSettings.close(in, out, err);
            throw var8_9;
        }
        out = p.getOutputStream();
        p.waitFor();
        TerminalLineSettings.close(in, out, err);
        String result = bout.toString();
        Log.trace("Result: ", result);
        return result;
    }

    private static /* varargs */ void close(Closeable ... closeables) {
        for (Closeable c : closeables) {
            try {
                c.close();
                continue;
            }
            catch (Exception e) {
                // empty catch block
            }
        }
    }
}

