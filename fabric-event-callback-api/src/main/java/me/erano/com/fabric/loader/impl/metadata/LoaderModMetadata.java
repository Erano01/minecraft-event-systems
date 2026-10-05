package me.erano.com.fabric.loader.impl.metadata;

import me.erano.com.fabric.loader.api.metadata.ModMetadata;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// Gercekte V0/V1 ModMetadata implementasyonlari; burada sadece id ve entrypoint'ler.
public record LoaderModMetadata(String id, Map<String, List<EntrypointMetadata>> entrypoints) implements ModMetadata {
    @Override
    public String getId() {
        return id;
    }

    public Collection<String> getEntrypointKeys() {
        return entrypoints.keySet();
    }

    public List<EntrypointMetadata> getEntrypoints(String key) {
        return entrypoints.getOrDefault(key, List.of());
    }
}
