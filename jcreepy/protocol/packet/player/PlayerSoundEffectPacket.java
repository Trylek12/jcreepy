
package jcreepy.protocol.packet.player;

import jcreepy.math.Vector3;
import jcreepy.network.Packet;

public final class PlayerSoundEffectPacket
extends Packet {
    private final float x;
    private final float y;
    private final float z;
    private final float volume;
    private final float pitch;
    private final String soundName;

    public PlayerSoundEffectPacket(String soundName, Vector3 position, float volume, float pitch) {
        this(soundName, position.getX(), position.getY(), position.getZ(), volume, pitch);
    }

    public PlayerSoundEffectPacket(String soundName, float x, float y, float z, float volume, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.soundName = soundName;
        this.volume = volume;
        this.pitch = pitch;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public float getZ() {
        return this.z;
    }

    public String getSoundName() {
        return this.soundName;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }
}

