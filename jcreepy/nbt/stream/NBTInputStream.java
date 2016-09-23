
package jcreepy.nbt.stream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import jcreepy.nbt.ByteArrayTag;
import jcreepy.nbt.ByteTag;
import jcreepy.nbt.CompoundMap;
import jcreepy.nbt.CompoundTag;
import jcreepy.nbt.DoubleTag;
import jcreepy.nbt.EndTag;
import jcreepy.nbt.FloatTag;
import jcreepy.nbt.IntArrayTag;
import jcreepy.nbt.IntTag;
import jcreepy.nbt.ListTag;
import jcreepy.nbt.LongTag;
import jcreepy.nbt.NBTConstants;
import jcreepy.nbt.ShortArrayTag;
import jcreepy.nbt.ShortTag;
import jcreepy.nbt.StringTag;
import jcreepy.nbt.Tag;
import jcreepy.nbt.TagType;
import jcreepy.nbt.stream.EndianSwitchableInputStream;

public final class NBTInputStream
implements Closeable {
    private final EndianSwitchableInputStream is;

    public NBTInputStream(InputStream is) throws IOException {
        this(is, true, ByteOrder.BIG_ENDIAN);
    }

    public NBTInputStream(InputStream is, boolean compressed) throws IOException {
        this(is, compressed, ByteOrder.BIG_ENDIAN);
    }

    public NBTInputStream(InputStream is, boolean compressed, ByteOrder endianness) throws IOException {
        this.is = new EndianSwitchableInputStream(compressed ? new GZIPInputStream(is) : is, endianness);
    }

    public Tag readTag() throws IOException {
        return this.readTag(0);
    }

    private Tag readTag(int depth) throws IOException {
        String name;
        int typeId = this.is.readUnsignedByte();
        TagType type = TagType.getById(typeId);
        if (type != TagType.TAG_END) {
            int nameLength = this.is.readUnsignedShort();
            byte[] nameBytes = new byte[nameLength];
            this.is.readFully(nameBytes);
            name = new String(nameBytes, NBTConstants.CHARSET.name());
        } else {
            name = "";
        }
        return this.readTagPayload(type, name, depth);
    }

    private Tag readTagPayload(TagType type, String name, int depth) throws IOException {
        switch (type) {
            case TAG_END: {
                if (depth == 0) {
                    throw new IOException("TAG_End found without a TAG_Compound/TAG_List tag preceding it.");
                }
                return new EndTag();
            }
            case TAG_BYTE: {
                return new ByteTag(name, this.is.readByte());
            }
            case TAG_SHORT: {
                return new ShortTag(name, this.is.readShort());
            }
            case TAG_INT: {
                return new IntTag(name, this.is.readInt());
            }
            case TAG_LONG: {
                return new LongTag(name, this.is.readLong());
            }
            case TAG_FLOAT: {
                return new FloatTag(name, this.is.readFloat());
            }
            case TAG_DOUBLE: {
                return new DoubleTag(name, this.is.readDouble());
            }
            case TAG_BYTE_ARRAY: {
                int length = this.is.readInt();
                byte[] bytes = new byte[length];
                this.is.readFully(bytes);
                return new ByteArrayTag(name, bytes);
            }
            case TAG_STRING: {
                short length = this.is.readShort();
                byte[] bytes = new byte[length];
                this.is.readFully(bytes);
                return new StringTag(name, new String(bytes, NBTConstants.CHARSET.name()));
            }
            case TAG_LIST: {
                TagType childType = TagType.getById(this.is.readByte());
                int length = this.is.readInt();
                Class clazz = childType.getTagClass();
                ArrayList<Tag> tagList = new ArrayList<Tag>(length);
                for (int i = 0; i < length; ++i) {
                    Tag tag = this.readTagPayload(childType, "", depth + 1);
                    if (tag instanceof EndTag) {
                        throw new IOException("TAG_End not permitted in a list.");
                    }
                    if (!clazz.isInstance(tag)) {
                        throw new IOException("Mixed tag types within a list.");
                    }
                    tagList.add(tag);
                }
                return new ListTag(name, clazz, tagList);
            }
            case TAG_COMPOUND: {
                Tag tag;
                CompoundMap compoundTagList = new CompoundMap();
                while (!((tag = this.readTag(depth + 1)) instanceof EndTag)) {
                    compoundTagList.put(tag);
                }
                return new CompoundTag(name, compoundTagList);
            }
            case TAG_INT_ARRAY: {
                int length = this.is.readInt();
                int[] ints = new int[length];
                for (int i = 0; i < length; ++i) {
                    ints[i] = this.is.readInt();
                }
                return new IntArrayTag(name, ints);
            }
            case TAG_SHORT_ARRAY: {
                int length = this.is.readInt();
                short[] shorts = new short[length];
                for (int i = 0; i < length; ++i) {
                    shorts[i] = this.is.readShort();
                }
                return new ShortArrayTag(name, shorts);
            }
        }
        throw new IOException("Invalid tag type: " + (Object)((Object)type) + ".");
    }

    @Override
    public void close() throws IOException {
        this.is.close();
    }

    public ByteOrder getByteOrder() {
        return this.is.getEndianness();
    }

}

