
package io.netty.handler.stream;

import io.netty.buffer.MessageBuf;
import io.netty.handler.stream.ChunkedInput;

public interface ChunkedMessageInput<T>
extends ChunkedInput<MessageBuf<T>> {
}

