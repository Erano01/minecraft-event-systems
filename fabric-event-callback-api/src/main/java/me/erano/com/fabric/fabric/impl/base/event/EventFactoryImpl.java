package me.erano.com.fabric.fabric.impl.base.event;

import com.google.common.collect.MapMaker;
import me.erano.com.fabric.fabric.api.event.Event;
import me.erano.com.fabric.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

// Gercekte ayrica buildEmptyInvoker (Proxy ile bos invoker uretimi) var; hicbir yerden cagrilmiyor.
public final class EventFactoryImpl {
    // Olusturulan tum event'ler. Weak key: event nesnesine baska referans kalmazsa GC toplayabilir.
    // MapMaker concurrent bir harita uretir; event'ler farkli thread'lerde class init sirasinda yaratilabilir.
    private static final Set<ArrayBackedEvent<?>> ARRAY_BACKED_EVENTS = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());

    private EventFactoryImpl() {
    }

    // Tum invoker'lari yeniden kurar. update() event'in kilidini ALMAZ (bkz. concurrency-model.md).
    public static void invalidate() {
        ARRAY_BACKED_EVENTS.forEach(ArrayBackedEvent::update);
    }

    public static <T> Event<T> createArrayBacked(Class<? super T> type, Function<T[], T> invokerFactory) {
        ArrayBackedEvent<T> event = new ArrayBackedEvent<>(type, invokerFactory);
        ARRAY_BACKED_EVENTS.add(event);
        return event;
    }

    public static void ensureContainsDefault(Identifier[] defaultPhases) {
        for (Identifier id : defaultPhases) {
            if (id.equals(Event.DEFAULT_PHASE)) {
                return;
            }
        }
        throw new IllegalArgumentException("The event phases must contain Event.DEFAULT_PHASE.");
    }

    public static void ensureNoDuplicates(Identifier[] defaultPhases) {
        for (int i = 0; i < defaultPhases.length; i++) {
            for (int j = i + 1; j < defaultPhases.length; j++) {
                if (defaultPhases[i].equals(defaultPhases[j])) {
                    throw new IllegalArgumentException("Duplicate event phase: " + defaultPhases[i]);
                }
            }
        }
    }
}
