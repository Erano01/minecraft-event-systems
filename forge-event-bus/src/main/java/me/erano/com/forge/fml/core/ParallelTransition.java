package me.erano.com.forge.fml.core;

import me.erano.com.forge.fml.IModStateTransition;
import me.erano.com.forge.fml.ModContainer;
import me.erano.com.forge.fml.ModLoadingStage;
import me.erano.com.forge.fml.ThreadSelector;
import me.erano.com.forge.fml.event.IModBusEvent;
import me.erano.com.forge.fml.event.lifecycle.ParallelDispatchEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

// Event'leri paralel executor'da dagitir; tum mod'lar bitince o asamanin DeferredWorkQueue'sunu
// sync executor'da (main thread) calistirir.
record ParallelTransition(ModLoadingStage stage, BiFunction<ModContainer, ModLoadingStage, ParallelDispatchEvent> event)
        implements IModStateTransition {

    @Override
    public ThreadSelector threadSelector() {
        return ThreadSelector.PARALLEL;
    }

    @Override
    public Function<ModContainer, IModBusEvent> eventFunction() {
        return mod -> event.apply(mod, stage);
    }

    @Override
    public BiFunction<Executor, CompletableFuture<Void>, CompletableFuture<Void>> finalActivityGenerator() {
        return (syncExecutor, prev) -> prev.thenApplyAsync(t -> {
            stage.getDeferredWorkQueue().runTasks();
            return t;
        }, syncExecutor);
    }
}
