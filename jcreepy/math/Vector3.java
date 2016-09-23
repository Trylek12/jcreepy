
package jcreepy.math;

public class Vector3 {
    public static final Vector3 ZERO = new Vector3(0, 0, 0);
    public static final Vector3 UNIT_X;
    public static final Vector3 RIGHT;
    public static final Vector3 UNIT_Y;
    public static final Vector3 UP;
    public static final Vector3 UNIT_Z;
    public static final Vector3 FORWARD;
    public static final Vector3 ONE;
    private final float x;
    private final float y;
    private final float z;

    public Vector3(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3(double x, double y, double z) {
        this((float)x, (float)y, (float)z);
    }

    public Vector3(int x, int y, int z) {
        this((float)x, (float)y, (float)z);
    }

    public Vector3(Vector3 o) {
        this(o.x, o.y, o.z);
    }

    public Vector3() {
        this(0, 0, 0);
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

    static {
        RIGHT = Vector3.UNIT_X = new Vector3(1, 0, 0);
        UP = Vector3.UNIT_Y = new Vector3(0, 1, 0);
        FORWARD = Vector3.UNIT_Z = new Vector3(0, 0, 1);
        ONE = new Vector3(1, 1, 1);
    }
}

