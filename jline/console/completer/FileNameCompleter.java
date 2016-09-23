
package jline.console.completer;

import java.io.File;
import java.util.List;
import jline.console.completer.Completer;
import jline.internal.Configuration;
import jline.internal.Preconditions;


public class FileNameCompleter
implements Completer {
    private static final boolean OS_IS_WINDOWS;

    @Override
    public int complete(String buffer, int cursor, List<CharSequence> candidates) {
        Preconditions.checkNotNull(candidates);
        if (buffer == null) {
            buffer = "";
        }
        if (OS_IS_WINDOWS) {
            buffer = buffer.replace('/', '\\');
        }
        String translated = buffer;
        File homeDir = this.getUserHome();
        if (translated.startsWith("~" + this.separator())) {
            translated = homeDir.getPath() + translated.substring(1);
        } else if (translated.startsWith("~")) {
            translated = homeDir.getParentFile().getAbsolutePath();
        } else if (!translated.startsWith(this.separator())) {
            String cwd = this.getUserDir().getAbsolutePath();
            translated = cwd + this.separator() + translated;
        }
        File file = new File(translated);
        File dir = translated.endsWith(this.separator()) ? file : file.getParentFile();
        File[] entries = dir == null ? new File[]{} : dir.listFiles();
        return this.matchFiles(buffer, translated, entries, candidates);
    }

    protected String separator() {
        return File.separator;
    }

    protected File getUserHome() {
        return Configuration.getUserHome();
    }

    protected File getUserDir() {
        return new File(".");
    }

    protected int matchFiles(String buffer, String translated, File[] files, List<CharSequence> candidates) {
        if (files == null) {
            return -1;
        }
        int matches = 0;
        for (File file2 : files) {
            if (!file2.getAbsolutePath().startsWith(translated)) continue;
            ++matches;
        }
        for (File file2 : files) {
            if (!file2.getAbsolutePath().startsWith(translated)) continue;
            String name = file2.getName() + (matches == 1 && file2.isDirectory() ? this.separator() : " ");
            candidates.add(this.render(file2, name).toString());
        }
        int index = buffer.lastIndexOf(this.separator());
        return index + this.separator().length();
    }

    protected CharSequence render(File file, CharSequence name) {
        return name;
    }

    static {
        String os = Configuration.getOsName();
        OS_IS_WINDOWS = os.contains("windows");
    }
}

