package me.erano.com.forge.fml;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public record ModLoadingState(String name, ModLoadingPhase phase, IModStateTransition transition) {
    public CompletableFuture<Void> buildTransition(Executor syncExecutor, Executor parallelExecutor) {
        return transition.build(syncExecutor, parallelExecutor);
    }
}
