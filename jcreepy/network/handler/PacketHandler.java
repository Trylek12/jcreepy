
package jcreepy.network.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import jcreepy.network.Packet;
import jcreepy.network.Session;

public abstract class PacketHandler<T extends Packet> {
    public abstract void handlePacket(T var1, Session var2) throws Exception;

    public void addToQueue(ExecutorService ex, T packet, Session session) {
        ex.submit(new Runnable((Packet)packet, session){
            final /* synthetic */ Packet val$packet;
            final /* synthetic */ Session val$session;

            @Override
            public void run() {
                block2 : {
                    try {
                        PacketHandler.this.handlePacket(this.val$packet, this.val$session);
                    }
                    catch (Exception ex) {
                        if (this.val$session.getChannel() == null) break block2;
                        this.val$session.getChannel().pipeline().fireExceptionCaught(ex);
                    }
                }
            }
        });
    }

}

