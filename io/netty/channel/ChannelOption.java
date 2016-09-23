
package io.netty.channel;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.UniqueName;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChannelOption<T>
extends UniqueName {
    private static final ConcurrentMap<String, Boolean> names = new ConcurrentHashMap<String, Boolean>();
    public static final ChannelOption<ByteBufAllocator> ALLOCATOR = new ChannelOption<T>("ALLOCATOR");
    public static final ChannelOption<Integer> CONNECT_TIMEOUT_MILLIS = new ChannelOption<T>("CONNECT_TIMEOUT_MILLIS");
    public static final ChannelOption<Integer> WRITE_SPIN_COUNT = new ChannelOption<T>("WRITE_SPIN_COUNT");
    public static final ChannelOption<Boolean> ALLOW_HALF_CLOSURE = new ChannelOption<T>("ALLOW_HALF_CLOSURE");
    public static final ChannelOption<Boolean> SO_BROADCAST = new ChannelOption<T>("SO_BROADCAST");
    public static final ChannelOption<Boolean> SO_KEEPALIVE = new ChannelOption<T>("SO_KEEPALIVE");
    public static final ChannelOption<Integer> SO_SNDBUF = new ChannelOption<T>("SO_SNDBUF");
    public static final ChannelOption<Integer> SO_RCVBUF = new ChannelOption<T>("SO_RCVBUF");
    public static final ChannelOption<Boolean> SO_REUSEADDR = new ChannelOption<T>("SO_REUSEADDR");
    public static final ChannelOption<Integer> SO_LINGER = new ChannelOption<T>("SO_LINGER");
    public static final ChannelOption<Integer> SO_BACKLOG = new ChannelOption<T>("SO_BACKLOG");
    public static final ChannelOption<Integer> IP_TOS = new ChannelOption<T>("IP_TOS");
    public static final ChannelOption<InetAddress> IP_MULTICAST_ADDR = new ChannelOption<T>("IP_MULTICAST_ADDR");
    public static final ChannelOption<NetworkInterface> IP_MULTICAST_IF = new ChannelOption<T>("IP_MULTICAST_IF");
    public static final ChannelOption<Integer> IP_MULTICAST_TTL = new ChannelOption<T>("IP_MULTICAST_TTL");
    public static final ChannelOption<Boolean> IP_MULTICAST_LOOP_DISABLED = new ChannelOption<T>("IP_MULTICAST_LOOP_DISABLED");
    public static final ChannelOption<Integer> UDP_RECEIVE_PACKET_SIZE = new ChannelOption<T>("UDP_RECEIVE_PACKET_SIZE");
    public static final ChannelOption<Boolean> TCP_NODELAY = new ChannelOption<T>("TCP_NODELAY");
    public static final ChannelOption<Boolean> SCTP_DISABLE_FRAGMENTS = new ChannelOption<T>("SCTP_DISABLE_FRAGMENTS");
    public static final ChannelOption<Boolean> SCTP_EXPLICIT_COMPLETE = new ChannelOption<T>("SCTP_EXPLICIT_COMPLETE");
    public static final ChannelOption<Integer> SCTP_FRAGMENT_INTERLEAVE = new ChannelOption<T>("SCTP_FRAGMENT_INTERLEAVE");
    public static final ChannelOption<List<Integer>> SCTP_INIT_MAXSTREAMS = new ChannelOption<List<Integer>>("SCTP_INIT_MAXSTREAMS"){

        @Override
        public void validate(List<Integer> value) {
            super.validate(value);
            if (value.size() != 2) {
                throw new IllegalArgumentException("value must be a List of 2 Integers: " + value);
            }
            if (value.get(0) == null) {
                throw new NullPointerException("value[0]");
            }
            if (value.get(1) == null) {
                throw new NullPointerException("value[1]");
            }
        }
    };
    public static final ChannelOption<Boolean> SCTP_NODELAY = new ChannelOption<T>("SCTP_NODELAY");
    public static final ChannelOption<SocketAddress> SCTP_PRIMARY_ADDR = new ChannelOption<T>("SCTP_PRIMARY_ADDR");
    public static final ChannelOption<SocketAddress> SCTP_SET_PEER_PRIMARY_ADDR = new ChannelOption<T>("SCTP_SET_PEER_PRIMARY_ADDR");
    public static final ChannelOption<Long> AIO_READ_TIMEOUT = new ChannelOption<T>("AIO_READ_TIMEOUT");
    public static final ChannelOption<Long> AIO_WRITE_TIMEOUT = new ChannelOption<T>("AIO_WRITE_TIMEOUT");

    public ChannelOption(String name) {
        super(names, name, new Object[0]);
    }

    public void validate(T value) {
        if (value == null) {
            throw new NullPointerException("value");
        }
    }

}

