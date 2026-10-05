package me.erano.com.forge.eventbus.internal;

import me.erano.com.forge.eventbus.api.event.characteristic.Cancellable;
import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.api.listener.ObjBooleanBiConsumer;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Listener listesini TEK bir fonksiyona derler. Dispatch aninda dongu/instanceof/reflection yok:
// kucuk listeler icin acik yazilmis lambda'lar uretilir, JIT tum zinciri inline edebilir.
// Uretilen invoker immutable'dir: yakaladigi diziler bir daha degismez.
//
// Gercekte ayrica: MonitorAware event'ler ve <=4 always-cancelling listener icin ek ozel durumlar var.
final class InvokerFactory {
    private InvokerFactory() {
    }

    static <T extends Event> Consumer<T> createMonitoringInvoker(List<EventListener> listeners, List<EventListener> monitoringListeners) {
        Consumer<T> invoker = createInvokerFromUnwrapped(InvokerFactoryUtils.unwrapConsumers(listeners));
        if (monitoringListeners.isEmpty()) {
            return invoker;
        }
        List<ObjBooleanBiConsumer<T>> unwrappedMonitors = InvokerFactoryUtils.unwrapMonitors(monitoringListeners);
        if (unwrappedMonitors.size() == 1) {
            ObjBooleanBiConsumer<T> firstMonitor = unwrappedMonitors.getFirst();
            return event -> {
                invoker.accept(event);
                firstMonitor.accept(event, false);
            };
        }
        @SuppressWarnings("unchecked")
        ObjBooleanBiConsumer<T>[] monitors = unwrappedMonitors.toArray(new ObjBooleanBiConsumer[0]);
        return event -> {
            invoker.accept(event);
            for (ObjBooleanBiConsumer<T> monitor : monitors) {
                monitor.accept(event, false);
            }
        };
    }

    static <T extends Event & Cancellable> Predicate<T> createCancellableMonitoringInvoker(List<EventListener> listeners, List<EventListener> monitoringListeners) {
        Predicate<T> cancellableInvoker = createCancellableInvokerFromUnwrapped(InvokerFactoryUtils.unwrapPredicates(listeners));
        if (monitoringListeners.isEmpty()) {
            return cancellableInvoker;
        }
        List<ObjBooleanBiConsumer<T>> unwrappedMonitors = InvokerFactoryUtils.unwrapMonitors(monitoringListeners);
        if (unwrappedMonitors.size() == 1) {
            ObjBooleanBiConsumer<T> firstMonitor = unwrappedMonitors.getFirst();
            return event -> {
                boolean cancelled = cancellableInvoker.test(event);
                firstMonitor.accept(event, cancelled);
                return cancelled;
            };
        }
        @SuppressWarnings("unchecked")
        ObjBooleanBiConsumer<T>[] monitors = unwrappedMonitors.toArray(new ObjBooleanBiConsumer[0]);
        return event -> {
            boolean cancelled = cancellableInvoker.test(event);
            for (ObjBooleanBiConsumer<T> monitor : monitors) {
                monitor.accept(event, cancelled);
            }
            return cancelled;
        };
    }

    private static <T extends Event> Consumer<T> createInvokerFromUnwrapped(List<Consumer<T>> listeners) {
        switch (listeners.size()) {
            case 0:
                return Constants.getNoOpConsumer();
            case 1:
                return listeners.getFirst();
            case 2:
                return listeners.getFirst().andThen(listeners.getLast());
            case 3: {
                Consumer<T> first = listeners.getFirst();
                Consumer<T> second = listeners.get(1);
                Consumer<T> third = listeners.getLast();
                return event -> {
                    first.accept(event);
                    second.accept(event);
                    third.accept(event);
                };
            }
            case 4: {
                Consumer<T> first = listeners.getFirst();
                Consumer<T> second = listeners.get(1);
                Consumer<T> third = listeners.get(2);
                Consumer<T> fourth = listeners.getLast();
                return event -> {
                    first.accept(event);
                    second.accept(event);
                    third.accept(event);
                    fourth.accept(event);
                };
            }
            default: {
                @SuppressWarnings("unchecked")
                Consumer<T>[] listenersArray = listeners.toArray(new Consumer[0]);
                return event -> {
                    for (Consumer<T> consumer : listenersArray) {
                        consumer.accept(event);
                    }
                };
            }
        }
    }

    // Kisa devre (||): bir listener true dondururse sonrakiler calismaz.
    private static <T extends Event & Cancellable> Predicate<T> createCancellableInvokerFromUnwrapped(List<Predicate<T>> listeners) {
        switch (listeners.size()) {
            case 0:
                return Constants.getNoOpPredicate();
            case 1:
                return listeners.getFirst();
            case 2:
                return listeners.getFirst().or(listeners.getLast());
            case 3: {
                Predicate<T> first = listeners.getFirst();
                Predicate<T> second = listeners.get(1);
                Predicate<T> third = listeners.getLast();
                return event -> first.test(event) || second.test(event) || third.test(event);
            }
            default: {
                @SuppressWarnings("unchecked")
                Predicate<T>[] listenersArray = listeners.toArray(new Predicate[0]);
                return event -> {
                    for (Predicate<T> predicate : listenersArray) {
                        if (predicate.test(event)) {
                            return true;
                        }
                    }
                    return false;
                };
            }
        }
    }
}
