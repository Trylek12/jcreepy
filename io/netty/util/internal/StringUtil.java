
package io.netty.util.internal;

import java.util.ArrayList;
import java.util.Formatter;

public final class StringUtil {
    public static final String NEWLINE;
    private static final String EMPTY_STRING = "";

    private StringUtil() {
    }

    public static String[] split(String value, char delim) {
        int i;
        int end = value.length();
        ArrayList<String> res = new ArrayList<String>();
        int start = 0;
        for (i = 0; i < end; ++i) {
            if (value.charAt(i) != delim) continue;
            if (start == i) {
                res.add("");
            } else {
                res.add(value.substring(start, i));
            }
            start = i + 1;
        }
        if (start == 0) {
            res.add(value);
        } else if (start != end) {
            res.add(value.substring(start, end));
        } else {
            for (i = res.size() - 1; i >= 0 && ((String)res.get(i)).isEmpty(); --i) {
                res.remove(i);
            }
        }
        return res.toArray(new String[res.size()]);
    }

    static {
        String newLine;
        try {
            newLine = new Formatter().format("%n", new Object[0]).toString();
        }
        catch (Exception e) {
            newLine = "\n";
        }
        NEWLINE = newLine;
    }
}

