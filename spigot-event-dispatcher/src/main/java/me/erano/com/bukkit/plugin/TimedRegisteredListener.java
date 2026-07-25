package me.erano.com.bukkit.plugin;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.EventException;
import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/* JADX INFO: loaded from: spigot-26.2.jar:META-INF/libraries/spigot-api-26.2-R0.1-SNAPSHOT.jar:org/bukkit/plugin/TimedRegisteredListener.class */
public class TimedRegisteredListener extends RegisteredListener {
    private int count;
    private long totalTime;
    private Class<? extends Event> eventClass;
    private boolean multiple;

    public TimedRegisteredListener(@NotNull Listener pluginListener, @NotNull EventExecutor eventExecutor, @NotNull EventPriority eventPriority, @NotNull Application registeredPlugin, boolean listenCancelled) {
        super(pluginListener, eventExecutor, eventPriority, registeredPlugin, listenCancelled);
        this.multiple = false;
    }

    @Override // org.bukkit.plugin.RegisteredListener
    public void callEvent(@NotNull Event event) throws EventException {
        if (event.isAsynchronous()) {
            super.callEvent(event);
            return;
        }
        this.count++;
        Class cls = event.getClass();
        if (this.eventClass == null) {
            this.eventClass = cls;
        } else if (!this.eventClass.equals(cls)) {
            this.multiple = true;
            this.eventClass = getCommonSuperclass(cls, this.eventClass).asSubclass(Event.class);
        }
        long start = System.nanoTime();
        super.callEvent(event);
        this.totalTime += System.nanoTime() - start;
    }

    @NotNull
    private static Class<?> getCommonSuperclass(@NotNull Class<?> class1, @NotNull Class<?> class2) {
        while (!class1.isAssignableFrom(class2)) {
            class1 = class1.getSuperclass();
        }
        return class1;
    }

    public void reset() {
        this.count = 0;
        this.totalTime = 0L;
    }

    public int getCount() {
        return this.count;
    }

    public long getTotalTime() {
        return this.totalTime;
    }

    @Nullable
    public Class<? extends Event> getEventClass() {
        return this.eventClass;
    }

    public boolean hasMultiple() {
        return this.multiple;
    }
}
