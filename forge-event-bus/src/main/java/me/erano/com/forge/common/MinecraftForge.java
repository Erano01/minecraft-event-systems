package me.erano.com.forge.common;

import me.erano.com.forge.eventbus.api.bus.BusGroup;

public class MinecraftForge {
    // Game bus: oyun event'leri (oyuncu girisi, chat, tick...). Gercekte BusGroup.DEFAULT'u saran bir
    // EventBusMigrationHelper; register(Object) kolayligi icin JDK'nin IMPL_LOOKUP'ini kullanir.
    public static final BusGroup EVENT_BUS = BusGroup.DEFAULT;

    private MinecraftForge() {
    }
}
