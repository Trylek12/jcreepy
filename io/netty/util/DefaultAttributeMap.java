
package io.netty.util;

import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.netty.util.AttributeMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class DefaultAttributeMap
implements AttributeMap {
    private Map<AttributeKey<?>, Attribute<?>> map;

    @Override
    public synchronized <T> Attribute<T> attr(AttributeKey<T> key) {
        Attribute attr;
        Map map = this.map;
        if (map == null) {
            map = this.map = new IdentityHashMap(2);
        }
        if ((attr = map.get(key)) == null) {
            attr = new DefaultAttribute();
            map.put(key, attr);
        }
        return attr;
    }

    private class DefaultAttribute<T>
    extends AtomicReference<T>
    implements Attribute<T> {
        private static final long serialVersionUID = -2661411462200283011L;

        private DefaultAttribute() {
        }

        @Override
        public T setIfAbsent(T value) {
            if (this.compareAndSet(null, value)) {
                return null;
            }
            return (T)this.get();
        }

        @Override
        public void remove() {
            this.set(null);
        }
    }

}

