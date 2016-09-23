
package jcreepy.protocol.packet.window;

import jcreepy.network.Packet;

public final class WindowEnchantItemPacket
extends Packet {
    private final int transaction;
    private final int enchantment;

    public WindowEnchantItemPacket(int transaction, int enchantment) {
        this.transaction = transaction;
        this.enchantment = enchantment;
    }

    public int getTransaction() {
        return this.transaction;
    }

    public int getEnchantment() {
        return this.enchantment;
    }
}

