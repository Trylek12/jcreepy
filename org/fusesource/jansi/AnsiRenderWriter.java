
package org.fusesource.jansi;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Locale;
import org.fusesource.jansi.AnsiRenderer;

public class AnsiRenderWriter
extends PrintWriter {
    public AnsiRenderWriter(OutputStream out) {
        super(out);
    }

    public AnsiRenderWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public AnsiRenderWriter(Writer out) {
        super(out);
    }

    public AnsiRenderWriter(Writer out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public void write(String s) {
        if (AnsiRenderer.test(s)) {
            super.write(AnsiRenderer.render(s));
        } else {
            super.write(s);
        }
    }

    public /* varargs */ PrintWriter format(String format, Object ... args) {
        this.print(String.format(format, args));
        return this;
    }

    public /* varargs */ PrintWriter format(Locale l, String format, Object ... args) {
        this.print(String.format(l, format, args));
        return this;
    }
}

