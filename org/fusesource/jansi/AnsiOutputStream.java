
package org.fusesource.jansi;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import org.fusesource.jansi.Ansi;


public class AnsiOutputStream
extends FilterOutputStream {
    public static final byte[] REST_CODE = AnsiOutputStream.resetCode();
    private static final int MAX_ESCAPE_SEQUENCE_LENGTH = 100;
    private byte[] buffer = new byte[100];
    private int pos = 0;
    private int startOfValue;
    private final ArrayList<Object> options = new ArrayList();
    private static final int LOOKING_FOR_FIRST_ESC_CHAR = 0;
    private static final int LOOKING_FOR_SECOND_ESC_CHAR = 1;
    private static final int LOOKING_FOR_NEXT_ARG = 2;
    private static final int LOOKING_FOR_STR_ARG_END = 3;
    private static final int LOOKING_FOR_INT_ARG_END = 4;
    private static final int LOOKING_FOR_OSC_COMMAND = 5;
    private static final int LOOKING_FOR_OSC_COMMAND_END = 6;
    private static final int LOOKING_FOR_OSC_PARAM = 7;
    private static final int LOOKING_FOR_ST = 8;
    int state = 0;
    private static final int FIRST_ESC_CHAR = 27;
    private static final int SECOND_ESC_CHAR = 91;
    private static final int SECOND_OSC_CHAR = 93;
    private static final int BEL = 7;
    private static final int SECOND_ST_CHAR = 92;
    protected static final int ERASE_SCREEN_TO_END = 0;
    protected static final int ERASE_SCREEN_TO_BEGINING = 1;
    protected static final int ERASE_SCREEN = 2;
    protected static final int ERASE_LINE_TO_END = 0;
    protected static final int ERASE_LINE_TO_BEGINING = 1;
    protected static final int ERASE_LINE = 2;
    protected static final int ATTRIBUTE_INTENSITY_BOLD = 1;
    protected static final int ATTRIBUTE_INTENSITY_FAINT = 2;
    protected static final int ATTRIBUTE_ITALIC = 3;
    protected static final int ATTRIBUTE_UNDERLINE = 4;
    protected static final int ATTRIBUTE_BLINK_SLOW = 5;
    protected static final int ATTRIBUTE_BLINK_FAST = 6;
    protected static final int ATTRIBUTE_NEGATIVE_ON = 7;
    protected static final int ATTRIBUTE_CONCEAL_ON = 8;
    protected static final int ATTRIBUTE_UNDERLINE_DOUBLE = 21;
    protected static final int ATTRIBUTE_INTENSITY_NORMAL = 22;
    protected static final int ATTRIBUTE_UNDERLINE_OFF = 24;
    protected static final int ATTRIBUTE_BLINK_OFF = 25;
    protected static final int ATTRIBUTE_NEGATIVE_Off = 27;
    protected static final int ATTRIBUTE_CONCEAL_OFF = 28;
    protected static final int BLACK = 0;
    protected static final int RED = 1;
    protected static final int GREEN = 2;
    protected static final int YELLOW = 3;
    protected static final int BLUE = 4;
    protected static final int MAGENTA = 5;
    protected static final int CYAN = 6;
    protected static final int WHITE = 7;

    public AnsiOutputStream(OutputStream os) {
        super(os);
    }

    @Override
    public void write(int data) throws IOException {
        switch (this.state) {
            case 0: {
                if (data == 27) {
                    this.buffer[this.pos++] = (byte)data;
                    this.state = 1;
                    break;
                }
                this.out.write(data);
                break;
            }
            case 1: {
                this.buffer[this.pos++] = (byte)data;
                if (data == 91) {
                    this.state = 2;
                    break;
                }
                if (data == 93) {
                    this.state = 5;
                    break;
                }
                this.reset(false);
                break;
            }
            case 2: {
                this.buffer[this.pos++] = (byte)data;
                if (34 == data) {
                    this.startOfValue = this.pos - 1;
                    this.state = 3;
                    break;
                }
                if (48 <= data && data <= 57) {
                    this.startOfValue = this.pos - 1;
                    this.state = 4;
                    break;
                }
                if (59 == data) {
                    this.options.add(null);
                    break;
                }
                if (63 == data) {
                    this.options.add(new Character('?'));
                    break;
                }
                if (61 == data) {
                    this.options.add(new Character('='));
                    break;
                }
                this.reset(this.processEscapeCommand(this.options, data));
                break;
            }
            case 4: {
                this.buffer[this.pos++] = (byte)data;
                if (48 <= data && data <= 57) break;
                String strValue = new String(this.buffer, this.startOfValue, this.pos - 1 - this.startOfValue, "UTF-8");
                Integer value = new Integer(strValue);
                this.options.add(value);
                if (data == 59) {
                    this.state = 2;
                    break;
                }
                this.reset(this.processEscapeCommand(this.options, data));
                break;
            }
            case 3: {
                this.buffer[this.pos++] = (byte)data;
                if (34 == data) break;
                String value = new String(this.buffer, this.startOfValue, this.pos - 1 - this.startOfValue, "UTF-8");
                this.options.add(value);
                if (data == 59) {
                    this.state = 2;
                    break;
                }
                this.reset(this.processEscapeCommand(this.options, data));
                break;
            }
            case 5: {
                this.buffer[this.pos++] = (byte)data;
                if (48 <= data && data <= 57) {
                    this.startOfValue = this.pos - 1;
                    this.state = 6;
                    break;
                }
                this.reset(false);
                break;
            }
            case 6: {
                this.buffer[this.pos++] = (byte)data;
                if (59 == data) {
                    String strValue = new String(this.buffer, this.startOfValue, this.pos - 1 - this.startOfValue, "UTF-8");
                    Integer value = new Integer(strValue);
                    this.options.add(value);
                    this.startOfValue = this.pos;
                    this.state = 7;
                    break;
                }
                if (48 <= data && data <= 57) break;
                this.reset(false);
                break;
            }
            case 7: {
                this.buffer[this.pos++] = (byte)data;
                if (7 == data) {
                    String value = new String(this.buffer, this.startOfValue, this.pos - 1 - this.startOfValue, "UTF-8");
                    this.options.add(value);
                    this.reset(this.processOperatingSystemCommand(this.options));
                    break;
                }
                if (27 != data) break;
                this.state = 8;
                break;
            }
            case 8: {
                this.buffer[this.pos++] = (byte)data;
                if (92 == data) {
                    String value = new String(this.buffer, this.startOfValue, this.pos - 2 - this.startOfValue, "UTF-8");
                    this.options.add(value);
                    this.reset(this.processOperatingSystemCommand(this.options));
                    break;
                }
                this.state = 7;
            }
        }
        if (this.pos >= this.buffer.length) {
            this.reset(false);
        }
    }

    private void reset(boolean skipBuffer) throws IOException {
        if (!skipBuffer) {
            this.out.write(this.buffer, 0, this.pos);
        }
        this.pos = 0;
        this.startOfValue = 0;
        this.options.clear();
        this.state = 0;
    }

    private boolean processEscapeCommand(ArrayList<Object> options, int command) throws IOException {
        try {
            switch (command) {
                case 65: {
                    this.processCursorUp(this.optionInt(options, 0, 1));
                    return true;
                }
                case 66: {
                    this.processCursorDown(this.optionInt(options, 0, 1));
                    return true;
                }
                case 67: {
                    this.processCursorRight(this.optionInt(options, 0, 1));
                    return true;
                }
                case 68: {
                    this.processCursorLeft(this.optionInt(options, 0, 1));
                    return true;
                }
                case 69: {
                    this.processCursorDownLine(this.optionInt(options, 0, 1));
                    return true;
                }
                case 70: {
                    this.processCursorUpLine(this.optionInt(options, 0, 1));
                    return true;
                }
                case 71: {
                    this.processCursorToColumn(this.optionInt(options, 0));
                    return true;
                }
                case 72: 
                case 102: {
                    this.processCursorTo(this.optionInt(options, 0, 1), this.optionInt(options, 1, 1));
                    return true;
                }
                case 74: {
                    this.processEraseScreen(this.optionInt(options, 0, 0));
                    return true;
                }
                case 75: {
                    this.processEraseLine(this.optionInt(options, 0, 0));
                    return true;
                }
                case 83: {
                    this.processScrollUp(this.optionInt(options, 0, 1));
                    return true;
                }
                case 84: {
                    this.processScrollDown(this.optionInt(options, 0, 1));
                    return true;
                }
                case 109: {
                    for (Object next : options) {
                        if (next == null || next.getClass() == Integer.class) continue;
                        throw new IllegalArgumentException();
                    }
                    int count = 0;
                    block25 : for (Object next2 : options) {
                        if (next2 == null) continue;
                        ++count;
                        int value = (Integer)next2;
                        if (30 <= value && value <= 37) {
                            this.processSetForegroundColor(value - 30);
                            continue;
                        }
                        if (40 <= value && value <= 47) {
                            this.processSetBackgroundColor(value - 40);
                            continue;
                        }
                        switch (value) {
                            case 39: {
                                this.processDefaultTextColor();
                                continue block25;
                            }
                            case 49: {
                                this.processDefaultBackgroundColor();
                                continue block25;
                            }
                            case 0: {
                                this.processAttributeRest();
                                continue block25;
                            }
                        }
                        this.processSetAttribute(value);
                    }
                    if (count == 0) {
                        this.processAttributeRest();
                    }
                    return true;
                }
                case 115: {
                    this.processSaveCursorPosition();
                    return true;
                }
                case 117: {
                    this.processRestoreCursorPosition();
                    return true;
                }
            }
            if (97 <= command && 122 <= command) {
                this.processUnknownExtension(options, command);
                return true;
            }
            if (65 <= command && 90 <= command) {
                this.processUnknownExtension(options, command);
                return true;
            }
            return false;
        }
        catch (IllegalArgumentException ignore) {
            return false;
        }
    }

    private boolean processOperatingSystemCommand(ArrayList<Object> options) throws IOException {
        int command = this.optionInt(options, 0);
        String label = (String)options.get(1);
        try {
            switch (command) {
                case 0: {
                    this.processChangeIconNameAndWindowTitle(label);
                    return true;
                }
                case 1: {
                    this.processChangeIconName(label);
                    return true;
                }
                case 2: {
                    this.processChangeWindowTitle(label);
                    return true;
                }
            }
            this.processUnknownOperatingSystemCommand(command, label);
            return true;
        }
        catch (IllegalArgumentException ignore) {
            return false;
        }
    }

    protected void processRestoreCursorPosition() throws IOException {
    }

    protected void processSaveCursorPosition() throws IOException {
    }

    protected void processScrollDown(int optionInt) throws IOException {
    }

    protected void processScrollUp(int optionInt) throws IOException {
    }

    protected void processEraseScreen(int eraseOption) throws IOException {
    }

    protected void processEraseLine(int eraseOption) throws IOException {
    }

    protected void processSetAttribute(int attribute) throws IOException {
    }

    protected void processSetForegroundColor(int color) throws IOException {
    }

    protected void processSetBackgroundColor(int color) throws IOException {
    }

    protected void processDefaultTextColor() throws IOException {
    }

    protected void processDefaultBackgroundColor() throws IOException {
    }

    protected void processAttributeRest() throws IOException {
    }

    protected void processCursorTo(int row, int col) throws IOException {
    }

    protected void processCursorToColumn(int x) throws IOException {
    }

    protected void processCursorUpLine(int count) throws IOException {
    }

    protected void processCursorDownLine(int count) throws IOException {
        for (int i = 0; i < count; ++i) {
            this.out.write(10);
        }
    }

    protected void processCursorLeft(int count) throws IOException {
    }

    protected void processCursorRight(int count) throws IOException {
        for (int i = 0; i < count; ++i) {
            this.out.write(32);
        }
    }

    protected void processCursorDown(int count) throws IOException {
    }

    protected void processCursorUp(int count) throws IOException {
    }

    protected void processUnknownExtension(ArrayList<Object> options, int command) {
    }

    protected void processChangeIconNameAndWindowTitle(String label) {
        this.processChangeIconName(label);
        this.processChangeWindowTitle(label);
    }

    protected void processChangeIconName(String label) {
    }

    protected void processChangeWindowTitle(String label) {
    }

    protected void processUnknownOperatingSystemCommand(int command, String param) {
    }

    private int optionInt(ArrayList<Object> options, int index) {
        if (options.size() <= index) {
            throw new IllegalArgumentException();
        }
        Object value = options.get(index);
        if (value == null) {
            throw new IllegalArgumentException();
        }
        if (!value.getClass().equals(Integer.class)) {
            throw new IllegalArgumentException();
        }
        return (Integer)value;
    }

    private int optionInt(ArrayList<Object> options, int index, int defaultValue) {
        if (options.size() > index) {
            Object value = options.get(index);
            if (value == null) {
                return defaultValue;
            }
            return (Integer)value;
        }
        return defaultValue;
    }

    @Override
    public void close() throws IOException {
        this.write(REST_CODE);
        this.flush();
        super.close();
    }

    private static byte[] resetCode() {
        try {
            return new Ansi().reset().toString().getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}

