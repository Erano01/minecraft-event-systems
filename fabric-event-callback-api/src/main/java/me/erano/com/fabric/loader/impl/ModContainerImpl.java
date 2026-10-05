package me.erano.com.fabric.loader.impl;

import me.erano.com.fabric.loader.api.ModContainer;
import me.erano.com.fabric.loader.api.metadata.ModMetadata;
import me.erano.com.fabric.loader.impl.metadata.LoaderModMetadata;

// Gercekte ayrica mod'un dosya sistemi kokleri, origin'i, bagimliliklari ve alt mod'lari var.
public final class ModContainerImpl implements ModContainer {
    private final LoaderModMetadata info;
    private final String origin;

    public ModContainerImpl(LoaderModMetadata info, String origin) {
        this.info = info;
        this.origin = origin;
    }

    public LoaderModMetadata getInfo() {
        return info;
    }

    @Override
    public ModMetadata getMetadata() {
        return info;
    }

    public String getOrigin() {
        return origin;
    }
}
