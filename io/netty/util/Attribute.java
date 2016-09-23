
package io.netty.util;

public interface Attribute<T> {
    public T get();

    public void set(T var1);

    public T getAndSet(T var1);

    public T setIfAbsent(T var1);

    public boolean compareAndSet(T var1, T var2);

    public void remove();
}

