
package org.fusesource.jansi;

import org.fusesource.jansi.Ansi;

public class AnsiRenderer {
    public static final String BEGIN_TOKEN = "@|";
    private static final int BEGIN_TOKEN_LEN = 2;
    public static final String END_TOKEN = "|@";
    private static final int END_TOKEN_LEN = 2;
    public static final String CODE_TEXT_SEPARATOR = " ";
    public static final String CODE_LIST_SEPARATOR = ",";

    public static String render(String input) throws IllegalArgumentException {
        StringBuffer buff = new StringBuffer();
        int i = 0;
        do {
            int j;
            if ((j = input.indexOf("@|", i)) == -1) {
                if (i == 0) {
                    return input;
                }
                buff.append(input.substring(i, input.length()));
                return buff.toString();
            }
            buff.append(input.substring(i, j));
            int k = input.indexOf("|@", j);
            if (k == -1) {
                return input;
            }
            String spec = input.substring(j += 2, k);
            String[] items = spec.split(" ", 2);
            if (items.length == 1) {
                return input;
            }
            String replacement = AnsiRenderer.render(items[1], items[0].split(","));
            buff.append(replacement);
            i = k + 2;
        } while (true);
    }

    private static /* varargs */ String render(String text, String ... codes) {
        Ansi ansi = Ansi.ansi();
        for (String name : codes) {
            Code code = Code.valueOf(name.toUpperCase());
            if (code.isColor()) {
                if (code.isBackground()) {
                    ansi = ansi.bg(code.getColor());
                    continue;
                }
                ansi = ansi.fg(code.getColor());
                continue;
            }
            if (!code.isAttribute()) continue;
            ansi = ansi.a(code.getAttribute());
        }
        return ansi.a(text).reset().toString();
    }

    public static boolean test(String text) {
        return text != null && text.contains("@|");
    }

    /*
     * This class specifies class file version 49.0 but uses Java 6 signatures.  Assumed Java 6.
     */
    public static enum Code {
        BLACK(Ansi.Color.BLACK),
        RED(Ansi.Color.RED),
        GREEN(Ansi.Color.GREEN),
        YELLOW(Ansi.Color.YELLOW),
        BLUE(Ansi.Color.BLUE),
        MAGENTA(Ansi.Color.MAGENTA),
        CYAN(Ansi.Color.CYAN),
        WHITE(Ansi.Color.WHITE),
        FG_BLACK(Ansi.Color.BLACK, false),
        FG_RED(Ansi.Color.RED, false),
        FG_GREEN(Ansi.Color.GREEN, false),
        FG_YELLOW(Ansi.Color.YELLOW, false),
        FG_BLUE(Ansi.Color.BLUE, false),
        FG_MAGENTA(Ansi.Color.MAGENTA, false),
        FG_CYAN(Ansi.Color.CYAN, false),
        FG_WHITE(Ansi.Color.WHITE, false),
        BG_BLACK(Ansi.Color.BLACK, true),
        BG_RED(Ansi.Color.RED, true),
        BG_GREEN(Ansi.Color.GREEN, true),
        BG_YELLOW(Ansi.Color.YELLOW, true),
        BG_BLUE(Ansi.Color.BLUE, true),
        BG_MAGENTA(Ansi.Color.MAGENTA, true),
        BG_CYAN(Ansi.Color.CYAN, true),
        BG_WHITE(Ansi.Color.WHITE, true),
        RESET(Ansi.Attribute.RESET),
        INTENSITY_BOLD(Ansi.Attribute.INTENSITY_BOLD),
        INTENSITY_FAINT(Ansi.Attribute.INTENSITY_FAINT),
        ITALIC(Ansi.Attribute.ITALIC),
        UNDERLINE(Ansi.Attribute.UNDERLINE),
        BLINK_SLOW(Ansi.Attribute.BLINK_SLOW),
        BLINK_FAST(Ansi.Attribute.BLINK_FAST),
        BLINK_OFF(Ansi.Attribute.BLINK_OFF),
        NEGATIVE_ON(Ansi.Attribute.NEGATIVE_ON),
        NEGATIVE_OFF(Ansi.Attribute.NEGATIVE_OFF),
        CONCEAL_ON(Ansi.Attribute.CONCEAL_ON),
        CONCEAL_OFF(Ansi.Attribute.CONCEAL_OFF),
        UNDERLINE_DOUBLE(Ansi.Attribute.UNDERLINE_DOUBLE),
        UNDERLINE_OFF(Ansi.Attribute.UNDERLINE_OFF),
        BOLD(Ansi.Attribute.INTENSITY_BOLD),
        FAINT(Ansi.Attribute.INTENSITY_FAINT);
        
        private final Enum n;
        private final boolean background;

        private Code(Enum n2, boolean background) {
            this.n = n2;
            this.background = background;
        }

        private Code(Enum n2) {
            this(n2, false);
        }

        public boolean isColor() {
            return this.n instanceof Ansi.Color;
        }

        public Ansi.Color getColor() {
            return (Ansi.Color)this.n;
        }

        public boolean isAttribute() {
            return this.n instanceof Ansi.Attribute;
        }

        public Ansi.Attribute getAttribute() {
            return (Ansi.Attribute)this.n;
        }

        public boolean isBackground() {
            return this.background;
        }
    }

}

