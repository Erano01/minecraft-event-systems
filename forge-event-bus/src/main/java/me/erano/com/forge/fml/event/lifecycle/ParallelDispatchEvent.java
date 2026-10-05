package me.erano.com.forge.fml.event.lifecycle;

import me.erano.com.forge.fml.DeferredWorkQueue;
import me.erano.com.forge.fml.ModContainer;
import me.erano.com.forge.fml.ModLoadingStage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

// Paralel dagitilan lifecycle event'leri. Listener'lari modloading-worker thread'lerinde calisir;
// thread-safe olmayan isler enqueueWork ile main thread'e devredilmelidir.
public abstract class ParallelDispatchEvent extends ModLifecycleEvent {
    private final ModLoadingStage modLoadingStage;

    protected ParallelDispatchEvent(ModContainer container, ModLoadingStage stage) {
        super(container);
        this.modLoadingStage = stage;
    }

    private Optional<DeferredWorkQueue> getQueue() {
        return DeferredWorkQueue.lookup(Optional.of(modLoadingStage));
    }

    public CompletableFuture<Void> enqueueWork(Runnable work) {
        return getQueue().map(q -> q.enqueueWork(getContainer(), work))
                .orElseThrow(() -> new RuntimeException("No work queue found!"));
    }

    public <T> CompletableFuture<T> enqueueWork(Supplier<T> work) {
        return getQueue().map(q -> q.enqueueWork(getContainer(), work))
                .orElseThrow(() -> new RuntimeException("No work queue found!"));
    }
}
