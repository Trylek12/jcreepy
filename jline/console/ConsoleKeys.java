
package jline.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import jline.console.KeyMap;
import jline.console.Operation;
import jline.internal.Log;


public class ConsoleKeys {
    private KeyMap keys;
    private Map<String, KeyMap> keyMaps = KeyMap.keyMaps();
    private Map<String, String> variables = new HashMap<String, String>();

    public ConsoleKeys(String appName, URL inputrcUrl) {
        this.loadKeys(appName, inputrcUrl);
    }

    protected boolean isViEditMode() {
        return this.keys.isViKeyMap();
    }

    protected boolean setKeyMap(String name) {
        KeyMap map = this.keyMaps.get(name);
        if (map == null) {
            return false;
        }
        this.keys = map;
        return true;
    }

    protected Map<String, KeyMap> getKeyMaps() {
        return this.keyMaps;
    }

    protected KeyMap getKeys() {
        return this.keys;
    }

    protected void setKeys(KeyMap keys) {
        this.keys = keys;
    }

    protected boolean getViEditMode() {
        return this.keys.isViKeyMap();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    protected void loadKeys(String appName, URL inputrcUrl) {
        this.keys = this.keyMaps.get("emacs");
        try {
            InputStream input = inputrcUrl.openStream();
            try {
                this.loadKeys(input, appName);
                Log.debug("Loaded user configuration: ", inputrcUrl);
            }
            finally {
                try {
                    input.close();
                }
                catch (IOException e) {}
            }
        }
        catch (IOException e) {
            if (inputrcUrl.getProtocol().equals("file")) {
                File file = new File(inputrcUrl.getPath());
                if (file.exists()) {
                    Log.warn("Unable to read user configuration: ", inputrcUrl, e);
                }
            }
            Log.warn("Unable to read user configuration: ", inputrcUrl, e);
        }
    }

    private void loadKeys(InputStream input, String appName) throws IOException {
        String line;
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        boolean parsing = true;
        ArrayList<Boolean> ifsStack = new ArrayList<Boolean>();
        while ((line = reader.readLine()) != null) {
            try {
                String val;
                if ((line = line.trim()).length() == 0 || line.charAt(0) == '#') continue;
                int i = 0;
                if (line.charAt(i) == '$') {
                    ++i;
                    while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                        ++i;
                    }
                    int s = i;
                    while (i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                        ++i;
                    }
                    String cmd = line.substring(s, i);
                    while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                        ++i;
                    }
                    s = i;
                    while (i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                        ++i;
                    }
                    String args = line.substring(s, i);
                    if ("if".equalsIgnoreCase(cmd)) {
                        ifsStack.add(parsing);
                        if (!parsing || args.startsWith("term=")) continue;
                        if (args.startsWith("mode=")) {
                            if (args.equalsIgnoreCase("mode=vi")) {
                                parsing = this.isViEditMode();
                                continue;
                            }
                            if (args.equals("mode=emacs")) {
                                parsing = !this.isViEditMode();
                                continue;
                            }
                            parsing = false;
                            continue;
                        }
                        parsing = args.equalsIgnoreCase(appName);
                        continue;
                    }
                    if ("else".equalsIgnoreCase(cmd)) {
                        if (ifsStack.isEmpty()) {
                            throw new IllegalArgumentException("$else found without matching $if");
                        }
                        boolean invert = true;
                        Iterator i$ = ifsStack.iterator();
                        while (i$.hasNext()) {
                            boolean b = (Boolean)i$.next();
                            if (b) continue;
                            invert = false;
                            break;
                        }
                        if (!invert) continue;
                        parsing = !parsing;
                        continue;
                    }
                    if ("endif".equalsIgnoreCase(cmd)) {
                        if (ifsStack.isEmpty()) {
                            throw new IllegalArgumentException("endif found without matching $if");
                        }
                        parsing = (Boolean)ifsStack.remove(ifsStack.size() - 1);
                        continue;
                    }
                    if (!"include".equalsIgnoreCase(cmd)) continue;
                }
                if (!parsing) continue;
                String keySeq = "";
                if (line.charAt(i++) == '\"') {
                    boolean esc = false;
                    do {
                        if (i >= line.length()) {
                            throw new IllegalArgumentException("Missing closing quote on line '" + line + "'");
                        }
                        if (esc) {
                            esc = false;
                        } else if (line.charAt(i) == '\\') {
                            esc = true;
                        } else if (line.charAt(i) == '\"') break;
                        ++i;
                    } while (true);
                }
                while (i < line.length() && line.charAt(i) != ':' && line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                    ++i;
                }
                keySeq = line.substring(0, i);
                boolean equivalency = i + 1 < line.length() && line.charAt(i) == ':' && line.charAt(i + 1) == '=';
                ++i;
                if (equivalency) {
                    ++i;
                }
                if (keySeq.equalsIgnoreCase("set")) {
                    while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                        ++i;
                    }
                    int s = i;
                    while (i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                        ++i;
                    }
                    String key = line.substring(s, i);
                    while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                        ++i;
                    }
                    s = i;
                    while (i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                        ++i;
                    }
                    val = line.substring(s, i);
                    this.setVar(key, val);
                    continue;
                }
                while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) {
                    ++i;
                }
                int start = i;
                if (i < line.length() && (line.charAt(i) == '\'' || line.charAt(i) == '\"')) {
                    char delim = line.charAt(i++);
                    boolean esc = false;
                    while (i < line.length()) {
                        if (esc) {
                            esc = false;
                        } else if (line.charAt(i) == '\\') {
                            esc = true;
                        } else if (line.charAt(i) == delim) break;
                        ++i;
                    }
                }
                while (i < line.length() && line.charAt(i) != ' ' && line.charAt(i) != '\t') {
                    ++i;
                }
                val = line.substring(Math.min(start, line.length()), Math.min(i, line.length()));
                if (keySeq.charAt(0) == '\"') {
                    keySeq = this.translateQuoted(keySeq);
                } else {
                    String keyName = keySeq.lastIndexOf(45) > 0 ? keySeq.substring(keySeq.lastIndexOf(45) + 1) : keySeq;
                    char key = this.getKeyFromName(keyName);
                    keyName = keySeq.toLowerCase();
                    keySeq = "";
                    if (keyName.contains("meta-") || keyName.contains("m-")) {
                        keySeq = keySeq + "\u001b";
                    }
                    if (keyName.contains("control-") || keyName.contains("c-") || keyName.contains("ctrl-")) {
                        key = (char)(Character.toUpperCase(key) & 31);
                    }
                    keySeq = keySeq + key;
                }
                if (val.length() > 0 && (val.charAt(0) == '\'' || val.charAt(0) == '\"')) {
                    this.keys.bind(keySeq, this.translateQuoted(val));
                    continue;
                }
                String operationName = val.replace('-', '_').toUpperCase();
                try {
                    this.keys.bind(keySeq, (Object)Operation.valueOf(operationName));
                    continue;
                }
                catch (IllegalArgumentException e) {
                    Log.info("Unable to bind key for unsupported operation: ", val);
                }
            }
            catch (IllegalArgumentException e) {
                Log.warn("Unable to parse user configuration: ", e);
            }
        }
    }

    private String translateQuoted(String keySeq) {
        String str = keySeq.substring(1, keySeq.length() - 1);
        keySeq = "";
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c == '\\') {
                boolean meta;
                boolean ctrl = str.regionMatches(i, "\\C-", 0, 3) || str.regionMatches(i, "\\M-\\C-", 0, 6);
                if ((i += ((meta = str.regionMatches(i, "\\M-", 0, 3) || str.regionMatches(i, "\\C-\\M-", 0, 6)) ? 3 : 0) + (ctrl ? 3 : 0) + (!meta && !ctrl ? 1 : 0)) >= str.length()) break;
                c = str.charAt(i);
                if (meta) {
                    keySeq = keySeq + "\u001b";
                }
                if (ctrl) {
                    char c2 = c = c == '?' ? '' : (char)(Character.toUpperCase(c) & 31);
                }
                if (!meta && !ctrl) {
                    switch (c) {
                        int k;
                        int j;
                        case 'a': {
                            c = '\u0007';
                            break;
                        }
                        case 'b': {
                            c = '\b';
                            break;
                        }
                        case 'd': {
                            c = '';
                            break;
                        }
                        case 'e': {
                            c = '\u001b';
                            break;
                        }
                        case 'f': {
                            c = '\f';
                            break;
                        }
                        case 'n': {
                            c = '\n';
                            break;
                        }
                        case 'r': {
                            c = '\r';
                            break;
                        }
                        case 't': {
                            c = '\t';
                            break;
                        }
                        case 'v': {
                            c = '\u000b';
                            break;
                        }
                        case '\\': {
                            c = '\\';
                            break;
                        }
                        case '0': 
                        case '1': 
                        case '2': 
                        case '3': 
                        case '4': 
                        case '5': 
                        case '6': 
                        case '7': {
                            c = '\u0000';
                            for (j = 0; j < 3 && i < str.length() && (k = Character.digit(str.charAt(i), 8)) >= 0; ++j, ++i) {
                                c = (char)(c * 8 + k);
                            }
                            c = (char)(c & 255);
                            break;
                        }
                        case 'x': {
                            ++i;
                            c = '\u0000';
                            for (j = 0; j < 2 && i < str.length() && (k = Character.digit(str.charAt(i), 16)) >= 0; ++j, ++i) {
                                c = (char)(c * 16 + k);
                            }
                            c = (char)(c & 255);
                            break;
                        }
                        case 'u': {
                            ++i;
                            c = '\u0000';
                            for (j = 0; j < 4 && i < str.length() && (k = Character.digit(str.charAt(i), 16)) >= 0; ++j, ++i) {
                                c = (char)(c * 16 + k);
                            }
                            break;
                        }
                    }
                }
                keySeq = keySeq + c;
                continue;
            }
            keySeq = keySeq + c;
        }
        return keySeq;
    }

    private char getKeyFromName(String name) {
        if ("DEL".equalsIgnoreCase(name) || "Rubout".equalsIgnoreCase(name)) {
            return '';
        }
        if ("ESC".equalsIgnoreCase(name) || "Escape".equalsIgnoreCase(name)) {
            return '\u001b';
        }
        if ("LFD".equalsIgnoreCase(name) || "NewLine".equalsIgnoreCase(name)) {
            return '\n';
        }
        if ("RET".equalsIgnoreCase(name) || "Return".equalsIgnoreCase(name)) {
            return '\r';
        }
        if ("SPC".equalsIgnoreCase(name) || "Space".equalsIgnoreCase(name)) {
            return ' ';
        }
        if ("Tab".equalsIgnoreCase(name)) {
            return '\t';
        }
        return name.charAt(0);
    }

    private void setVar(String key, String val) {
        if ("keymap".equalsIgnoreCase(key)) {
            if (this.keyMaps.containsKey(val)) {
                this.keys = this.keyMaps.get(val);
            }
        } else if ("editing-mode".equals(key)) {
            if ("vi".equalsIgnoreCase(val)) {
                this.keys = this.keyMaps.get("vi-insert");
            } else if ("emacs".equalsIgnoreCase(key)) {
                this.keys = this.keyMaps.get("emacs");
            }
        } else if ("blink-matching-paren".equals(key)) {
            if ("on".equalsIgnoreCase(val)) {
                this.keys.setBlinkMatchingParen(true);
            } else if ("off".equalsIgnoreCase(val)) {
                this.keys.setBlinkMatchingParen(false);
            }
        }
        this.variables.put(key, val);
    }

    public String getVariable(String var) {
        return this.variables.get(var);
    }
}

