package me.erano.com.fabric.loader.impl.metadata;

// fabric.mod.json'daki tek bir entrypoint girdisi.
public record EntrypointMetadata(String adapter, String value) {
    public String getAdapter() {
        return adapter;
    }

    public String getValue() {
        return value;
    }
}
