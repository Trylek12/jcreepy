
package io.netty.handler.codec.serialization;

import io.netty.handler.codec.serialization.ReferenceMap;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class SoftReferenceMap<K, V>
extends ReferenceMap<K, V> {
    public SoftReferenceMap(Map<K, Reference<V>> delegate) {
        super(delegate);
    }

    @Override
    Reference<V> fold(V value) {
        return new SoftReference<V>(value);
    }
}

