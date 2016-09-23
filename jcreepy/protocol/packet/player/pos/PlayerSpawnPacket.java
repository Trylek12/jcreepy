
package jcreepy.protocol.packet.player.pos;

import java.util.List;
import jcreepy.math.Vector3;
import jcreepy.protocol.packet.entity.EntityPacket;
import jcreepy.protocol.util.Parameter;

public final class PlayerSpawnPacket
extends EntityPacket {
    private final int x;
    private final int y;
    private final int z;
    private final int yaw;
    private final int pitch;
    private final int item;
    private final String name;
    private final List<Parameter<?>> parameters;

    public PlayerSpawnPacket(int id, String name, Vector3 position, int yaw, int pitch, int item, List<Parameter<?>> parameters) {
        this(id, name, (int)position.getX(), (int)position.getY(), (int)position.getZ(), yaw, pitch, item, parameters);
    }

    public PlayerSpawnPacket(int id, String name, int x, int y, int z, int yaw, int pitch, int item, List<Parameter<?>> parameters) {
        super(id);
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.item = item;
        this.parameters = parameters;
    }

    public String getPlayerName() {
        return this.name;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getYaw() {
        return this.yaw;
    }

    public int getPitch() {
        return this.pitch;
    }

    public int getId() {
        return this.item;
    }

    public List<Parameter<?>> getParameters() {
        return this.parameters;
    }
}

