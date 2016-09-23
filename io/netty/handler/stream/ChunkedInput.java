
package io.netty.handler.stream;

public interface ChunkedInput<B> {
    public boolean isEndOfInput() throws Exception;

    public void close() throws Exception;

    public boolean readChunk(B var1) throws Exception;
}

