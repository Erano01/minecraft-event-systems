package me.erano.com.bukkit.event;

import me.erano.com.bukkit.plugin.Plugin;
import me.erano.com.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class HandlerList {
    private volatile RegisteredListener[] handlers = null;
    private final EnumMap<EventPriority, ArrayList<RegisteredListener>> handlerslots = new EnumMap<>(EventPriority.class);
    private static ArrayList<HandlerList> allLists = new ArrayList<>();

    public static void bakeAll() {
        synchronized (allLists) {
            for (HandlerList h : allLists) {
                h.bake();
            }
        }
    }

    public static void unregisterAll() {
        synchronized (allLists) {
            for (HandlerList h : allLists) {
                synchronized (h) {
                    for (List<RegisteredListener> list : h.handlerslots.values()) {
                        list.clear();
                    }
                    h.handlers = null;
                }
            }
        }
    }

    public static void unregisterAll(@NotNull Plugin plugin) {
        synchronized (allLists) {
            for (HandlerList h : allLists) {
                h.unregister(plugin);
            }
        }
    }

    public static void unregisterAll(@NotNull Listener listener) {
        synchronized (allLists) {
            for (HandlerList h : allLists) {
                h.unregister(listener);
            }
        }
    }

    public HandlerList() {
        for (EventPriority o : EventPriority.values()) {
            this.handlerslots.put(o, new ArrayList<>());
        }
        synchronized (allLists) {
            allLists.add(this);
        }
    }

    public synchronized void register(@NotNull RegisteredListener listener) {
        if (this.handlerslots.get(listener.getPriority()).contains(listener)) {
            throw new IllegalStateException("This listener is already registered to priority " + listener.getPriority().toString());
        }
        this.handlers = null;
        this.handlerslots.get(listener.getPriority()).add(listener);
    }

    public void registerAll(@NotNull Collection<RegisteredListener> listeners) {
        for (RegisteredListener listener : listeners) {
            register(listener);
        }
    }

    public synchronized void unregister(@NotNull RegisteredListener listener) {
        if (this.handlerslots.get(listener.getPriority()).remove(listener)) {
            this.handlers = null;
        }
    }

    public synchronized void unregister(@NotNull Plugin plugin) {
        boolean changed = false;
        for (List<RegisteredListener> list : this.handlerslots.values()) {
            ListIterator<RegisteredListener> i = list.listIterator();
            while (i.hasNext()) {
                if (i.next().getPlugin().equals(plugin)) {
                    i.remove();
                    changed = true;
                }
            }
        }
        if (changed) {
            this.handlers = null;
        }
    }

    public synchronized void unregister(@NotNull Listener listener) {
        boolean changed = false;
        for (List<RegisteredListener> list : this.handlerslots.values()) {
            ListIterator<RegisteredListener> i = list.listIterator();
            while (i.hasNext()) {
                if (i.next().getListener().equals(listener)) {
                    i.remove();
                    changed = true;
                }
            }
        }
        if (changed) {
            this.handlers = null;
        }
    }

    public synchronized void bake() {
        if (this.handlers != null) {
            return;
        }
        List<RegisteredListener> entries = new ArrayList<>();
        for (Map.Entry<EventPriority, ArrayList<RegisteredListener>> entry : this.handlerslots.entrySet()) {
            entries.addAll(entry.getValue());
        }
        this.handlers = entries.toArray(new RegisteredListener[entries.size()]);
    }

    @NotNull
    public RegisteredListener[] getRegisteredListeners() {
        while (true) {
            RegisteredListener[] handlers = this.handlers;
            if (handlers != null) {
                return handlers;
            }
            bake();
        }
    }

    @NotNull
    public static ArrayList<RegisteredListener> getRegisteredListeners(@NotNull Plugin plugin) {
        ArrayList<RegisteredListener> listeners = new ArrayList<>();
        synchronized (allLists) {
            for (HandlerList h : allLists) {
                synchronized (h) {
                    for (List<RegisteredListener> list : h.handlerslots.values()) {
                        for (RegisteredListener listener : list) {
                            if (listener.getPlugin().equals(plugin)) {
                                listeners.add(listener);
                            }
                        }
                    }
                }
            }
        }
        return listeners;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static ArrayList<HandlerList> getHandlerLists() {
        ArrayList<HandlerList> arrayList;
        synchronized (allLists) {
            arrayList = (ArrayList<HandlerList>) allLists.clone();
        }
        return arrayList;
    }
}
