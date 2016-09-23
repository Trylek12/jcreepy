
package io.netty.buffer;

import io.netty.buffer.AbstractByteBuf;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ChannelBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.internal.DetectionUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;

final class DefaultCompositeByteBuf
extends AbstractByteBuf
implements CompositeByteBuf,
ByteBuf.Unsafe {
    private static final ByteBuffer[] EMPTY_NIOBUFFERS = new ByteBuffer[0];
    private final ByteBufAllocator alloc;
    private final List<Component> components = new ArrayList<Component>();
    private final int maxNumComponents;
    private Component lastAccessed;
    private int lastAccessedId;
    private boolean freed;
    private Queue<ByteBuf> suspendedDeallocations;

    public DefaultCompositeByteBuf(ByteBufAllocator alloc, int maxNumComponents) {
        super(Integer.MAX_VALUE);
        if (alloc == null) {
            throw new NullPointerException("alloc");
        }
        this.alloc = alloc;
        this.maxNumComponents = maxNumComponents;
    }

    public /* varargs */ DefaultCompositeByteBuf(ByteBufAllocator alloc, int maxNumComponents, ByteBuf ... buffers) {
        super(Integer.MAX_VALUE);
        if (alloc == null) {
            throw new NullPointerException("alloc");
        }
        if (maxNumComponents < 2) {
            throw new IllegalArgumentException("maxNumComponents: " + maxNumComponents + " (expected: >= 2)");
        }
        this.alloc = alloc;
        this.maxNumComponents = maxNumComponents;
        this.addComponents0(0, buffers);
        this.consolidateIfNeeded();
        this.setIndex(0, this.capacity());
    }

    public DefaultCompositeByteBuf(ByteBufAllocator alloc, int maxNumComponents, Iterable<ByteBuf> buffers) {
        super(Integer.MAX_VALUE);
        if (alloc == null) {
            throw new NullPointerException("alloc");
        }
        if (maxNumComponents < 2) {
            throw new IllegalArgumentException("maxNumComponents: " + maxNumComponents + " (expected: >= 2)");
        }
        this.alloc = alloc;
        this.maxNumComponents = maxNumComponents;
        this.addComponents0(0, buffers);
        this.consolidateIfNeeded();
        this.setIndex(0, this.capacity());
    }

    @Override
    public CompositeByteBuf addComponent(ByteBuf buffer) {
        this.addComponent0(this.components.size(), buffer, false);
        this.consolidateIfNeeded();
        return this;
    }

    @Override
    public /* varargs */ CompositeByteBuf addComponents(ByteBuf ... buffers) {
        this.addComponents0(this.components.size(), buffers);
        this.consolidateIfNeeded();
        return this;
    }

    @Override
    public CompositeByteBuf addComponents(Iterable<ByteBuf> buffers) {
        this.addComponents0(this.components.size(), buffers);
        this.consolidateIfNeeded();
        return this;
    }

    @Override
    public CompositeByteBuf addComponent(int cIndex, ByteBuf buffer) {
        this.addComponent0(cIndex, buffer, false);
        this.consolidateIfNeeded();
        return this;
    }

    private int addComponent0(int cIndex, ByteBuf buffer, boolean addedBySelf) {
        this.checkComponentIndex(cIndex);
        if (buffer == null) {
            throw new NullPointerException("buffer");
        }
        if (buffer instanceof Iterable) {
            Iterable composite = (Iterable)((Object)buffer);
            return this.addComponents0(cIndex, composite);
        }
        int readableBytes = buffer.readableBytes();
        if (readableBytes == 0) {
            return cIndex;
        }
        Component c = new Component(buffer.order(ByteOrder.BIG_ENDIAN).slice(), addedBySelf);
        if (cIndex == this.components.size()) {
            this.components.add(c);
            if (cIndex == 0) {
                c.endOffset = readableBytes;
            } else {
                Component prev = this.components.get(cIndex - 1);
                c.offset = prev.endOffset;
                c.endOffset = c.offset + readableBytes;
            }
        } else {
            this.components.add(cIndex, c);
            this.updateComponentOffsets(cIndex);
        }
        return cIndex;
    }

    @Override
    public /* varargs */ CompositeByteBuf addComponents(int cIndex, ByteBuf ... buffers) {
        this.addComponents0(cIndex, buffers);
        this.consolidateIfNeeded();
        return this;
    }

    private /* varargs */ int addComponents0(int cIndex, ByteBuf ... buffers) {
        this.checkComponentIndex(cIndex);
        if (buffers == null) {
            throw new NullPointerException("buffers");
        }
        int readableBytes = 0;
        for (ByteBuf b2 : buffers) {
            if (b2 == null) break;
            readableBytes += b2.readableBytes();
        }
        if (readableBytes == 0) {
            return cIndex;
        }
        for (ByteBuf b2 : buffers) {
            int size;
            if (b2 == null) break;
            if (!b2.readable() || (cIndex = this.addComponent0(cIndex, b2, false) + 1) <= (size = this.components.size())) continue;
            cIndex = size;
        }
        return cIndex;
    }

    @Override
    public CompositeByteBuf addComponents(int cIndex, Iterable<ByteBuf> buffers) {
        this.addComponents0(cIndex, buffers);
        this.consolidateIfNeeded();
        return this;
    }

    private int addComponents0(int cIndex, Iterable<ByteBuf> buffers) {
        if (buffers == null) {
            throw new NullPointerException("buffers");
        }
        if (buffers instanceof DefaultCompositeByteBuf) {
            List<Component> list = ((DefaultCompositeByteBuf)buffers).components;
            ByteBuf[] array = new ByteBuf[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = list.get((int)i).buf;
            }
            return this.addComponents0(cIndex, array);
        }
        if (buffers instanceof List) {
            List list = (List)buffers;
            ByteBuf[] array = new ByteBuf[list.size()];
            for (int i = 0; i < array.length; ++i) {
                array[i] = (ByteBuf)list.get(i);
            }
            return this.addComponents0(cIndex, array);
        }
        if (buffers instanceof Collection) {
            Collection col = (Collection)buffers;
            ByteBuf[] array = new ByteBuf[col.size()];
            int i = 0;
            for (ByteBuf b : col) {
                array[i++] = b;
            }
            return this.addComponents0(cIndex, array);
        }
        ArrayList<ByteBuf> list = new ArrayList<ByteBuf>();
        for (ByteBuf b : buffers) {
            list.add(b);
        }
        return this.addComponents0(cIndex, list.toArray(new ByteBuf[list.size()]));
    }

    private void consolidateIfNeeded() {
        int numComponents = this.components.size();
        if (numComponents > this.maxNumComponents) {
            int capacity = this.components.get((int)(numComponents - 1)).endOffset;
            ByteBuf consolidated = this.alloc().buffer(capacity);
            for (int i = 0; i < numComponents; ++i) {
                Component c = this.components.get(i);
                ByteBuf b = c.buf;
                consolidated.writeBytes(b);
                c.freeIfNecessary();
            }
            Component c = new Component(consolidated, true);
            c.endOffset = c.length;
            this.components.clear();
            this.components.add(c);
        }
    }

    private void checkComponentIndex(int cIndex) {
        assert (!this.freed);
        if (cIndex < 0 || cIndex > this.components.size()) {
            throw new IndexOutOfBoundsException(String.format("cIndex: %d (expected: >= 0 && <= numComponents(%d))", cIndex, this.components.size()));
        }
    }

    private void checkComponentIndex(int cIndex, int numComponents) {
        assert (!this.freed);
        if (cIndex < 0 || cIndex + numComponents > this.components.size()) {
            throw new IndexOutOfBoundsException(String.format("cIndex: %d, numComponents: %d (expected: cIndex >= 0 && cIndex + numComponents <= totalNumComponents(%d))", cIndex, numComponents, this.components.size()));
        }
    }

    private void updateComponentOffsets(int cIndex) {
        Component c;
        this.lastAccessed = c = this.components.get(cIndex);
        this.lastAccessedId = cIndex;
        if (cIndex == 0) {
            c.offset = 0;
            c.endOffset = c.length;
            ++cIndex;
        }
        for (int i = cIndex; i < this.components.size(); ++i) {
            Component prev = this.components.get(i - 1);
            Component cur = this.components.get(i);
            cur.offset = prev.endOffset;
            cur.endOffset = cur.offset + cur.length;
        }
    }

    @Override
    public CompositeByteBuf removeComponent(int cIndex) {
        this.checkComponentIndex(cIndex);
        this.components.remove(cIndex);
        this.updateComponentOffsets(cIndex);
        return this;
    }

    @Override
    public CompositeByteBuf removeComponents(int cIndex, int numComponents) {
        this.checkComponentIndex(cIndex, numComponents);
        this.components.subList(cIndex, cIndex + numComponents).clear();
        this.updateComponentOffsets(cIndex);
        return this;
    }

    @Override
    public Iterator<ByteBuf> iterator() {
        assert (!this.freed);
        ArrayList<ByteBuf> list = new ArrayList<ByteBuf>(this.components.size());
        for (Component c : this.components) {
            list.add(c.buf);
        }
        return list.iterator();
    }

    @Override
    public List<ByteBuf> decompose(int offset, int length) {
        int readableBytes;
        if (length == 0) {
            return Collections.emptyList();
        }
        if (offset + length > this.capacity()) {
            throw new IndexOutOfBoundsException("Too many bytes to decompose - Need " + (offset + length) + ", capacity is " + this.capacity());
        }
        int componentId = this.toComponentIndex(offset);
        ArrayList<ByteBuf> slice = new ArrayList<ByteBuf>(this.components.size());
        Component firstC = this.components.get(componentId);
        ByteBuf first = firstC.buf.duplicate();
        first.readerIndex(offset - firstC.offset);
        ByteBuf buf = first;
        int bytesToSlice = length;
        do {
            if (bytesToSlice <= (readableBytes = buf.readableBytes())) {
                buf.writerIndex(buf.readerIndex() + bytesToSlice);
                slice.add(buf);
                break;
            }
            slice.add(buf);
            buf = this.components.get((int)(++componentId)).buf.duplicate();
        } while ((bytesToSlice -= readableBytes) > 0);
        for (int i = 0; i < slice.size(); ++i) {
            slice.set(i, slice.get(i).slice());
        }
        return slice;
    }

    @Override
    public boolean isDirect() {
        if (this.components.size() == 1) {
            return this.components.get((int)0).buf.isDirect();
        }
        return false;
    }

    @Override
    public boolean hasArray() {
        if (this.components.size() == 1) {
            return this.components.get((int)0).buf.hasArray();
        }
        return false;
    }

    @Override
    public byte[] array() {
        if (this.components.size() == 1) {
            return this.components.get((int)0).buf.array();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int arrayOffset() {
        if (this.components.size() == 1) {
            return this.components.get((int)0).buf.arrayOffset();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public int capacity() {
        if (this.components.isEmpty()) {
            return 0;
        }
        return this.components.get((int)(this.components.size() - 1)).endOffset;
    }

    @Override
    public CompositeByteBuf capacity(int newCapacity) {
        assert (!this.freed);
        if (newCapacity < 0 || newCapacity > this.maxCapacity()) {
            throw new IllegalArgumentException("newCapacity: " + newCapacity);
        }
        int oldCapacity = this.capacity();
        if (newCapacity > oldCapacity) {
            int paddingLength = newCapacity - oldCapacity;
            if (this.components.isEmpty()) {
                ByteBuf padding = this.alloc().buffer(paddingLength, paddingLength);
                padding.setIndex(0, paddingLength);
                this.addComponent0(0, padding, true);
            } else {
                ByteBuf padding = this.alloc().buffer(paddingLength);
                padding.setIndex(0, paddingLength);
                this.addComponent0(this.components.size(), padding, true);
                this.consolidateIfNeeded();
            }
        } else if (newCapacity < oldCapacity) {
            int bytesToTrim = oldCapacity - newCapacity;
            ListIterator<Component> i = this.components.listIterator(this.components.size());
            while (i.hasPrevious()) {
                Component c = i.previous();
                if (bytesToTrim >= c.length) {
                    bytesToTrim -= c.length;
                    i.remove();
                    continue;
                }
                Component newC = new Component(c.buf.slice(0, c.length - bytesToTrim), c.allocatedBySelf);
                newC.offset = c.offset;
                newC.endOffset = newC.offset + newC.length;
                i.set(newC);
                break;
            }
            if (this.readerIndex() > newCapacity) {
                this.setIndex(newCapacity, newCapacity);
            } else if (this.writerIndex() > newCapacity) {
                this.writerIndex(newCapacity);
            }
        }
        return this;
    }

    @Override
    public ByteBufAllocator alloc() {
        return this.alloc;
    }

    @Override
    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    @Override
    public int numComponents() {
        return this.components.size();
    }

    @Override
    public int maxNumComponents() {
        return this.maxNumComponents;
    }

    @Override
    public int toComponentIndex(int offset) {
        assert (!this.freed);
        if (offset < 0 || offset >= this.capacity()) {
            throw new IndexOutOfBoundsException(String.format("offset: %d (expected: >= 0 && < capacity(%d))", offset, this.capacity()));
        }
        Component c = this.lastAccessed;
        if (c == null) {
            this.lastAccessed = c = this.components.get(0);
        }
        if (offset >= c.offset) {
            if (offset < c.endOffset) {
                return this.lastAccessedId;
            }
            for (int i = this.lastAccessedId + 1; i < this.components.size(); ++i) {
                c = this.components.get(i);
                if (offset >= c.endOffset) continue;
                this.lastAccessedId = i;
                this.lastAccessed = c;
                return i;
            }
        } else {
            for (int i = this.lastAccessedId - 1; i >= 0; --i) {
                c = this.components.get(i);
                if (offset < c.offset) continue;
                this.lastAccessedId = i;
                this.lastAccessed = c;
                return i;
            }
        }
        throw new IllegalStateException("should not reach here - concurrent modification?");
    }

    @Override
    public int toByteIndex(int cIndex) {
        this.checkComponentIndex(cIndex);
        return this.components.get((int)cIndex).offset;
    }

    @Override
    public byte getByte(int index) {
        Component c = this.findComponent(index);
        return c.buf.getByte(index - c.offset);
    }

    @Override
    public short getShort(int index) {
        Component c = this.findComponent(index);
        if (index + 2 <= c.endOffset) {
            return c.buf.getShort(index - c.offset);
        }
        if (this.order() == ByteOrder.BIG_ENDIAN) {
            return (short)((this.getByte(index) & 255) << 8 | this.getByte(index + 1) & 255);
        }
        return (short)(this.getByte(index) & 255 | (this.getByte(index + 1) & 255) << 8);
    }

    @Override
    public int getUnsignedMedium(int index) {
        Component c = this.findComponent(index);
        if (index + 3 <= c.endOffset) {
            return c.buf.getUnsignedMedium(index - c.offset);
        }
        if (this.order() == ByteOrder.BIG_ENDIAN) {
            return (this.getShort(index) & 65535) << 8 | this.getByte(index + 2) & 255;
        }
        return this.getShort(index) & 65535 | (this.getByte(index + 2) & 255) << 16;
    }

    @Override
    public int getInt(int index) {
        Component c = this.findComponent(index);
        if (index + 4 <= c.endOffset) {
            return c.buf.getInt(index - c.offset);
        }
        if (this.order() == ByteOrder.BIG_ENDIAN) {
            return (this.getShort(index) & 65535) << 16 | this.getShort(index + 2) & 65535;
        }
        return this.getShort(index) & 65535 | (this.getShort(index + 2) & 65535) << 16;
    }

    @Override
    public long getLong(int index) {
        Component c = this.findComponent(index);
        if (index + 8 <= c.endOffset) {
            return c.buf.getLong(index - c.offset);
        }
        if (this.order() == ByteOrder.BIG_ENDIAN) {
            return ((long)this.getInt(index) & 0xFFFFFFFFL) << 32 | (long)this.getInt(index + 4) & 0xFFFFFFFFL;
        }
        return (long)this.getInt(index) & 0xFFFFFFFFL | ((long)this.getInt(index + 4) & 0xFFFFFFFFL) << 32;
    }

    @Override
    public CompositeByteBuf getBytes(int index, byte[] dst, int dstIndex, int length) {
        if (index > this.capacity() - length || dstIndex > dst.length - length) {
            throw new IndexOutOfBoundsException("Too many bytes to read - Needs " + (index + length) + ", maximum is " + this.capacity() + " or " + dst.length);
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("index must be >= 0");
        }
        if (length == 0) {
            return this;
        }
        int i = this.toComponentIndex(index);
        while (length > 0) {
            Component c = this.components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.getBytes(index - adjustment, dst, dstIndex, localLength);
            index += localLength;
            dstIndex += localLength;
            length -= localLength;
            ++i;
        }
        return this;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public CompositeByteBuf getBytes(int index, ByteBuffer dst) {
        int limit = dst.limit();
        int length = dst.remaining();
        if (index > this.capacity() - length) {
            throw new IndexOutOfBoundsException("Too many bytes to be read - Needs " + (index + length) + ", maximum is " + this.capacity());
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("index must be >= 0");
        }
        if (length == 0) {
            return this;
        }
        int i = this.toComponentIndex(index);
        try {
            while (length > 0) {
                Component c = this.components.get(i);
                ByteBuf s = c.buf;
                int adjustment = c.offset;
                int localLength = Math.min(length, s.capacity() - (index - adjustment));
                dst.limit(dst.position() + localLength);
                s.getBytes(index - adjustment, dst);
                index += localLength;
                length -= localLength;
                ++i;
            }
        }
        finally {
            dst.limit(limit);
        }
        return this;
    }

    @Override
    public CompositeByteBuf getBytes(int index, ByteBuf dst, int dstIndex, int length) {
        if (index > this.capacity() - length || dstIndex > dst.capacity() - length) {
            throw new IndexOutOfBoundsException("Too many bytes to be read - Needs " + (index + length) + " or " + (dstIndex + length) + ", maximum is " + this.capacity() + " or " + dst.capacity());
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("index must be >= 0");
        }
        if (length == 0) {
            return this;
        }
        int i = this.toComponentIndex(index);
        while (length > 0) {
            Component c = this.components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.getBytes(index - adjustment, dst, dstIndex, localLength);
            index += localLength;
            dstIndex += localLength;
            length -= localLength;
            ++i;
        }
        return this;
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length) throws IOException {
        if (DetectionUtil.javaVersion() < 7) {
            return out.write(this.copiedNioBuffer(index, length));
        }
        long writtenBytes = out.write(this.nioBuffers(index, length));
        if (writtenBytes > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int)writtenBytes;
    }

    @Override
    public CompositeByteBuf getBytes(int index, OutputStream out, int length) throws IOException {
        if (index > this.capacity() - length) {
            throw new IndexOutOfBoundsException("Too many bytes to be read - needs " + (index + length) + ", maximum of " + this.capacity());
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("index must be >= 0");
        }
        if (length == 0) {
            return this;
        }
        int i = this.toComponentIndex(index);
        while (length > 0) {
            Component c = this.components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.getBytes(index - adjustment, out, localLength);
            index += localLength;
            length -= localLength;
            ++i;
        }
        return this;
    }

    @Override
    public CompositeByteBuf setByte(int index, int value) {
        Component c = this.findComponent(index);
        c.buf.setByte(index - c.offset, value);
        return this;
    }

    @Override
    public CompositeByteBuf setShort(int index, int value) {
        Component c = this.findComponent(index);
        if (index + 2 <= c.endOffset) {
            c.buf.setShort(index - c.offset, value);
        } else if (this.order() == ByteOrder.BIG_ENDIAN) {
            this.setByte(index, (byte)(value >>> 8));
            this.setByte(index + 1, (byte)value);
        } else {
            this.setByte(index, (byte)value);
            this.setByte(index + 1, (byte)(value >>> 8));
        }
        return this;
    }

    @Override
    public CompositeByteBuf setMedium(int index, int value) {
        Component c = this.findComponent(index);
        if (index + 3 <= c.endOffset) {
            c.buf.setMedium(index - c.offset, value);
        } else if (this.order() == ByteOrder.BIG_ENDIAN) {
            this.setShort(index, (short)(value >> 8));
            this.setByte(index + 2, (byte)value);
        } else {
            this.setShort(index, (short)value);
            this.setByte(index + 2, (byte)(value >>> 16));
        }
        return this;
    }

    @Override
    public CompositeByteBuf setInt(int index, int value) {
        Component c = this.findComponent(index);
        if (index + 4 <= c.endOffset) {
            c.buf.setInt(index - c.offset, value);
        } else if (this.order() == ByteOrder.BIG_ENDIAN) {
            this.setShort(index, (short)(value >>> 16));
            this.setShort(index + 2, (short)value);
        } else {
            this.setShort(index, (short)value);
            this.setShort(index + 2, (short)(value >>> 16));
        }
        return this;
    }

    @Override
    public CompositeByteBuf setLong(int index, long value) {
        Component c = this.findComponent(index);
        if (index + 8 <= c.endOffset) {
            c.buf.setLong(index - c.offset, value);
        } else if (this.order() == ByteOrder.BIG_ENDIAN) {
            this.setInt(index, (int)(value >>> 32));
            this.setInt(index + 4, (int)value);
        } else {
            this.setInt(index, (int)value);
            this.setInt(index + 4, (int)(value >>> 32));
        }
        return this;
    }

    @Override
    public CompositeByteBuf setBytes(int index, byte[] src, int srcIndex, int length) {
        int componentId = this.toComponentIndex(index);
        if (index > this.capacity() - length || srcIndex > src.length - length) {
            throw new IndexOutOfBoundsException("Too many bytes to read - needs " + (index + length) + " or " + (srcIndex + length) + ", maximum is " + this.capacity() + " or " + src.length);
        }
        int i = componentId;
        while (length > 0) {
            Component c = this.components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.setBytes(index - adjustment, src, srcIndex, localLength);
            index += localLength;
            srcIndex += localLength;
            length -= localLength;
            ++i;
        }
        return this;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public CompositeByteBuf setBytes(int index, ByteBuffer src) {
        int componentId = this.toComponentIndex(index);
        int limit = src.limit();
        int length = src.remaining();
        if (index > this.capacity() - length) {
            throw new IndexOutOfBoundsException("Too many bytes to be written - Needs " + (index + length) + ", maximum is " + this.capacity());
        }
        int i = componentId;
        try {
            while (length > 0) {
                Component c = this.components.get(i);
                ByteBuf s = c.buf;
                int adjustment = c.offset;
                int localLength = Math.min(length, s.capacity() - (index - adjustment));
                src.limit(src.position() + localLength);
                s.setBytes(index - adjustment, src);
                index += localLength;
                length -= localLength;
                ++i;
            }
        }
        finally {
            src.limit(limit);
        }
        return this;
    }

    @Override
    public CompositeByteBuf setBytes(int index, ByteBuf src, int srcIndex, int length) {
        int componentId = this.toComponentIndex(index);
        if (index > this.capacity() - length || srcIndex > src.capacity() - length) {
            throw new IndexOutOfBoundsException("Too many bytes to be written - Needs " + (index + length) + " or " + (srcIndex + length) + ", maximum is " + this.capacity() + " or " + src.capacity());
        }
        int i = componentId;
        while (length > 0) {
            Component c = this.components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.setBytes(index - adjustment, src, srcIndex, localLength);
            index += localLength;
            srcIndex += localLength;
            length -= localLength;
            ++i;
        }
        return this;
    }

    @Override
    public int setBytes(int index, InputStream in, int length) throws IOException {
        int componentId = this.toComponentIndex(index);
        if (index > this.capacity() - length) {
            throw new IndexOutOfBoundsException("Too many bytes to write - Needs " + (index + length) + ", maximum is " + this.capacity());
        }
        int i = componentId;
        int readBytes = 0;
        do {
            Component c = this.components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            int localReadBytes = s.setBytes(index - adjustment, in, localLength);
            if (localReadBytes < 0) {
                if (readBytes != 0) break;
                return -1;
            }
            if (localReadBytes == localLength) {
                index += localLength;
                length -= localLength;
                readBytes += localLength;
                ++i;
                continue;
            }
            index += localReadBytes;
            length -= localReadBytes;
            readBytes += localReadBytes;
        } while (length > 0);
        return readBytes;
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length) throws IOException {
        int componentId = this.toComponentIndex(index);
        if (index > this.capacity() - length) {
            throw new IndexOutOfBoundsException("Too many bytes to write - Needs " + (index + length) + ", maximum is " + this.capacity());
        }
        int i = componentId;
        int readBytes = 0;
        do {
            Component c = this.components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            int localReadBytes = s.setBytes(index - adjustment, in, localLength);
            if (localReadBytes == 0) break;
            if (localReadBytes < 0) {
                if (readBytes != 0) break;
                return -1;
            }
            if (localReadBytes == localLength) {
                index += localLength;
                length -= localLength;
                readBytes += localLength;
                ++i;
                continue;
            }
            index += localReadBytes;
            length -= localReadBytes;
            readBytes += localReadBytes;
        } while (length > 0);
        return readBytes;
    }

    @Override
    public ByteBuf copy(int index, int length) {
        int componentId = this.toComponentIndex(index);
        if (index > this.capacity() - length) {
            throw new IndexOutOfBoundsException("Too many bytes to copy - Needs " + (index + length) + ", maximum is " + this.capacity());
        }
        ByteBuf dst = Unpooled.buffer(length);
        this.copyTo(index, length, componentId, dst);
        return dst;
    }

    private void copyTo(int index, int length, int componentId, ByteBuf dst) {
        int dstIndex = 0;
        int i = componentId;
        while (length > 0) {
            Component c = this.components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            s.getBytes(index - adjustment, dst, dstIndex, localLength);
            index += localLength;
            dstIndex += localLength;
            length -= localLength;
            ++i;
        }
        dst.writerIndex(dst.capacity());
    }

    @Override
    public ByteBuf component(int cIndex) {
        this.checkComponentIndex(cIndex);
        return this.components.get((int)cIndex).buf;
    }

    @Override
    public ByteBuf componentAtOffset(int offset) {
        return this.findComponent((int)offset).buf;
    }

    private Component findComponent(int offset) {
        assert (!this.freed);
        if (offset < 0 || offset >= this.capacity()) {
            throw new IndexOutOfBoundsException(String.format("offset: %d (expected: >= 0 && < capacity(%d))", offset, this.capacity()));
        }
        Component c = this.lastAccessed;
        if (c == null) {
            this.lastAccessed = c = this.components.get(0);
        }
        if (offset >= c.offset) {
            if (offset < c.endOffset) {
                return c;
            }
            for (int i = this.lastAccessedId + 1; i < this.components.size(); ++i) {
                c = this.components.get(i);
                if (offset >= c.endOffset) continue;
                this.lastAccessedId = i;
                this.lastAccessed = c;
                return c;
            }
        } else {
            for (int i = this.lastAccessedId - 1; i >= 0; --i) {
                c = this.components.get(i);
                if (offset < c.offset) continue;
                this.lastAccessedId = i;
                this.lastAccessed = c;
                return c;
            }
        }
        throw new IllegalStateException("should not reach here - concurrent modification?");
    }

    @Override
    public boolean hasNioBuffer() {
        if (this.components.size() == 1) {
            return this.components.get((int)0).buf.hasNioBuffer();
        }
        return false;
    }

    @Override
    public ByteBuffer nioBuffer(int index, int length) {
        if (this.components.size() == 1) {
            return this.components.get((int)0).buf.nioBuffer(index, length);
        }
        throw new UnsupportedOperationException();
    }

    private ByteBuffer copiedNioBuffer(int index, int length) {
        assert (!this.freed);
        if (this.components.size() == 1) {
            return DefaultCompositeByteBuf.toNioBuffer(this.components.get((int)0).buf, index, length);
        }
        ByteBuffer[] buffers = this.nioBuffers(index, length);
        ByteBuffer merged = ByteBuffer.allocate(length).order(this.order());
        for (ByteBuffer b : buffers) {
            merged.put(b);
        }
        merged.flip();
        return merged;
    }

    @Override
    public boolean hasNioBuffers() {
        return true;
    }

    @Override
    public ByteBuffer[] nioBuffers(int index, int length) {
        if (index + length > this.capacity()) {
            throw new IndexOutOfBoundsException("Too many bytes to convert - Needs" + (index + length) + ", maximum is " + this.capacity());
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("index must be >= 0");
        }
        if (length == 0) {
            return EMPTY_NIOBUFFERS;
        }
        int componentId = this.toComponentIndex(index);
        ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>(this.components.size());
        int i = componentId;
        while (length > 0) {
            Component c = this.components.get(i);
            ByteBuf s = c.buf;
            int adjustment = c.offset;
            int localLength = Math.min(length, s.capacity() - (index - adjustment));
            buffers.add(DefaultCompositeByteBuf.toNioBuffer(s, index - adjustment, localLength));
            index += localLength;
            length -= localLength;
            ++i;
        }
        return buffers.toArray(new ByteBuffer[buffers.size()]);
    }

    private static ByteBuffer toNioBuffer(ByteBuf buf, int index, int length) {
        if (buf.hasNioBuffer()) {
            return buf.nioBuffer(index, length);
        }
        return buf.copy(index, length).nioBuffer(0, length);
    }

    @Override
    public CompositeByteBuf consolidate() {
        assert (!this.freed);
        int numComponents = this.numComponents();
        if (numComponents <= 1) {
            return this;
        }
        Component last = this.components.get(numComponents - 1);
        int capacity = last.endOffset;
        ByteBuf consolidated = this.alloc().buffer(capacity);
        for (int i = 0; i < numComponents; ++i) {
            Component c = this.components.get(i);
            ByteBuf b = c.buf;
            consolidated.writeBytes(b);
            c.freeIfNecessary();
        }
        this.components.clear();
        this.components.add(new Component(consolidated, true));
        this.updateComponentOffsets(0);
        return this;
    }

    @Override
    public CompositeByteBuf consolidate(int cIndex, int numComponents) {
        this.checkComponentIndex(cIndex, numComponents);
        if (numComponents <= 1) {
            return this;
        }
        int endCIndex = cIndex + numComponents;
        Component last = this.components.get(endCIndex - 1);
        int capacity = last.endOffset - this.components.get((int)cIndex).offset;
        ByteBuf consolidated = this.alloc().buffer(capacity);
        for (int i = cIndex; i < endCIndex; ++i) {
            Component c = this.components.get(i);
            ByteBuf b = c.buf;
            consolidated.writeBytes(b);
            c.freeIfNecessary();
        }
        this.components.subList(cIndex + 1, endCIndex).clear();
        this.components.set(cIndex, new Component(consolidated, true));
        this.updateComponentOffsets(cIndex);
        return this;
    }

    @Override
    public CompositeByteBuf discardReadComponents() {
        assert (!this.freed);
        int readerIndex = this.readerIndex();
        if (readerIndex == 0) {
            return this;
        }
        int writerIndex = this.writerIndex();
        if (readerIndex == writerIndex && writerIndex == this.capacity()) {
            for (Component c : this.components) {
                c.freeIfNecessary();
            }
            this.components.clear();
            this.setIndex(0, 0);
            this.adjustMarkers(readerIndex);
            return this;
        }
        int firstComponentId = this.toComponentIndex(readerIndex);
        for (int i = 0; i < firstComponentId; ++i) {
            this.components.get(i).freeIfNecessary();
        }
        this.components.subList(0, firstComponentId).clear();
        Component first = this.components.get(0);
        this.updateComponentOffsets(0);
        this.setIndex(readerIndex - first.offset, writerIndex - first.offset);
        this.adjustMarkers(first.offset);
        return this;
    }

    @Override
    public CompositeByteBuf discardReadBytes() {
        assert (!this.freed);
        int readerIndex = this.readerIndex();
        if (readerIndex == 0) {
            return this;
        }
        int writerIndex = this.writerIndex();
        if (readerIndex == writerIndex && writerIndex == this.capacity()) {
            for (Component c : this.components) {
                c.freeIfNecessary();
            }
            this.components.clear();
            this.setIndex(0, 0);
            this.adjustMarkers(readerIndex);
            return this;
        }
        int firstComponentId = this.toComponentIndex(readerIndex);
        for (int i = 0; i < firstComponentId; ++i) {
            this.components.get(i).freeIfNecessary();
        }
        this.components.subList(0, firstComponentId).clear();
        Component c = this.components.get(0);
        int adjustment = readerIndex - c.offset;
        if (adjustment == c.length) {
            this.components.remove(0);
        } else {
            Component newC = new Component(c.buf.slice(adjustment, c.length - adjustment), c.allocatedBySelf);
            this.components.set(0, newC);
        }
        this.updateComponentOffsets(0);
        this.setIndex(0, writerIndex - readerIndex);
        this.adjustMarkers(readerIndex);
        return this;
    }

    @Override
    public String toString() {
        String result = super.toString();
        result = result.substring(0, result.length() - 1);
        return result + ", components=" + this.components.size() + ')';
    }

    @Override
    public CompositeByteBuf readerIndex(int readerIndex) {
        return (CompositeByteBuf)super.readerIndex(readerIndex);
    }

    @Override
    public CompositeByteBuf writerIndex(int writerIndex) {
        return (CompositeByteBuf)super.writerIndex(writerIndex);
    }

    @Override
    public CompositeByteBuf setIndex(int readerIndex, int writerIndex) {
        return (CompositeByteBuf)super.setIndex(readerIndex, writerIndex);
    }

    @Override
    public CompositeByteBuf clear() {
        return (CompositeByteBuf)super.clear();
    }

    @Override
    public CompositeByteBuf markReaderIndex() {
        return (CompositeByteBuf)super.markReaderIndex();
    }

    @Override
    public CompositeByteBuf resetReaderIndex() {
        return (CompositeByteBuf)super.resetReaderIndex();
    }

    @Override
    public CompositeByteBuf markWriterIndex() {
        return (CompositeByteBuf)super.markWriterIndex();
    }

    @Override
    public CompositeByteBuf resetWriterIndex() {
        return (CompositeByteBuf)super.resetWriterIndex();
    }

    @Override
    public CompositeByteBuf ensureWritableBytes(int minWritableBytes) {
        return (CompositeByteBuf)super.ensureWritableBytes(minWritableBytes);
    }

    @Override
    public CompositeByteBuf getBytes(int index, ByteBuf dst) {
        return (CompositeByteBuf)super.getBytes(index, dst);
    }

    @Override
    public CompositeByteBuf getBytes(int index, ByteBuf dst, int length) {
        return (CompositeByteBuf)super.getBytes(index, dst, length);
    }

    @Override
    public CompositeByteBuf getBytes(int index, byte[] dst) {
        return (CompositeByteBuf)super.getBytes(index, dst);
    }

    @Override
    public CompositeByteBuf setBoolean(int index, boolean value) {
        return (CompositeByteBuf)super.setBoolean(index, value);
    }

    @Override
    public CompositeByteBuf setChar(int index, int value) {
        return (CompositeByteBuf)super.setChar(index, value);
    }

    @Override
    public CompositeByteBuf setFloat(int index, float value) {
        return (CompositeByteBuf)super.setFloat(index, value);
    }

    @Override
    public CompositeByteBuf setDouble(int index, double value) {
        return (CompositeByteBuf)super.setDouble(index, value);
    }

    @Override
    public CompositeByteBuf setBytes(int index, ByteBuf src) {
        return (CompositeByteBuf)super.setBytes(index, src);
    }

    @Override
    public CompositeByteBuf setBytes(int index, ByteBuf src, int length) {
        return (CompositeByteBuf)super.setBytes(index, src, length);
    }

    @Override
    public CompositeByteBuf setBytes(int index, byte[] src) {
        return (CompositeByteBuf)super.setBytes(index, src);
    }

    @Override
    public CompositeByteBuf setZero(int index, int length) {
        return (CompositeByteBuf)super.setZero(index, length);
    }

    @Override
    public CompositeByteBuf readBytes(ByteBuf dst) {
        return (CompositeByteBuf)super.readBytes(dst);
    }

    @Override
    public CompositeByteBuf readBytes(ByteBuf dst, int length) {
        return (CompositeByteBuf)super.readBytes(dst, length);
    }

    @Override
    public CompositeByteBuf readBytes(ByteBuf dst, int dstIndex, int length) {
        return (CompositeByteBuf)super.readBytes(dst, dstIndex, length);
    }

    @Override
    public CompositeByteBuf readBytes(byte[] dst) {
        return (CompositeByteBuf)super.readBytes(dst);
    }

    @Override
    public CompositeByteBuf readBytes(byte[] dst, int dstIndex, int length) {
        return (CompositeByteBuf)super.readBytes(dst, dstIndex, length);
    }

    @Override
    public CompositeByteBuf readBytes(ByteBuffer dst) {
        return (CompositeByteBuf)super.readBytes(dst);
    }

    @Override
    public CompositeByteBuf readBytes(OutputStream out, int length) throws IOException {
        return (CompositeByteBuf)super.readBytes(out, length);
    }

    @Override
    public CompositeByteBuf skipBytes(int length) {
        return (CompositeByteBuf)super.skipBytes(length);
    }

    @Override
    public CompositeByteBuf writeBoolean(boolean value) {
        return (CompositeByteBuf)super.writeBoolean(value);
    }

    @Override
    public CompositeByteBuf writeByte(int value) {
        return (CompositeByteBuf)super.writeByte(value);
    }

    @Override
    public CompositeByteBuf writeShort(int value) {
        return (CompositeByteBuf)super.writeShort(value);
    }

    @Override
    public CompositeByteBuf writeMedium(int value) {
        return (CompositeByteBuf)super.writeMedium(value);
    }

    @Override
    public CompositeByteBuf writeInt(int value) {
        return (CompositeByteBuf)super.writeInt(value);
    }

    @Override
    public CompositeByteBuf writeLong(long value) {
        return (CompositeByteBuf)super.writeLong(value);
    }

    @Override
    public CompositeByteBuf writeChar(int value) {
        return (CompositeByteBuf)super.writeChar(value);
    }

    @Override
    public CompositeByteBuf writeFloat(float value) {
        return (CompositeByteBuf)super.writeFloat(value);
    }

    @Override
    public CompositeByteBuf writeDouble(double value) {
        return (CompositeByteBuf)super.writeDouble(value);
    }

    @Override
    public CompositeByteBuf writeBytes(ByteBuf src) {
        return (CompositeByteBuf)super.writeBytes(src);
    }

    @Override
    public CompositeByteBuf writeBytes(ByteBuf src, int length) {
        return (CompositeByteBuf)super.writeBytes(src, length);
    }

    @Override
    public CompositeByteBuf writeBytes(ByteBuf src, int srcIndex, int length) {
        return (CompositeByteBuf)super.writeBytes(src, srcIndex, length);
    }

    @Override
    public CompositeByteBuf writeBytes(byte[] src) {
        return (CompositeByteBuf)super.writeBytes(src);
    }

    @Override
    public CompositeByteBuf writeBytes(byte[] src, int srcIndex, int length) {
        return (CompositeByteBuf)super.writeBytes(src, srcIndex, length);
    }

    @Override
    public CompositeByteBuf writeBytes(ByteBuffer src) {
        return (CompositeByteBuf)super.writeBytes(src);
    }

    @Override
    public CompositeByteBuf writeZero(int length) {
        return (CompositeByteBuf)super.writeZero(length);
    }

    @Override
    public ByteBuffer[] nioBuffers() {
        return this.nioBuffers(this.readerIndex(), this.readableBytes());
    }

    @Override
    public ByteBuffer internalNioBuffer() {
        if (this.components.size() == 1) {
            return this.components.get((int)0).buf.unsafe().internalNioBuffer();
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer[] internalNioBuffers() {
        ByteBuffer[] nioBuffers = new ByteBuffer[this.components.size()];
        int index = 0;
        for (Component component : this.components) {
            nioBuffers[index++] = component.buf.unsafe().internalNioBuffer();
        }
        return nioBuffers;
    }

    @Override
    public void discardSomeReadBytes() {
        this.discardReadComponents();
    }

    @Override
    public boolean isFreed() {
        return this.freed;
    }

    @Override
    public void free() {
        if (this.freed) {
            return;
        }
        this.freed = true;
        this.resumeIntermediaryDeallocations();
        for (Component c : this.components) {
            c.freeIfNecessary();
        }
    }

    @Override
    public void suspendIntermediaryDeallocations() {
        if (this.suspendedDeallocations == null) {
            this.suspendedDeallocations = new ArrayDeque<ByteBuf>(2);
        }
    }

    @Override
    public void resumeIntermediaryDeallocations() {
        if (this.suspendedDeallocations == null) {
            return;
        }
        Queue<ByteBuf> suspendedDeallocations = this.suspendedDeallocations;
        this.suspendedDeallocations = null;
        for (ByteBuf buf : suspendedDeallocations) {
            buf.unsafe().free();
        }
    }

    @Override
    public ByteBuf unwrap() {
        return null;
    }

    @Override
    public ByteBuf.Unsafe unsafe() {
        return this;
    }

    private final class Component {
        final ByteBuf buf;
        final int length;
        final boolean allocatedBySelf;
        int offset;
        int endOffset;

        Component(ByteBuf buf, boolean allocatedBySelf) {
            this.buf = buf;
            this.length = buf.readableBytes();
            this.allocatedBySelf = allocatedBySelf;
        }

        void freeIfNecessary() {
            if (!this.allocatedBySelf) {
                return;
            }
            ByteBuf buf = this.buf;
            while (buf.unwrap() != null) {
                buf = buf.unwrap();
            }
            if (DefaultCompositeByteBuf.this.suspendedDeallocations == null) {
                buf.unsafe().free();
            } else {
                DefaultCompositeByteBuf.this.suspendedDeallocations.add(buf);
            }
        }
    }

}

