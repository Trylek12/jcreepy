
package io.netty.monitor.spi;

import io.netty.monitor.MonitorRegistry;
import io.netty.monitor.spi.MonitorProvider;

public interface MonitorRegistryFactory {
    public MonitorProvider provider();

    public MonitorRegistry newMonitorRegistry();
}

