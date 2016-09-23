/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package io.netty.logging;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.logging.Slf4JLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4JLoggerFactory
extends InternalLoggerFactory {
    @Override
    public InternalLogger newInstance(String name) {
        Logger logger = LoggerFactory.getLogger((String)name);
        return new Slf4JLogger(logger);
    }
}

