
package jcreepy.protocol.codec.player;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import jcreepy.network.Packet;
import jcreepy.network.codec.PacketCodec;
import jcreepy.protocol.packet.player.PlayerAbilityPacket;
import jcreepy.util.LogicUtil;

public class PlayerAbilityCodec
extends PacketCodec<PlayerAbilityPacket> {
    public PlayerAbilityCodec() {
        super(PlayerAbilityPacket.class, 202);
    }

    @Override
    public PlayerAbilityPacket decode(ByteBuf buffer) throws IOException {
        byte flag = buffer.readByte();
        boolean godMode = LogicUtil.getBit(flag, 1);
        boolean isFlying = LogicUtil.getBit(flag, 2);
        boolean canFly = LogicUtil.getBit(flag, 4);
        boolean creativeMode = LogicUtil.getBit(flag, 8);
        byte flyingSpeed = buffer.readByte();
        byte walkingSpeed = buffer.readByte();
        return new PlayerAbilityPacket(godMode, isFlying, canFly, creativeMode, flyingSpeed, walkingSpeed);
    }

    @Override
    public ByteBuf encode(PlayerAbilityPacket packet) throws IOException {
        ByteBuf buffer = Unpooled.buffer();
        byte flag = 0;
        flag = LogicUtil.setBit(flag, 1, packet.isGodMode());
        flag = LogicUtil.setBit(flag, 2, packet.isFlying());
        flag = LogicUtil.setBit(flag, 4, packet.canFly());
        flag = LogicUtil.setBit(flag, 8, packet.isCreativeMode());
        buffer.writeByte(flag);
        buffer.writeByte(packet.getFlyingSpeed());
        buffer.writeByte(packet.getWalkingSpeed());
        return buffer;
    }
}

