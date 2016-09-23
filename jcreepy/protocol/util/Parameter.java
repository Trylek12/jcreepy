
package jcreepy.protocol.util;

public class Parameter<T> {
    public static final int TYPE_BYTE = 0;
    public static final int TYPE_SHORT = 1;
    public static final int TYPE_INT = 2;
    public static final int TYPE_FLOAT = 3;
    public static final int TYPE_STRING = 4;
    public static final int TYPE_ITEM = 5;
    private final int type;
    private final int index;
    private final T value;

    public Parameter(int type, int index, T value) {
        this.type = type;
        this.index = index;
        this.value = value;
    }

    public int getType() {
        return this.type;
    }

    public int getIndex() {
        return this.index;
    }

    public T getValue() {
        return this.value;
    }

    public boolean equals(Object other) {
        if (!(other instanceof Parameter)) {
            return false;
        }
        Parameter param = (Parameter)other;
        return this.type == param.type && this.index == param.index && this.value.equals(param.value);
    }
}

