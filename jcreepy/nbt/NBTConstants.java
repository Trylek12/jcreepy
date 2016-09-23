
package jcreepy.nbt;

import java.nio.charset.Charset;
import jcreepy.nbt.TagType;

public final class NBTConstants {
    public static final Charset CHARSET = Charset.forName("UTF-8");
    @Deprecated
    public static final int TYPE_END = TagType.TAG_END.getId();
    @Deprecated
    public static final int TYPE_BYTE = TagType.TAG_BYTE.getId();
    @Deprecated
    public static final int TYPE_SHORT = TagType.TAG_SHORT.getId();
    @Deprecated
    public static final int TYPE_INT = TagType.TAG_INT.getId();
    @Deprecated
    public static final int TYPE_LONG = TagType.TAG_LONG.getId();
    @Deprecated
    public static final int TYPE_FLOAT = TagType.TAG_FLOAT.getId();
    @Deprecated
    public static final int TYPE_DOUBLE = TagType.TAG_DOUBLE.getId();
    @Deprecated
    public static final int TYPE_BYTE_ARRAY = TagType.TAG_BYTE_ARRAY.getId();
    @Deprecated
    public static final int TYPE_STRING = TagType.TAG_STRING.getId();
    @Deprecated
    public static final int TYPE_LIST = TagType.TAG_LIST.getId();
    @Deprecated
    public static final int TYPE_COMPOUND = TagType.TAG_COMPOUND.getId();
    @Deprecated
    public static final int TYPE_INT_ARRAY = TagType.TAG_INT_ARRAY.getId();
    @Deprecated
    public static final int TYPE_SHORT_ARRAY = TagType.TAG_SHORT_ARRAY.getId();

    private NBTConstants() {
    }
}

