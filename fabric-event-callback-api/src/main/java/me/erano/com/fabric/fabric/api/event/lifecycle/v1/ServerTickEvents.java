package me.erano.com.fabric.fabric.api.event.lifecycle.v1;

import me.erano.com.fabric.fabric.api.event.Event;
import me.erano.com.fabric.fabric.api.event.EventFactory;
import me.erano.com.fabric.minecraft.server.MinecraftServer;

// Gercekte ayrica START_LEVEL_TICK / END_LEVEL_TICK (ServerLevel) var.
public final class ServerTickEvents {
    public static final Event<StartTick> START_SERVER_TICK = EventFactory.createArrayBacked(StartTick.class, callbacks -> server -> {
        for (StartTick event : callbacks) {
            event.onStartTick(server);
        }
    });
    public static final Event<EndTick> END_SERVER_TICK = EventFactory.createArrayBacked(EndTick.class, callbacks -> server -> {
        for (EndTick event : callbacks) {
            event.onEndTick(server);
        }
    });

    private ServerTickEvents() {
    }

    @FunctionalInterface
    public interface StartTick {
        void onStartTick(MinecraftServer server);
    }

    @FunctionalInterface
    public interface EndTick {
        void onEndTick(MinecraftServer server);
    }
}
