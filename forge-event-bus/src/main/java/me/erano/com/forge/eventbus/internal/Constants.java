package me.erano.com.forge.eventbus.internal;

import me.erano.com.forge.eventbus.api.listener.EventListener;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class Constants {
    // Gercekte SELF_DESTRUCTING = 1 ve MONITOR_AWARE = 2 de var; bu modulde yok.
    static final int CHARACTERISTIC_CANCELLABLE = 4;
    static final int CHARACTERISTIC_INHERITABLE = 8;

    static final Consumer<Event> NO_OP_CONSUMER = event -> {
    };
    static final Predicate<Event> NO_OP_PREDICATE = event -> false;

    // VolatileCallSite hedefleri. NULL -> "invoker gecersiz, buildInvoker() ile yeniden kur",
    // NO_OP -> "hic listener yok".
    static final MethodHandle MH_NULL_CONSUMER = MethodHandles.constant(Consumer.class, null);
    static final MethodHandle MH_NO_OP_CONSUMER = MethodHandles.constant(Consumer.class, NO_OP_CONSUMER);
    static final MethodHandle MH_NULL_PREDICATE = MethodHandles.constant(Predicate.class, null);
    static final MethodHandle MH_NO_OP_PREDICATE = MethodHandles.constant(Predicate.class, NO_OP_PREDICATE);

    // Yuksek oncelik once. List.sort stabil oldugu icin ayni oncelikte kayit sirasi korunur.
    static final Comparator<EventListener> PRIORITY_COMPARATOR = (a, b) -> b.priority() - a.priority();

    private Constants() {
    }

    @SuppressWarnings("unchecked")
    static <T extends Event> Consumer<T> getNoOpConsumer() {
        return (Consumer<T>) NO_OP_CONSUMER;
    }

    @SuppressWarnings("unchecked")
    static <T extends Event> Predicate<T> getNoOpPredicate() {
        return (Predicate<T>) NO_OP_PREDICATE;
    }

    static boolean isCancellable(int characteristics) {
        return (characteristics & CHARACTERISTIC_CANCELLABLE) != 0;
    }

    static boolean isInheritable(int characteristics) {
        return (characteristics & CHARACTERISTIC_INHERITABLE) != 0;
    }

    static boolean notInheritable(int characteristics) {
        return (characteristics & CHARACTERISTIC_INHERITABLE) == 0;
    }
}
