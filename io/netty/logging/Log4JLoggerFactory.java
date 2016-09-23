/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.apache.log4j.Logger
 */
package io.netty.logging;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.logging.Log4JLogger;
import org.apache.log4j.Logger;

public class Log4JLoggerFactory
extends InternalLoggerFactory {
    @Override
    public InternalLogger newInstance(String name) {
        Logger logger = Logger.getLogger((String)name);
        return new Log4JLogger(logger);
    }
}

