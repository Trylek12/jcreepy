/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.apache.commons.logging.Log
 *  org.apache.commons.logging.LogFactory
 */
package io.netty.logging;

import io.netty.logging.CommonsLogger;
import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class CommonsLoggerFactory
extends InternalLoggerFactory {
    @Override
    public InternalLogger newInstance(String name) {
        return new CommonsLogger(LogFactory.getLog((String)name), name);
    }
}

