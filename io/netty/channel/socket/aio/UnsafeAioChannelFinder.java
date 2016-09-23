
package io.netty.channel.socket.aio;

import io.netty.channel.socket.aio.AbstractAioChannel;
import io.netty.channel.socket.aio.AioChannelFinder;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import sun.misc.Unsafe;

final class UnsafeAioChannelFinder
implements AioChannelFinder {
    private static final Unsafe UNSAFE = UnsafeAioChannelFinder.getUnsafe();
    private static volatile Map<Class<?>, Long> offsetCache = new HashMap();

    UnsafeAioChannelFinder() {
    }

    @Override
    public AbstractAioChannel findChannel(Runnable command) throws Exception {
        Long offset;
        while ((offset = UnsafeAioChannelFinder.findFieldOffset(command)) != null) {
            Object next = UNSAFE.getObject((Object)command, offset);
            if (next instanceof AbstractAioChannel) {
                return (AbstractAioChannel)next;
            }
            command = (Runnable)next;
        }
        return null;
    }

    private static Long findFieldOffset(Object command) throws Exception {
        Map offsetCache = UnsafeAioChannelFinder.offsetCache;
        Class commandType = command.getClass();
        Long res = offsetCache.get(commandType);
        if (res != null) {
            return res;
        }
        for (Field f : commandType.getDeclaredFields()) {
            if (f.getType() == Runnable.class) {
                res = UNSAFE.objectFieldOffset(f);
                UnsafeAioChannelFinder.put(offsetCache, commandType, res);
                return res;
            }
            if (f.getType() != Object.class) continue;
            f.setAccessible(true);
            Object candidate = f.get(command);
            if (!(candidate instanceof AbstractAioChannel)) continue;
            res = UNSAFE.objectFieldOffset(f);
            UnsafeAioChannelFinder.put(offsetCache, commandType, res);
            return res;
        }
        return null;
    }

    private static void put(Map<Class<?>, Long> oldCache, Class<?> key, Long value) {
        HashMap newCache = new HashMap(oldCache.size());
        newCache.putAll(oldCache);
        newCache.put(key, value);
        offsetCache = newCache;
    }

    private static Unsafe getUnsafe() {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            return (Unsafe)singleoneInstanceField.get(null);
        }
        catch (Throwable cause) {
            throw new RuntimeException("Error while obtaining sun.misc.Unsafe", cause);
        }
    }
}

