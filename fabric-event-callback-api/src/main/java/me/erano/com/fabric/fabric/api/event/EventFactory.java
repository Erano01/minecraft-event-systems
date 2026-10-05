package me.erano.com.fabric.fabric.api.event;

import me.erano.com.fabric.fabric.impl.base.event.EventFactoryImpl;
import me.erano.com.fabric.minecraft.resources.Identifier;

import java.util.function.Function;

public final class EventFactory {
    private EventFactory() {
    }

    // invokerFactory: listener dizisinden TEK bir T uretir. Event'in semantigi (sirayla cagir, ilk
    // false'ta dur, sonucu birlestir...) tamamen bu fonksiyonda; cekirdekte oncelik/iptal kavrami yok.
    public static <T> Event<T> createArrayBacked(Class<? super T> type, Function<T[], T> invokerFactory) {
        return EventFactoryImpl.createArrayBacked(type, invokerFactory);
    }

    public static <T> Event<T> createArrayBacked(Class<T> type, T emptyInvoker, Function<T[], T> invokerFactory) {
        return createArrayBacked(type, listeners -> {
            if (listeners.length == 0) {
                return emptyInvoker;
            }
            if (listeners.length == 1) {
                return listeners[0];
            }
            return invokerFactory.apply(listeners);
        });
    }

    // Varsayilan fazlar verilen sirayla birbirine baglanir (a -> b -> c).
    public static <T> Event<T> createWithPhases(Class<? super T> type, Function<T[], T> invokerFactory, Identifier... defaultPhases) {
        EventFactoryImpl.ensureContainsDefault(defaultPhases);
        EventFactoryImpl.ensureNoDuplicates(defaultPhases);
        Event<T> event = createArrayBacked(type, invokerFactory);
        for (int i = 1; i < defaultPhases.length; i++) {
            event.addPhaseOrdering(defaultPhases[i - 1], defaultPhases[i]);
        }
        return event;
    }

    // Gercekte @Deprecated(forRemoval = true).
    @Deprecated
    public static void invalidate() {
        EventFactoryImpl.invalidate();
    }
}
