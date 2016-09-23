
package io.netty.monitor;

public interface EventRateMonitor {
    public static final EventRateMonitor NOOP = new EventRateMonitor(){

        @Override
        public void events(long count) {
        }

        @Override
        public void event() {
        }
    };

    public void event();

    public void events(long var1);

}

