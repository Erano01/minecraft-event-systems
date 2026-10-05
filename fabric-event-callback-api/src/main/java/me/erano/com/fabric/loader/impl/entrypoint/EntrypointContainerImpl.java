package me.erano.com.fabric.loader.impl.entrypoint;

import me.erano.com.fabric.loader.api.EntrypointException;
import me.erano.com.fabric.loader.api.ModContainer;
import me.erano.com.fabric.loader.api.entrypoint.EntrypointContainer;

// Entrypoint nesnesi ilk getEntrypoint() cagrisinda olusturulur. synchronized: lazy init'in birden fazla
// thread'den cagrilmasi durumunda nesnenin bir kez olusmasi ve guvenle gorunmesi icin.
public final class EntrypointContainerImpl<T> implements EntrypointContainer<T> {
    private final String key;
    private final Class<T> type;
    private final EntrypointStorage.Entry entry;
    private T instance;

    public EntrypointContainerImpl(String key, Class<T> type, EntrypointStorage.Entry entry) {
        this.key = key;
        this.type = type;
        this.entry = entry;
    }

    @Override
    public synchronized T getEntrypoint() {
        if (instance == null) {
            try {
                instance = entry.getOrCreate(type);
            } catch (Exception e) {
                throw new EntrypointException(key, getProvider().getMetadata().getId(), e);
            }
        }
        return instance;
    }

    @Override
    public ModContainer getProvider() {
        return entry.getModContainer();
    }

    @Override
    public String getDefinition() {
        return entry.getDefinition();
    }
}
