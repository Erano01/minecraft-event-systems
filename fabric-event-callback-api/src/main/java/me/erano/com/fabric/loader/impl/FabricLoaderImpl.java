package me.erano.com.fabric.loader.impl;

import me.erano.com.fabric.loader.api.LanguageAdapter;
import me.erano.com.fabric.loader.api.entrypoint.EntrypointContainer;
import me.erano.com.fabric.loader.impl.entrypoint.EntrypointStorage;
import me.erano.com.fabric.loader.impl.metadata.EntrypointMetadata;
import me.erano.com.fabric.loader.impl.metadata.LoaderModMetadata;
import me.erano.com.fabric.loader.impl.metadata.ModMetadataParser;
import me.erano.com.fabric.loader.impl.util.DefaultLanguageAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

// Gercekte mod'lar dosya sisteminden/jar'lardan bulunur, bagimliliklar cozulur, jar-in-jar acilir ve
// Mixin/transformer'lar hazirlanir. Burada classpath'teki fabric.mod.json dosyalari okunuyor.
public final class FabricLoaderImpl {
    public static final FabricLoaderImpl INSTANCE = new FabricLoaderImpl();

    private final List<ModContainerImpl> mods = new ArrayList<>();
    private final Map<String, LanguageAdapter> adapterMap = new HashMap<>();
    private final EntrypointStorage entrypointStorage = new EntrypointStorage();

    private FabricLoaderImpl() {
    }

    public void load() {
        adapterMap.put("default", DefaultLanguageAdapter.INSTANCE);
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("fabric.mod.json");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try (InputStream in = url.openStream()) {
                    LoaderModMetadata metadata = ModMetadataParser.parse(in);
                    mods.add(new ModContainerImpl(metadata, url.toString()));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to discover mods", e);
        }
        setupMods();
    }

    private void setupMods() {
        for (ModContainerImpl mod : mods) {
            try {
                for (String key : mod.getInfo().getEntrypointKeys()) {
                    for (EntrypointMetadata in : mod.getInfo().getEntrypoints(key)) {
                        entrypointStorage.add(mod, key, in, adapterMap);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to setup mod %s (%s)", mod.getInfo().getId(), mod.getOrigin()), e);
            }
        }
    }

    public List<ModContainerImpl> getAllMods() {
        return List.copyOf(mods);
    }

    public boolean hasEntrypoints(String key) {
        return entrypointStorage.hasEntrypoints(key);
    }

    public <T> List<EntrypointContainer<T>> getEntrypointContainers(String key, Class<T> type) {
        return entrypointStorage.getEntrypointContainers(key, type);
    }

    public <T> List<T> getEntrypoints(String key, Class<T> type) {
        return entrypointStorage.getEntrypoints(key, type);
    }

    // Entrypoint'ler CAGIRAN thread'de, sirayla cagrilir; paralellik yok. Bir mod hata verirse digerleri
    // yine cagrilir, hatalar toplanip sonda birlikte firlatilir.
    public <T> void invokeEntrypoints(String key, Class<T> type, Consumer<? super T> invoker) {
        if (!hasEntrypoints(key)) {
            return;
        }
        RuntimeException exception = null;
        for (EntrypointContainer<T> container : getEntrypointContainers(key, type)) {
            try {
                invoker.accept(container.getEntrypoint());
            } catch (Throwable t) {
                RuntimeException wrapped = new RuntimeException(String.format(
                        "Could not execute entrypoint stage '%s' due to errors, provided by '%s' at '%s'!",
                        key, container.getProvider().getMetadata().getId(), container.getDefinition()), t);
                if (exception == null) {
                    exception = wrapped;
                } else {
                    exception.addSuppressed(wrapped);
                }
            }
        }
        if (exception != null) {
            throw exception;
        }
    }
}
