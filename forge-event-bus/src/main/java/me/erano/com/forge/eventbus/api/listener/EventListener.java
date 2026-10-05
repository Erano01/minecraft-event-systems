package me.erano.com.forge.eventbus.api.listener;

import me.erano.com.forge.eventbus.internal.Event;

// addListener/register'in dondurdugu kayit tutamaci; removeListener'a geri verilir.
public interface EventListener {
    Class<? extends Event> eventType();

    byte priority();

    default boolean alwaysCancelling() {
        return false;
    }
}
