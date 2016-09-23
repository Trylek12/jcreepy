
package jline.console.completer;

import java.util.List;
import jline.console.completer.Completer;


public final class NullCompleter
implements Completer {
    public static final NullCompleter INSTANCE = new NullCompleter();

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        return -1;
    }
}

