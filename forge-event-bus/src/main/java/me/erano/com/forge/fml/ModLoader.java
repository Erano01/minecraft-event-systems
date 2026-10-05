package me.erano.com.forge.fml;

import me.erano.com.forge.fml.core.ModStateProvider;
import me.erano.com.forge.fml.javafmlmod.FMLModContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

// Mod'lari tek tek degil, asama asama ve paralel ilerletir. Main thread bu sinifin metotlarini
// cagirir ve her asamanin bitmesini bekler.
public final class ModLoader {
    private ModLoader() {
    }

    // Gercekte mod'lar jar taramasiyla bulunur ve bagimliliklar mods.toml'dan cozulur.
    public static void gatherAndInitializeMods(List<ModInfo> modInfos) {
        List<ModContainer> containers = new ArrayList<>();
        for (ModInfo info : modInfos) {
            containers.add(new FMLModContainer(info));
        }
        Map<String, ModContainer> byId = containers.stream().collect(Collectors.toMap(ModContainer::getModId, Function.identity()));
        for (ModContainer container : containers) {
            for (String dep : container.getModInfo().dependencies()) {
                ModContainer depContainer = byId.get(dep);
                if (depContainer == null) {
                    throw new IllegalStateException(container.getModId() + " depends on missing mod " + dep);
                }
                container.dependencies.add(depContainer);
            }
        }
        ModList.setLoadedMods(containers);
        runPhase(ModLoadingPhase.GATHER);
    }

    public static void loadMods() {
        runPhase(ModLoadingPhase.LOAD);
    }

    public static void finishMods() {
        runPhase(ModLoadingPhase.COMPLETE);
    }

    private static void runPhase(ModLoadingPhase phase) {
        ModWorkManager.DrivenExecutor syncExecutor = ModWorkManager.syncExecutor();
        Executor parallelExecutor = ModWorkManager.parallelExecutor();
        for (ModLoadingState state : ModStateProvider.getAllStates()) {
            if (state.phase() == phase) {
                dispatchAndHandleError(state, syncExecutor, parallelExecutor);
            }
        }
    }

    private static void dispatchAndHandleError(ModLoadingState state, ModWorkManager.DrivenExecutor syncExecutor, Executor parallelExecutor) {
        Runnable ticker = () -> {
        };
        syncExecutor.drive(ticker);
        CompletableFuture<Void> transition = state.buildTransition(syncExecutor, parallelExecutor);
        waitForTransition(state, syncExecutor, ticker, transition);
    }

    // Main thread burada BLOKE OLMAZ: gecis bitene kadar sync executor'in kuyrugunu bosaltir.
    // Worker'larin sync executor'a biraktigi isler (orn. DeferredWorkQueue.runTasks) boylece main
    // thread'de calisir. Gercekte ticker, yukleme ekranini ciziyor; burada bos (busy-wait).
    private static void waitForTransition(ModLoadingState state, ModWorkManager.DrivenExecutor syncExecutor, Runnable ticker, CompletableFuture<Void> transition) {
        while (!transition.isDone()) {
            syncExecutor.drive(ticker);
        }
        try {
            transition.join();
        } catch (CompletionException e) {
            throw new IllegalStateException("Failed to complete lifecycle event " + state.name(), e.getCause());
        }
    }
}
