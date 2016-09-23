
package io.netty.handler.ssl;

import java.util.concurrent.Executor;

final class ImmediateExecutor
implements Executor {
    static final ImmediateExecutor INSTANCE = new ImmediateExecutor();

    ImmediateExecutor() {
    }

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}

