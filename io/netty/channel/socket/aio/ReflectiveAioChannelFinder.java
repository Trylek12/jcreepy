
package io.netty.channel.socket.aio;

import io.netty.channel.socket.aio.AbstractAioChannel;
import io.netty.channel.socket.aio.AioChannelFinder;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

final class ReflectiveAioChannelFinder
implements AioChannelFinder {
    private static volatile Map<Class<?>, Field> fieldCache = new HashMap();

    ReflectiveAioChannelFinder() {
    }

    @Override
    public AbstractAioChannel findChannel(Runnable command) throws Exception {
        Field f;
        while ((f = ReflectiveAioChannelFinder.findField(command)) != null) {
            Object next = f.get(command);
            if (next instanceof AbstractAioChannel) {
                return (AbstractAioChannel)next;
            }
            command = (Runnable)next;
        }
        return null;
    }

    private static Field findField(Object command) throws Exception {
        Map fieldCache = ReflectiveAioChannelFinder.fieldCache;
        Class commandType = command.getClass();
        Field res = fieldCache.get(commandType);
        if (res != null) {
            return res;
        }
        for (Field f : commandType.getDeclaredFields()) {
            if (f.getType() == Runnable.class) {
                f.setAccessible(true);
                ReflectiveAioChannelFinder.put(fieldCache, commandType, f);
                return f;
            }
            if (f.getType() != Object.class) continue;
            f.setAccessible(true);
            Object candidate = f.get(command);
            if (!(candidate instanceof AbstractAioChannel)) continue;
            ReflectiveAioChannelFinder.put(fieldCache, commandType, f);
            return f;
        }
        return null;
    }

    private static void put(Map<Class<?>, Field> oldCache, Class<?> key, Field value) {
        HashMap newCache = new HashMap(oldCache.size());
        newCache.putAll(oldCache);
        newCache.put(key, value);
        fieldCache = newCache;
    }
}

