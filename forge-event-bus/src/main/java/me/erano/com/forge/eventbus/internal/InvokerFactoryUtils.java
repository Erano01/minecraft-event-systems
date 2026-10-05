package me.erano.com.forge.eventbus.internal;

import me.erano.com.forge.eventbus.api.event.characteristic.Cancellable;
import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.api.listener.ObjBooleanBiConsumer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

// EventListener kayitlarini cagrilabilir fonksiyonlara (Consumer/Predicate) acar.
final class InvokerFactoryUtils {
    private InvokerFactoryUtils() {
    }

    static <T extends Event> List<Consumer<T>> unwrapConsumers(List<EventListener> listeners) {
        return listeners.stream().map(listener -> {
            if (listener instanceof EventListenerImpl.HasConsumer<?> consumerListener) {
                return InvokerFactoryUtils.<Consumer<T>>uncheckedCast(consumerListener.consumer());
            }
            throw new IllegalStateException("Unexpected listener type: " + listener.getClass());
        }).toList();
    }

    // alwaysCancelling bir listener'a gelince durur: ondan sonrakiler zaten hic calismayacak.
    static <T extends Event & Cancellable> List<Predicate<T>> unwrapPredicates(List<EventListener> listeners) {
        ArrayList<Predicate<T>> unwrappedPredicates = new ArrayList<>(listeners.size());
        for (EventListener listener : listeners) {
            if (listener instanceof EventListenerImpl.HasPredicate<?> predicateListener) {
                unwrappedPredicates.add(uncheckedCast(predicateListener.predicate()));
            } else if (listener instanceof EventListenerImpl.ConsumerListener<?> consumerListener) {
                unwrappedPredicates.add(uncheckedCast(EventListenerImpl.WrappedConsumerListener.wrap(false, consumerListener.consumer())));
            } else {
                throw new IllegalStateException("Unexpected listener type: " + listener.getClass());
            }
            if (listener instanceof EventListenerImpl.WrappedConsumerListener<?> wrapped && wrapped.alwaysCancelling()) {
                unwrappedPredicates.trimToSize();
                break;
            }
        }
        return unwrappedPredicates;
    }

    static <T extends Event> List<ObjBooleanBiConsumer<T>> unwrapMonitors(List<EventListener> monitoringListeners) {
        return monitoringListeners.stream()
                .map(EventListenerImpl.MonitoringListener.class::cast)
                .map(listener -> InvokerFactoryUtils.<ObjBooleanBiConsumer<T>>uncheckedCast(listener.booleanBiConsumer()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> T uncheckedCast(Object obj) {
        return (T) obj;
    }
}
