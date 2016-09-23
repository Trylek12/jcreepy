
package jcreepy.protocol.util;

import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import jcreepy.inventory.ItemStack;
import jcreepy.nbt.CompoundMap;
import jcreepy.nbt.CompoundTag;
import jcreepy.nbt.Tag;
import jcreepy.nbt.stream.NBTInputStream;
import jcreepy.nbt.stream.NBTOutputStream;
import jcreepy.protocol.util.Parameter;

public final class ByteBufUtils {
    public static void writeParameters(ByteBuf buf, List<Parameter<?>> parameters) {
        for (Parameter parameter : parameters) {
            int type = parameter.getType();
            int index = parameter.getIndex();
            buf.writeByte(type << 5 | index);
            switch (type) {
                case 0: {
                    buf.writeByte(((Byte)parameter.getValue()).byteValue());
                    break;
                }
                case 1: {
                    buf.writeShort(((Short)parameter.getValue()).shortValue());
                    break;
                }
                case 2: {
                    buf.writeInt((Integer)parameter.getValue());
                    break;
                }
                case 3: {
                    buf.writeFloat(((Float)parameter.getValue()).floatValue());
                    break;
                }
                case 4: {
                    ByteBufUtils.writeString(buf, (String)parameter.getValue());
                    break;
                }
                case 5: {
                    ItemStack item = (ItemStack)parameter.getValue();
                    buf.writeShort(item.getId());
                    buf.writeByte(item.getAmount());
                    buf.writeShort(item.getData());
                }
            }
        }
        buf.writeByte(127);
    }

    public static List<Parameter<?>> readParameters(ByteBuf buf) {
        ArrayList parameters = new ArrayList();
        short b = buf.readUnsignedByte();
        while (b != 127) {
            int type = b >> 5;
            int index = b & 31;
            switch (type) {
                case 0: {
                    parameters.add(new Parameter<Byte>(type, index, Byte.valueOf(buf.readByte())));
                    break;
                }
                case 1: {
                    parameters.add(new Parameter<Short>(type, index, buf.readShort()));
                    break;
                }
                case 2: {
                    parameters.add(new Parameter<Integer>(type, index, buf.readInt()));
                    break;
                }
                case 3: {
                    parameters.add(new Parameter<Float>(type, index, Float.valueOf(buf.readFloat())));
                    break;
                }
                case 4: {
                    parameters.add(new Parameter<String>(type, index, ByteBufUtils.readString(buf)));
                    break;
                }
                case 5: {
                    short id = buf.readShort();
                    byte amount = buf.readByte();
                    short data = buf.readShort();
                    ItemStack item = new ItemStack(id, amount, data);
                    parameters.add(new Parameter<ItemStack>(type, index, item));
                }
            }
            b = buf.readUnsignedByte();
        }
        return parameters;
    }

    public static void writeString(ByteBuf buf, String str) {
        int len = str.length();
        if (len > 65535) {
            throw new IllegalArgumentException("String too long.");
        }
        buf.writeShort(len);
        for (int i = 0; i < len; ++i) {
            buf.writeChar(str.charAt(i));
        }
    }

    public static String readString(ByteBuf buf) {
        int len = buf.readUnsignedShort();
        char[] characters = new char[len];
        for (int i = 0; i < len; ++i) {
            characters[i] = buf.readChar();
        }
        return new String(characters);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Loose catch block
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    public static CompoundMap readCompound(ByteBuf buf) {
        NBTInputStream str;
        block12 : {
            short len = buf.readShort();
            if (len < 0) return null;
            byte[] bytes = new byte[len];
            buf.readBytes(bytes);
            str = null;
            str = new NBTInputStream(new ByteArrayInputStream(bytes));
            Tag tag = str.readTag();
            if (!(tag instanceof CompoundTag)) break block12;
            CompoundMap compoundMap = ((CompoundTag)tag).getValue();
            if (str == null) return compoundMap;
            try {
                str.close();
                return compoundMap;
            }
            catch (IOException e) {
                // empty catch block
            }
            return compoundMap;
        }
        if (str == null) return null;
        try {
            str.close();
            return null;
        }
        catch (IOException e) {
            return null;
        }
        catch (IOException e) {
            if (str == null) return null;
            try {
                str.close();
                return null;
            }
            catch (IOException e) {
                return null;
            }
            catch (Throwable throwable) {
                if (str == null) throw throwable;
                try {
                    str.close();
                    throw throwable;
                }
                catch (IOException e) {
                    // empty catch block
                }
                throw throwable;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Loose catch block
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     * Lifted jumps to return sites
     */
    public static void writeCompound(ByteBuf buf, CompoundMap data) {
        if (data == null) {
            buf.writeShort(-1);
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NBTOutputStream str = null;
        str = new NBTOutputStream(out);
        str.writeTag(new CompoundTag("", data));
        str.close();
        str = null;
        buf.writeShort(out.size());
        buf.writeBytes(out.toByteArray());
        if (str == null) return;
        try {
            str.close();
            return;
        }
        catch (IOException e) {
            return;
        }
        catch (IOException e) {
            if (str == null) return;
            try {
                str.close();
                return;
            }
            catch (IOException e) {
                return;
            }
            catch (Throwable throwable) {
                if (str == null) throw throwable;
                try {
                    str.close();
                    throw throwable;
                }
                catch (IOException e) {
                    // empty catch block
                }
                throw throwable;
            }
        }
    }

    public static ItemStack readItemStack(ByteBuf buffer) {
        short id = buffer.readShort();
        if (id != -1) {
            byte amount = buffer.readByte();
            short data = buffer.readShort();
            CompoundMap nbtData = ByteBufUtils.readCompound(buffer);
            return new ItemStack(id, amount, data).setNBTData(nbtData);
        }
        return null;
    }

    public static void writeItemStack(ByteBuf buffer, ItemStack item) {
        int id = item == null ? -1 : (int)item.getId();
        buffer.writeShort(id);
        if (id != -1) {
            buffer.writeByte(item.getAmount());
            buffer.writeShort(item.getData());
            ByteBufUtils.writeCompound(buffer, item.getNBTData());
        }
    }

    private ByteBufUtils() {
    }
}

