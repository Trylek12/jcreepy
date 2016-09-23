
package jcreepy.network.handler;

import java.util.HashMap;
import java.util.Map;
import jcreepy.network.Packet;
import jcreepy.network.handler.PacketHandler;

public class HandlerLookupService {
    protected final Map<Class<? extends Packet>, PacketHandler<?>> handlers = new HashMap();

    protected <T extends Packet> void bind(Class<T> clazz, Class<? extends PacketHandler<T>> handlerClass) throws InstantiationException, IllegalAccessException {
        PacketHandler<T> handler = handlerClass.newInstance();
        this.handlers.put(clazz, handler);
    }

    protected <T extends Packet> void bind(Class<T> clazz, PacketHandler<T> handler) throws InstantiationException, IllegalAccessException {
        this.handlers.put(clazz, handler);
    }

    public <T extends Packet> PacketHandler<T> find(Class<T> clazz) {
        return this.handlers.get(clazz);
    }

    protected HandlerLookupService() {
    }
}

