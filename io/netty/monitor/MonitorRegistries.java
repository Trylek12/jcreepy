
package io.netty.monitor;

import io.netty.monitor.MonitorRegistry;
import io.netty.monitor.spi.MonitorProvider;
import io.netty.monitor.spi.MonitorRegistryFactory;
import java.util.Iterator;
import java.util.ServiceLoader;

public final class MonitorRegistries
implements Iterable<MonitorRegistry> {
    private static final ServiceLoader<MonitorRegistryFactory> FACTORIES = ServiceLoader.load(MonitorRegistryFactory.class);

    public static MonitorRegistries instance() {
        return Holder.INSTANCE;
    }

    public MonitorRegistry forProvider(MonitorProvider provider) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        for (MonitorRegistryFactory candidate : FACTORIES) {
            if (!candidate.provider().equals(provider)) continue;
            return candidate.newMonitorRegistry();
        }
        throw new IllegalArgumentException("Could not find MonitorRegistryFactory by provider " + provider + " among the set of registered MonitorRegistryFactories");
    }

    public MonitorRegistry unique() {
        Iterator<MonitorRegistry> registries = this.iterator();
        if (!registries.hasNext()) {
            throw new IllegalStateException("Could not find any MonitorRegistries the classpath - implementations need to be registered in META-INF/services/" + MonitorRegistryFactory.class.getName());
        }
        MonitorRegistry candidate = registries.next();
        if (registries.hasNext()) {
            throw new IllegalStateException("Found more than one MonitorRegistryFactory on the classpath - check if there is more than one implementation registered in META-INF/services/" + MonitorRegistryFactory.class.getName());
        }
        return candidate;
    }

    @Override
    public Iterator<MonitorRegistry> iterator() {
        return new MonitorRegistryIterator(FACTORIES.iterator());
    }

    private MonitorRegistries() {
    }

    private static final class MonitorRegistryIterator
    implements Iterator<MonitorRegistry> {
        private final Iterator<MonitorRegistryFactory> factories;

        private MonitorRegistryIterator(Iterator<MonitorRegistryFactory> factories) {
            this.factories = factories;
        }

        @Override
        public boolean hasNext() {
            return this.factories.hasNext();
        }

        @Override
        public MonitorRegistry next() {
            return this.factories.next().newMonitorRegistry();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Removing a MonitorRegistry is not supported");
        }
    }

    private static interface Holder {
        public static final MonitorRegistries INSTANCE = new MonitorRegistries();
    }

}

