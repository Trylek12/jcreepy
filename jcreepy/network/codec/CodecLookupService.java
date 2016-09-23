
package jcreepy.network.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;

public abstract class CodecLookupService {
    protected final PacketCodec<?>[] opcodeTable = new PacketCodec[65536];
    protected final Map<Class<? extends Packet>, PacketCodec<?>> classTable = new HashMap();
    private int nextId = 0;

    protected <T extends Packet, C extends PacketCodec<T>> C bind(Class<C> clazz) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        boolean dynamicId = false;
        Constructor<C> constructor = clazz.getConstructor(new Class[0]);
        PacketCodec codec = (PacketCodec)constructor.newInstance(new Object[0]);
        this.nextId = this.nextId > codec.getOpcode() ? this.nextId : codec.getOpcode() + 1;
        this.opcodeTable[codec.getOpcode()] = codec;
        this.classTable.put(codec.getType(), codec);
        return (C)codec;
    }

    private int getNextId() {
        while (this.opcodeTable[this.nextId] != null) {
            ++this.nextId;
        }
        return this.nextId;
    }

    public PacketCodec<?> find(int opcode) {
        if (opcode > -1 && opcode < this.opcodeTable.length) {
            return this.opcodeTable[opcode];
        }
        return null;
    }

    public <T extends Packet> PacketCodec<T> find(Class<T> clazz) {
        return this.classTable.get(clazz);
    }

    public Collection<PacketCodec<?>> getCodecs() {
        return Collections.unmodifiableCollection(this.classTable.values());
    }

    protected CodecLookupService() {
    }
}

