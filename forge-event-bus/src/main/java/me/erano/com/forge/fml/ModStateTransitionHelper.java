package me.erano.com.forge.fml;

import me.erano.com.forge.fml.event.IModBusEvent;

import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

final class ModStateTransitionHelper {
    private ModStateTransitionHelper() {
    }

    static CompletableFuture<Void> build(IModStateTransition transition, Executor syncExecutor, Executor parallelExecutor) {
        Executor executor = transition.threadSelector().apply(syncExecutor, parallelExecutor);
        CompletableFuture<Void> dispatch = addCompletableFutureTaskForModDispatch(executor, transition.eventFunction());
        return transition.finalActivityGenerator().apply(syncExecutor, dispatch);
    }

    // Her mod icin bir CompletableFuture. Bir mod'un isi, bagimli oldugu mod'larin future'lari bitince
    // baslar (allOf(deps)); bagimsiz mod'lar ayni anda farkli worker'larda calisir. Siralama kilitle
    // degil, future grafigiyle saglaniyor.
    // Gercekte hatalar 'gather' ile toplanip hepsi birden raporlanir; burada allOf ilk hatayi tasir.
    private static CompletableFuture<Void> addCompletableFutureTaskForModDispatch(Executor executor, Function<ModContainer, IModBusEvent> eventGenerator) {
        LinkedHashMap<String, CompletableFuture<Void>> modFutures = new LinkedHashMap<>();
        for (ModContainer mod : ModList.getLoadedMods()) {
            CompletableFuture<?>[] deps = mod.dependencies.stream()
                    .map(dep -> {
                        CompletableFuture<Void> future = modFutures.get(dep.getModId());
                        if (future == null) {
                            throw new IllegalStateException("Could not find dependency future " + dep.getModId() + " for " + mod.getModId());
                        }
                        return future;
                    })
                    .toArray(CompletableFuture[]::new);
            CompletableFuture<Void> dispatch = CompletableFuture.allOf(deps).thenRunAsync(() -> {
                ModLoadingContext.get().setActiveContainer(mod);
                Runnable handler = mod.activityMap.get(mod.modLoadingStage);
                if (handler != null) {
                    handler.run();
                }
                mod.acceptEvent(eventGenerator.apply(mod));
            }, executor).whenComplete((result, exception) -> {
                mod.modLoadingStage = mod.modLoadingStage.nextState(exception);
                ModLoadingContext.get().setActiveContainer(null);
            });
            modFutures.put(mod.getModId(), dispatch);
        }
        return CompletableFuture.allOf(modFutures.values().toArray(CompletableFuture[]::new));
    }
}
