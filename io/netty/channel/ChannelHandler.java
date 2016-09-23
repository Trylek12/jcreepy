
package io.netty.channel;

import io.netty.channel.ChannelHandlerContext;
import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface ChannelHandler {
    public void beforeAdd(ChannelHandlerContext var1) throws Exception;

    public void afterAdd(ChannelHandlerContext var1) throws Exception;

    public void beforeRemove(ChannelHandlerContext var1) throws Exception;

    public void afterRemove(ChannelHandlerContext var1) throws Exception;

    public void exceptionCaught(ChannelHandlerContext var1, Throwable var2) throws Exception;

    public void userEventTriggered(ChannelHandlerContext var1, Object var2) throws Exception;

    @Inherited
    @Documented
    @Target(value={ElementType.TYPE})
    @Retention(value=RetentionPolicy.RUNTIME)
    public static @interface Sharable {
    }

}

