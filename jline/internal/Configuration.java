
package jline.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import jline.internal.Log;
import jline.internal.Preconditions;
import jline.internal.Urls;

public class Configuration {
    public static final String JLINE_CONFIGURATION = "jline.configuration";
    public static final String JLINE_RC = ".jline.rc";
    private static volatile Properties properties;

    private static Properties initProperties() {
        URL url = Configuration.determineUrl();
        Properties props = new Properties();
        try {
            Configuration.loadProperties(url, props);
        }
        catch (IOException e) {
            Log.debug("Unable to read configuration from: ", url, e);
        }
        return props;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void loadProperties(URL url, Properties props) throws IOException {
        Log.debug("Loading properties from: ", url);
        InputStream input = url.openStream();
        try {
            props.load(new BufferedInputStream(input));
        }
        finally {
            try {
                input.close();
            }
            catch (IOException e) {}
        }
        if (Log.DEBUG) {
            Log.debug("Loaded properties:");
            for (Map.Entry entry : props.entrySet()) {
                Log.debug("  ", entry.getKey(), "=", entry.getValue());
            }
        }
    }

    private static URL determineUrl() {
        String tmp = System.getProperty("jline.configuration");
        if (tmp != null) {
            return Urls.create(tmp);
        }
        File file = new File(Configuration.getUserHome(), ".jline.rc");
        return Urls.create(file);
    }

    public static void reset() {
        Log.debug("Resetting");
        properties = null;
        Configuration.getProperties();
    }

    public static Properties getProperties() {
        if (properties == null) {
            properties = Configuration.initProperties();
        }
        return properties;
    }

    public static String getString(String name, String defaultValue) {
        Preconditions.checkNotNull(name);
        String value = System.getProperty(name);
        if (value == null && (value = Configuration.getProperties().getProperty(name)) == null) {
            value = defaultValue;
        }
        return value;
    }

    public static String getString(String name) {
        return Configuration.getString(name, null);
    }

    public static boolean getBoolean(String name, boolean defaultValue) {
        String value = Configuration.getString(name);
        if (value == null) {
            return defaultValue;
        }
        return value.length() == 0 || value.equalsIgnoreCase("1") || value.equalsIgnoreCase("on") || value.equalsIgnoreCase("true");
    }

    public static int getInteger(String name, int defaultValue) {
        String str = Configuration.getString(name);
        if (str == null) {
            return defaultValue;
        }
        return Integer.parseInt(str);
    }

    public static long getLong(String name, long defaultValue) {
        String str = Configuration.getString(name);
        if (str == null) {
            return defaultValue;
        }
        return Long.parseLong(str);
    }

    public static String getLineSeparator() {
        return System.getProperty("line.separator");
    }

    public static File getUserHome() {
        return new File(System.getProperty("user.home"));
    }

    public static String getOsName() {
        return System.getProperty("os.name").toLowerCase();
    }

    public static boolean isWindows() {
        return Configuration.getOsName().startsWith("windows");
    }

    public static String getFileEncoding() {
        return System.getProperty("file.encoding");
    }

    public static String getEncoding() {
        String ctype = System.getenv("LC_CTYPE");
        if (ctype != null && ctype.indexOf(46) > 0) {
            return ctype.substring(ctype.indexOf(46) + 1);
        }
        return System.getProperty("input.encoding", Charset.defaultCharset().name());
    }
}

