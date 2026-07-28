package me.erano.com.bukkit.event;

import me.erano.com.bukkit.plugin.Plugin;
import me.erano.com.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class HandlerList {
    private volatile RegisteredListener[] handlers = null;
    private final EnumMap<EventPriority, ArrayList<RegisteredListener>> handlerslots = new EnumMap(EventPriority.class);
    private static ArrayList<HandlerList> allLists = new ArrayList();

    public static void bakeAll() {
        synchronized(allLists) {
            Iterator var1 = allLists.iterator();

            while(var1.hasNext()) {
                HandlerList h = (HandlerList)var1.next();
                h.bake();
            }

        }
    }

    public static void unregisterAll() {
        synchronized(allLists) {
            Iterator var1 = allLists.iterator();

            while(var1.hasNext()) {
                HandlerList h = (HandlerList)var1.next();
                synchronized(h) {
                    Iterator var3 = h.handlerslots.values().iterator();

                    while(var3.hasNext()) {
                        List<RegisteredListener> list = (List)var3.next();
                        list.clear();
                    }

                    h.handlers = null;
                }
            }

        }
    }

    public static void unregisterAll(@NotNull Plugin plugin) {
        synchronized(allLists) {
            Iterator var2 = allLists.iterator();

            while(var2.hasNext()) {
                HandlerList h = (HandlerList)var2.next();
                h.unregister(plugin);
            }

        }
    }

    public static void unregisterAll(@NotNull Listener listener) {
        synchronized(allLists) {
            Iterator var2 = allLists.iterator();

            while(var2.hasNext()) {
                HandlerList h = (HandlerList)var2.next();
                h.unregister(listener);
            }

        }
    }

    public HandlerList() {
        EventPriority[] var1 = EventPriority.values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            EventPriority o = var1[var3];
            this.handlerslots.put(o, new ArrayList());
        }

        synchronized(allLists) {
            allLists.add(this);
        }
    }

    public synchronized void register(@NotNull RegisteredListener listener) {
        if (((ArrayList)this.handlerslots.get(listener.getPriority())).contains(listener)) {
            throw new IllegalStateException("This listener is already registered to priority " + listener.getPriority().toString());
        } else {
            this.handlers = null;
            ((ArrayList)this.handlerslots.get(listener.getPriority())).add(listener);
        }
    }

    public void registerAll(@NotNull Collection<RegisteredListener> listeners) {
        Iterator var2 = listeners.iterator();

        while(var2.hasNext()) {
            RegisteredListener listener = (RegisteredListener)var2.next();
            this.register(listener);
        }

    }

    public synchronized void unregister(@NotNull RegisteredListener listener) {
        if (((ArrayList)this.handlerslots.get(listener.getPriority())).remove(listener)) {
            this.handlers = null;
        }

    }

    public synchronized void unregister(@NotNull Plugin plugin) {
        boolean changed = false;
        Iterator var3 = this.handlerslots.values().iterator();

        while(var3.hasNext()) {
            List<RegisteredListener> list = (List)var3.next();
            ListIterator i = list.listIterator();

            while(i.hasNext()) {
                if (((RegisteredListener)i.next()).getPlugin().equals(plugin)) {
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
        Iterator var3 = this.handlerslots.values().iterator();

        while(var3.hasNext()) {
            List<RegisteredListener> list = (List)var3.next();
            ListIterator i = list.listIterator();

            while(i.hasNext()) {
                if (((RegisteredListener)i.next()).getListener().equals(listener)) {
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
        if (this.handlers == null) {
            List<RegisteredListener> entries = new ArrayList();
            Iterator var2 = this.handlerslots.entrySet().iterator();

            while(var2.hasNext()) {
                Map.Entry<EventPriority, ArrayList<RegisteredListener>> entry = (Map.Entry)var2.next();
                entries.addAll((Collection)entry.getValue());
            }

            this.handlers = (RegisteredListener[])entries.toArray(new RegisteredListener[entries.size()]);
        }
    }

    @NotNull
    public RegisteredListener[] getRegisteredListeners() {
        RegisteredListener[] handlers;
        do {
            handlers = this.handlers;
            if (handlers == null) {
                this.bake();
            }
        } while(handlers == null);

        return handlers;
    }

    @NotNull
    public static ArrayList<RegisteredListener> getRegisteredListeners(@NotNull Plugin plugin) {
        ArrayList<RegisteredListener> listeners = new ArrayList();
        synchronized(allLists) {
            Iterator var2 = allLists.iterator();

            while(var2.hasNext()) {
                HandlerList h = (HandlerList)var2.next();
                synchronized(h) {
                    Iterator var5 = h.handlerslots.values().iterator();

                    while(var5.hasNext()) {
                        List<RegisteredListener> list = (List)var5.next();
                        Iterator var7 = list.iterator();

                        while(var7.hasNext()) {
                            RegisteredListener listener = (RegisteredListener)var7.next();
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
    public static ArrayList<HandlerList> getHandlerLists() {
        ArrayList var1;
        synchronized(allLists) {
            var1 = (ArrayList)allLists.clone();
        }

        return var1;
    }
}
