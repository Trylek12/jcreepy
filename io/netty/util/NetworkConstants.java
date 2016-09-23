
package io.netty.util;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Enumeration;

public final class NetworkConstants {
    public static final InetAddress LOCALHOST;
    public static final NetworkInterface LOOPBACK_IF;
    public static final int SOMAXCONN;
    private static final InternalLogger logger;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void validateHost(InetAddress host) throws IOException {
        ServerSocket ss = null;
        Socket s1 = null;
        Socket s2 = null;
        try {
            ss = new ServerSocket();
            ss.setReuseAddress(false);
            ss.bind(new InetSocketAddress(host, 0));
            s1 = new Socket(host, ss.getLocalPort());
            s2 = ss.accept();
        }
        finally {
            if (s2 != null) {
                try {
                    s2.close();
                }
                catch (IOException e) {}
            }
            if (s1 != null) {
                try {
                    s1.close();
                }
                catch (IOException e) {}
            }
            if (ss != null) {
                try {
                    ss.close();
                }
                catch (IOException e) {}
            }
        }
    }

    private NetworkConstants() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    static {
        NetworkConstants.logger = InternalLoggerFactory.getInstance(NetworkConstants.class);
        try {
            localhost = InetAddress.getLocalHost();
            NetworkConstants.validateHost(localhost);
        }
        catch (IOException e) {
            try {
                localhost = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
                NetworkConstants.validateHost(localhost);
            }
            catch (IOException e1) {
                try {
                    localhost = InetAddress.getByAddress(new byte[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1});
                    NetworkConstants.validateHost(localhost);
                }
                catch (IOException e2) {
                    throw new Error("Failed to resolve localhost - incorrect network configuration?", e2);
                }
            }
        }
        NetworkConstants.LOCALHOST = localhost;
        try {
            loopbackInterface = NetworkInterface.getByInetAddress(NetworkConstants.LOCALHOST);
        }
        catch (SocketException e) {
            loopbackInterface = null;
        }
        if (loopbackInterface == null) {
            try {
                interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    networkInterface = interfaces.nextElement();
                    if (!networkInterface.isLoopback()) continue;
                    loopbackInterface = networkInterface;
                    break;
                }
            }
            catch (SocketException e) {
                NetworkConstants.logger.error("Failed to enumerate network interfaces", e);
            }
        }
        NetworkConstants.LOOPBACK_IF = loopbackInterface;
        somaxconn = 3072;
        in = null;
        try {
            in = new BufferedReader(new FileReader("/proc/sys/net/core/somaxconn"));
            somaxconn = Integer.parseInt(in.readLine());
            ** if (in == null) goto lbl-1000
        }
        catch (Exception e) {
            if (in != null) {
                try {
                    in.close();
                }
                catch (Exception e) {}
            }
            catch (Throwable var5_13) {
                if (in == null) throw var5_13;
                try {
                    in.close();
                    throw var5_13;
                }
                catch (Exception e) {
                    // empty catch block
                }
                throw var5_13;
            }
        }
lbl-1000: // 1 sources:
        {
            try {
                in.close();
            }
            catch (Exception e) {}
        }
lbl-1000: // 2 sources:
        {
        }
        NetworkConstants.SOMAXCONN = somaxconn;
    }
}

