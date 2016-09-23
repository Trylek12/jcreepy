
package io.netty.bootstrap;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractBootstrap<B extends AbstractBootstrap<?>> {
    private EventLoopGroup group;
    private ChannelFactory factory;
    private SocketAddress localAddress;
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap();
    private final Map<AttributeKey<?>, Object> attrs = new LinkedHashMap();
    private ChannelHandler handler;

    public B group(EventLoopGroup group) {
        if (group == null) {
            throw new NullPointerException("group");
        }
        if (this.group != null) {
            throw new IllegalStateException("group set already");
        }
        this.group = group;
        return (B)this;
    }

    public B channel(Class<? extends Channel> channelClass) {
        if (channelClass == null) {
            throw new NullPointerException("channelClass");
        }
        return this.channelFactory(new BootstrapChannelFactory(channelClass));
    }

    public B channelFactory(ChannelFactory factory) {
        if (factory == null) {
            throw new NullPointerException("factory");
        }
        if (this.factory != null) {
            throw new IllegalStateException("factory set already");
        }
        this.factory = factory;
        return (B)this;
    }

    public B localAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
        return (B)this;
    }

    public B localAddress(int port) {
        return this.localAddress(new InetSocketAddress(port));
    }

    public B localAddress(String host, int port) {
        return this.localAddress(new InetSocketAddress(host, port));
    }

    public B localAddress(InetAddress host, int port) {
        return this.localAddress(new InetSocketAddress(host, port));
    }

    public <T> B option(ChannelOption<T> option, T value) {
        if (option == null) {
            throw new NullPointerException("option");
        }
        if (value == null) {
            this.options.remove(option);
        } else {
            this.options.put(option, value);
        }
        return (B)this;
    }

    public <T> B attr(AttributeKey<T> key, T value) {
        if (key == null) {
            throw new NullPointerException("key");
        }
        if (value == null) {
            this.attrs.remove(key);
        } else {
            this.attrs.put(key, value);
        }
        AbstractBootstrap b = this;
        return (B)b;
    }

    public void shutdown() {
        if (this.group != null) {
            this.group.shutdown();
        }
    }

    protected void validate() {
        if (this.group == null) {
            throw new IllegalStateException("group not set");
        }
        if (this.factory == null) {
            throw new IllegalStateException("factory not set");
        }
    }

    protected final void validate(ChannelFuture future) {
        if (future == null) {
            throw new NullPointerException("future");
        }
        this.validate();
    }

    public ChannelFuture bind() {
        this.validate();
        Channel channel = this.factory().newChannel();
        return this.bind(channel.newFuture());
    }

    public B handler(ChannelHandler handler) {
        if (handler == null) {
            throw new NullPointerException("handler");
        }
        this.handler = handler;
        return (B)this;
    }

    protected static boolean ensureOpen(ChannelFuture future) {
        if (!future.channel().isOpen()) {
            future.setFailure(new ChannelException("initialization failure"));
            return false;
        }
        return true;
    }

    public abstract ChannelFuture bind(ChannelFuture var1);

    protected final SocketAddress localAddress() {
        return this.localAddress;
    }

    protected final ChannelFactory factory() {
        return this.factory;
    }

    protected final ChannelHandler handler() {
        return this.handler;
    }

    protected final EventLoopGroup group() {
        return this.group;
    }

    protected final Map<ChannelOption<?>, Object> options() {
        return this.options;
    }

    protected final Map<AttributeKey<?>, Object> attrs() {
        return this.attrs;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(this.getClass().getSimpleName());
        buf.append('(');
        if (this.group != null) {
            buf.append("group: ");
            buf.append(this.group.getClass().getSimpleName());
            buf.append(", ");
        }
        if (this.factory != null) {
            buf.append("factory: ");
            buf.append(this.factory);
            buf.append(", ");
        }
        if (this.localAddress != null) {
            buf.append("localAddress: ");
            buf.append(this.localAddress);
            buf.append(", ");
        }
        if (this.options != null && !this.options.isEmpty()) {
            buf.append("options: ");
            buf.append(this.options);
            buf.append(", ");
        }
        if (this.attrs != null && !this.attrs.isEmpty()) {
            buf.append("attrs: ");
            buf.append(this.attrs);
            buf.append(", ");
        }
        if (this.handler != null) {
            buf.append("handler: ");
            buf.append(this.handler);
            buf.append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }
        return buf.toString();
    }

    public static interface ChannelFactory {
        public Channel newChannel();
    }

    private static final class BootstrapChannelFactory
    implements ChannelFactory {
        private final Class<? extends Channel> clazz;

        BootstrapChannelFactory(Class<? extends Channel> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Channel newChannel() {
            try {
                return this.clazz.newInstance();
            }
            catch (Throwable t) {
                throw new ChannelException("Unable to create Channel from class " + this.clazz, t);
            }
        }

        public String toString() {
            return this.clazz.getSimpleName() + ".class";
        }
    }

}

