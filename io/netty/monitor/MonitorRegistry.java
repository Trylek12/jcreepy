
package io.netty.monitor;

import io.netty.monitor.CounterMonitor;
import io.netty.monitor.EventRateMonitor;
import io.netty.monitor.MonitorName;
import io.netty.monitor.NoopMonitorRegistry;
import io.netty.monitor.ValueDistributionMonitor;
import io.netty.monitor.ValueMonitor;
import java.util.concurrent.TimeUnit;

public interface MonitorRegistry {
    public static final MonitorRegistry NOOP = NoopMonitorRegistry.INSTANCE;

    public ValueDistributionMonitor newValueDistributionMonitor(MonitorName var1);

    public EventRateMonitor newEventRateMonitor(MonitorName var1, TimeUnit var2);

    public <T> ValueMonitor<T> registerValueMonitor(MonitorName var1, ValueMonitor<T> var2);

    public CounterMonitor newCounterMonitor(MonitorName var1);
}

