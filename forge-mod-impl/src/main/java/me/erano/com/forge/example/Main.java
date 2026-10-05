package me.erano.com.forge.example;

import me.erano.com.forge.event.ServerChatEvent;
import me.erano.com.forge.event.TickEvent;
import me.erano.com.forge.event.entity.player.PlayerEvent;
import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.fml.ModInfo;
import me.erano.com.forge.fml.ModLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * "Sunucu"yu temsil eder: FML'e mod'lari yukletir, sonra oyun event'lerini game bus'a post eder.
 * Gercekte bunu Forge'un patch'ledigi Minecraft kodu yapar; mod'lar sadece dinler.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        List<ModInfo> mods = List.of(
                new ModInfo(ExampleMod.MOD_ID, ExampleMod.class, List.of(), List.of(ModBusSubscriber.class)),
                new ModInfo(DependentMod.MOD_ID, DependentMod.class, List.of(ExampleMod.MOD_ID), List.of()),
                new ModInfo(IndependentMod.MOD_ID, IndependentMod.class, List.of(), List.of()));

        System.out.println("=== 1) Mod yukleme: lifecycle event'leri modloading-worker thread'lerinde paralel ===");
        System.out.println("-- CONSTRUCT");
        ModLoader.gatherAndInitializeMods(mods);
        System.out.println("-- COMMON_SETUP");
        ModLoader.loadMods();
        System.out.println("-- COMPLETE");
        ModLoader.finishMods();

        System.out.println();
        System.out.println("=== 2) Game bus: oncelik sirasi (HIGH -> NORMAL -> MONITOR) ===");
        PlayerEvent.PlayerLoggedInEvent.BUS.post(new PlayerEvent.PlayerLoggedInEvent("Erano"));

        System.out.println();
        System.out.println("=== 3) Iptal: true donen listener zinciri keser, MONITOR yine calisir ===");
        boolean cancelled = ServerChatEvent.BUS.post(new ServerChatEvent("Erano", "herkese merhaba", "herkese merhaba"));
        System.out.println("  post() -> " + cancelled);
        cancelled = ServerChatEvent.BUS.post(new ServerChatEvent("Erano", "spam spam spam", "spam spam spam"));
        System.out.println("  post() -> " + cancelled);

        System.out.println();
        System.out.println("=== 4) Concurrency demo: concurrent addListener/removeListener altinda post ===");
        concurrencyDemo();
    }

    /*
     * Okuma yolu (post -> VolatileCallSite.getTarget) kilitsiz, yazma yolu (addListener/removeListener)
     * backingList monitoru altinda. Publisher thread'ler surekli post ederken churn thread'ler ayni
     * bus'a listener ekleyip cikararak invoker'i surekli gecersiz kilar. Beklenen: kalici sayac
     * listener'i her event'i tam bir kez gorur, hicbir exception olmaz.
     */
    private static void concurrencyDemo() throws InterruptedException {
        int publisherCount = 4;
        int ticksPerPublisher = 500;
        int churnThreadCount = 2;

        EventBus<TickEvent.ServerTickEvent.Post> tickBus = TickEvent.ServerTickEvent.Post.BUS;
        AtomicInteger delivered = new AtomicInteger();
        tickBus.addListener(event -> delivered.incrementAndGet());

        AtomicBoolean churnRunning = new AtomicBoolean(true);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch publishersDone = new CountDownLatch(publisherCount);

        for (int i = 0; i < churnThreadCount; i++) {
            Thread t = new Thread(() -> {
                await(startLatch);
                while (churnRunning.get()) {
                    EventListener listener = tickBus.addListener(event -> {
                    });
                    tickBus.removeListener(listener);
                }
            }, "listener-churn-" + i);
            t.setDaemon(true);
            t.start();
        }

        List<Thread> publishers = new ArrayList<>();
        for (int i = 0; i < publisherCount; i++) {
            Thread t = new Thread(() -> {
                await(startLatch);
                for (int tick = 0; tick < ticksPerPublisher; tick++) {
                    tickBus.post(new TickEvent.ServerTickEvent.Post(() -> true));
                }
                publishersDone.countDown();
            }, "tick-publisher-" + i);
            publishers.add(t);
            t.start();
        }

        startLatch.countDown();
        publishersDone.await();
        churnRunning.set(false);

        int expected = publisherCount * ticksPerPublisher;
        System.out.println("  " + expected + " event post edildi, " + delivered.get()
                + " tanesi bus surekli baska thread'lerce degistirilirken sayac listener'ina ulasti.");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
