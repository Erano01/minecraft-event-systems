package me.erano.com.forge.eventbus.internal;

import me.erano.com.forge.eventbus.api.bus.BusGroup;
import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.eventbus.api.event.InheritableEvent;
import me.erano.com.forge.eventbus.api.event.MutableEvent;
import me.erano.com.forge.eventbus.api.event.RecordEvent;
import me.erano.com.forge.eventbus.api.listener.EventListener;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public record BusGroupImpl(String name, Class<?> baseType, ConcurrentHashMap<Class<? extends Event>, EventBus<?>> eventBuses)
        implements BusGroup {

    // Grup isimleri JVM genelinde tekil (orn. "default", "modBusFor<modid>").
    private static final Set<String> BUS_GROUP_NAMES = ConcurrentHashMap.newKeySet();

    public BusGroupImpl {
        if (!BUS_GROUP_NAMES.add(Objects.requireNonNull(name))) {
            throw new IllegalArgumentException("BusGroup name \"" + name + "\" is already in use");
        }
    }

    public BusGroupImpl(String name, Class<?> baseType) {
        this(name, baseType, new ConcurrentHashMap<>());
    }

    @Override
    public void startup() {
        for (EventBus<?> eventBus : eventBuses.values()) {
            ((AbstractEventBusImpl<?, ?>) eventBus).startup();
        }
    }

    @Override
    public void shutdown() {
        for (EventBus<?> eventBus : eventBuses.values()) {
            ((AbstractEventBusImpl<?, ?>) eventBus).shutdown();
        }
    }

    @Override
    public void dispose() {
        for (EventBus<?> eventBus : eventBuses.values()) {
            ((AbstractEventBusImpl<?, ?>) eventBus).dispose();
        }
        eventBuses.clear();
        BUS_GROUP_NAMES.remove(name);
    }

    @Override
    public void trim() {
        for (EventBus<?> eventBus : eventBuses.values()) {
            ((AbstractEventBusImpl<?, ?>) eventBus).trim();
        }
    }

    @Override
    public Collection<EventListener> register(MethodHandles.Lookup callerLookup, Class<?> utilityClassWithStaticListeners) {
        return EventListenerFactory.register(this, callerLookup, utilityClassWithStaticListeners, null);
    }

    @Override
    public Collection<EventListener> register(MethodHandles.Lookup callerLookup, Object listener) {
        return EventListenerFactory.register(this, callerLookup, listener.getClass(), listener);
    }

    @Override
    public void unregister(Collection<EventListener> listeners) {
        if (listeners.isEmpty()) {
            throw new IllegalArgumentException("Listeners cannot be empty! You should be getting the collection from the BusGroup#register method.");
        }
        for (EventListener listener : listeners) {
            getOrCreateEventBus(listener.eventType()).removeListener(listener);
        }
    }

    // Hizli yol: kilitsiz ConcurrentHashMap.get. Yavas yol: bus KILIT DISINDA olusturulur, sonra
    // putIfAbsent ile yarisi kazanan yayinlanir. Gercek koddaki zayif nokta da burada (birebir korundu):
    // createEventBus, yarisi kaybedecek olsa bile kendini ust tiplerin children listesine kilitsiz ekler.
    @SuppressWarnings("unchecked")
    public <T extends Event> EventBus<T> getOrCreateEventBus(Class<T> eventType) {
        EventBus<T> existing = (EventBus<T>) eventBuses.get(eventType);
        if (existing != null) {
            return existing;
        }
        EventBus<T> computedEventBus = createEventBus(eventType);
        synchronized (eventBuses) {
            EventBus<T> previous = (EventBus<T>) eventBuses.putIfAbsent(eventType, computedEventBus);
            return previous == null ? computedEventBus : previous;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Event> EventBus<T> createEventBus(Class<T> eventType) {
        if (baseType != Event.class && !baseType.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException("BusGroup \"" + name + "\" requires all events on it to inherit from "
                    + baseType + " but " + eventType + " doesn't.");
        }
        int characteristics = AbstractEventBusImpl.computeEventCharacteristics(eventType);

        // Kalitim: ust tiplerin mevcut listener'lari yeni bus'a kopyalanir.
        ArrayList<EventListener> backingList = new ArrayList<>();
        List<EventBus<?>> parents = Collections.emptyList();
        if (Constants.isInheritable(characteristics)) {
            parents = getParentEvents(eventType);
            for (EventBus<?> parent : parents) {
                backingList.addAll(((AbstractEventBusImpl<?, ?>) parent).backingList());
            }
        }

        AbstractEventBusImpl<T, ?> eventBusImpl = Constants.isCancellable(characteristics)
                ? new CancellableEventBusImpl(name, eventType, backingList, characteristics)
                : new EventBusImpl<>(name, eventType, backingList, characteristics);

        if (Constants.isInheritable(characteristics)) {
            for (EventBus<?> parent : parents) {
                ((AbstractEventBusImpl<?, ?>) parent).children().add(eventBusImpl);
            }
        }
        return eventBusImpl;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T extends Event> List<EventBus<?>> getParentEvents(Class<T> eventType) {
        ArrayList<EventBus<?>> parentEvents = new ArrayList<>();
        Class<? super T> parent = eventType.getSuperclass();
        if (parent != null && InheritableEvent.class.isAssignableFrom(parent) && parent != MutableEvent.class) {
            parentEvents.add(getOrCreateEventBus((Class) parent));
        }
        for (Class<?> iface : eventType.getInterfaces()) {
            if (iface != InheritableEvent.class && InheritableEvent.class.isAssignableFrom(iface)
                    && iface != RecordEvent.class && iface != Event.class) {
                parentEvents.add(getOrCreateEventBus((Class) iface));
            }
        }
        return parentEvents;
    }

    @Override
    public boolean equals(Object that) {
        return this == that || (that instanceof BusGroupImpl busGroup && name.equals(busGroup.name));
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
