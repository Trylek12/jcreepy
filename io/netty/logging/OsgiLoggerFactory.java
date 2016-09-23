/*
 * Decompiled with CFR 0_115.
 * 
 * Could not load the following classes:
 *  org.osgi.framework.BundleContext
 *  org.osgi.framework.ServiceReference
 *  org.osgi.service.log.LogService
 *  org.osgi.util.tracker.ServiceTracker
 *  org.osgi.util.tracker.ServiceTrackerCustomizer
 */
package io.netty.logging;

import io.netty.logging.InternalLogger;
import io.netty.logging.InternalLoggerFactory;
import io.netty.logging.JdkLoggerFactory;
import io.netty.logging.OsgiLogger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class OsgiLoggerFactory
extends InternalLoggerFactory {
    private final ServiceTracker logServiceTracker;
    private final InternalLoggerFactory fallback;
    volatile LogService logService;

    public OsgiLoggerFactory(BundleContext ctx) {
        this(ctx, null);
    }

    public OsgiLoggerFactory(BundleContext ctx, InternalLoggerFactory fallback) {
        if (ctx == null) {
            throw new NullPointerException("ctx");
        }
        if (fallback == null && (fallback = InternalLoggerFactory.getDefaultFactory()) instanceof OsgiLoggerFactory) {
            fallback = new JdkLoggerFactory();
        }
        this.fallback = fallback;
        this.logServiceTracker = new ServiceTracker(ctx, "org.osgi.service.log.LogService", null){

            public Object addingService(ServiceReference reference) {
                LogService service;
                OsgiLoggerFactory.this.logService = service = (LogService)super.addingService(reference);
                return service;
            }

            public void removedService(ServiceReference reference, Object service) {
                OsgiLoggerFactory.this.logService = null;
            }
        };
        this.logServiceTracker.open();
    }

    public InternalLoggerFactory getFallback() {
        return this.fallback;
    }

    public LogService getLogService() {
        return this.logService;
    }

    public void destroy() {
        this.logService = null;
        this.logServiceTracker.close();
    }

    @Override
    public InternalLogger newInstance(String name) {
        return new OsgiLogger(this, name, this.fallback.newInstance(name));
    }

}

