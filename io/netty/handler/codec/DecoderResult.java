
package io.netty.handler.codec;

public class DecoderResult {
    public static final DecoderResult SUCCESS = new DecoderResult(false, null);
    private final boolean partial;
    private final Throwable cause;

    public static DecoderResult failure(Throwable cause) {
        if (cause == null) {
            throw new NullPointerException("cause");
        }
        return new DecoderResult(false, cause);
    }

    public static DecoderResult partialFailure(Throwable cause) {
        if (cause == null) {
            throw new NullPointerException("cause");
        }
        return new DecoderResult(true, cause);
    }

    protected DecoderResult(boolean partial, Throwable cause) {
        if (partial && cause == null) {
            throw new IllegalArgumentException("successful result cannot be partial.");
        }
        this.partial = partial;
        this.cause = cause;
    }

    public boolean isSuccess() {
        return this.cause == null;
    }

    public boolean isFailure() {
        return this.cause != null;
    }

    public boolean isCompleteFailure() {
        return this.cause != null && !this.partial;
    }

    public boolean isPartialFailure() {
        return this.partial;
    }

    public Throwable cause() {
        return this.cause;
    }

    public String toString() {
        if (this.isSuccess()) {
            return "success";
        }
        String cause = this.cause().toString();
        StringBuilder buf = new StringBuilder(cause.length() + 17);
        if (this.isPartialFailure()) {
            buf.append("partial_");
        }
        buf.append("failure(");
        buf.append(cause);
        buf.append(')');
        return buf.toString();
    }
}

