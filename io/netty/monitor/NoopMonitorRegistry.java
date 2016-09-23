
package io.netty.monitor;

import io.netty.monitor.CounterMonitor;
import io.netty.monitor.EventRateMonitor;
import io.netty.monitor.MonitorName;
import io.netty.monitor.MonitorRegistry;
import io.netty.monitor.ValueDistributionMonitor;
import io.netty.monitor.ValueMonitor;
import java.util.concurrent.TimeUnit;

final class NoopMonitorRegistry
implements MonitorRegistry {
    static final NoopMonitorRegistry INSTANCE = new NoopMonitorRegistry();

    @Override
    public ValueDistributionMonitor newValueDistributionMonitor(MonitorName monitorName) {
        return ValueDistributionMonitor.NOOP;
    }

    @Override
    public EventRateMonitor newEventRateMonitor(MonitorName monitorName, TimeUnit rateUnit) {
        return EventRateMonitor.NOOP;
    }

    @Override
    public <T> ValueMonitor<T> registerValueMonitor(MonitorName monitorName, ValueMonitor<T> valueMonitor) {
        return valueMonitor;
    }

    @Override
    public CounterMonitor newCounterMonitor(MonitorName monitorName) {
        return CounterMonitor.NOOP;
    }

    private NoopMonitorRegistry() {
    }
}

