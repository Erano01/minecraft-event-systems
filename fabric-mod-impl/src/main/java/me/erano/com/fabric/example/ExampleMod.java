package me.erano.com.fabric.example;

import me.erano.com.fabric.api.ModInitializer;
import me.erano.com.fabric.fabric.api.event.Event;
import me.erano.com.fabric.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import me.erano.com.fabric.fabric.api.event.lifecycle.v1.ServerTickEvents;
import me.erano.com.fabric.fabric.api.message.v1.ServerMessageEvents;
import me.erano.com.fabric.minecraft.resources.Identifier;

// fabric.mod.json'daki "main" entrypoint'i. Loader bu sinifi DefaultLanguageAdapter ile no-arg
// constructor'dan olusturur ve onInitialize()'i cagirir. Tum kayitlar burada, oyun baslamadan yapilir.
public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "examplemod";
    private static final Identifier EARLY_PHASE = Identifier.fromNamespaceAndPath(MOD_ID, "early");

    @Override
    public void onInitialize() {
        Log.line("ExampleMod.onInitialize (main entrypoint)");

        ServerLifecycleEvents.SERVER_STARTING.register(server -> Log.line("SERVER_STARTING"));

        // Fazlar: once varsayilan faza bir listener, SONRA ozel bir faza bir listener kaydediliyor.
        // addPhaseOrdering(EARLY, DEFAULT) ile EARLY fazi kayit sirasindan bagimsiz olarak once calisir.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> Log.line("SERVER_STARTED [default faz, ilk kaydedilen]"));
        ServerLifecycleEvents.SERVER_STARTED.register(EARLY_PHASE, server -> Log.line("SERVER_STARTED [examplemod:early faz, sonra kaydedilen]"));
        ServerLifecycleEvents.SERVER_STARTED.addPhaseOrdering(EARLY_PHASE, Event.DEFAULT_PHASE);

        ServerTickEvents.START_SERVER_TICK.register(server -> Log.line("START_SERVER_TICK #" + server.getTickCount()));
        ServerTickEvents.END_SERVER_TICK.register(server -> Log.line("END_SERVER_TICK   #" + server.getTickCount()));

        // ALLOW_*: false donen ilk listener zinciri keser ve mesaj yayinlanmaz (CHAT_MESSAGE de calismaz).
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, bound) -> {
            boolean allowed = !message.content().contains("spam");
            Log.line("ALLOW_CHAT_MESSAGE <" + sender.name() + "> \"" + message.content() + "\" -> " + (allowed ? "izin" : "ENGELLENDI"));
            return allowed;
        });
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, bound) ->
                Log.line("CHAT_MESSAGE <" + sender.name() + "> " + message.content()));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> Log.line("SERVER_STOPPING"));
    }

    // fabric.mod.json'daki "server" entrypoint'i "ExampleMod::onInitializeServer" bicimiyle verildi:
    // DefaultLanguageAdapter bu statik metodu MethodHandleProxies ile DedicatedServerModInitializer'a uyarlar.
    public static void onInitializeServer() {
        Log.line("ExampleMod::onInitializeServer (server entrypoint, Class::method bicimi)");
    }
}
