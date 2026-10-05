package me.erano.com.forge.eventbus.api.bus;

import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.internal.BusGroupImpl;
import me.erano.com.forge.eventbus.internal.Event;

import java.util.function.Consumer;

// Event TIPI basina bir bus. Gercek oyun event'leri kendi bus'larini statik BUS alaninda tutar.
public interface EventBus<T extends Event> {
    EventListener addListener(Consumer<T> listener);

    EventListener addListener(byte priority, Consumer<T> listener);

    EventListener addListener(EventListener listener);

    void removeListener(EventListener listener);

    // true -> event iptal edildi (sadece CancellableEventBus'ta anlamli).
    boolean post(T event);

    T fire(T event);

    boolean hasListeners();

    static <E extends Event> EventBus<E> create(Class<E> eventType) {
        return create(BusGroup.DEFAULT, eventType);
    }

    static <E extends Event> EventBus<E> create(BusGroup busGroup, Class<E> eventType) {
        return ((BusGroupImpl) busGroup).getOrCreateEventBus(eventType);
    }
}
