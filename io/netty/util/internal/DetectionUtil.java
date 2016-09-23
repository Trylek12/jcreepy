
package io.netty.util.internal;

import io.netty.util.internal.SystemPropertyUtil;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import sun.misc.Cleaner;

public final class DetectionUtil {
    private static final int JAVA_VERSION = DetectionUtil.javaVersion0();
    private static final boolean HAS_UNSAFE = DetectionUtil.hasUnsafe(AtomicInteger.class.getClassLoader());
    private static final boolean CAN_FREE_DIRECT_BUFFER;
    private static final boolean IS_WINDOWS;
    private static final boolean IS_ROOT;

    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    public static boolean isRoot() {
        return IS_ROOT;
    }

    public static boolean hasUnsafe() {
        return HAS_UNSAFE;
    }

    public static int javaVersion() {
        return JAVA_VERSION;
    }

    public static boolean canFreeDirectBuffer() {
        return CAN_FREE_DIRECT_BUFFER;
    }

    private static boolean hasUnsafe(ClassLoader loader) {
        boolean noUnsafe = SystemPropertyUtil.getBoolean("io.netty.noUnsafe", false);
        if (noUnsafe) {
            return false;
        }
        boolean tryUnsafe = SystemPropertyUtil.contains("io.netty.tryUnsafe") ? SystemPropertyUtil.getBoolean("io.netty.tryUnsafe", true) : SystemPropertyUtil.getBoolean("org.jboss.netty.tryUnsafe", true);
        if (!tryUnsafe) {
            return false;
        }
        try {
            Class unsafeClazz = Class.forName("sun.misc.Unsafe", true, loader);
            return DetectionUtil.hasUnsafeField(unsafeClazz);
        }
        catch (Exception e) {
            return false;
        }
    }

    private static boolean hasUnsafeField(final Class<?> unsafeClass) throws PrivilegedActionException {
        return (Boolean)AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>(){

            @Override
            public Boolean run() throws Exception {
                unsafeClass.getDeclaredField("theUnsafe");
                return true;
            }
        });
    }

    private static int javaVersion0() {
        try {
            Class.forName("android.app.Application", false, ClassLoader.getSystemClassLoader());
            return 6;
        }
        catch (Exception e) {
            try {
                Class.forName("java.util.concurrent.LinkedTransferQueue", false, BlockingQueue.class.getClassLoader());
                return 7;
            }
            catch (Exception e) {
                return 6;
            }
        }
    }

    private DetectionUtil() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    static {
        Pattern PERMISSION_DENIED = Pattern.compile(".*permission.*denied.*");
        String os = SystemPropertyUtil.get("os.name", "").toLowerCase(Locale.UK);
        IS_WINDOWS = os.contains("win");
        boolean root = false;
        if (!IS_WINDOWS) {
            for (int i = 1023; i > 0; --i) {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket();
                    ss.setReuseAddress(true);
                    ss.bind(new InetSocketAddress(i));
                    root = true;
                }
                catch (Exception e) {
                    String message = e.getMessage();
                    if (message == null) {
                        message = "";
                    }
                    if (!PERMISSION_DENIED.matcher(message = message.toLowerCase()).matches()) continue;
                    break;
                }
                finally {
                    if (ss != null) {
                        try {
                            ss.close();
                        }
                        catch (Exception e) {}
                    }
                }
            }
        }
        IS_ROOT = root;
        boolean canFreeDirectBuffer = false;
        try {
            ByteBuffer direct = ByteBuffer.allocateDirect(1);
            Field cleanerField = direct.getClass().getDeclaredField("cleaner");
            cleanerField.setAccessible(true);
            Cleaner cleaner = (Cleaner)cleanerField.get(direct);
            cleaner.clean();
            canFreeDirectBuffer = true;
        }
        catch (Throwable t) {
            // empty catch block
        }
        CAN_FREE_DIRECT_BUFFER = canFreeDirectBuffer;
    }

}

