package me.erano.com.bukkit.example;

import me.erano.com.bukkit.entity.Player;
import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.Listener;
import me.erano.com.bukkit.event.player.AsyncPlayerChatEvent;
import me.erano.com.bukkit.event.player.PlayerJoinEvent;
import me.erano.com.bukkit.example.event.AsyncPingEvent;
import me.erano.com.bukkit.plugin.EventExecutor;
import me.erano.com.bukkit.plugin.InvalidPluginException;
import me.erano.com.bukkit.plugin.Plugin;
import me.erano.com.bukkit.plugin.PluginDescriptionFile;
import me.erano.com.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * "Sunucu" tarafi (bizim kodumuz). Gercek Bukkit'te plugin'i yukleyip enable eden ve fiili oyun
 * olaylarini (bir oyuncu katildiginda PlayerJoinEvent gibi) tetikleyen PLUGIN DEGIL, sunucunun
 * kendisidir (CraftBukkit/NMS). ExamplePlugin sadece dinler.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException, InvalidPluginException {
        DemoServer server = new DemoServer();

        System.out.println("=== 0) Plugin yukleme ve enable (CraftServer.loadPlugins/enablePlugins sirasi) ===");
        // Gercekte bu bilgi plugin jar'indaki plugin.yml'den okunur.
        Plugin plugin = server.loadPlugin(new PluginDescriptionFile("ExamplePlugin", "1.0", "me.erano.com.bukkit.example.ExamplePlugin"));
        server.enablePlugins(); // -> SimplePluginManager.enablePlugin -> JavaPluginLoader.enablePlugin
                                //    -> setEnabled(true) -> onEnable() -> registerEvents -> PluginEnableEvent

        Player player = new DemoPlayer("Erano");

        System.out.println();
        System.out.println("=== 1) Senkron event, ana thread'de ===");
        server.getPluginManager().callEvent(new PlayerJoinEvent(player, player.getName() + " joined the game"));

        System.out.println();
        System.out.println("=== 2) Ana thread'den ASYNC event tetiklemeyi denemek (reddedilmesi beklenir) ===");
        try {
            chat(server, player, "merhaba", true);
        } catch (IllegalStateException e) {
            System.out.println("Beklendigi gibi reddedildi: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== 3) Async event'i ag thread'inden tetiklemek (+ Cancellable / ignoreCancelled) ===");
        Thread netty = new Thread(() -> {
            chat(server, player, "herkese merhaba", true);
            chat(server, player, "bu bir kufur", true);
        }, "Netty Server IO #1");
        netty.start();
        netty.join();

        System.out.println();
        System.out.println("=== 4) Concurrency demo: HandlerList uzerinde concurrent register/unregister + concurrent async dispatch ===");
        concurrencyDemo(server, plugin);

        System.out.println();
        System.out.println("=== 5) Plugin'leri devre disi birakmak ===");
        server.disablePlugins(); // -> PluginDisableEvent -> setEnabled(false) -> onDisable() -> HandlerList.unregisterAll(plugin)
        System.out.println("PlayerJoinEvent icin kayitli listener sayisi: " + PlayerJoinEvent.getHandlerList().getRegisteredListeners().length);
    }

    /*
     * ServerGamePacketListenerImpl.chat(String, PlayerChatMessage, boolean async)'in sadelesmis
     * hali: event'i olustur, PluginManager'a ver, iptal edilmediyse listener'larin degistirmis
     * olabilecegi format ile mesaji yayinla (bizde: konsola yaz).
     */
    private static void chat(@NotNull DemoServer server, @NotNull Player player, @NotNull String message, boolean async) {
        Set<Player> recipients = new HashSet<>();
        recipients.add(player);
        AsyncPlayerChatEvent event = new AsyncPlayerChatEvent(async, player, message, recipients);
        server.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        server.getLogger().info(String.format(event.getFormat(), event.getPlayer().getName(), event.getMessage()));
    }

    /*
     * HandlerList'in thread-guvenligi mekanizmasini (volatile 'handlers' dizisi + synchronized
     * register/unregister/bake) gercek contention altinda calistiriyoruz:
     *  - "publisher" thread'leri surekli AsyncPingEvent tetikliyor -> her seferinde
     *    HandlerList.getRegisteredListeners() (spin/retry) okunuyor.
     *  - "churn" thread'leri es zamanli olarak HandlerList'e register/unregister yapip
     *    'handlers' alanini surekli null'a dusuruyor (cache invalidation).
     * Beklenen sonuc: hicbir ArrayIndexOutOfBounds/NullPointerException olmadan, kayitli
     * PingCounterListener her event'i dogru sayar - volatile alan uzerinden safe publication
     * (JCiP, Ch. 3).
     */
    private static void concurrencyDemo(@NotNull DemoServer server, @NotNull Plugin plugin) throws InterruptedException {
        int publisherCount = 4;
        int pingsPerPublisher = 500;
        int churnThreadCount = 2;

        AtomicInteger delivered = new AtomicInteger();
        AtomicBoolean churnRunning = new AtomicBoolean(true);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch publishersDone = new CountDownLatch(publisherCount);

        server.getPluginManager().registerEvents(new PingCounterListener(delivered), plugin);

        List<Thread> churnThreads = new ArrayList<>();
        for (int i = 0; i < churnThreadCount; i++) {
            Thread t = new Thread(() -> {
                await(startLatch);
                EventExecutor noop = (l, event) -> {
                };
                while (churnRunning.get()) {
                    RegisteredListener rl = new RegisteredListener(new Listener() {
                    }, noop, EventPriority.NORMAL, plugin, false);
                    AsyncPingEvent.getHandlerList().register(rl);
                    AsyncPingEvent.getHandlerList().unregister(rl);
                }
            }, "handlerlist-churn-" + i);
            t.setDaemon(true);
            churnThreads.add(t);
            t.start();
        }

        List<Thread> publishers = new ArrayList<>();
        for (int i = 0; i < publisherCount; i++) {
            int id = i;
            Thread t = new Thread(() -> {
                await(startLatch);
                for (int p = 0; p < pingsPerPublisher; p++) {
                    server.getPluginManager().callEvent(new AsyncPingEvent());
                }
                publishersDone.countDown();
            }, "ping-publisher-" + id);
            publishers.add(t);
            t.start();
        }

        startLatch.countDown();
        publishersDone.await();
        churnRunning.set(false);

        int expected = publisherCount * pingsPerPublisher;
        System.out.println(expected + " async event gonderildi, " + delivered.get()
                + " tanesi HandlerList surekli baska thread'lerce mutate edilirken PingCounterListener'a dogru sekilde ulasti.");
    }

    private static void await(@NotNull CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
