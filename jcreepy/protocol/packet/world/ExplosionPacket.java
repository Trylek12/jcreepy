
package jcreepy.protocol.packet.world;

import jcreepy.math.Vector3;
import jcreepy.network.Packet;

public final class ExplosionPacket
extends Packet {
    private final double x;
    private final double y;
    private final double z;
    private final float radius;
    private final byte[] coordinates;

    public ExplosionPacket(Vector3 position, float radius, byte[] coordinates) {
        this(position.getX(), position.getY(), position.getZ(), radius, coordinates);
    }

    public ExplosionPacket(double x, double y, double z, float radius, byte[] coordinates) {
        if (coordinates.length % 3 != 0) {
            throw new IllegalArgumentException();
        }
        this.x = x;
        this.y = y;
        this.z = z;
        this.radius = radius;
        this.coordinates = coordinates;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getRadius() {
        return this.radius;
    }

    public int getRecords() {
        return this.coordinates.length / 3;
    }

    public byte[] getCoordinates() {
        return this.coordinates;
    }
}

