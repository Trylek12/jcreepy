
package jcreepy.data;

public enum Animation {
    NONE(0),
    SWING_ARM(1),
    DAMAGE_ANIMATION(2),
    LEAVE_BED(3),
    EAT_FOOD(4),
    UNKNOWN_ANIMATION(102),
    CROUCH(104),
    UNCROUCH(105);
    
    private final int id;

    private Animation(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public static Animation get(int id) {
        return Animation.values()[id];
    }
}

