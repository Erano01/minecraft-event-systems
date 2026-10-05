package me.erano.com.forge.eventbus.api.listener;

// MONITOR listener'lari icin: (event, wasCancelled). Boxing olmasin diye BiConsumer degil.
@FunctionalInterface
public interface ObjBooleanBiConsumer<T> {
    void accept(T t, boolean z);
}
