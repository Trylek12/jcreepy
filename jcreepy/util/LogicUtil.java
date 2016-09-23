
package jcreepy.util;

public class LogicUtil {
    public static boolean bothNullOrEqual(Object a, Object b) {
        return a == null || b == null ? a == b : a.equals(b);
    }

    public static boolean getBit(int value, int bit) {
        return (value & bit) == bit;
    }

    public static byte setBit(byte value, int bit, boolean state) {
        return (byte)LogicUtil.setBit((int)value, bit, state);
    }

    public static short setBit(short value, int bit, boolean state) {
        return (short)LogicUtil.setBit((int)value, bit, state);
    }

    public static int setBit(int value, int bit, boolean state) {
        return state ? value | bit : value & ~ bit;
    }
}

