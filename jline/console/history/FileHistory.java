
package jline.console.history;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import jline.console.history.History;
import jline.console.history.MemoryHistory;
import jline.console.history.PersistentHistory;
import jline.internal.Log;
import jline.internal.Preconditions;

public class FileHistory
extends MemoryHistory
implements PersistentHistory,
Flushable {
    private final File file;

    public FileHistory(File file) throws IOException {
        this.file = Preconditions.checkNotNull(file);
        this.load(file);
    }

    public File getFile() {
        return this.file;
    }

    public void load(File file) throws IOException {
        Preconditions.checkNotNull(file);
        if (file.exists()) {
            Log.trace("Loading history from: ", file);
            this.load(new FileReader(file));
        }
    }

    public void load(InputStream input) throws IOException {
        Preconditions.checkNotNull(input);
        this.load(new InputStreamReader(input));
    }

    public void load(Reader reader) throws IOException {
        String item;
        Preconditions.checkNotNull(reader);
        BufferedReader input = new BufferedReader(reader);
        while ((item = input.readLine()) != null) {
            this.internalAdd(item);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void flush() throws IOException {
        Log.trace("Flushing history");
        if (!this.file.exists()) {
            File dir = this.file.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                Log.warn("Failed to create directory: ", dir);
            }
            if (!this.file.createNewFile()) {
                Log.warn("Failed to create file: ", this.file);
            }
        }
        PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(this.file)));
        try {
            for (History.Entry entry : this) {
                out.println(entry.value());
            }
        }
        finally {
            out.close();
        }
    }

    public void purge() throws IOException {
        Log.trace("Purging history");
        this.clear();
        if (!this.file.delete()) {
            Log.warn("Failed to delete history file: ", this.file);
        }
    }
}

