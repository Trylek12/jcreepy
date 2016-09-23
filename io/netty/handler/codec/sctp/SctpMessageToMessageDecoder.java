
package io.netty.handler.codec.sctp;

import io.netty.channel.socket.SctpMessage;
import io.netty.handler.codec.CodecException;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.sctp.SctpMessageCompletionHandler;

public abstract class SctpMessageToMessageDecoder<O>
extends MessageToMessageDecoder<SctpMessage, O> {
    public SctpMessageToMessageDecoder() {
        super(new Class[0]);
    }

    @Override
    public boolean isDecodable(Object msg) throws Exception {
        if (msg instanceof SctpMessage) {
            SctpMessage sctpMsg = (SctpMessage)msg;
            if (sctpMsg.isComplete()) {
                return true;
            }
            throw new CodecException(String.format("Received SctpMessage is not complete, please add %s in the pipeline before this handler", SctpMessageCompletionHandler.class.getSimpleName()));
        }
        return false;
    }
}

