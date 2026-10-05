package me.erano.com.fabric.api;

// fabric.mod.json'daki "main" entrypoint'i. Hem client'ta hem server'da, oyun baslarken bir kez cagrilir.
@FunctionalInterface
public interface ModInitializer {
    void onInitialize();
}
