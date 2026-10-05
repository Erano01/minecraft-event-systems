package me.erano.com.forge.eventbus.internal;

import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.eventbus.api.event.InheritableEvent;
import me.erano.com.forge.eventbus.api.event.characteristic.Cancellable;
import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.api.listener.Priority;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

// EventBusImpl ve CancellableEventBusImpl'in ortak mantigi. I = invoker tipi (Consumer veya Predicate).
//
// Senkronizasyon politikasi:
//  - backingList / monitorBackingList / children: backingList'in monitoru ile korunur (yazma yolu).
//  - invoker: VolatileCallSite icinde, kilitsiz okunur (okuma yolu). Bkz. concurrency-model.md.
public interface AbstractEventBusImpl<T extends Event, I> extends EventBus<T> {
    ArrayList<EventListener> backingList();

    ArrayList<EventListener> monitorBackingList();

    List<AbstractEventBusImpl<?, ?>> children();

    AtomicBoolean shutdownFlag();

    AtomicBoolean alreadyInvalidated();

    int eventCharacteristics();

    I maybeGetInvoker();

    void invalidateInvoker();

    I buildInvoker();

    void setNoOpInvoker();

    static int computeEventCharacteristics(Class<?> eventType) {
        int characteristics = 0;
        if (Cancellable.class.isAssignableFrom(eventType)) {
            characteristics |= Constants.CHARACTERISTIC_CANCELLABLE;
        }
        if (InheritableEvent.class.isAssignableFrom(eventType)) {
            characteristics |= Constants.CHARACTERISTIC_INHERITABLE;
        }
        return characteristics;
    }

    static List<AbstractEventBusImpl<?, ?>> makeEventChildrenList(Class<?> eventType, int eventCharacteristics) {
        if (Constants.notInheritable(eventCharacteristics) || Modifier.isFinal(eventType.getModifiers())) {
            return Collections.emptyList();
        }
        Class<?>[] permittedSubclasses = eventType.getPermittedSubclasses();
        if (permittedSubclasses != null) {
            return new ArrayList<>(permittedSubclasses.length);
        }
        return new ArrayList<>();
    }

    @Override
    default EventListener addListener(EventListener listener) {
        synchronized (backingList()) {
            boolean added = listener.priority() == Priority.MONITOR
                    ? monitorBackingList().add(listener)
                    : backingList().add(listener);
            if (added) {
                invalidateInvoker();
                if (notInheritable()) {
                    return listener;
                }
                // Kalitim kayit aninda cozulur: ust tipe eklenen listener alt tiplerin bus'larina da eklenir.
                for (AbstractEventBusImpl<?, ?> child : children()) {
                    child.addListener(listener);
                }
            }
            return listener;
        }
    }

    @Override
    default void removeListener(EventListener listener) {
        synchronized (backingList()) {
            boolean removed = listener.priority() == Priority.MONITOR
                    ? monitorBackingList().remove(listener)
                    : backingList().remove(listener);
            if (removed) {
                invalidateInvoker();
                if (notInheritable()) {
                    return;
                }
                for (AbstractEventBusImpl<?, ?> child : children()) {
                    child.removeListener(listener);
                }
            }
        }
    }

    // Kilitsiz okuma; invoker gecersizse (null) buildInvoker() kilit altinda yeniden kurar.
    default I getInvoker() {
        I invoker = maybeGetInvoker();
        if (invoker == null) {
            invoker = buildInvoker();
        }
        return invoker;
    }

    default void startup() {
        if (!shutdownFlag().compareAndSet(true, false)) {
            return;
        }
        synchronized (backingList()) {
            alreadyInvalidated().setOpaque(false);
            invalidateInvoker();
            children().forEach(AbstractEventBusImpl::startup);
        }
    }

    // Kapali bus'ta post() no-op'tur; alreadyInvalidated=true oldugu icin yeni listener'lar invoker'i geri acmaz.
    default void shutdown() {
        if (!shutdownFlag().compareAndSet(false, true)) {
            return;
        }
        synchronized (backingList()) {
            setNoOpInvoker();
            alreadyInvalidated().set(true);
            children().forEach(AbstractEventBusImpl::shutdown);
        }
    }

    default void dispose() {
        shutdown();
        synchronized (backingList()) {
            backingList().clear();
            monitorBackingList().clear();
            backingList().trimToSize();
            monitorBackingList().trimToSize();
            children().forEach(AbstractEventBusImpl::dispose);
            if (children() instanceof ArrayList<?> childrenArrayList) {
                childrenArrayList.clear();
                childrenArrayList.trimToSize();
            }
        }
    }

    default void trim() {
        synchronized (backingList()) {
            backingList().trimToSize();
            monitorBackingList().trimToSize();
            if (children() instanceof ArrayList<?> childrenArrayList) {
                childrenArrayList.trimToSize();
            }
        }
    }

    private boolean notInheritable() {
        return Constants.notInheritable(eventCharacteristics());
    }
}
