
package io.netty.channel.group;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ServerChannel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelGroupFuture;
import io.netty.channel.group.CombinedIterator;
import io.netty.channel.group.DefaultChannelGroupFuture;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultChannelGroup
extends AbstractSet<Channel>
implements ChannelGroup {
    private static final AtomicInteger nextId = new AtomicInteger();
    private final String name;
    private final ConcurrentMap<Integer, Channel> serverChannels = new ConcurrentHashMap<Integer, Channel>();
    private final ConcurrentMap<Integer, Channel> nonServerChannels = new ConcurrentHashMap<Integer, Channel>();
    private final ChannelFutureListener remover;

    public DefaultChannelGroup() {
        this("group-0x" + Integer.toHexString(nextId.incrementAndGet()));
    }

    public DefaultChannelGroup(String name) {
        this.remover = new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                DefaultChannelGroup.this.remove(future.channel());
            }
        };
        if (name == null) {
            throw new NullPointerException("name");
        }
        this.name = name;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public boolean isEmpty() {
        return this.nonServerChannels.isEmpty() && this.serverChannels.isEmpty();
    }

    @Override
    public int size() {
        return this.nonServerChannels.size() + this.serverChannels.size();
    }

    @Override
    public Channel find(Integer id) {
        Channel c = this.nonServerChannels.get(id);
        if (c != null) {
            return c;
        }
        return this.serverChannels.get(id);
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Integer) {
            return this.nonServerChannels.containsKey(o) || this.serverChannels.containsKey(o);
        }
        if (o instanceof Channel) {
            Channel c = (Channel)o;
            if (o instanceof ServerChannel) {
                return this.serverChannels.containsKey(c.id());
            }
            return this.nonServerChannels.containsKey(c.id());
        }
        return false;
    }

    @Override
    public boolean add(Channel channel) {
        boolean added;
        ConcurrentMap<Integer, Channel> map = channel instanceof ServerChannel ? this.serverChannels : this.nonServerChannels;
        boolean bl = added = map.putIfAbsent(channel.id(), channel) == null;
        if (added) {
            channel.closeFuture().addListener(this.remover);
        }
        return added;
    }

    @Override
    public boolean remove(Object o) {
        Channel c = null;
        if (o instanceof Integer) {
            c = this.nonServerChannels.remove(o);
            if (c == null) {
                c = this.serverChannels.remove(o);
            }
        } else if (o instanceof Channel) {
            c = (Channel)o;
            c = c instanceof ServerChannel ? this.serverChannels.remove(c.id()) : this.nonServerChannels.remove(c.id());
        }
        if (c == null) {
            return false;
        }
        c.closeFuture().removeListener(this.remover);
        return true;
    }

    @Override
    public void clear() {
        this.nonServerChannels.clear();
        this.serverChannels.clear();
    }

    @Override
    public Iterator<Channel> iterator() {
        return new CombinedIterator<Channel>(this.serverChannels.values().iterator(), this.nonServerChannels.values().iterator());
    }

    @Override
    public Object[] toArray() {
        ArrayList<Channel> channels = new ArrayList<Channel>(this.size());
        channels.addAll(this.serverChannels.values());
        channels.addAll(this.nonServerChannels.values());
        return channels.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        ArrayList<Channel> channels = new ArrayList<Channel>(this.size());
        channels.addAll(this.serverChannels.values());
        channels.addAll(this.nonServerChannels.values());
        return channels.toArray(a);
    }

    @Override
    public ChannelGroupFuture close() {
        LinkedHashMap<Integer, ChannelFuture> futures = new LinkedHashMap<Integer, ChannelFuture>(this.size());
        for (Channel c2 : this.serverChannels.values()) {
            futures.put(c2.id(), c2.close().awaitUninterruptibly());
        }
        for (Channel c2 : this.nonServerChannels.values()) {
            futures.put(c2.id(), c2.close());
        }
        return new DefaultChannelGroupFuture((ChannelGroup)this, futures);
    }

    @Override
    public ChannelGroupFuture disconnect() {
        LinkedHashMap<Integer, ChannelFuture> futures = new LinkedHashMap<Integer, ChannelFuture>(this.size());
        for (Channel c2 : this.serverChannels.values()) {
            futures.put(c2.id(), c2.disconnect());
        }
        for (Channel c2 : this.nonServerChannels.values()) {
            futures.put(c2.id(), c2.disconnect());
        }
        return new DefaultChannelGroupFuture((ChannelGroup)this, futures);
    }

    @Override
    public ChannelGroupFuture write(Object message) {
        LinkedHashMap<Integer, ChannelFuture> futures = new LinkedHashMap<Integer, ChannelFuture>(this.size());
        if (message instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf)message;
            for (Channel c : this.nonServerChannels.values()) {
                futures.put(c.id(), c.write(buf.duplicate()));
            }
        } else {
            for (Channel c : this.nonServerChannels.values()) {
                futures.put(c.id(), c.write(message));
            }
        }
        return new DefaultChannelGroupFuture((ChannelGroup)this, futures);
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int compareTo(ChannelGroup o) {
        int v = this.name().compareTo(o.name());
        if (v != 0) {
            return v;
        }
        return System.identityHashCode(this) - System.identityHashCode(o);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(name: " + this.name() + ", size: " + this.size() + ')';
    }

}

