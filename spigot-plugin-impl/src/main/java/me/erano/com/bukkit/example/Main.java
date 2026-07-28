package me.erano.com.bukkit.example;

import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.Listener;
import me.erano.com.bukkit.event.example.AsyncPingEvent;
import me.erano.com.bukkit.event.example.AsyncPlayerChatEvent;
import me.erano.com.bukkit.event.example.PlayerJoinEvent;
import me.erano.com.bukkit.plugin.EventExecutor;
import me.erano.com.bukkit.plugin.RegisteredListener;
import me.erano.com.bukkit.plugin.SimplePluginManager;
import me.erano.com.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Bu sinif "sunucu"yu (bootstrap) temsil ediyor. Gercek Bukkit'te bir plugin'i yukleyip
 * enable eden ve fiili oyun olaylarini (bir oyuncu gercekten katildiginda PlayerJoinEvent
 * gibi) tetikleyen sey PLUGIN DEGIL, sunucunun kendisidir (CraftBukkit/NMS katmani).
 * ExamplePlugin sadece dinler (bkz. ExamplePlugin.onEnable -> registerEvents). O yuzden
 * event tetikleme kodu burada, plugin sinifinin DISINDA.
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {
        SimplePluginManager pluginManager = new SimplePluginManager();
        JavaPluginLoader pluginLoader = new JavaPluginLoader();

        ExamplePlugin plugin = new ExamplePlugin();
        plugin.attachPluginManager(pluginManager);
        pluginLoader.enablePlugin(plugin); // -> setEnabled(true) -> onEnable() -> registerEvents(...)

        System.out.println("=== 1) Senkron event, ana thread'de ===");
        pluginManager.callEvent(new PlayerJoinEvent("Erano"));

        System.out.println();
        System.out.println("=== 2) Ana thread'den ASYNC event tetiklemeyi denemek (reddedilmesi beklenir) ===");
        try {
            pluginManager.callEvent(new AsyncPlayerChatEvent("Erano", "merhaba"));
        } catch (IllegalStateException e) {
            System.out.println("Beklendigi gibi reddedildi: " + e.getMessage());
        }

        System.out.println();
        System.out.println("=== 3) Async event'i dogru sekilde, ayri bir thread'den tetiklemek ===");
        Thread chatThread = new Thread(() ->
                pluginManager.callEvent(new AsyncPlayerChatEvent("Erano", "herkese merhaba")), "chat-worker");
        chatThread.start();
        chatThread.join();

        System.out.println();
        System.out.println("=== 4) Concurrency demo: HandlerList uzerinde concurrent register/unregister + concurrent async dispatch ===");
        concurrencyDemo(pluginManager, plugin);

        System.out.println();
        System.out.println("=== 5) Plugin devre disi birakiliyor ===");
        pluginLoader.disablePlugin(plugin);
    }

    /*
     * HandlerList'in gercek Bukkit'ten birebir tasidigimiz thread-guvenligi mekanizmasini
     * (volatile 'handlers' dizisi + synchronized register/unregister/bake, bkz.
     * spigot-event-dispatcher: event.HandlerList) gercek contention altinda calistiriyoruz:
     *  - "publisher" thread'leri surekli AsyncPingEvent tetikliyor -> her seferinde
     *    HandlerList.getRegisteredListeners() (spin/retry) okunuyor.
     *  - "churn" thread'leri es zamanli olarak HandlerList'e register/unregister yapip
     *    'handlers' alanini surekli null'a dusuruyor (cache invalidation).
     * Beklenen sonuc: hicbir ArrayIndexOutOfBounds/NullPointerException olmadan, gercekten
     * kayitli olan PingCounterListener yine de her event icin dogru sayilir - bu da volatile
     * alan uzerinden "safe publication"in calistigini kanitliyor (JCiP, Ch. 3).
     */
    private static void concurrencyDemo(SimplePluginManager pluginManager, ExamplePlugin plugin) throws InterruptedException {
        int publisherCount = 4;
        int pingsPerPublisher = 500;
        int churnThreadCount = 2;

        AtomicInteger delivered = new AtomicInteger();
        AtomicBoolean churnRunning = new AtomicBoolean(true);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch publishersDone = new CountDownLatch(publisherCount);

        pluginManager.registerEvents(new PingCounterListener(delivered), plugin);

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
                    pluginManager.callEvent(new AsyncPingEvent());
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
