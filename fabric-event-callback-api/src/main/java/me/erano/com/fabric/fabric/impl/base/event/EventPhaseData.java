package me.erano.com.fabric.fabric.impl.base.event;

import me.erano.com.fabric.fabric.impl.base.toposort.SortableNode;
import me.erano.com.fabric.minecraft.resources.Identifier;

import java.lang.reflect.Array;
import java.util.Arrays;

// Bir fazin listener'lari. Ekleme copy-on-write: eski dizi hic degismez, yeni dizi olusturulur.
// Alanlar ArrayBackedEvent'in kilidi altinda degisir.
class EventPhaseData<T> extends SortableNode<EventPhaseData<T>> {
    final Identifier id;
    T[] listeners;

    @SuppressWarnings("unchecked")
    EventPhaseData(Identifier id, Class<?> listenerClass) {
        this.id = id;
        this.listeners = (T[]) Array.newInstance(listenerClass, 0);
    }

    void addListener(T listener) {
        int oldLength = listeners.length;
        listeners = Arrays.copyOf(listeners, oldLength + 1);
        listeners[oldLength] = listener;
    }

    @Override
    protected String getDescription() {
        return id.toString();
    }
}
