
package jcreepy.protocol.packet.player;

import jcreepy.network.Packet;

public final class PlayerLocaleViewDistancePacket
extends Packet {
    public static byte VIEW_FAR = 0;
    public static byte VIEW_NORMAL = 1;
    public static byte VIEW_SHORT = 2;
    public static byte VIEW_TINY = 3;
    private final String locale;
    private final byte viewDistance;
    private final byte chatFlags;
    private final byte difficulty;
    private final boolean showCape;

    public PlayerLocaleViewDistancePacket(String locale, byte viewDistance, byte chatFlags, byte difficulty, boolean showCape) {
        this.locale = locale;
        this.viewDistance = viewDistance;
        this.chatFlags = chatFlags;
        this.difficulty = difficulty;
        this.showCape = showCape;
    }

    public String getLocale() {
        return this.locale;
    }

    public byte getViewDistance() {
        return this.viewDistance;
    }

    public byte getChatFlags() {
        return this.chatFlags;
    }

    public byte getDifficulty() {
        return this.difficulty;
    }

    public boolean showsCape() {
        return this.showCape;
    }
}

