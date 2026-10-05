package me.erano.com.forge.eventbus.api.listener;

// Enum degil byte: ara degerler (orn. 10) de verilebilir.
// MONITOR (-128) ayri bir listede tutulur ve her zaman en son, iptal bilgisiyle calisir.
public final class Priority {
    public static final byte HIGHEST = 127;
    public static final byte HIGH = 64;
    public static final byte NORMAL = 0;
    public static final byte LOW = -64;
    public static final byte LOWEST = -127;
    public static final byte MONITOR = -128;

    private Priority() {
    }
}
