
package jline.console.completer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import jline.console.completer.Completer;
import jline.internal.Preconditions;


public class AggregateCompleter
implements Completer {
    private final List<Completer> completers = new ArrayList<Completer>();

    public AggregateCompleter() {
    }

    public AggregateCompleter(Collection<Completer> completers) {
        Preconditions.checkNotNull(completers);
        this.completers.addAll(completers);
    }

    public /* varargs */ AggregateCompleter(Completer ... completers) {
        this(Arrays.asList(completers));
    }

    public Collection<Completer> getCompleters() {
        return this.completers;
    }

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        Preconditions.checkNotNull(candidates);
        ArrayList<Completion> completions = new ArrayList<Completion>(this.completers.size());
        int max = -1;
        for (Completer completer : this.completers) {
            Completion completion = new Completion(candidates);
            completion.complete(completer, buffer, cursor);
            max = Math.max(max, completion.cursor);
            completions.add(completion);
        }
        for (Completion completion : completions) {
            if (completion.cursor != max) continue;
            candidates.addAll(completion.candidates);
        }
        return max;
    }

    public String toString() {
        return this.getClass().getSimpleName() + "{" + "completers=" + this.completers + '}';
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private class Completion {
        public final List<CharSequence> candidates;
        public int cursor;

        public Completion(List<CharSequence> candidates) {
            Preconditions.checkNotNull(candidates);
            this.candidates = new LinkedList<CharSequence>(candidates);
        }

        public void complete(Completer completer, String buffer, int cursor) {
            Preconditions.checkNotNull(completer);
            this.cursor = completer.complete(buffer, cursor, this.candidates);
        }
    }

}

