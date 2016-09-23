
package io.netty.logging;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.logging.JdkLogger;
import java.util.logging.Logger;

public class JdkLoggerFactory
extends InternalLoggerFactory {
    @Override
    public InternalLogger newInstance(String name) {
        Logger logger = Logger.getLogger(name);
        return new JdkLogger(logger, name);
    }
}

