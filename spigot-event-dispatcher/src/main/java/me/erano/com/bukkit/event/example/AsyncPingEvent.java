package me.erano.com.bukkit.event.example;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/*
 * Kasitli olarak sade tutuldu (Cancellable degil, ekstra alan yok): tek amaci
 * HandlerList'in concurrency mekanizmasini (volatile 'handlers' dizisi + synchronized
 * register/unregister/bake) yuksek contention altinda gozlemlemek. Bkz. Application.main
 * -> concurrencyDemo().
 */
public class AsyncPingEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public AsyncPingEvent() {
        super(true);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
