
package jline.console;

import java.util.HashMap;
import java.util.Map;
import jline.console.Operation;


public class KeyMap {
    public static final String VI_MOVE = "vi-move";
    public static final String VI_INSERT = "vi-insert";
    public static final String EMACS = "emacs";
    public static final String EMACS_STANDARD = "emacs-standard";
    public static final String EMACS_CTLX = "emacs-ctlx";
    public static final String EMACS_META = "emacs-meta";
    private static final int KEYMAP_LENGTH = 256;
    private static final Object NULL_FUNCTION = new Object();
    private Object[] mapping = new Object[256];
    private Object anotherKey = null;
    private String name;
    private boolean isViKeyMap;
    public static final char CTRL_D = '\u0004';
    public static final char CTRL_G = '\u0007';
    public static final char CTRL_H = '\b';
    public static final char CTRL_I = '\t';
    public static final char CTRL_J = '\n';
    public static final char CTRL_M = '\r';
    public static final char CTRL_R = '\u0012';
    public static final char CTRL_U = '\u0015';
    public static final char CTRL_X = '\u0018';
    public static final char CTRL_Y = '\u0019';
    public static final char ESCAPE = '\u001b';
    public static final char CTRL_OB = '\u001b';
    public static final char CTRL_CB = '\u001d';
    public static final int DELETE = 127;

    public KeyMap(String name, boolean isViKeyMap) {
        this(name, new Object[256], isViKeyMap);
    }

    protected KeyMap(String name, Object[] mapping, boolean isViKeyMap) {
        this.mapping = mapping;
        this.name = name;
        this.isViKeyMap = isViKeyMap;
    }

    public boolean isViKeyMap() {
        return this.isViKeyMap;
    }

    public String getName() {
        return this.name;
    }

    public Object getAnotherKey() {
        return this.anotherKey;
    }

    public void from(KeyMap other) {
        this.mapping = other.mapping;
        this.anotherKey = other.anotherKey;
    }

    public Object getBound(CharSequence keySeq) {
        if (keySeq != null && keySeq.length() > 0) {
            KeyMap map = this;
            for (int i = 0; i < keySeq.length(); ++i) {
                char c = keySeq.charAt(i);
                if (c > '\u00ff') {
                    return Operation.SELF_INSERT;
                }
                if (map.mapping[c] instanceof KeyMap) {
                    if (i == keySeq.length() - 1) {
                        return map.mapping[c];
                    }
                } else {
                    return map.mapping[c];
                }
                map = (KeyMap)map.mapping[c];
            }
        }
        return null;
    }

    public void bindIfNotBound(CharSequence keySeq, Object function) {
        KeyMap.bind(this, keySeq, function, true);
    }

    public void bind(CharSequence keySeq, Object function) {
        KeyMap.bind(this, keySeq, function, false);
    }

    private static void bind(KeyMap map, CharSequence keySeq, Object function) {
        KeyMap.bind(map, keySeq, function, false);
    }

    private static void bind(KeyMap map, CharSequence keySeq, Object function, boolean onlyIfNotBound) {
        if (keySeq != null && keySeq.length() > 0) {
            for (int i = 0; i < keySeq.length(); ++i) {
                char c = keySeq.charAt(i);
                if (c >= map.mapping.length) {
                    return;
                }
                if (i < keySeq.length() - 1) {
                    if (!(map.mapping[c] instanceof KeyMap)) {
                        KeyMap m = new KeyMap("anonymous", false);
                        if (map.mapping[c] != Operation.DO_LOWERCASE_VERSION) {
                            m.anotherKey = map.mapping[c];
                        }
                        map.mapping[c] = m;
                    }
                    map = (KeyMap)map.mapping[c];
                    continue;
                }
                if (function == null) {
                    function = NULL_FUNCTION;
                }
                if (map.mapping[c] instanceof KeyMap) {
                    map.anotherKey = function;
                    continue;
                }
                Object op = map.mapping[c];
                if (!onlyIfNotBound || op == null || op == Operation.DO_LOWERCASE_VERSION || op == Operation.VI_MOVEMENT_MODE) {
                    // empty if block
                }
                map.mapping[c] = function;
            }
        }
    }

    public void setBlinkMatchingParen(boolean on) {
        if (on) {
            this.bind("}", (Object)Operation.INSERT_CLOSE_CURLY);
            this.bind(")", (Object)Operation.INSERT_CLOSE_PAREN);
            this.bind("]", (Object)Operation.INSERT_CLOSE_SQUARE);
        }
    }

    private static void bindArrowKeys(KeyMap map) {
        KeyMap.bind(map, "\u001b[0A", (Object)Operation.PREVIOUS_HISTORY);
        KeyMap.bind(map, "\u001b[0B", (Object)Operation.BACKWARD_CHAR);
        KeyMap.bind(map, "\u001b[0C", (Object)Operation.FORWARD_CHAR);
        KeyMap.bind(map, "\u001b[0D", (Object)Operation.NEXT_HISTORY);
        KeyMap.bind(map, "\u00e0\u0000", (Object)Operation.KILL_WHOLE_LINE);
        KeyMap.bind(map, "\u00e0G", (Object)Operation.BEGINNING_OF_LINE);
        KeyMap.bind(map, "\u00e0H", (Object)Operation.PREVIOUS_HISTORY);
        KeyMap.bind(map, "\u00e0I", (Object)Operation.BEGINNING_OF_HISTORY);
        KeyMap.bind(map, "\u00e0K", (Object)Operation.BACKWARD_CHAR);
        KeyMap.bind(map, "\u00e0M", (Object)Operation.FORWARD_CHAR);
        KeyMap.bind(map, "\u00e0O", (Object)Operation.END_OF_LINE);
        KeyMap.bind(map, "\u00e0P", (Object)Operation.NEXT_HISTORY);
        KeyMap.bind(map, "\u00e0Q", (Object)Operation.END_OF_HISTORY);
        KeyMap.bind(map, "\u00e0R", (Object)Operation.OVERWRITE_MODE);
        KeyMap.bind(map, "\u00e0S", (Object)Operation.DELETE_CHAR);
        KeyMap.bind(map, "\u0000H", (Object)Operation.PREVIOUS_HISTORY);
        KeyMap.bind(map, "\u0000K", (Object)Operation.BACKWARD_CHAR);
        KeyMap.bind(map, "\u0000M", (Object)Operation.FORWARD_CHAR);
        KeyMap.bind(map, "\u0000P", (Object)Operation.NEXT_HISTORY);
        KeyMap.bind(map, "\u0000S", (Object)Operation.DELETE_CHAR);
        KeyMap.bind(map, "\u001b[A", (Object)Operation.PREVIOUS_HISTORY);
        KeyMap.bind(map, "\u001b[B", (Object)Operation.NEXT_HISTORY);
        KeyMap.bind(map, "\u001b[C", (Object)Operation.FORWARD_CHAR);
        KeyMap.bind(map, "\u001b[D", (Object)Operation.BACKWARD_CHAR);
        KeyMap.bind(map, "\u001b[H", (Object)Operation.BEGINNING_OF_LINE);
        KeyMap.bind(map, "\u001b[F", (Object)Operation.END_OF_LINE);
        KeyMap.bind(map, "\u001b[OA", (Object)Operation.PREVIOUS_HISTORY);
        KeyMap.bind(map, "\u001b[OB", (Object)Operation.NEXT_HISTORY);
        KeyMap.bind(map, "\u001b[OC", (Object)Operation.FORWARD_CHAR);
        KeyMap.bind(map, "\u001b[OD", (Object)Operation.BACKWARD_CHAR);
        KeyMap.bind(map, "\u001b[OH", (Object)Operation.BEGINNING_OF_LINE);
        KeyMap.bind(map, "\u001b[OF", (Object)Operation.END_OF_LINE);
        KeyMap.bind(map, "\u001b[3~", (Object)Operation.DELETE_CHAR);
        KeyMap.bind(map, "\u001c0H", (Object)Operation.PREVIOUS_HISTORY);
        KeyMap.bind(map, "\u001c0P", (Object)Operation.NEXT_HISTORY);
        KeyMap.bind(map, "\u001c0M", (Object)Operation.FORWARD_CHAR);
        KeyMap.bind(map, "\u001c0K", (Object)Operation.BACKWARD_CHAR);
    }

    public static boolean isMeta(char c) {
        return c > '' && c <= '\u00ff';
    }

    public static char unMeta(char c) {
        return (char)(c & 127);
    }

    public static char meta(char c) {
        return (char)(c | 128);
    }

    public static Map<String, KeyMap> keyMaps() {
        HashMap<String, KeyMap> keyMaps = new HashMap<String, KeyMap>();
        KeyMap emacs = KeyMap.emacs();
        KeyMap.bindArrowKeys(emacs);
        keyMaps.put("emacs", emacs);
        keyMaps.put("emacs-standard", emacs);
        keyMaps.put("emacs-ctlx", (KeyMap)emacs.getBound("\u0018"));
        keyMaps.put("emacs-meta", (KeyMap)emacs.getBound("\u001b"));
        KeyMap viMov = KeyMap.viMovement();
        KeyMap.bindArrowKeys(viMov);
        keyMaps.put("vi-move", viMov);
        keyMaps.put("vi-command", viMov);
        KeyMap viIns = KeyMap.viInsertion();
        KeyMap.bindArrowKeys(viIns);
        keyMaps.put("vi-insert", viIns);
        keyMaps.put("vi", viIns);
        return keyMaps;
    }

    public static KeyMap emacs() {
        Object[] map = new Object[256];
        Object[] ctrl = new Object[]{Operation.SET_MARK, Operation.BEGINNING_OF_LINE, Operation.BACKWARD_CHAR, null, Operation.EXIT_OR_DELETE_CHAR, Operation.END_OF_LINE, Operation.FORWARD_CHAR, Operation.ABORT, Operation.BACKWARD_DELETE_CHAR, Operation.COMPLETE, Operation.ACCEPT_LINE, Operation.KILL_LINE, Operation.CLEAR_SCREEN, Operation.ACCEPT_LINE, Operation.NEXT_HISTORY, null, Operation.PREVIOUS_HISTORY, Operation.QUOTED_INSERT, Operation.REVERSE_SEARCH_HISTORY, Operation.FORWARD_SEARCH_HISTORY, Operation.TRANSPOSE_CHARS, Operation.UNIX_LINE_DISCARD, Operation.QUOTED_INSERT, Operation.UNIX_WORD_RUBOUT, KeyMap.emacsCtrlX(), Operation.YANK, null, KeyMap.emacsMeta(), null, Operation.CHARACTER_SEARCH, null, Operation.UNDO};
        System.arraycopy(ctrl, 0, map, 0, ctrl.length);
        for (int i = 32; i < 256; ++i) {
            map[i] = Operation.SELF_INSERT;
        }
        map[127] = Operation.BACKWARD_DELETE_CHAR;
        return new KeyMap("emacs", map, false);
    }

    public static KeyMap emacsCtrlX() {
        Object[] map = new Object[256];
        map[7] = Operation.ABORT;
        map[18] = Operation.RE_READ_INIT_FILE;
        map[21] = Operation.UNDO;
        map[24] = Operation.EXCHANGE_POINT_AND_MARK;
        map[40] = Operation.START_KBD_MACRO;
        map[41] = Operation.END_KBD_MACRO;
        for (int i = 65; i <= 90; ++i) {
            map[i] = Operation.DO_LOWERCASE_VERSION;
        }
        map[101] = Operation.CALL_LAST_KBD_MACRO;
        map[127] = Operation.KILL_LINE;
        return new KeyMap("emacs-ctlx", map, false);
    }

    public static KeyMap emacsMeta() {
        Object[] map = new Object[256];
        map[7] = Operation.ABORT;
        map[8] = Operation.BACKWARD_KILL_WORD;
        map[9] = Operation.TAB_INSERT;
        map[10] = Operation.VI_EDITING_MODE;
        map[13] = Operation.VI_EDITING_MODE;
        map[18] = Operation.REVERT_LINE;
        map[25] = Operation.YANK_NTH_ARG;
        map[27] = Operation.COMPLETE;
        map[29] = Operation.CHARACTER_SEARCH_BACKWARD;
        map[32] = Operation.SET_MARK;
        map[35] = Operation.INSERT_COMMENT;
        map[38] = Operation.TILDE_EXPAND;
        map[42] = Operation.INSERT_COMPLETIONS;
        map[45] = Operation.DIGIT_ARGUMENT;
        map[46] = Operation.YANK_LAST_ARG;
        map[60] = Operation.BEGINNING_OF_HISTORY;
        map[61] = Operation.POSSIBLE_COMPLETIONS;
        map[62] = Operation.END_OF_HISTORY;
        map[63] = Operation.POSSIBLE_COMPLETIONS;
        for (int i = 65; i <= 90; ++i) {
            map[i] = Operation.DO_LOWERCASE_VERSION;
        }
        map[92] = Operation.DELETE_HORIZONTAL_SPACE;
        map[95] = Operation.YANK_LAST_ARG;
        map[98] = Operation.BACKWARD_WORD;
        map[99] = Operation.CAPITALIZE_WORD;
        map[100] = Operation.KILL_WORD;
        map[102] = Operation.FORWARD_WORD;
        map[108] = Operation.DOWNCASE_WORD;
        map[112] = Operation.NON_INCREMENTAL_REVERSE_SEARCH_HISTORY;
        map[114] = Operation.REVERT_LINE;
        map[116] = Operation.TRANSPOSE_WORDS;
        map[117] = Operation.UPCASE_WORD;
        map[121] = Operation.YANK_POP;
        map[126] = Operation.TILDE_EXPAND;
        map[127] = Operation.BACKWARD_KILL_WORD;
        return new KeyMap("emacs-meta", map, false);
    }

    public static KeyMap viInsertion() {
        Object[] map = new Object[256];
        Object[] ctrl = new Object[]{null, Operation.SELF_INSERT, Operation.SELF_INSERT, Operation.SELF_INSERT, Operation.VI_EOF_MAYBE, Operation.SELF_INSERT, Operation.SELF_INSERT, Operation.SELF_INSERT, Operation.BACKWARD_DELETE_CHAR, Operation.COMPLETE, Operation.ACCEPT_LINE, Operation.SELF_INSERT, Operation.SELF_INSERT, Operation.ACCEPT_LINE, Operation.MENU_COMPLETE, Operation.SELF_INSERT, Operation.MENU_COMPLETE_BACKWARD, Operation.SELF_INSERT, Operation.REVERSE_SEARCH_HISTORY, Operation.FORWARD_SEARCH_HISTORY, Operation.TRANSPOSE_CHARS, Operation.UNIX_LINE_DISCARD, Operation.QUOTED_INSERT, Operation.UNIX_WORD_RUBOUT, Operation.SELF_INSERT, Operation.YANK, Operation.SELF_INSERT, Operation.VI_MOVEMENT_MODE, Operation.SELF_INSERT, Operation.SELF_INSERT, Operation.SELF_INSERT, Operation.UNDO};
        System.arraycopy(ctrl, 0, map, 0, ctrl.length);
        for (int i = 32; i < 256; ++i) {
            map[i] = Operation.SELF_INSERT;
        }
        map[127] = Operation.BACKWARD_DELETE_CHAR;
        return new KeyMap("vi-insert", map, false);
    }

    public static KeyMap viMovement() {
        Object[] map = new Object[256];
        Object[] low = new Object[]{null, null, null, null, Operation.VI_EOF_MAYBE, Operation.EMACS_EDITING_MODE, null, Operation.ABORT, Operation.BACKWARD_CHAR, null, Operation.VI_MOVE_ACCEPT_LINE, Operation.KILL_LINE, Operation.CLEAR_SCREEN, Operation.VI_MOVE_ACCEPT_LINE, Operation.VI_NEXT_HISTORY, null, Operation.VI_PREVIOUS_HISTORY, Operation.QUOTED_INSERT, Operation.REVERSE_SEARCH_HISTORY, Operation.FORWARD_SEARCH_HISTORY, Operation.TRANSPOSE_CHARS, Operation.UNIX_LINE_DISCARD, Operation.QUOTED_INSERT, Operation.UNIX_WORD_RUBOUT, null, Operation.YANK, null, KeyMap.emacsMeta(), null, Operation.CHARACTER_SEARCH, null, Operation.UNDO, Operation.FORWARD_CHAR, null, null, Operation.VI_INSERT_COMMENT, Operation.END_OF_LINE, Operation.VI_MATCH, Operation.VI_TILDE_EXPAND, null, null, null, Operation.VI_COMPLETE, Operation.VI_NEXT_HISTORY, Operation.VI_CHAR_SEARCH, Operation.VI_PREVIOUS_HISTORY, Operation.VI_REDO, Operation.VI_SEARCH, Operation.VI_BEGNNING_OF_LINE_OR_ARG_DIGIT, Operation.VI_ARG_DIGIT, Operation.VI_ARG_DIGIT, Operation.VI_ARG_DIGIT, Operation.VI_ARG_DIGIT, Operation.VI_ARG_DIGIT, Operation.VI_ARG_DIGIT, Operation.VI_ARG_DIGIT, Operation.VI_ARG_DIGIT, Operation.VI_ARG_DIGIT, null, Operation.VI_CHAR_SEARCH, null, Operation.VI_COMPLETE, null, Operation.VI_SEARCH, null, Operation.VI_APPEND_EOL, Operation.VI_PREV_WORD, Operation.VI_CHANGE_TO, Operation.VI_DELETE_TO, Operation.VI_END_WORD, Operation.VI_CHAR_SEARCH, Operation.VI_FETCH_HISTORY, null, Operation.VI_INSERT_BEG, null, null, null, null, Operation.VI_SEARCH_AGAIN, null, Operation.VI_PUT, null, Operation.VI_REPLACE, Operation.VI_SUBST, Operation.VI_CHAR_SEARCH, Operation.REVERT_LINE, null, Operation.VI_NEXT_WORD, Operation.VI_RUBOUT, Operation.VI_YANK_TO, null, null, Operation.VI_COMPLETE, null, Operation.VI_FIRST_PRINT, Operation.VI_YANK_ARG, Operation.VI_GOTO_MARK, Operation.VI_APPEND_MODE, Operation.VI_PREV_WORD, Operation.VI_CHANGE_TO, Operation.VI_DELETE_TO, Operation.VI_END_WORD, Operation.VI_CHAR_SEARCH, null, Operation.BACKWARD_CHAR, Operation.VI_INSERTION_MODE, Operation.NEXT_HISTORY, Operation.PREVIOUS_HISTORY, Operation.FORWARD_CHAR, Operation.VI_SET_MARK, Operation.VI_SEARCH_AGAIN, null, Operation.VI_PUT, null, Operation.VI_CHANGE_CHAR, Operation.VI_SUBST, Operation.VI_CHAR_SEARCH, Operation.UNDO, null, Operation.VI_NEXT_WORD, Operation.VI_DELETE, Operation.VI_YANK_TO, null, null, Operation.VI_COLUMN, null, Operation.VI_CHANGE_CASE, Operation.VI_DELETE};
        System.arraycopy(low, 0, map, 0, low.length);
        for (int i = 128; i < 256; ++i) {
            map[i] = null;
        }
        return new KeyMap("vi-move", map, false);
    }
}

