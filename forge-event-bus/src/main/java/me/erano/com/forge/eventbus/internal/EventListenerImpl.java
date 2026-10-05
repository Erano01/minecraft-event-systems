package me.erano.com.forge.eventbus.internal;

import me.erano.com.forge.eventbus.api.event.characteristic.Cancellable;
import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.api.listener.ObjBooleanBiConsumer;
import me.erano.com.forge.eventbus.api.listener.Priority;

import java.util.function.Consumer;
import java.util.function.Predicate;

// Kayitli listener'lar. Hepsi record: final alanlar, immutable -> senkronizasyonsuz paylasilabilir.
public interface EventListenerImpl extends EventListener {

    interface HasConsumer<T extends Event> extends EventListenerImpl {
        Consumer<T> consumer();
    }

    interface HasPredicate<T extends Event> extends EventListenerImpl {
        Predicate<T> predicate();
    }

    record ConsumerListener<T extends Event>(Class<T> eventType, byte priority, Consumer<T> consumer)
            implements HasConsumer<T> {
    }

    record PredicateListener<T extends Event & Cancellable>(Class<T> eventType, byte priority, Predicate<T> predicate)
            implements HasPredicate<T> {
        public PredicateListener {
            assert priority != Priority.MONITOR : "Monitoring listeners cannot cancel events";
        }
    }

    record MonitoringListener<T extends Event>(Class<T> eventType, ObjBooleanBiConsumer<T> booleanBiConsumer)
            implements EventListenerImpl {
        public MonitoringListener(Class<T> eventType, Consumer<T> listener) {
            this(eventType, (event, wasCancelled) -> listener.accept(event));
        }

        @Override
        public byte priority() {
            return Priority.MONITOR;
        }
    }

    // Cancellable bus'a Consumer ile eklenen listener: hem Consumer hem de onu saran Predicate'i tutar.
    record WrappedConsumerListener<T extends Event>(Class<T> eventType, byte priority, boolean alwaysCancelling,
                                                    Consumer<T> consumer, Predicate<T> predicate)
            implements HasConsumer<T>, HasPredicate<T> {

        public WrappedConsumerListener(Class<T> eventType, byte priority, Consumer<T> consumer) {
            this(eventType, priority, false, consumer, wrap(false, consumer));
        }

        public WrappedConsumerListener(Class<T> eventType, byte priority, boolean alwaysCancelling, Consumer<T> consumer) {
            this(eventType, priority, alwaysCancelling, consumer, wrap(alwaysCancelling, consumer));
        }

        public static <T extends Event> Predicate<T> wrap(boolean alwaysCancelling, Consumer<T> consumer) {
            if (alwaysCancelling) {
                return event -> {
                    consumer.accept(event);
                    return true;
                };
            }
            return event -> {
                consumer.accept(event);
                return false;
            };
        }

        // Uretilen predicate lambda'si esitlige katilmaz.
        @Override
        public boolean equals(Object obj) {
            return obj instanceof WrappedConsumerListener<?> that
                    && eventType == that.eventType
                    && priority == that.priority
                    && alwaysCancelling == that.alwaysCancelling
                    && consumer.equals(that.consumer);
        }

        @Override
        public int hashCode() {
            return eventType.hashCode() * 31 + priority * 31 + Boolean.hashCode(alwaysCancelling) * 31 + consumer.hashCode();
        }
    }
}
