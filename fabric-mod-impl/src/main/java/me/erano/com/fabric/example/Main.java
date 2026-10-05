package me.erano.com.fabric.example;

import me.erano.com.fabric.fabric.api.event.Event;
import me.erano.com.fabric.fabric.api.event.EventFactory;
import me.erano.com.fabric.loader.impl.FabricLoaderImpl;
import me.erano.com.fabric.loader.impl.ModContainerImpl;
import me.erano.com.fabric.loader.impl.game.minecraft.Hooks;
import me.erano.com.fabric.minecraft.network.chat.ChatType;
import me.erano.com.fabric.minecraft.network.chat.PlayerChatMessage;
import me.erano.com.fabric.minecraft.server.MinecraftServer;
import me.erano.com.fabric.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Demo duzenegi: dedicated server'in baslangicini taklit eder.
 *  - main thread: loader mod'lari bulur, entrypoint'leri sirayla cagirir (Hooks.startServer).
 *  - "Netty Server IO" thread: gelen chat mesajlarini server'in is kuyruguna birakir (server.execute).
 *  - "Server thread": tick dongusu; lifecycle/tick/chat event'leri hep bu thread'de tetiklenir.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 1) Loader: fabric.mod.json kesfi + entrypoint'ler (sirayla, cagiran thread'de) ===");
        FabricLoaderImpl loader = FabricLoaderImpl.INSTANCE;
        loader.load();
        for (ModContainerImpl mod : loader.getAllMods()) {
            System.out.println("  bulunan mod: " + mod.getMetadata().getId() + " (" + mod.getInfo().getEntrypointKeys() + ")");
        }
        MinecraftServer server = new MinecraftServer();
        Hooks.startServer(server);

        System.out.println();
        System.out.println("=== 2) Diger thread'den server thread'ine is devri (MinecraftServer bir Executor) ===");
        Thread netty = new Thread(() -> {
            ServerPlayer erano = new ServerPlayer("Erano");
            ChatType.Bound chat = new ChatType.Bound("chat");
            Log.line("chat paketi alindi, server.execute(...) ile kuyruga birakiliyor");
            server.execute(() -> server.getPlayerList().broadcastChatMessage(new PlayerChatMessage("herkese merhaba"), erano, chat));
            server.execute(() -> server.getPlayerList().broadcastChatMessage(new PlayerChatMessage("spam spam spam"), erano, chat));
        }, "Netty Server IO");
        netty.start();
        netty.join();

        System.out.println();
        System.out.println("=== 3) Server: lifecycle fazlari, tick event'leri, chat (hepsi Server thread'de) ===");
        Thread serverThread = new Thread(() -> server.runServer(2), "Server thread");
        serverThread.start();
        serverThread.join();

        System.out.println();
        System.out.println("=== 4) Concurrency demo: concurrent register altinda kilitsiz invoker() ===");
        concurrencyDemo();
    }

    @FunctionalInterface
    interface Ping {
        void onPing();
    }

    /*
     * Okuma yolu: invoker() = tek bir volatile okuma. Yazma yolu: register() kilit altinda yeni bir dizi
     * olusturup invoker'i yeniden kurar. Unregister olmadigi icin sadece ekleme yapiliyor.
     * Beklenen: (a) bastan kayitli sayac listener'i her ping'i bir kez gorur, (b) hicbir kayit kaybolmaz:
     * sonda tek bir ping'e yanit veren listener sayisi = 1 + registrar * kayit.
     */
    private static void concurrencyDemo() throws InterruptedException {
        int publisherCount = 4;
        int pingsPerPublisher = 500;
        int registrarCount = 2;
        int registrationsPerRegistrar = 250;

        AtomicInteger counted = new AtomicInteger();
        AtomicInteger responders = new AtomicInteger();
        Event<Ping> ping = EventFactory.createArrayBacked(Ping.class, listeners -> () -> {
            for (Ping listener : listeners) {
                listener.onPing();
            }
        });
        ping.register(counted::incrementAndGet);
        ping.register(responders::incrementAndGet);

        CountDownLatch start = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < registrarCount; i++) {
            threads.add(new Thread(() -> {
                await(start);
                for (int r = 0; r < registrationsPerRegistrar; r++) {
                    ping.register(responders::incrementAndGet);
                }
            }, "registrar-" + i));
        }
        for (int i = 0; i < publisherCount; i++) {
            threads.add(new Thread(() -> {
                await(start);
                for (int p = 0; p < pingsPerPublisher; p++) {
                    ping.invoker().onPing();
                }
            }, "ping-publisher-" + i));
        }
        threads.forEach(Thread::start);
        start.countDown();
        for (Thread t : threads) {
            t.join();
        }

        int expectedPings = publisherCount * pingsPerPublisher;
        System.out.println("  " + expectedPings + " ping gonderildi, sayac listener'i " + counted.get() + " tanesini gordu.");

        responders.set(0);
        ping.invoker().onPing();
        int expectedListeners = 1 + registrarCount * registrationsPerRegistrar;
        System.out.println("  Son ping'e yanit veren listener: " + responders.get() + " (beklenen " + expectedListeners + ", kayip kayit yok).");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
