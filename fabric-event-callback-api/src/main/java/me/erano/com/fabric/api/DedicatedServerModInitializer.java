package me.erano.com.fabric.api;

// fabric.mod.json'daki "server" entrypoint'i; tum "main" entrypoint'lerinden sonra cagrilir.
@FunctionalInterface
public interface DedicatedServerModInitializer {
    void onInitializeServer();
}
