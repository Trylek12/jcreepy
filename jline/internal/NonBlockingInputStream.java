
package jline.internal;

import java.io.IOException;
import java.io.InputStream;
import jline.internal.Log;

public class NonBlockingInputStream
extends InputStream
implements Runnable {
    private InputStream in;
    private int ch = -2;
    private boolean threadIsReading = false;
    private boolean isShutdown = false;
    private IOException exception = null;
    private boolean nonBlockingEnabled;

    public NonBlockingInputStream(InputStream in, boolean isNonBlockingEnabled) {
        this.in = in;
        this.nonBlockingEnabled = isNonBlockingEnabled;
        if (isNonBlockingEnabled) {
            Thread t = new Thread(this);
            t.setName("NonBlockingInputStreamThread");
            t.setDaemon(true);
            t.start();
        }
    }

    public synchronized void shutdown() {
        if (!this.isShutdown && this.nonBlockingEnabled) {
            this.isShutdown = true;
            this.notify();
        }
    }

    public boolean isNonBlockingEnabled() {
        return this.nonBlockingEnabled && !this.isShutdown;
    }

    public void close() throws IOException {
        this.in.close();
        this.shutdown();
    }

    public int read() throws IOException {
        if (this.nonBlockingEnabled) {
            return this.read(0, false);
        }
        return this.in.read();
    }

    public int peek(long timeout) throws IOException {
        if (!this.nonBlockingEnabled || this.isShutdown) {
            throw new UnsupportedOperationException("peek() cannot be called as non-blocking operation is disabled");
        }
        return this.read(timeout, true);
    }

    public int read(long timeout) throws IOException {
        if (!this.nonBlockingEnabled || this.isShutdown) {
            throw new UnsupportedOperationException("read() with timeout cannot be called as non-blocking operation is disabled");
        }
        return this.read(timeout, false);
    }

    private synchronized int read(long timeout, boolean isPeek) throws IOException {
        if (this.exception != null) {
            assert (this.ch == -2);
            IOException toBeThrown = this.exception;
            if (!isPeek) {
                this.exception = null;
            }
            throw toBeThrown;
        }
        if (this.ch >= -1) {
            assert (this.exception == null);
        } else if ((timeout == 0 || this.isShutdown) && !this.threadIsReading) {
            this.ch = this.in.read();
        } else {
            boolean isInfinite;
            if (!this.threadIsReading) {
                this.threadIsReading = true;
                this.notify();
            }
            boolean bl = isInfinite = timeout <= 0;
            while (isInfinite || timeout > 0) {
                long start = System.currentTimeMillis();
                try {
                    this.wait(timeout);
                }
                catch (InterruptedException e) {
                    // empty catch block
                }
                if (this.exception != null) {
                    assert (this.ch == -2);
                    IOException toBeThrown = this.exception;
                    if (!isPeek) {
                        this.exception = null;
                    }
                    throw toBeThrown;
                }
                if (this.ch >= -1) {
                    assert (this.exception == null);
                    break;
                }
                if (isInfinite) continue;
                timeout -= System.currentTimeMillis() - start;
            }
        }
        int ret = this.ch;
        if (!isPeek) {
            this.ch = -2;
        }
        return ret;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if (len == 0) {
            return 0;
        }
        int c = this.nonBlockingEnabled ? this.read(0) : this.in.read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte)c;
        return 1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void run() {
        Log.debug("NonBlockingInputStream start");
        boolean needToShutdown = false;
        boolean needToRead = false;
        while (!needToShutdown) {
            NonBlockingInputStream nonBlockingInputStream = this;
            synchronized (nonBlockingInputStream) {
                needToShutdown = this.isShutdown;
                needToRead = this.threadIsReading;
                try {
                    if (!needToShutdown && !needToRead) {
                        this.wait(0);
                    }
                }
                catch (InterruptedException e) {
                    // empty catch block
                }
            }
            if (needToShutdown || !needToRead) continue;
            int charRead = -2;
            IOException failure = null;
            try {
                charRead = this.in.read();
            }
            catch (IOException e2) {
                failure = e2;
            }
            NonBlockingInputStream e2 = this;
            synchronized (e2) {
                this.exception = failure;
                this.ch = charRead;
                this.threadIsReading = false;
                this.notify();
                continue;
            }
        }
        Log.debug("NonBlockingInputStream shutdown");
    }
}

