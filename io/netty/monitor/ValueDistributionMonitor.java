
package io.netty.monitor;

public interface ValueDistributionMonitor {
    public static final ValueDistributionMonitor NOOP = new ValueDistributionMonitor(){

        @Override
        public void update(long value) {
        }

        @Override
        public void reset() {
        }
    };

    public void reset();

    public void update(long var1);

}

