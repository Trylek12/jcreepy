
package io.netty.monitor;

public interface CounterMonitor {
    public static final CounterMonitor NOOP = new CounterMonitor(){

        @Override
        public void reset() {
        }

        @Override
        public void inc(long delta) {
        }

        @Override
        public void inc() {
        }

        @Override
        public void decr(long delta) {
        }

        @Override
        public void decr() {
        }
    };

    public void inc();

    public void inc(long var1);

    public void decr();

    public void decr(long var1);

    public void reset();

}

