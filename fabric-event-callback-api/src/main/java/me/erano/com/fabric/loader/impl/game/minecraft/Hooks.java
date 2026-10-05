package me.erano.com.fabric.loader.impl.game.minecraft;

import me.erano.com.fabric.api.DedicatedServerModInitializer;
import me.erano.com.fabric.api.ModInitializer;
import me.erano.com.fabric.loader.impl.FabricLoaderImpl;

// Loader'in oyun koduna enjekte ettigi cagri noktalari. Gercekte EntrypointPatch, Minecraft'in server
// main'ine startServer cagrisini bytecode olarak ekler; ayrica prepareModInit(runDir, gameInstance) cagrilir.
public final class Hooks {
    private Hooks() {
    }

    public static void startServer(Object gameInstance) {
        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.invokeEntrypoints("main", ModInitializer.class, ModInitializer::onInitialize);
        loader.invokeEntrypoints("server", DedicatedServerModInitializer.class, DedicatedServerModInitializer::onInitializeServer);
    }
}
