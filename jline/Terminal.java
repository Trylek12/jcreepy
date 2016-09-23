
package jline;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Terminal {
    public void init() throws Exception;

    public void restore() throws Exception;

    public void reset() throws Exception;

    public boolean isSupported();

    public int getWidth();

    public int getHeight();

    public boolean isAnsiSupported();

    public OutputStream wrapOutIfNeeded(OutputStream var1);

    public InputStream wrapInIfNeeded(InputStream var1) throws IOException;

    public boolean hasWeirdWrap();

    public boolean isEchoEnabled();

    public void setEchoEnabled(boolean var1);
}

