
package io.netty.handler.codec.serialization;

import io.netty.handler.codec.serialization.CachingClassResolver;
import io.netty.handler.codec.serialization.ClassLoaderClassResolver;
import io.netty.handler.codec.serialization.ClassResolver;
import io.netty.handler.codec.serialization.SoftReferenceMap;
import io.netty.handler.codec.serialization.WeakReferenceMap;
import java.lang.ref.Reference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassResolvers {
    public static ClassResolver cacheDisabled(ClassLoader classLoader) {
        return new ClassLoaderClassResolver(ClassResolvers.defaultClassLoader(classLoader));
    }

    public static ClassResolver weakCachingResolver(ClassLoader classLoader) {
        return new CachingClassResolver(new ClassLoaderClassResolver(ClassResolvers.defaultClassLoader(classLoader)), new WeakReferenceMap(new HashMap()));
    }

    public static ClassResolver softCachingResolver(ClassLoader classLoader) {
        return new CachingClassResolver(new ClassLoaderClassResolver(ClassResolvers.defaultClassLoader(classLoader)), new SoftReferenceMap(new HashMap()));
    }

    public static ClassResolver weakCachingConcurrentResolver(ClassLoader classLoader) {
        return new CachingClassResolver(new ClassLoaderClassResolver(ClassResolvers.defaultClassLoader(classLoader)), new WeakReferenceMap(new ConcurrentHashMap()));
    }

    public static ClassResolver softCachingConcurrentResolver(ClassLoader classLoader) {
        return new CachingClassResolver(new ClassLoaderClassResolver(ClassResolvers.defaultClassLoader(classLoader)), new SoftReferenceMap(new ConcurrentHashMap()));
    }

    static ClassLoader defaultClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            return classLoader;
        }
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            return contextClassLoader;
        }
        return ClassResolvers.class.getClassLoader();
    }

    private ClassResolvers() {
    }
}

