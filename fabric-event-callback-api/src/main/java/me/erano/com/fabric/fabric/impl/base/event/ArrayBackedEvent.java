package me.erano.com.fabric.fabric.impl.base.event;

import me.erano.com.fabric.fabric.api.event.Event;
import me.erano.com.fabric.fabric.impl.base.toposort.NodeSorting;
import me.erano.com.fabric.minecraft.resources.Identifier;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

// Senkronizasyon politikasi:
//  - handlers, phases, sortedPhases (ve fazlarin listener dizileri): 'lock' ile korunur, sadece yazma yolu.
//  - invoker (Event'te volatile): her degisiklikten sonra kilit altinda yeniden kurulup yayinlanir;
//    okuma yolu kilitsizdir. Uretilen invoker'in yakaladigi dizi bir daha degistirilmez.
// Unregister YOK: listener eklenir ama cikarilamaz.
class ArrayBackedEvent<T> extends Event<T> {
    private final Function<T[], T> invokerFactory;
    private final Object lock = new Object();
    private T[] handlers;
    private final Map<Identifier, EventPhaseData<T>> phases = new LinkedHashMap<>();
    private final List<EventPhaseData<T>> sortedPhases = new ArrayList<>();

    @SuppressWarnings("unchecked")
    ArrayBackedEvent(Class<? super T> type, Function<T[], T> invokerFactory) {
        this.invokerFactory = invokerFactory;
        this.handlers = (T[]) Array.newInstance(type, 0);
        update();
    }

    void update() {
        this.invoker = invokerFactory.apply(handlers);
    }

    @Override
    public void register(T listener) {
        register(DEFAULT_PHASE, listener);
    }

    @Override
    public void register(Identifier phaseIdentifier, T listener) {
        Objects.requireNonNull(phaseIdentifier, "Tried to register a listener for a null phase!");
        Objects.requireNonNull(listener, "Tried to register a null listener!");
        synchronized (lock) {
            getOrCreatePhase(phaseIdentifier, true).addListener(listener);
            rebuildInvoker(handlers.length + 1);
        }
    }

    private EventPhaseData<T> getOrCreatePhase(Identifier id, boolean sortIfCreate) {
        EventPhaseData<T> phase = phases.get(id);
        if (phase == null) {
            phase = new EventPhaseData<>(id, handlers.getClass().getComponentType());
            phases.put(id, phase);
            sortedPhases.add(phase);
            if (sortIfCreate) {
                NodeSorting.sort(sortedPhases, "event phases", Comparator.comparing(data -> data.id));
            }
        }
        return phase;
    }

    // Tum fazlarin listener'lari, faz sirasiyla tek bir YENI diziye kopyalanir; sonra invoker yeniden kurulur.
    @SuppressWarnings("unchecked")
    private void rebuildInvoker(int newLength) {
        if (sortedPhases.size() == 1) {
            handlers = sortedPhases.getFirst().listeners;
        } else {
            T[] newHandlers = (T[]) Array.newInstance(handlers.getClass().getComponentType(), newLength);
            int newHandlersIndex = 0;
            for (EventPhaseData<T> existingPhase : sortedPhases) {
                int length = existingPhase.listeners.length;
                System.arraycopy(existingPhase.listeners, 0, newHandlers, newHandlersIndex, length);
                newHandlersIndex += length;
            }
            handlers = newHandlers;
        }
        update();
    }

    @Override
    public void addPhaseOrdering(Identifier firstPhase, Identifier secondPhase) {
        Objects.requireNonNull(firstPhase, "Tried to add an ordering for a null phase.");
        Objects.requireNonNull(secondPhase, "Tried to add an ordering for a null phase.");
        if (firstPhase.equals(secondPhase)) {
            throw new IllegalArgumentException("Tried to add a phase that depends on itself.");
        }
        synchronized (lock) {
            EventPhaseData<T> first = getOrCreatePhase(firstPhase, false);
            EventPhaseData<T> second = getOrCreatePhase(secondPhase, false);
            EventPhaseData.link(first, second);
            NodeSorting.sort(sortedPhases, "event phases", Comparator.comparing(data -> data.id));
            rebuildInvoker(handlers.length);
        }
    }
}
