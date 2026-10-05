package me.erano.com.fabric.loader.api.entrypoint;

import me.erano.com.fabric.loader.api.ModContainer;

public interface EntrypointContainer<T> {
    T getEntrypoint();

    ModContainer getProvider();

    default String getDefinition() {
        return "";
    }
}
