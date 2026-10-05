package me.erano.com.fabric.loader.impl.entrypoint;

import me.erano.com.fabric.loader.api.EntrypointException;
import me.erano.com.fabric.loader.api.LanguageAdapter;
import me.erano.com.fabric.loader.api.entrypoint.EntrypointContainer;
import me.erano.com.fabric.loader.impl.ModContainerImpl;
import me.erano.com.fabric.loader.impl.metadata.EntrypointMetadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

// entrypoint anahtari ("main", "client", "server") -> girdiler. entryMap duz HashMap: loader baslarken tek
// thread'de doldurulur, sonra sadece okunur. Gercekte ayrica 0.3.x eski stil girdiler (OldEntry) var.
public final class EntrypointStorage {
    private final Map<String, List<Entry>> entryMap = new HashMap<>();

    interface Entry {
        <T> T getOrCreate(Class<T> type) throws Exception;

        ModContainerImpl getModContainer();

        String getDefinition();
    }

    // Ayni girdi farkli arayuz tipleriyle istenebilir (orn. hem ModInitializer hem baska bir arayuz);
    // her tip icin bir nesne. Erisim synchronized (instanceMap bir IdentityHashMap).
    private static final class NewEntry implements Entry {
        private final ModContainerImpl mod;
        private final LanguageAdapter adapter;
        private final String value;
        private final Map<Class<?>, Object> instanceMap = new IdentityHashMap<>(1);

        NewEntry(ModContainerImpl mod, LanguageAdapter adapter, String value) {
            this.mod = mod;
            this.adapter = adapter;
            this.value = value;
        }

        @Override
        @SuppressWarnings("unchecked")
        public synchronized <T> T getOrCreate(Class<T> type) throws Exception {
            Object ret = instanceMap.get(type);
            if (ret == null) {
                ret = adapter.create(mod, value, type);
                Object prev = instanceMap.putIfAbsent(type, ret);
                if (prev != null) {
                    ret = prev;
                }
            }
            return (T) ret;
        }

        @Override
        public ModContainerImpl getModContainer() {
            return mod;
        }

        @Override
        public String getDefinition() {
            return value;
        }

        @Override
        public String toString() {
            return mod.getMetadata().getId() + "->(0.3.x)" + value;
        }
    }

    private List<Entry> getOrCreateEntries(String key) {
        return entryMap.computeIfAbsent(key, z -> new ArrayList<>());
    }

    public void add(ModContainerImpl modContainer, String key, EntrypointMetadata metadata, Map<String, LanguageAdapter> adapterMap) throws Exception {
        if (!adapterMap.containsKey(metadata.getAdapter())) {
            throw new Exception("Could not find adapter '" + metadata.getAdapter() + "' (mod " + modContainer.getMetadata().getId() + "!)");
        }
        getOrCreateEntries(key).add(new NewEntry(modContainer, adapterMap.get(metadata.getAdapter()), metadata.getValue()));
    }

    public boolean hasEntrypoints(String key) {
        return entryMap.containsKey(key);
    }

    // Nesneler burada olusturulmaz; container ilk getEntrypoint() cagrisinda olusturur.
    public <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
        List<Entry> entries = entryMap.get(key);
        if (entries == null) {
            return Collections.emptyList();
        }
        List<EntrypointContainer<T>> results = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            results.add(new EntrypointContainerImpl<>(key, type, entry));
        }
        return results;
    }

    public <T> List<T> getEntrypoints(String key, Class<T> type) {
        List<Entry> entries = entryMap.get(key);
        if (entries == null) {
            return Collections.emptyList();
        }
        EntrypointException exception = null;
        List<T> results = new ArrayList<>(entries.size());
        for (Entry entry : entries) {
            try {
                T result = entry.getOrCreate(type);
                if (result != null) {
                    results.add(result);
                }
            } catch (Throwable t) {
                if (exception == null) {
                    exception = new EntrypointException(key, entry.getModContainer().getMetadata().getId(), t);
                } else {
                    exception.addSuppressed(t);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
        return results;
    }
}
