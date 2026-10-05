package me.erano.com.forge.eventbus.api.bus;

import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.internal.BusGroupImpl;
import me.erano.com.forge.eventbus.internal.Event;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

// Event tipi -> EventBus haritasi. Forge'da iki tur var: game bus (DEFAULT = MinecraftForge.EVENT_BUS)
// ve her mod icin ayri bir mod bus ("modBusFor<modid>", sadece IModBusEvent kabul eder).
public interface BusGroup {
    BusGroup DEFAULT = create("default");

    String name();

    void startup();

    void shutdown();

    void dispose();

    void trim();

    // Lookup: LambdaMetafactory'nin listener metoduna erisebilmesi icin cagiranin erisim hakki.
    Collection<EventListener> register(MethodHandles.Lookup callerLookup, Class<?> utilityClassWithStaticListeners);

    Collection<EventListener> register(MethodHandles.Lookup callerLookup, Object listener);

    void unregister(Collection<EventListener> listeners);

    static BusGroup create(String name) {
        return new BusGroupImpl(name, Event.class);
    }

    static BusGroup create(String name, Class<?> baseType) {
        return new BusGroupImpl(name, baseType);
    }
}
