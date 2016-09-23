
package jcreepy.nbt;

import java.util.Arrays;
import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;

public final class ByteArrayTag
extends Tag<byte[]> {
    private final byte[] value;

    public ByteArrayTag(String name, byte[] value) {
        super(TagType.TAG_BYTE_ARRAY, name);
        this.value = value;
    }

    @Override
    public byte[] getValue() {
        return this.value;
    }

    @Override
    public ByteArrayTag clone() {
        byte[] clonedArray = this.cloneArray(this.value);
        return new ByteArrayTag(this.getName(), clonedArray);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ByteArrayTag)) {
            return false;
        }
        ByteArrayTag tag = (ByteArrayTag)other;
        return Arrays.equals(this.value, tag.value) && this.getName().equals(tag.getName());
    }

    private byte[] cloneArray(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        int length = byteArray.length;
        byte[] newArray = new byte[length];
        System.arraycopy(byteArray, 0, newArray, 0, length);
        return newArray;
    }
}

