package me.erano.com.forge.fml;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Function;
import java.util.function.Supplier;

// Paralel asamalarda thread-safe OLMAYAN isler (orn. vanilla registry'lere yazmak) buraya birakilir;
// asama bitince sync executor'da, yani main thread'de, sirayla calistirilir.
// JCiP: paylasilan state'e erisimi tek thread'e devretmek (thread confinement) icin bir is kuyrugu.
public class DeferredWorkQueue {
    private static final Map<ModLoadingStage, DeferredWorkQueue> workQueues = new HashMap<>();

    // Birden fazla worker thread ayni anda enqueueWork yapar -> concurrent kuyruk.
    private final ConcurrentLinkedDeque<TaskInfo> tasks = new ConcurrentLinkedDeque<>();
    private final ModLoadingStage modLoadingStage;

    public DeferredWorkQueue(ModLoadingStage modLoadingStage) {
        this.modLoadingStage = modLoadingStage;
        workQueues.put(modLoadingStage, this);
    }

    public static Optional<DeferredWorkQueue> lookup(Optional<ModLoadingStage> stage) {
        return Optional.ofNullable(workQueues.get(stage.orElse(null)));
    }

    public void runTasks() {
        if (tasks.isEmpty()) {
            return;
        }
        System.out.printf("  [%s] DeferredWorkQueue(%s): %d is calistiriliyor%n",
                Thread.currentThread().getName(), modLoadingStage, tasks.size());
        RuntimeException aggregate = new RuntimeException();
        for (TaskInfo ti : tasks) {
            ModLoadingContext.get().setActiveContainer(ti.owner);
            try {
                ti.future.exceptionally(t -> {
                    aggregate.addSuppressed(t);
                    return null;
                });
                ti.task.run();
            } finally {
                ModLoadingContext.get().setActiveContainer(null);
            }
        }
        if (aggregate.getSuppressed().length > 0) {
            throw aggregate;
        }
    }

    public CompletableFuture<Void> enqueueWork(ModContainer modInfo, Runnable work) {
        return enqueueWork(modInfo, taskInfo -> CompletableFuture.runAsync(work, r -> taskInfo.task = r));
    }

    public <T> CompletableFuture<T> enqueueWork(ModContainer modInfo, Supplier<T> work) {
        return enqueueWork(modInfo, taskInfo -> CompletableFuture.supplyAsync(work, r -> taskInfo.task = r));
    }

    // Is hemen calistirilmaz: CompletableFuture'in executor'u isi sadece TaskInfo.task'a yazar.
    // task/future alanlari volatile degil; ConcurrentLinkedDeque.add -> iterasyon arasindaki
    // happens-before ile main thread'e guvenle yayinlanir.
    private <T> CompletableFuture<T> enqueueWork(ModContainer modContainer, Function<TaskInfo, CompletableFuture<T>> function) {
        TaskInfo taskInfo = new TaskInfo(modContainer);
        CompletableFuture<T> future = function.apply(taskInfo);
        taskInfo.future = future;
        tasks.add(taskInfo);
        return future;
    }

    private static final class TaskInfo {
        private final ModContainer owner;
        private Runnable task;
        private CompletableFuture<?> future;

        private TaskInfo(ModContainer owner) {
            this.owner = owner;
        }
    }
}
