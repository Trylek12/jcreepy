
package jline.console;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Stack;
import jline.Terminal;
import jline.TerminalFactory;
import jline.console.ConsoleKeys;
import jline.console.CursorBuffer;
import jline.console.KeyMap;
import jline.console.Operation;
import jline.console.completer.CandidateListCompletionHandler;
import jline.console.completer.Completer;
import jline.console.completer.CompletionHandler;
import jline.console.history.History;
import jline.console.history.MemoryHistory;
import jline.internal.Configuration;
import jline.internal.InputStreamReader;
import jline.internal.Log;
import jline.internal.NonBlockingInputStream;
import jline.internal.Nullable;
import jline.internal.Preconditions;
import jline.internal.Urls;
import org.fusesource.jansi.AnsiOutputStream;


public class ConsoleReader {
    public static final String JLINE_NOBELL = "jline.nobell";
    public static final String JLINE_ESC_TIMEOUT = "jline.esc.timeout";
    public static final String JLINE_INPUTRC = "jline.inputrc";
    public static final String INPUT_RC = ".inputrc";
    public static final String DEFAULT_INPUT_RC = "/etc/inputrc";
    public static final char BACKSPACE = '\b';
    public static final char RESET_LINE = '\r';
    public static final char KEYBOARD_BELL = '\u0007';
    public static final char NULL_MASK = '\u0000';
    public static final int TAB_WIDTH = 4;
    private static final ResourceBundle resources = ResourceBundle.getBundle(CandidateListCompletionHandler.class.getName());
    private final Terminal terminal;
    private final Writer out;
    private final CursorBuffer buf = new CursorBuffer();
    private String prompt;
    private int promptLen;
    private boolean expandEvents = true;
    private boolean bellEnabled = !Configuration.getBoolean("jline.nobell", true);
    private Character mask;
    private Character echoCharacter;
    private StringBuffer searchTerm = null;
    private String previousSearchTerm = "";
    private int searchIndex = -1;
    private int parenBlinkTimeout = 500;
    private NonBlockingInputStream in;
    private long escapeTimeout;
    private Reader reader;
    private boolean isUnitTestInput;
    private char charSearchChar = '\u0000';
    private char charSearchLastInvokeChar = '\u0000';
    private char charSearchFirstInvokeChar = '\u0000';
    private String yankBuffer = "";
    private String encoding;
    private boolean recording;
    private String macro = "";
    private String appName;
    private URL inputrcUrl;
    private ConsoleKeys consoleKeys;
    private String commentBegin = null;
    private boolean skipLF = false;
    private State state = State.NORMAL;
    public static final String JLINE_COMPLETION_THRESHOLD = "jline.completion.threshold";
    private final List<Completer> completers = new LinkedList<Completer>();
    private CompletionHandler completionHandler = new CandidateListCompletionHandler();
    private int autoprintThreshold = Configuration.getInteger("jline.completion.threshold", 100);
    private boolean paginationEnabled;
    private History history = new MemoryHistory();
    private boolean historyEnabled = true;
    public static final String CR = Configuration.getLineSeparator();
    private final Map<Character, ActionListener> triggeredActions = new HashMap<Character, ActionListener>();
    private Thread maskThread;

    public ConsoleReader() throws IOException {
        this(null, new FileInputStream(FileDescriptor.in), System.out, null);
    }

    public ConsoleReader(InputStream in, OutputStream out) throws IOException {
        this(null, in, out, null);
    }

    public ConsoleReader(InputStream in, OutputStream out, Terminal term) throws IOException {
        this(null, in, out, term);
    }

    public ConsoleReader(@Nullable String appName, InputStream in, OutputStream out, @Nullable Terminal term) throws IOException {
        this(appName, in, out, term, null);
    }

    public ConsoleReader(@Nullable String appName, InputStream in, OutputStream out, @Nullable Terminal term, @Nullable String encoding) throws IOException {
        this.appName = appName != null ? appName : "JLine";
        this.encoding = encoding != null ? encoding : Configuration.getEncoding();
        this.terminal = term != null ? term : TerminalFactory.get();
        this.out = new OutputStreamWriter(this.terminal.wrapOutIfNeeded(out), this.encoding);
        this.setInput(in);
        this.inputrcUrl = this.getInputRc();
        this.consoleKeys = new ConsoleKeys(appName, this.inputrcUrl);
    }

    private URL getInputRc() throws IOException {
        String path = Configuration.getString("jline.inputrc");
        if (path == null) {
            File f = new File(Configuration.getUserHome(), ".inputrc");
            if (!f.exists()) {
                f = new File("/etc/inputrc");
            }
            return f.toURI().toURL();
        }
        return Urls.create(path);
    }

    public KeyMap getKeys() {
        return this.consoleKeys.getKeys();
    }

    void setInput(InputStream in) throws IOException {
        boolean nonBlockingEnabled;
        this.escapeTimeout = Configuration.getLong("jline.esc.timeout", 100);
        this.isUnitTestInput = in instanceof ByteArrayInputStream;
        boolean bl = nonBlockingEnabled = this.escapeTimeout > 0 && this.terminal.isSupported() && in != null;
        if (this.in != null) {
            this.in.shutdown();
        }
        InputStream wrapped = this.terminal.wrapInIfNeeded(in);
        this.in = new NonBlockingInputStream(wrapped, nonBlockingEnabled);
        this.reader = new InputStreamReader((InputStream)this.in, this.encoding);
    }

    public void shutdown() {
        if (this.in != null) {
            this.in.shutdown();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void finalize() throws Throwable {
        try {
            this.shutdown();
        }
        finally {
            super.finalize();
        }
    }

    public InputStream getInput() {
        return this.in;
    }

    public Writer getOutput() {
        return this.out;
    }

    public Terminal getTerminal() {
        return this.terminal;
    }

    public CursorBuffer getCursorBuffer() {
        return this.buf;
    }

    public void setExpandEvents(boolean expand) {
        this.expandEvents = expand;
    }

    public boolean getExpandEvents() {
        return this.expandEvents;
    }

    public void setBellEnabled(boolean enabled) {
        this.bellEnabled = enabled;
    }

    public boolean getBellEnabled() {
        return this.bellEnabled;
    }

    public void setCommentBegin(String commentBegin) {
        this.commentBegin = commentBegin;
    }

    public String getCommentBegin() {
        String str = this.commentBegin;
        if (str == null && (str = this.consoleKeys.getVariable("comment-begin")) == null) {
            str = "#";
        }
        return str;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
        this.promptLen = prompt == null ? 0 : this.stripAnsi(this.lastLine(prompt)).length();
    }

    public String getPrompt() {
        return this.prompt;
    }

    public void setEchoCharacter(Character c) {
        this.echoCharacter = c;
    }

    public Character getEchoCharacter() {
        return this.echoCharacter;
    }

    protected final boolean resetLine() throws IOException {
        if (this.buf.cursor == 0) {
            return false;
        }
        this.backspaceAll();
        return true;
    }

    int getCursorPosition() {
        return this.promptLen + this.buf.cursor;
    }

    private String lastLine(String str) {
        if (str == null) {
            return "";
        }
        int last = str.lastIndexOf("\n");
        if (last >= 0) {
            return str.substring(last + 1, str.length());
        }
        return str;
    }

    private String stripAnsi(String str) {
        if (str == null) {
            return "";
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            AnsiOutputStream aos = new AnsiOutputStream(baos);
            aos.write(str.getBytes());
            aos.flush();
            return baos.toString();
        }
        catch (IOException e) {
            return str;
        }
    }

    public final boolean setCursorPosition(int position) throws IOException {
        if (position == this.buf.cursor) {
            return true;
        }
        return this.moveCursor(position - this.buf.cursor) != 0;
    }

    private void setBuffer(String buffer) throws IOException {
        if (buffer.equals(this.buf.buffer.toString())) {
            return;
        }
        int sameIndex = 0;
        int l1 = buffer.length();
        int l2 = this.buf.buffer.length();
        for (int i = 0; i < l1 && i < l2 && buffer.charAt(i) == this.buf.buffer.charAt(i); ++i) {
            ++sameIndex;
        }
        int diff = this.buf.cursor - sameIndex;
        if (diff < 0) {
            this.moveToEnd();
            diff = this.buf.buffer.length() - sameIndex;
        }
        this.backspace(diff);
        this.killLine();
        this.buf.buffer.setLength(sameIndex);
        this.putString(buffer.substring(sameIndex));
    }

    private void setBuffer(CharSequence buffer) throws IOException {
        this.setBuffer(String.valueOf(buffer));
    }

    public final void drawLine() throws IOException {
        String prompt = this.getPrompt();
        if (prompt != null) {
            this.print(prompt);
        }
        this.print(this.buf.buffer.toString());
        if (this.buf.length() != this.buf.cursor) {
            this.back(this.buf.length() - this.buf.cursor - 1);
        }
        this.drawBuffer();
    }

    public final void redrawLine() throws IOException {
        this.print(13);
        this.drawLine();
    }

    final String finishBuffer() throws IOException {
        String str;
        String historyLine = str = this.buf.buffer.toString();
        if (this.expandEvents) {
            str = this.expandEvents(str);
            historyLine = str.replaceAll("\\!", "\\\\!");
        }
        if (str.length() > 0) {
            if (this.mask == null && this.isHistoryEnabled()) {
                this.history.add(historyLine);
            } else {
                this.mask = null;
            }
        }
        this.history.moveToEnd();
        this.buf.buffer.setLength(0);
        this.buf.cursor = 0;
        return str;
    }

    protected String expandEvents(String str) throws IOException {
        String result;
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        block14 : for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (escaped) {
                sb.append(c);
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            escaped = false;
            switch (c) {
                case '!': {
                    if (i + 1 < str.length()) {
                        c = str.charAt(++i);
                        boolean neg = false;
                        String rep = null;
                        switch (c) {
                            int idx;
                            int i1;
                            case '!': {
                                if (this.history.size() == 0) {
                                    throw new IllegalArgumentException("!!: event not found");
                                }
                                rep = this.history.get(this.history.index() - 1).toString();
                                break;
                            }
                            case '#': {
                                sb.append(sb.toString());
                                break;
                            }
                            case '?': {
                                i1 = str.indexOf(63, i + 1);
                                if (i1 < 0) {
                                    i1 = str.length();
                                }
                                String sc = str.substring(i + 1, i1);
                                i = i1;
                                idx = this.searchBackwards(sc);
                                if (idx < 0) {
                                    throw new IllegalArgumentException("!?" + sc + ": event not found");
                                }
                                rep = this.history.get(idx).toString();
                                break;
                            }
                            case '\t': 
                            case ' ': {
                                sb.append('!');
                                sb.append(c);
                                break;
                            }
                            case '-': {
                                neg = true;
                                ++i;
                            }
                            case '0': 
                            case '1': 
                            case '2': 
                            case '3': 
                            case '4': 
                            case '5': 
                            case '6': 
                            case '7': 
                            case '8': 
                            case '9': {
                                i1 = i;
                                while (i < str.length() && (c = str.charAt(i)) >= '0' && c <= '9') {
                                    ++i;
                                }
                                idx = 0;
                                try {
                                    idx = Integer.parseInt(str.substring(i1, i));
                                }
                                catch (NumberFormatException e) {
                                    throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                                }
                                if (neg) {
                                    if (idx < this.history.size()) {
                                        rep = this.history.get(this.history.index() - idx).toString();
                                        break;
                                    }
                                    throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                                }
                                if (idx >= this.history.index() - this.history.size() && idx < this.history.index()) {
                                    rep = this.history.get(idx).toString();
                                    break;
                                }
                                throw new IllegalArgumentException((neg ? "!-" : "!") + str.substring(i1, i) + ": event not found");
                            }
                            default: {
                                String ss = str.substring(i);
                                i = str.length();
                                idx = this.searchBackwards(ss, this.history.index(), true);
                                if (idx < 0) {
                                    throw new IllegalArgumentException("!" + ss + ": event not found");
                                }
                                rep = this.history.get(idx).toString();
                            }
                        }
                        if (rep == null) continue block14;
                        sb.append(rep);
                        continue block14;
                    }
                    sb.append(c);
                    continue block14;
                }
                case '^': {
                    if (i == 0) {
                        int i1 = str.indexOf(94, i + 1);
                        int i2 = str.indexOf(94, i1 + 1);
                        if (i2 < 0) {
                            i2 = str.length();
                        }
                        if (i1 > 0 && i2 > 0) {
                            String s1 = str.substring(i + 1, i1);
                            String s2 = str.substring(i1 + 1, i2);
                            String s = this.history.get(this.history.index() - 1).toString().replace(s1, s2);
                            sb.append(s);
                            i = i2 + 1;
                            continue block14;
                        }
                    }
                    sb.append(c);
                    continue block14;
                }
                default: {
                    sb.append(c);
                }
            }
        }
        if (escaped) {
            sb.append('\\');
        }
        if (!str.equals(result = sb.toString())) {
            this.print(result);
            this.println();
            this.flush();
        }
        return result;
    }

    public final void putString(CharSequence str) throws IOException {
        this.buf.write(str);
        if (this.mask == null) {
            this.print(str);
        } else if (this.mask.charValue() != '\u0000') {
            this.print(this.mask.charValue(), str.length());
        }
        this.drawBuffer();
    }

    private void drawBuffer(int clear) throws IOException {
        if (this.buf.cursor != this.buf.length() || clear != 0) {
            char[] chars = this.buf.buffer.substring(this.buf.cursor).toCharArray();
            if (this.mask != null) {
                Arrays.fill(chars, this.mask.charValue());
            }
            if (this.terminal.hasWeirdWrap()) {
                int width = this.terminal.getWidth();
                int pos = this.getCursorPosition();
                for (int i = 0; i < chars.length; ++i) {
                    this.print((int)chars[i]);
                    if ((pos + i + 1) % width != 0) continue;
                    this.print(32);
                    this.print(13);
                }
            } else {
                this.print(chars);
            }
            this.clearAhead(clear, chars.length);
            if (this.terminal.isAnsiSupported()) {
                if (chars.length > 0) {
                    this.back(chars.length);
                }
            } else {
                this.back(chars.length);
            }
        }
        if (this.terminal.hasWeirdWrap()) {
            int width = this.terminal.getWidth();
            if (this.getCursorPosition() > 0 && this.getCursorPosition() % width == 0 && this.buf.cursor == this.buf.length() && clear == 0) {
                this.print(32);
                this.print(13);
            }
        }
    }

    private void drawBuffer() throws IOException {
        this.drawBuffer(0);
    }

    private void clearAhead(int num, int delta) throws IOException {
        if (num == 0) {
            return;
        }
        if (this.terminal.isAnsiSupported()) {
            int i;
            int width = this.terminal.getWidth();
            int screenCursorCol = this.getCursorPosition() + delta;
            this.printAnsiSequence("K");
            int curCol = screenCursorCol % width;
            int endCol = (screenCursorCol + num - 1) % width;
            int lines = num / width;
            if (endCol < curCol) {
                ++lines;
            }
            for (i = 0; i < lines; ++i) {
                this.printAnsiSequence("B");
                this.printAnsiSequence("2K");
            }
            for (i = 0; i < lines; ++i) {
                this.printAnsiSequence("A");
            }
            return;
        }
        this.print(' ', num);
        this.back(num);
    }

    protected void back(int num) throws IOException {
        if (num == 0) {
            return;
        }
        if (this.terminal.isAnsiSupported()) {
            int width = this.getTerminal().getWidth();
            int cursor = this.getCursorPosition();
            int realCursor = cursor + num;
            int realCol = realCursor % width;
            int newCol = cursor % width;
            int moveup = num / width;
            int delta = realCol - newCol;
            if (delta < 0) {
                ++moveup;
            }
            if (moveup > 0) {
                this.printAnsiSequence("" + moveup + "A");
            }
            this.printAnsiSequence("" + (1 + newCol) + "G");
            return;
        }
        this.print('\b', num);
    }

    public void flush() throws IOException {
        this.out.flush();
    }

    private int backspaceAll() throws IOException {
        return this.backspace(Integer.MAX_VALUE);
    }

    private int backspace(int num) throws IOException {
        if (this.buf.cursor == 0) {
            return 0;
        }
        int count = 0;
        int termwidth = this.getTerminal().getWidth();
        int lines = this.getCursorPosition() / termwidth;
        count = this.moveCursor(-1 * num) * -1;
        this.buf.buffer.delete(this.buf.cursor, this.buf.cursor + count);
        if (this.getCursorPosition() / termwidth != lines && this.terminal.isAnsiSupported()) {
            this.printAnsiSequence("K");
        }
        this.drawBuffer(count);
        return count;
    }

    public boolean backspace() throws IOException {
        return this.backspace(1) == 1;
    }

    protected boolean moveToEnd() throws IOException {
        if (this.buf.cursor == this.buf.length()) {
            return true;
        }
        return this.moveCursor(this.buf.length() - this.buf.cursor) > 0;
    }

    private boolean deleteCurrentCharacter() throws IOException {
        if (this.buf.length() == 0 || this.buf.cursor == this.buf.length()) {
            return false;
        }
        this.buf.buffer.deleteCharAt(this.buf.cursor);
        this.drawBuffer(1);
        return true;
    }

    private Operation viDeleteChangeYankToRemap(Operation op) {
        switch (op) {
            case VI_EOF_MAYBE: 
            case ABORT: 
            case BACKWARD_CHAR: 
            case FORWARD_CHAR: 
            case END_OF_LINE: 
            case VI_MATCH: 
            case VI_BEGNNING_OF_LINE_OR_ARG_DIGIT: 
            case VI_ARG_DIGIT: 
            case VI_PREV_WORD: 
            case VI_END_WORD: 
            case VI_CHAR_SEARCH: 
            case VI_NEXT_WORD: 
            case VI_FIRST_PRINT: 
            case VI_GOTO_MARK: 
            case VI_COLUMN: 
            case VI_DELETE_TO: 
            case VI_YANK_TO: 
            case VI_CHANGE_TO: {
                return op;
            }
        }
        return Operation.VI_MOVEMENT_MODE;
    }

    private boolean viRubout(int count) throws IOException {
        boolean ok = true;
        for (int i = 0; ok && i < count; ++i) {
            ok = this.backspace();
        }
        return ok;
    }

    private boolean viDelete(int count) throws IOException {
        boolean ok = true;
        for (int i = 0; ok && i < count; ++i) {
            ok = this.deleteCurrentCharacter();
        }
        return ok;
    }

    private boolean viChangeCase(int count) throws IOException {
        boolean ok = true;
        for (int i = 0; ok && i < count; ++i) {
            boolean bl = ok = this.buf.cursor < this.buf.buffer.length();
            if (!ok) continue;
            char ch = this.buf.buffer.charAt(this.buf.cursor);
            if (Character.isUpperCase(ch)) {
                ch = Character.toLowerCase(ch);
            } else if (Character.isLowerCase(ch)) {
                ch = Character.toUpperCase(ch);
            }
            this.buf.buffer.setCharAt(this.buf.cursor, ch);
            this.drawBuffer(1);
            this.moveCursor(1);
        }
        return ok;
    }

    private boolean viChangeChar(int count, int c) throws IOException {
        if (c < 0 || c == 27 || c == 3) {
            return true;
        }
        boolean ok = true;
        for (int i = 0; ok && i < count; ++i) {
            boolean bl = ok = this.buf.cursor < this.buf.buffer.length();
            if (!ok) continue;
            this.buf.buffer.setCharAt(this.buf.cursor, (char)c);
            this.drawBuffer(1);
            if (i >= count - 1) continue;
            this.moveCursor(1);
        }
        return ok;
    }

    private boolean viPreviousWord(int count) throws IOException {
        boolean ok = true;
        if (this.buf.cursor == 0) {
            return false;
        }
        int pos = this.buf.cursor - 1;
        for (int i = 0; pos > 0 && i < count; ++i) {
            while (pos > 0 && this.isWhitespace(this.buf.buffer.charAt(pos))) {
                --pos;
            }
            while (pos > 0 && !this.isDelimiter(this.buf.buffer.charAt(pos - 1))) {
                --pos;
            }
            if (pos <= 0 || i >= count - 1) continue;
            --pos;
        }
        this.setCursorPosition(pos);
        return ok;
    }

    private boolean viDeleteTo(int startPos, int endPos) throws IOException {
        if (startPos == endPos) {
            return true;
        }
        if (endPos < startPos) {
            int tmp = endPos;
            endPos = startPos;
            startPos = tmp;
        }
        this.setCursorPosition(startPos);
        this.buf.cursor = startPos;
        this.buf.buffer.delete(startPos, endPos);
        this.drawBuffer(endPos - startPos);
        return true;
    }

    private boolean viYankTo(int startPos, int endPos) throws IOException {
        int cursorPos = startPos;
        if (endPos < startPos) {
            int tmp = endPos;
            endPos = startPos;
            startPos = tmp;
        }
        if (startPos == endPos) {
            this.yankBuffer = "";
            return true;
        }
        this.yankBuffer = this.buf.buffer.substring(startPos, endPos);
        this.setCursorPosition(cursorPos);
        return true;
    }

    private boolean viPut(int count) throws IOException {
        if (this.yankBuffer.length() == 0) {
            return true;
        }
        if (this.buf.cursor < this.buf.buffer.length()) {
            this.moveCursor(1);
        }
        for (int i = 0; i < count; ++i) {
            this.putString(this.yankBuffer);
        }
        this.moveCursor(-1);
        return true;
    }

    private boolean viCharSearch(int count, int invokeChar, int ch) throws IOException {
        if (ch < 0 || invokeChar < 0) {
            return false;
        }
        char searchChar = (char)ch;
        if (invokeChar == 59 || invokeChar == 44) {
            if (this.charSearchChar == '\u0000') {
                return false;
            }
            if (this.charSearchLastInvokeChar == ';' || this.charSearchLastInvokeChar == ',') {
                if (this.charSearchLastInvokeChar != invokeChar) {
                    this.charSearchFirstInvokeChar = this.switchCase(this.charSearchFirstInvokeChar);
                }
            } else if (invokeChar == 44) {
                this.charSearchFirstInvokeChar = this.switchCase(this.charSearchFirstInvokeChar);
            }
            searchChar = this.charSearchChar;
        } else {
            this.charSearchChar = searchChar;
            this.charSearchFirstInvokeChar = (char)invokeChar;
        }
        this.charSearchLastInvokeChar = (char)invokeChar;
        boolean isForward = Character.isLowerCase(this.charSearchFirstInvokeChar);
        boolean stopBefore = Character.toLowerCase(this.charSearchFirstInvokeChar) == 't';
        boolean ok = false;
        if (isForward) {
            block0 : while (count-- > 0) {
                for (int pos = this.buf.cursor + 1; pos < this.buf.buffer.length(); ++pos) {
                    if (this.buf.buffer.charAt(pos) != searchChar) continue;
                    this.setCursorPosition(pos);
                    ok = true;
                    continue block0;
                }
            }
            if (ok) {
                if (stopBefore) {
                    this.moveCursor(-1);
                }
                if (this.isInViMoveOperationState()) {
                    this.moveCursor(1);
                }
            }
        } else {
            block2 : while (count-- > 0) {
                for (int pos = this.buf.cursor - 1; pos >= 0; --pos) {
                    if (this.buf.buffer.charAt(pos) != searchChar) continue;
                    this.setCursorPosition(pos);
                    ok = true;
                    continue block2;
                }
            }
            if (ok && stopBefore) {
                this.moveCursor(1);
            }
        }
        return ok;
    }

    private char switchCase(char ch) {
        if (Character.isUpperCase(ch)) {
            return Character.toLowerCase(ch);
        }
        return Character.toUpperCase(ch);
    }

    private final boolean isInViMoveOperationState() {
        return this.state == State.VI_CHANGE_TO || this.state == State.VI_DELETE_TO || this.state == State.VI_YANK_TO;
    }

    private boolean viNextWord(int count) throws IOException {
        int pos = this.buf.cursor;
        int end = this.buf.buffer.length();
        for (int i = 0; pos < end && i < count; ++i) {
            while (pos < end && !this.isDelimiter(this.buf.buffer.charAt(pos))) {
                ++pos;
            }
            if (i >= count - 1 && this.state == State.VI_CHANGE_TO) continue;
            while (pos < end && this.isDelimiter(this.buf.buffer.charAt(pos))) {
                ++pos;
            }
        }
        this.setCursorPosition(pos);
        return true;
    }

    private boolean viEndWord(int count) throws IOException {
        int pos = this.buf.cursor;
        int end = this.buf.buffer.length();
        for (int i = 0; pos < end && i < count; ++i) {
            if (pos < end - 1 && !this.isDelimiter(this.buf.buffer.charAt(pos)) && this.isDelimiter(this.buf.buffer.charAt(pos + 1))) {
                ++pos;
            }
            while (pos < end && this.isDelimiter(this.buf.buffer.charAt(pos))) {
                ++pos;
            }
            while (pos < end - 1 && !this.isDelimiter(this.buf.buffer.charAt(pos + 1))) {
                ++pos;
            }
        }
        this.setCursorPosition(pos);
        return true;
    }

    private boolean previousWord() throws IOException {
        while (this.isDelimiter(this.buf.current()) && this.moveCursor(-1) != 0) {
        }
        while (!this.isDelimiter(this.buf.current()) && this.moveCursor(-1) != 0) {
        }
        return true;
    }

    private boolean nextWord() throws IOException {
        while (this.isDelimiter(this.buf.nextChar()) && this.moveCursor(1) != 0) {
        }
        while (!this.isDelimiter(this.buf.nextChar()) && this.moveCursor(1) != 0) {
        }
        return true;
    }

    private boolean unixWordRubout(int count) throws IOException {
        while (count > 0) {
            if (this.buf.cursor == 0) {
                return false;
            }
            while (this.isWhitespace(this.buf.current()) && this.backspace()) {
            }
            while (!this.isWhitespace(this.buf.current()) && this.backspace()) {
            }
            --count;
        }
        return true;
    }

    private String insertComment(boolean isViMode) throws IOException {
        String comment = this.getCommentBegin();
        this.setCursorPosition(0);
        this.putString(comment);
        if (isViMode) {
            this.consoleKeys.setKeyMap("vi-insert");
        }
        return this.accept();
    }

    private boolean insert(int count, CharSequence str) throws IOException {
        for (int i = 0; i < count; ++i) {
            this.buf.write(str);
            if (this.mask == null) {
                this.print(str);
                continue;
            }
            if (this.mask.charValue() == '\u0000') continue;
            this.print(this.mask.charValue(), str.length());
        }
        this.drawBuffer();
        return true;
    }

    private int viSearch(char searchChar) throws IOException {
        boolean isForward;
        int start;
        boolean isComplete;
        String searchTerm;
        int ch;
        int idx;
        int end;
        int i;
        isForward = searchChar == '/';
        CursorBuffer origBuffer = this.buf.copy();
        this.setCursorPosition(0);
        this.killLine();
        this.putString(Character.toString(searchChar));
        this.flush();
        boolean isAborted = false;
        isComplete = false;
        ch = -1;
        while (!isAborted && !isComplete && (ch = this.readCharacter()) != -1) {
            switch (ch) {
                case 27: {
                    isAborted = true;
                    break;
                }
                case 8: 
                case 127: {
                    this.backspace();
                    if (this.buf.cursor != 0) break;
                    isAborted = true;
                    break;
                }
                case 10: {
                    isComplete = true;
                    break;
                }
                default: {
                    this.putString(Character.toString((char)ch));
                }
            }
            this.flush();
        }
        if (ch == -1 || isAborted) {
            this.setCursorPosition(0);
            this.killLine();
            this.putString(origBuffer.buffer);
            this.setCursorPosition(origBuffer.cursor);
            return -1;
        }
        searchTerm = this.buf.buffer.substring(1);
        idx = -1;
        end = this.history.index();
        int n = start = end <= this.history.size() ? 0 : end - this.history.size();
        if (isForward) {
            for (i = start; i < end; ++i) {
                if (!this.history.get(i).toString().contains(searchTerm)) continue;
                idx = i;
                break;
            }
        } else {
            for (i = end - 1; i >= start; --i) {
                if (!this.history.get(i).toString().contains(searchTerm)) continue;
                idx = i;
                break;
            }
        }
        if (idx == -1) {
            this.setCursorPosition(0);
            this.killLine();
            this.putString(origBuffer.buffer);
            this.setCursorPosition(0);
            return -1;
        }
        this.setCursorPosition(0);
        this.killLine();
        this.putString(this.history.get(idx));
        this.setCursorPosition(0);
        this.flush();
        isComplete = false;
        while (!isComplete && (ch = this.readCharacter()) != -1) {
            boolean forward = isForward;
            switch (ch) {
                case 80: 
                case 112: {
                    forward = !isForward;
                }
                case 78: 
                case 110: {
                    int i;
                    boolean isMatch = false;
                    if (forward) {
                        for (i = idx + 1; !isMatch && i < end; ++i) {
                            if (!this.history.get(i).toString().contains(searchTerm)) continue;
                            idx = i;
                            isMatch = true;
                        }
                    } else {
                        for (i = idx - 1; !isMatch && i >= start; --i) {
                            if (!this.history.get(i).toString().contains(searchTerm)) continue;
                            idx = i;
                            isMatch = true;
                        }
                    }
                    if (!isMatch) break;
                    this.setCursorPosition(0);
                    this.killLine();
                    this.putString(this.history.get(idx));
                    this.setCursorPosition(0);
                    break;
                }
                default: {
                    isComplete = true;
                }
            }
            this.flush();
        }
        return ch;
    }

    public void setParenBlinkTimeout(int timeout) {
        this.parenBlinkTimeout = timeout;
    }

    private void insertClose(String s) throws IOException {
        this.putString(s);
        int closePosition = this.buf.cursor;
        this.moveCursor(-1);
        this.viMatch();
        if (this.in.isNonBlockingEnabled()) {
            this.in.peek(this.parenBlinkTimeout);
        }
        this.setCursorPosition(closePosition);
    }

    private boolean viMatch() throws IOException {
        int pos = this.buf.cursor;
        if (pos == this.buf.length()) {
            return false;
        }
        int type = this.getBracketType(this.buf.buffer.charAt(pos));
        int move = type < 0 ? -1 : 1;
        int count = 1;
        if (type == 0) {
            return false;
        }
        while (count > 0) {
            if ((pos += move) < 0 || pos >= this.buf.buffer.length()) {
                return false;
            }
            int curType = this.getBracketType(this.buf.buffer.charAt(pos));
            if (curType == type) {
                ++count;
                continue;
            }
            if (curType != - type) continue;
            --count;
        }
        if (move > 0 && this.isInViMoveOperationState()) {
            ++pos;
        }
        this.setCursorPosition(pos);
        return true;
    }

    private int getBracketType(char ch) {
        switch (ch) {
            case '[': {
                return 1;
            }
            case ']': {
                return -1;
            }
            case '{': {
                return 2;
            }
            case '}': {
                return -2;
            }
            case '(': {
                return 3;
            }
            case ')': {
                return -3;
            }
        }
        return 0;
    }

    private boolean deletePreviousWord() throws IOException {
        while (this.isDelimiter(this.buf.current()) && this.backspace()) {
        }
        while (!this.isDelimiter(this.buf.current()) && this.backspace()) {
        }
        return true;
    }

    private boolean deleteNextWord() throws IOException {
        while (this.isDelimiter(this.buf.nextChar()) && this.delete()) {
        }
        while (!this.isDelimiter(this.buf.nextChar()) && this.delete()) {
        }
        return true;
    }

    private boolean capitalizeWord() throws IOException {
        char c;
        boolean first = true;
        int i = 1;
        while (this.buf.cursor + i - 1 < this.buf.length() && !this.isDelimiter(c = this.buf.buffer.charAt(this.buf.cursor + i - 1))) {
            this.buf.buffer.setCharAt(this.buf.cursor + i - 1, first ? Character.toUpperCase(c) : Character.toLowerCase(c));
            first = false;
            ++i;
        }
        this.drawBuffer();
        this.moveCursor(i - 1);
        return true;
    }

    private boolean upCaseWord() throws IOException {
        char c;
        int i = 1;
        while (this.buf.cursor + i - 1 < this.buf.length() && !this.isDelimiter(c = this.buf.buffer.charAt(this.buf.cursor + i - 1))) {
            this.buf.buffer.setCharAt(this.buf.cursor + i - 1, Character.toUpperCase(c));
            ++i;
        }
        this.drawBuffer();
        this.moveCursor(i - 1);
        return true;
    }

    private boolean downCaseWord() throws IOException {
        char c;
        int i = 1;
        while (this.buf.cursor + i - 1 < this.buf.length() && !this.isDelimiter(c = this.buf.buffer.charAt(this.buf.cursor + i - 1))) {
            this.buf.buffer.setCharAt(this.buf.cursor + i - 1, Character.toLowerCase(c));
            ++i;
        }
        this.drawBuffer();
        this.moveCursor(i - 1);
        return true;
    }

    private boolean transposeChars(int count) throws IOException {
        while (count > 0) {
            if (this.buf.cursor == 0 || this.buf.cursor == this.buf.buffer.length()) {
                return false;
            }
            int first = this.buf.cursor - 1;
            int second = this.buf.cursor;
            char tmp = this.buf.buffer.charAt(first);
            this.buf.buffer.setCharAt(first, this.buf.buffer.charAt(second));
            this.buf.buffer.setCharAt(second, tmp);
            this.moveInternal(-1);
            this.drawBuffer();
            this.moveInternal(2);
            --count;
        }
        return true;
    }

    public boolean isKeyMap(String name) {
        KeyMap map = this.consoleKeys.getKeys();
        KeyMap mapByName = this.consoleKeys.getKeyMaps().get(name);
        if (mapByName == null) {
            return false;
        }
        return map == mapByName;
    }

    public String accept() throws IOException {
        this.moveToEnd();
        this.println();
        this.flush();
        return this.finishBuffer();
    }

    public int moveCursor(int num) throws IOException {
        int where = num;
        if (this.buf.cursor == 0 && where <= 0) {
            return 0;
        }
        if (this.buf.cursor == this.buf.buffer.length() && where >= 0) {
            return 0;
        }
        if (this.buf.cursor + where < 0) {
            where = - this.buf.cursor;
        } else if (this.buf.cursor + where > this.buf.buffer.length()) {
            where = this.buf.buffer.length() - this.buf.cursor;
        }
        this.moveInternal(where);
        return where;
    }

    private void moveInternal(int where) throws IOException {
        this.buf.cursor += where;
        if (this.terminal.isAnsiSupported()) {
            if (where < 0) {
                this.back(Math.abs(where));
            } else {
                int oldLine;
                int width = this.getTerminal().getWidth();
                int cursor = this.getCursorPosition();
                int newLine = cursor / width;
                if (newLine > (oldLine = (cursor - where) / width)) {
                    this.printAnsiSequence("" + (newLine - oldLine) + "B");
                }
                this.printAnsiSequence("" + (1 + cursor % width) + "G");
            }
            return;
        }
        if (where < 0) {
            int len = 0;
            for (int i = this.buf.cursor; i < this.buf.cursor - where; ++i) {
                if (this.buf.buffer.charAt(i) == '\t') {
                    len += 4;
                    continue;
                }
                ++len;
            }
            char[] chars = new char[len];
            Arrays.fill(chars, '\b');
            this.out.write(chars);
            return;
        }
        if (this.buf.cursor == 0) {
            return;
        }
        if (this.mask == null) {
            this.print(this.buf.buffer.substring(this.buf.cursor - where, this.buf.cursor).toCharArray());
            return;
        }
        char c = this.mask.charValue();
        if (this.mask.charValue() == '\u0000') {
            return;
        }
        this.print(c, Math.abs(where));
    }

    public final boolean replace(int num, String replacement) {
        this.buf.buffer.replace(this.buf.cursor - num, this.buf.cursor, replacement);
        try {
            this.moveCursor(- num);
            this.drawBuffer(Math.max(0, num - replacement.length()));
            this.moveCursor(replacement.length());
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public final int readCharacter() throws IOException {
        int c = this.reader.read();
        if (c >= 0) {
            Log.trace("Keystroke: ", c);
            if (this.terminal.isSupported()) {
                this.clearEcho(c);
            }
        }
        return c;
    }

    private int clearEcho(int c) throws IOException {
        if (!this.terminal.isEchoEnabled()) {
            return 0;
        }
        int num = this.countEchoCharacters(c);
        this.back(num);
        this.drawBuffer(num);
        return num;
    }

    private int countEchoCharacters(int c) {
        if (c == 9) {
            int tabStop = 8;
            int position = this.getCursorPosition();
            return tabStop - position % tabStop;
        }
        return this.getPrintableCharacters(c).length();
    }

    private StringBuilder getPrintableCharacters(int ch) {
        StringBuilder sbuff = new StringBuilder();
        if (ch >= 32) {
            if (ch < 127) {
                sbuff.append(ch);
            } else if (ch == 127) {
                sbuff.append('^');
                sbuff.append('?');
            } else {
                sbuff.append('M');
                sbuff.append('-');
                if (ch >= 160) {
                    if (ch < 255) {
                        sbuff.append((char)(ch - 128));
                    } else {
                        sbuff.append('^');
                        sbuff.append('?');
                    }
                } else {
                    sbuff.append('^');
                    sbuff.append((char)(ch - 128 + 64));
                }
            }
        } else {
            sbuff.append('^');
            sbuff.append((char)(ch + 64));
        }
        return sbuff;
    }

    public final /* varargs */ int readCharacter(char ... allowed) throws IOException {
        char c;
        Arrays.sort(allowed);
        while (Arrays.binarySearch(allowed, c = (char)this.readCharacter()) < 0) {
        }
        return c;
    }

    public String readLine() throws IOException {
        return this.readLine((String)null);
    }

    public String readLine(Character mask) throws IOException {
        return this.readLine(null, mask);
    }

    public String readLine(String prompt) throws IOException {
        return this.readLine(prompt, null);
    }

    public boolean setKeyMap(String name) {
        return this.consoleKeys.setKeyMap(name);
    }

    public String getKeyMap() {
        return this.consoleKeys.getKeys().getName();
    }

    public String readLine(String prompt, Character mask) throws IOException {
        int repeatCount = 0;
        this.mask = mask;
        if (prompt != null) {
            this.setPrompt(prompt);
        } else {
            prompt = this.getPrompt();
        }
        try {
            if (!this.terminal.isSupported()) {
                this.beforeReadLine(prompt, mask);
            }
            if (prompt != null && prompt.length() > 0) {
                this.out.write(prompt);
                this.out.flush();
            }
            if (!this.terminal.isSupported()) {
                String string = this.readLineSimple();
                return string;
            }
            String originalPrompt = this.prompt;
            this.state = State.NORMAL;
            boolean success = true;
            StringBuilder sb = new StringBuilder();
            Stack<Character> pushBackChar = new Stack<Character>();
            do {
                int c;
                Object o;
                int n = c = pushBackChar.isEmpty() ? this.readCharacter() : (int)((Character)pushBackChar.pop()).charValue();
                if (c == -1) {
                    String string = null;
                    return string;
                }
                sb.append((char)c);
                if (this.recording) {
                    this.macro = this.macro + (char)c;
                }
                if ((o = this.getKeys().getBound(sb)) == Operation.DO_LOWERCASE_VERSION) {
                    sb.setLength(sb.length() - 1);
                    sb.append(Character.toLowerCase((char)c));
                    o = this.getKeys().getBound(sb);
                }
                if (o instanceof KeyMap) {
                    if (c != 27 || !pushBackChar.isEmpty() || !this.in.isNonBlockingEnabled() || this.in.peek(this.escapeTimeout) != -2 || (o = ((KeyMap)o).getAnotherKey()) == null || o instanceof KeyMap) continue;
                    sb.setLength(0);
                }
                while (o == null && sb.length() > 0) {
                    c = sb.charAt(sb.length() - 1);
                    sb.setLength(sb.length() - 1);
                    Object o2 = this.getKeys().getBound(sb);
                    if (!(o2 instanceof KeyMap) || (o = ((KeyMap)o2).getAnotherKey()) == null) continue;
                    pushBackChar.push(Character.valueOf((char)c));
                }
                if (o == null) continue;
                Log.trace("Binding: ", o);
                if (o instanceof String) {
                    String macro = (String)o;
                    for (int i = 0; i < macro.length(); ++i) {
                        pushBackChar.push(Character.valueOf(macro.charAt(macro.length() - 1 - i)));
                    }
                    sb.setLength(0);
                    continue;
                }
                if (o instanceof ActionListener) {
                    ((ActionListener)o).actionPerformed(null);
                    sb.setLength(0);
                    continue;
                }
                if (this.state == State.SEARCH) {
                    int cursorDest = -1;
                    switch ((Operation)((Object)o)) {
                        case ABORT: {
                            this.state = State.NORMAL;
                            break;
                        }
                        case REVERSE_SEARCH_HISTORY: 
                        case HISTORY_SEARCH_BACKWARD: {
                            if (this.searchTerm.length() == 0) {
                                this.searchTerm.append(this.previousSearchTerm);
                            }
                            if (this.searchIndex == -1) {
                                this.searchIndex = this.searchBackwards(this.searchTerm.toString());
                                break;
                            }
                            this.searchIndex = this.searchBackwards(this.searchTerm.toString(), this.searchIndex);
                            break;
                        }
                        case BACKWARD_DELETE_CHAR: {
                            if (this.searchTerm.length() <= 0) break;
                            this.searchTerm.deleteCharAt(this.searchTerm.length() - 1);
                            this.searchIndex = this.searchBackwards(this.searchTerm.toString());
                            break;
                        }
                        case SELF_INSERT: {
                            this.searchTerm.appendCodePoint(c);
                            this.searchIndex = this.searchBackwards(this.searchTerm.toString());
                            break;
                        }
                        default: {
                            if (this.searchIndex != -1) {
                                this.history.moveTo(this.searchIndex);
                                cursorDest = this.history.current().toString().indexOf(this.searchTerm.toString());
                            }
                            this.state = State.NORMAL;
                        }
                    }
                    if (this.state == State.SEARCH) {
                        if (this.searchTerm.length() == 0) {
                            this.printSearchStatus("", "");
                            this.searchIndex = -1;
                        } else if (this.searchIndex == -1) {
                            this.beep();
                        } else {
                            this.printSearchStatus(this.searchTerm.toString(), this.history.get(this.searchIndex).toString());
                        }
                    } else {
                        this.restoreLine(originalPrompt, cursorDest);
                    }
                }
                if (this.state != State.SEARCH) {
                    boolean isArgDigit = false;
                    int count = repeatCount == 0 ? 1 : repeatCount;
                    success = true;
                    if (o instanceof Operation) {
                        Operation op = (Operation)((Object)o);
                        int cursorStart = this.buf.cursor;
                        State origState = this.state;
                        if (this.state == State.VI_CHANGE_TO || this.state == State.VI_YANK_TO || this.state == State.VI_DELETE_TO) {
                            op = this.viDeleteChangeYankToRemap(op);
                        }
                        switch (op) {
                            case COMPLETE: {
                                success = this.complete();
                                break;
                            }
                            case POSSIBLE_COMPLETIONS: {
                                this.printCompletionCandidates();
                                break;
                            }
                            case BEGINNING_OF_LINE: {
                                success = this.setCursorPosition(0);
                                break;
                            }
                            case KILL_LINE: {
                                success = this.killLine();
                                break;
                            }
                            case KILL_WHOLE_LINE: {
                                success = this.setCursorPosition(0) && this.killLine();
                                break;
                            }
                            case CLEAR_SCREEN: {
                                success = this.clearScreen();
                                break;
                            }
                            case OVERWRITE_MODE: {
                                this.buf.setOverTyping(!this.buf.isOverTyping());
                                break;
                            }
                            case SELF_INSERT: {
                                this.putString(sb);
                                break;
                            }
                            case ACCEPT_LINE: {
                                String string = this.accept();
                                return string;
                            }
                            case VI_MOVE_ACCEPT_LINE: {
                                this.consoleKeys.setKeyMap("vi-insert");
                                String string = this.accept();
                                return string;
                            }
                            case BACKWARD_WORD: {
                                success = this.previousWord();
                                break;
                            }
                            case FORWARD_WORD: {
                                success = this.nextWord();
                                break;
                            }
                            case PREVIOUS_HISTORY: {
                                success = this.moveHistory(false);
                                break;
                            }
                            case VI_PREVIOUS_HISTORY: {
                                success = this.moveHistory(false, count) && this.setCursorPosition(0);
                                break;
                            }
                            case NEXT_HISTORY: {
                                success = this.moveHistory(true);
                                break;
                            }
                            case VI_NEXT_HISTORY: {
                                success = this.moveHistory(true, count) && this.setCursorPosition(0);
                                break;
                            }
                            case BACKWARD_DELETE_CHAR: {
                                success = this.backspace();
                                break;
                            }
                            case EXIT_OR_DELETE_CHAR: {
                                if (this.buf.buffer.length() == 0) {
                                    String string = null;
                                    return string;
                                }
                                success = this.deleteCurrentCharacter();
                                break;
                            }
                            case DELETE_CHAR: {
                                success = this.deleteCurrentCharacter();
                                break;
                            }
                            case BACKWARD_CHAR: {
                                success = this.moveCursor(- count) != 0;
                                break;
                            }
                            case FORWARD_CHAR: {
                                success = this.moveCursor(count) != 0;
                                break;
                            }
                            case UNIX_LINE_DISCARD: {
                                success = this.resetLine();
                                break;
                            }
                            case UNIX_WORD_RUBOUT: {
                                success = this.unixWordRubout(count);
                                break;
                            }
                            case BACKWARD_KILL_WORD: {
                                success = this.deletePreviousWord();
                                break;
                            }
                            case KILL_WORD: {
                                success = this.deleteNextWord();
                                break;
                            }
                            case BEGINNING_OF_HISTORY: {
                                success = this.history.moveToFirst();
                                if (!success) break;
                                this.setBuffer(this.history.current());
                                break;
                            }
                            case END_OF_HISTORY: {
                                success = this.history.moveToLast();
                                if (!success) break;
                                this.setBuffer(this.history.current());
                                break;
                            }
                            case REVERSE_SEARCH_HISTORY: 
                            case HISTORY_SEARCH_BACKWARD: {
                                if (this.searchTerm != null) {
                                    this.previousSearchTerm = this.searchTerm.toString();
                                }
                                this.searchTerm = new StringBuffer(this.buf.buffer);
                                this.state = State.SEARCH;
                                if (this.searchTerm.length() > 0) {
                                    this.searchIndex = this.searchBackwards(this.searchTerm.toString());
                                    if (this.searchIndex == -1) {
                                        this.beep();
                                    }
                                    this.printSearchStatus(this.searchTerm.toString(), this.searchIndex > -1 ? this.history.get(this.searchIndex).toString() : "");
                                    break;
                                }
                                this.searchIndex = -1;
                                this.printSearchStatus("", "");
                                break;
                            }
                            case CAPITALIZE_WORD: {
                                success = this.capitalizeWord();
                                break;
                            }
                            case UPCASE_WORD: {
                                success = this.upCaseWord();
                                break;
                            }
                            case DOWNCASE_WORD: {
                                success = this.downCaseWord();
                                break;
                            }
                            case END_OF_LINE: {
                                success = this.moveToEnd();
                                break;
                            }
                            case TAB_INSERT: {
                                this.putString("\t");
                                break;
                            }
                            case RE_READ_INIT_FILE: {
                                this.consoleKeys.loadKeys(this.appName, this.inputrcUrl);
                                break;
                            }
                            case START_KBD_MACRO: {
                                this.recording = true;
                                break;
                            }
                            case END_KBD_MACRO: {
                                this.recording = false;
                                this.macro = this.macro.substring(0, this.macro.length() - sb.length());
                                break;
                            }
                            case CALL_LAST_KBD_MACRO: {
                                for (int i = 0; i < this.macro.length(); ++i) {
                                    pushBackChar.push(Character.valueOf(this.macro.charAt(this.macro.length() - 1 - i)));
                                }
                                sb.setLength(0);
                                break;
                            }
                            case VI_EDITING_MODE: {
                                this.consoleKeys.setKeyMap("vi-insert");
                                break;
                            }
                            case VI_MOVEMENT_MODE: {
                                if (this.state == State.NORMAL) {
                                    this.moveCursor(-1);
                                }
                                this.consoleKeys.setKeyMap("vi-move");
                                break;
                            }
                            case VI_INSERTION_MODE: {
                                this.consoleKeys.setKeyMap("vi-insert");
                                break;
                            }
                            case VI_APPEND_MODE: {
                                this.moveCursor(1);
                                this.consoleKeys.setKeyMap("vi-insert");
                                break;
                            }
                            case VI_APPEND_EOL: {
                                success = this.moveToEnd();
                                this.consoleKeys.setKeyMap("vi-insert");
                                break;
                            }
                            case VI_EOF_MAYBE: {
                                if (this.buf.buffer.length() == 0) {
                                    String i = null;
                                    return i;
                                }
                                String i = this.accept();
                                return i;
                            }
                            case TRANSPOSE_CHARS: {
                                success = this.transposeChars(count);
                                break;
                            }
                            case INSERT_COMMENT: {
                                String i = this.insertComment(false);
                                return i;
                            }
                            case INSERT_CLOSE_CURLY: {
                                this.insertClose("}");
                                break;
                            }
                            case INSERT_CLOSE_PAREN: {
                                this.insertClose(")");
                                break;
                            }
                            case INSERT_CLOSE_SQUARE: {
                                this.insertClose("]");
                                break;
                            }
                            case VI_INSERT_COMMENT: {
                                String i = this.insertComment(true);
                                return i;
                            }
                            case VI_MATCH: {
                                success = this.viMatch();
                                break;
                            }
                            case VI_SEARCH: {
                                int lastChar = this.viSearch(sb.charAt(0));
                                if (lastChar == -1) break;
                                pushBackChar.push(Character.valueOf((char)lastChar));
                                break;
                            }
                            case VI_ARG_DIGIT: {
                                repeatCount = repeatCount * 10 + sb.charAt(0) - 48;
                                isArgDigit = true;
                                break;
                            }
                            case VI_BEGNNING_OF_LINE_OR_ARG_DIGIT: {
                                if (repeatCount > 0) {
                                    repeatCount = repeatCount * 10 + sb.charAt(0) - 48;
                                    isArgDigit = true;
                                    break;
                                }
                                success = this.setCursorPosition(0);
                                break;
                            }
                            case VI_PREV_WORD: {
                                success = this.viPreviousWord(count);
                                break;
                            }
                            case VI_NEXT_WORD: {
                                success = this.viNextWord(count);
                                break;
                            }
                            case VI_END_WORD: {
                                success = this.viEndWord(count);
                                break;
                            }
                            case VI_INSERT_BEG: {
                                success = this.setCursorPosition(0);
                                this.consoleKeys.setKeyMap("vi-insert");
                                break;
                            }
                            case VI_RUBOUT: {
                                success = this.viRubout(count);
                                break;
                            }
                            case VI_DELETE: {
                                success = this.viDelete(count);
                                break;
                            }
                            case VI_DELETE_TO: {
                                if (this.state == State.VI_DELETE_TO) {
                                    success = this.setCursorPosition(0) && this.killLine();
                                    this.state = origState = State.NORMAL;
                                    break;
                                }
                                this.state = State.VI_DELETE_TO;
                                break;
                            }
                            case VI_YANK_TO: {
                                if (this.state == State.VI_YANK_TO) {
                                    this.yankBuffer = this.buf.buffer.toString();
                                    this.state = origState = State.NORMAL;
                                    break;
                                }
                                this.state = State.VI_YANK_TO;
                                break;
                            }
                            case VI_CHANGE_TO: {
                                if (this.state == State.VI_CHANGE_TO) {
                                    success = this.setCursorPosition(0) && this.killLine();
                                    this.state = origState = State.NORMAL;
                                    this.consoleKeys.setKeyMap("vi-insert");
                                    break;
                                }
                                this.state = State.VI_CHANGE_TO;
                                break;
                            }
                            case VI_PUT: {
                                success = this.viPut(count);
                                break;
                            }
                            case VI_CHAR_SEARCH: {
                                int searchChar = c != 59 && c != 44 ? (pushBackChar.isEmpty() ? this.readCharacter() : (int)((Character)pushBackChar.pop()).charValue()) : 0;
                                success = this.viCharSearch(count, c, searchChar);
                                break;
                            }
                            case VI_CHANGE_CASE: {
                                success = this.viChangeCase(count);
                                break;
                            }
                            case VI_CHANGE_CHAR: {
                                success = this.viChangeChar(count, pushBackChar.isEmpty() ? this.readCharacter() : (int)((Character)pushBackChar.pop()).charValue());
                                break;
                            }
                            case EMACS_EDITING_MODE: {
                                this.consoleKeys.setKeyMap("emacs");
                                break;
                            }
                        }
                        if (origState != State.NORMAL) {
                            if (origState == State.VI_DELETE_TO) {
                                success = this.viDeleteTo(cursorStart, this.buf.cursor);
                            } else if (origState == State.VI_CHANGE_TO) {
                                success = this.viDeleteTo(cursorStart, this.buf.cursor);
                                this.consoleKeys.setKeyMap("vi-insert");
                            } else if (origState == State.VI_YANK_TO) {
                                success = this.viYankTo(cursorStart, this.buf.cursor);
                            }
                            this.state = State.NORMAL;
                        }
                        if (this.state == State.NORMAL && !isArgDigit) {
                            repeatCount = 0;
                        }
                    }
                }
                if (!success) {
                    this.beep();
                }
                sb.setLength(0);
                this.flush();
            } while (true);
        }
        finally {
            if (!this.terminal.isSupported()) {
                this.afterReadLine();
            }
        }
    }

    private String readLineSimple() throws IOException {
        int i;
        StringBuilder buff = new StringBuilder();
        if (this.skipLF) {
            this.skipLF = false;
            i = this.readCharacter();
            if (i == -1 || i == 13) {
                return buff.toString();
            }
            if (i != 10) {
                buff.append((char)i);
            }
        }
        while ((i = this.readCharacter()) != -1 || buff.length() != 0) {
            if (i == -1 || i == 10) {
                return buff.toString();
            }
            if (i == 13) {
                this.skipLF = true;
                return buff.toString();
            }
            buff.append((char)i);
        }
        return null;
    }

    public boolean addCompleter(Completer completer) {
        return this.completers.add(completer);
    }

    public boolean removeCompleter(Completer completer) {
        return this.completers.remove(completer);
    }

    public Collection<Completer> getCompleters() {
        return Collections.unmodifiableList(this.completers);
    }

    public void setCompletionHandler(CompletionHandler handler) {
        this.completionHandler = Preconditions.checkNotNull(handler);
    }

    public CompletionHandler getCompletionHandler() {
        return this.completionHandler;
    }

    protected boolean complete() throws IOException {
        Completer comp;
        if (this.completers.size() == 0) {
            return false;
        }
        LinkedList<CharSequence> candidates = new LinkedList<CharSequence>();
        String bufstr = this.buf.buffer.toString();
        int cursor = this.buf.cursor;
        int position = -1;
        Iterator<Completer> i$ = this.completers.iterator();
        while (i$.hasNext() && (position = (comp = i$.next()).complete(bufstr, cursor, candidates)) == -1) {
        }
        return candidates.size() != 0 && this.getCompletionHandler().complete(this, candidates, position);
    }

    protected void printCompletionCandidates() throws IOException {
        if (this.completers.size() == 0) {
            return;
        }
        LinkedList<CharSequence> candidates = new LinkedList<CharSequence>();
        String bufstr = this.buf.buffer.toString();
        int cursor = this.buf.cursor;
        for (Completer comp : this.completers) {
            if (comp.complete(bufstr, cursor, candidates) != -1) break;
        }
        CandidateListCompletionHandler.printCandidates(this, candidates);
        this.drawLine();
    }

    public void setAutoprintThreshold(int threshold) {
        this.autoprintThreshold = threshold;
    }

    public int getAutoprintThreshold() {
        return this.autoprintThreshold;
    }

    public void setPaginationEnabled(boolean enabled) {
        this.paginationEnabled = enabled;
    }

    public boolean isPaginationEnabled() {
        return this.paginationEnabled;
    }

    public void setHistory(History history) {
        this.history = history;
    }

    public History getHistory() {
        return this.history;
    }

    public void setHistoryEnabled(boolean enabled) {
        this.historyEnabled = enabled;
    }

    public boolean isHistoryEnabled() {
        return this.historyEnabled;
    }

    private boolean moveHistory(boolean next, int count) throws IOException {
        boolean ok = true;
        for (int i = 0; i < count && (ok = this.moveHistory(next)); ++i) {
        }
        return ok;
    }

    private boolean moveHistory(boolean next) throws IOException {
        if (next && !this.history.next()) {
            return false;
        }
        if (!next && !this.history.previous()) {
            return false;
        }
        this.setBuffer(this.history.current());
        return true;
    }

    private void print(int c) throws IOException {
        if (c == 9) {
            char[] chars = new char[4];
            Arrays.fill(chars, ' ');
            this.out.write(chars);
            return;
        }
        this.out.write(c);
    }

    private /* varargs */ void print(char ... buff) throws IOException {
        char[] chars;
        int len = 0;
        for (char c : buff) {
            if (c == '\t') {
                len += 4;
                continue;
            }
            ++len;
        }
        if (len == buff.length) {
            chars = buff;
        } else {
            chars = new char[len];
            int pos = 0;
            for (char c2 : buff) {
                if (c2 == '\t') {
                    Arrays.fill(chars, pos, pos + 4, ' ');
                    pos += 4;
                    continue;
                }
                chars[pos] = c2;
                ++pos;
            }
        }
        this.out.write(chars);
    }

    private void print(char c, int num) throws IOException {
        if (num == 1) {
            this.print((int)c);
        } else {
            char[] chars = new char[num];
            Arrays.fill(chars, c);
            this.print(chars);
        }
    }

    public final void print(CharSequence s) throws IOException {
        this.print(Preconditions.checkNotNull(s).toString().toCharArray());
    }

    public final void println(CharSequence s) throws IOException {
        this.print(Preconditions.checkNotNull(s).toString().toCharArray());
        this.println();
    }

    public final void println() throws IOException {
        this.print(CR);
    }

    public final boolean delete() throws IOException {
        return this.delete(1) == 1;
    }

    private int delete(int num) throws IOException {
        this.buf.buffer.delete(this.buf.cursor, this.buf.cursor + 1);
        this.drawBuffer(1);
        return 1;
    }

    public boolean killLine() throws IOException {
        int cp = this.buf.cursor;
        int len = this.buf.buffer.length();
        if (cp >= len) {
            return false;
        }
        int num = this.buf.buffer.length() - cp;
        this.clearAhead(num, 0);
        for (int i = 0; i < num; ++i) {
            this.buf.buffer.deleteCharAt(len - i - 1);
        }
        return true;
    }

    public boolean clearScreen() throws IOException {
        if (!this.terminal.isAnsiSupported()) {
            return false;
        }
        this.printAnsiSequence("2J");
        this.printAnsiSequence("1;1H");
        this.redrawLine();
        return true;
    }

    public void beep() throws IOException {
        if (this.bellEnabled) {
            this.print(7);
            this.flush();
        }
    }

    public boolean paste() throws IOException {
        Clipboard clipboard;
        try {
            clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        catch (Exception e) {
            return false;
        }
        if (clipboard == null) {
            return false;
        }
        Transferable transferable = clipboard.getContents(null);
        if (transferable == null) {
            return false;
        }
        try {
            String value;
            Object content = transferable.getTransferData(DataFlavor.plainTextFlavor);
            if (content == null) {
                try {
                    content = new DataFlavor().getReaderForText(transferable);
                }
                catch (Exception e) {
                    // empty catch block
                }
            }
            if (content == null) {
                return false;
            }
            if (content instanceof Reader) {
                String line;
                value = "";
                BufferedReader read = new BufferedReader((Reader)content);
                while ((line = read.readLine()) != null) {
                    if (value.length() > 0) {
                        value = value + "\n";
                    }
                    value = value + line;
                }
            } else {
                value = content.toString();
            }
            if (value == null) {
                return true;
            }
            this.putString(value);
            return true;
        }
        catch (UnsupportedFlavorException e) {
            Log.error("Paste failed: ", e);
            return false;
        }
    }

    public void addTriggeredAction(char c, ActionListener listener) {
        this.triggeredActions.put(Character.valueOf(c), listener);
    }

    public void printColumns(Collection<? extends CharSequence> items) throws IOException {
        if (items == null || items.isEmpty()) {
            return;
        }
        int width = this.getTerminal().getWidth();
        int height = this.getTerminal().getHeight();
        int maxWidth = 0;
        for (CharSequence item : items) {
            maxWidth = Math.max(maxWidth, item.length());
        }
        Log.debug("Max width: ", maxWidth += 3);
        int showLines = this.isPaginationEnabled() ? height - 1 : Integer.MAX_VALUE;
        StringBuilder buff = new StringBuilder();
        for (CharSequence item2 : items) {
            if (buff.length() + maxWidth > width) {
                this.println(buff);
                buff.setLength(0);
                if (--showLines == 0) {
                    this.print(resources.getString("DISPLAY_MORE"));
                    this.flush();
                    int c = this.readCharacter();
                    if (c == 13 || c == 10) {
                        showLines = 1;
                    } else if (c != 113) {
                        showLines = height - 1;
                    }
                    this.back(resources.getString("DISPLAY_MORE").length());
                    if (c == 113) break;
                }
            }
            buff.append(item2.toString());
            for (int i = 0; i < maxWidth - item2.length(); ++i) {
                buff.append(' ');
            }
        }
        if (buff.length() > 0) {
            this.println(buff);
        }
    }

    private void beforeReadLine(String prompt, Character mask) {
        if (mask != null && this.maskThread == null) {
            final String fullPrompt = "\r" + prompt + "                 " + "                 " + "                 " + "\r" + prompt;
            this.maskThread = new Thread(){

                public void run() {
                    while (!.interrupted()) {
                        try {
                            Writer out = ConsoleReader.this.getOutput();
                            out.write(fullPrompt);
                            out.flush();
                            .sleep(3);
                            continue;
                        }
                        catch (IOException e) {
                            return;
                        }
                        catch (InterruptedException e) {
                            return;
                        }
                    }
                }
            };
            this.maskThread.setPriority(10);
            this.maskThread.setDaemon(true);
            this.maskThread.start();
        }
    }

    private void afterReadLine() {
        if (this.maskThread != null && this.maskThread.isAlive()) {
            this.maskThread.interrupt();
        }
        this.maskThread = null;
    }

    public void resetPromptLine(String prompt, String buffer, int cursorDest) throws IOException {
        this.moveToEnd();
        this.buf.buffer.append(this.prompt);
        this.buf.cursor += this.prompt.length();
        this.setPrompt("");
        this.backspaceAll();
        this.setPrompt(prompt);
        this.redrawLine();
        this.setBuffer(buffer);
        if (cursorDest < 0) {
            cursorDest = buffer.length();
        }
        this.setCursorPosition(cursorDest);
        this.flush();
    }

    public void printSearchStatus(String searchTerm, String match) throws IOException {
        String prompt = "(reverse-i-search)`" + searchTerm + "': ";
        String buffer = match;
        int cursorDest = match.indexOf(searchTerm);
        this.resetPromptLine(prompt, buffer, cursorDest);
    }

    public void restoreLine(String originalPrompt, int cursorDest) throws IOException {
        String prompt = this.lastLine(originalPrompt);
        String buffer = this.buf.buffer.toString();
        this.resetPromptLine(prompt, buffer, cursorDest);
    }

    public int searchBackwards(String searchTerm, int startIndex) {
        return this.searchBackwards(searchTerm, startIndex, false);
    }

    public int searchBackwards(String searchTerm) {
        return this.searchBackwards(searchTerm, this.history.index());
    }

    public int searchBackwards(String searchTerm, int startIndex, boolean startsWith) {
        ListIterator<History.Entry> it = this.history.entries(startIndex);
        while (it.hasPrevious()) {
            History.Entry e = it.previous();
            if (!(startsWith ? e.value().toString().startsWith(searchTerm) : e.value().toString().contains(searchTerm))) continue;
            return e.index();
        }
        return -1;
    }

    private boolean isDelimiter(char c) {
        return !Character.isLetterOrDigit(c);
    }

    private boolean isWhitespace(char c) {
        return Character.isWhitespace(c);
    }

    private void printAnsiSequence(String sequence) throws IOException {
        this.print(27);
        this.print(91);
        this.print(sequence);
        this.flush();
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    private static enum State {
        NORMAL,
        SEARCH,
        VI_YANK_TO,
        VI_DELETE_TO,
        VI_CHANGE_TO;
        

        private State() {
        }
    }

}

