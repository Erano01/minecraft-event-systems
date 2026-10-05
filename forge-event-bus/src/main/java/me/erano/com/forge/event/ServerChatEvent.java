package me.erano.com.forge.event;

import me.erano.com.forge.eventbus.api.bus.CancellableEventBus;
import me.erano.com.forge.eventbus.api.event.MutableEvent;
import me.erano.com.forge.eventbus.api.event.characteristic.Cancellable;

// Gercekte player alani ServerPlayer, message ise Component; Minecraft siniflari olmadigi icin String.
public final class ServerChatEvent extends MutableEvent implements Cancellable {
    public static final CancellableEventBus<ServerChatEvent> BUS = CancellableEventBus.create(ServerChatEvent.class);
    private final String username;
    private final String rawText;
    private String message;

    public ServerChatEvent(String username, String rawText, String message) {
        this.username = username;
        this.rawText = rawText;
        this.message = message;
    }

    public String getUsername() {
        return username;
    }

    public String getRawText() {
        return rawText;
    }

    public void setMessage(String message) {
        this.message = java.util.Objects.requireNonNull(message);
    }

    public String getMessage() {
        return message;
    }
}
