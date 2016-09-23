
package jline.console.completer;

import java.util.Collection;
import jline.console.completer.StringsCompleter;
import jline.internal.Preconditions;


public class EnumCompleter
extends StringsCompleter {
    public EnumCompleter(Class<? extends Enum> source) {
        Preconditions.checkNotNull(source);
        for (Enum n : source.getEnumConstants()) {
            this.getStrings().add(n.name().toLowerCase());
        }
    }
}

