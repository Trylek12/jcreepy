
package jcreepy.data;

public enum GameMode {
    SURVIVAL,
    CREATIVE,
    ADVENTURE;
    

    private GameMode() {
    }

    public byte getId() {
        return (byte)this.ordinal();
    }

    public static GameMode get(int id) {
        return GameMode.values()[id];
    }

    public static GameMode get(String name) {
        return GameMode.valueOf(name.toUpperCase());
    }
}

