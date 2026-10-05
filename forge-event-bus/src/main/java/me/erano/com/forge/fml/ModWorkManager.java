package me.erano.com.forge.fml;

import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

public final class ModWorkManager {
    private static final SyncExecutor syncExecutor = new SyncExecutor();

    private ModWorkManager() {
    }

    // Isleri hemen calistirmayan, kuyruga alan executor. Kuyrugu main thread drive() ile bosaltir.
    public interface DrivenExecutor extends Executor {
        void drive(Runnable ticker);
    }

    private record SyncExecutor(ConcurrentLinkedDeque<Runnable> tasks) implements DrivenExecutor {
        private SyncExecutor() {
            this(new ConcurrentLinkedDeque<>());
        }

        private boolean driveOne() {
            Runnable task = tasks.pollFirst();
            if (task != null) {
                task.run();
            }
            return task != null;
        }

        @Override
        public void execute(Runnable command) {
            tasks.addLast(command);
        }

        @Override
        public void drive(Runnable ticker) {
            ticker.run();
            while (driveOne()) {
            }
        }
    }

    public static DrivenExecutor syncExecutor() {
        return syncExecutor;
    }

    public static Executor parallelExecutor() {
        return LazyInit.PARALLEL_EXECUTOR;
    }

    private static final AtomicInteger workerCount = new AtomicInteger();

    // Gercekte isim "modloading-worker-" + thread.getPoolIndex(). Ancak factory icinde thread henuz
    // havuza kaydolmadigi icin getPoolIndex() hep 0 doner ve tum worker'lar ayni ismi alir.
    // Paralelligi demoda ayirt edebilmek icin burada sayac kullaniliyor.
    private static ForkJoinWorkerThread newForkJoinWorkerThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setName("modloading-worker-" + workerCount.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    }

    // Holder idiom: havuz ilk kullanimda, class init kilidiyle guvenle olusturulur.
    // Gercekte thread sayisi FMLConfig.MAX_THREADS'den gelir.
    private static final class LazyInit {
        private static final ForkJoinPool PARALLEL_EXECUTOR = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors(), ModWorkManager::newForkJoinWorkerThread, null, false);
    }
}
