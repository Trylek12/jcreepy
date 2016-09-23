/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.jboss.logging.Logger
 */
package io.netty.logging;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.logging.JBossLogger;
import org.jboss.logging.Logger;

public class JBossLoggerFactory
extends InternalLoggerFactory {
    @Override
    public InternalLogger newInstance(String name) {
        Logger logger = Logger.getLogger((String)name);
        return new JBossLogger(logger);
    }
}

