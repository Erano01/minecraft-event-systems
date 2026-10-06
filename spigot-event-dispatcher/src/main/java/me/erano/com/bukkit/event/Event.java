package me.erano.com.bukkit.event;

import org.jetbrains.annotations.NotNull;

//subject, observable, publisher, event
public abstract class Event {
    private String name;
    private final boolean async;

    public enum Result {
        DENY,
        DEFAULT,
        ALLOW
    }

    @NotNull
    public abstract HandlerList getHandlers();

    public Event() {
        this(false);
    }

    public Event(boolean isAsync) {
        this.async = isAsync;
    }

    @NotNull
    public String getEventName() {
        if (this.name == null) {
            this.name = getClass().getSimpleName();
        }
        return this.name;
    }

    public final boolean isAsynchronous() {
        return this.async;
    }
}
