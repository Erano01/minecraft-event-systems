package me.erano.com.forge.eventbus.api.bus;

import me.erano.com.forge.eventbus.api.event.characteristic.Cancellable;
import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.api.listener.ObjBooleanBiConsumer;
import me.erano.com.forge.eventbus.internal.BusGroupImpl;
import me.erano.com.forge.eventbus.internal.Event;

import java.util.function.Consumer;
import java.util.function.Predicate;

public interface CancellableEventBus<T extends Event & Cancellable> extends EventBus<T> {
    EventListener addListener(byte priority, boolean alwaysCancelling, Consumer<T> listener);

    // true donerse event iptal edilir ve zincirdeki sonraki (MONITOR olmayan) listener'lar calismaz.
    EventListener addListener(Predicate<T> listener);

    EventListener addListener(byte priority, Predicate<T> listener);

    // MONITOR: iptal edilse bile en son calisir, wasCancelled bilgisini alir, iptal edemez.
    EventListener addListener(ObjBooleanBiConsumer<T> monitoringListener);

    default EventListener addListener(boolean alwaysCancelling, Consumer<T> listener) {
        return addListener((byte) 0, alwaysCancelling, listener);
    }

    static <T extends Event & Cancellable> CancellableEventBus<T> create(Class<T> eventType) {
        return create(BusGroup.DEFAULT, eventType);
    }

    static <T extends Event & Cancellable> CancellableEventBus<T> create(BusGroup busGroup, Class<T> eventType) {
        return (CancellableEventBus<T>) ((BusGroupImpl) busGroup).getOrCreateEventBus(eventType);
    }
}
