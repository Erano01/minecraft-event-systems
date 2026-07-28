package me.erano.com.bukkit.plugin;

import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.Listener;
import me.erano.com.bukkit.event.example.AsyncPingEvent;
import me.erano.com.bukkit.event.example.AsyncPlayerChatEvent;
import me.erano.com.bukkit.event.example.ExampleListener;
import me.erano.com.bukkit.event.example.PingCounterListener;
import me.erano.com.bukkit.event.example.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/*
 * Gercek Bukkit'te bu rolu "Plugin" arayuzu (+ "JavaPlugin" temel sinifi) ustlenir:
 * PluginDescriptionFile (isim/versiyon/yazar), PluginLoader referansi, FileConfiguration,
 * DataFolder, Logger gibi cok sayida sorumlulugu var. Bizim CLI calismamizda tek bir
 * "Application" ornegi hem programin giris noktasi hem de kendi uzerine listener
 * kaydedecegimiz "sahip" (plugin) rolunu oynuyor - tipki gercek bir JavaPlugin'in
 * onEnable() icinde "getServer().getPluginManager().registerEvents(this, this)" cagirmasi gibi.
 * Bu yuzden sadece event dispatch'in ihtiyac duydugu iki alan yeterli: isim ve enabled durumu.
 */
public class Application {
    private final String name;
    private volatile boolean enabled;

    public Application(@NotNull String name) {
        this.name = name;
        this.enabled = true;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public static void main(String[] args) throws InterruptedException {
        SimplePluginManager pluginManager = new SimplePluginManager();
        Application app = new Application("study-app");
        ExampleListener listener = new ExampleListener();
        pluginManager.registerEvents(listener, app);

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
        concurrencyDemo(pluginManager, app);
    }

    /*
     * HandlerList'in gercek Bukkit'ten birebir tasidigimiz thread-guvenligi mekanizmasini
     * (volatile 'handlers' dizisi + synchronized register/unregister/bake, bkz. HandlerList.java)
     * gercek contention altinda calistiriyoruz:
     *  - "publisher" thread'leri surekli AsyncPingEvent tetikliyor -> her seferinde
     *    HandlerList.getRegisteredListeners() (spin/retry) okunuyor.
     *  - "churn" thread'leri es zamanli olarak HandlerList'e register/unregister yapip
     *    'handlers' alanini surekli null'a dusuruyor (cache invalidation).
     * Beklenen sonuc: hicbir ArrayIndexOutOfBounds/NullPointerException olmadan, gercekten
     * kayitli olan PingCounterListener yine de her event icin dogru sayilir - bu da volatile
     * alan uzerinden "safe publication"in calistigini kanitliyor (JCiP, Ch. 3).
     */
    private static void concurrencyDemo(SimplePluginManager pluginManager, Application app) throws InterruptedException {
        int publisherCount = 4;
        int pingsPerPublisher = 500;
        int churnThreadCount = 2;

        AtomicInteger delivered = new AtomicInteger();
        AtomicBoolean churnRunning = new AtomicBoolean(true);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch publishersDone = new CountDownLatch(publisherCount);

        pluginManager.registerEvents(new PingCounterListener(delivered), app);

        List<Thread> churnThreads = new ArrayList<>();
        for (int i = 0; i < churnThreadCount; i++) {
            Thread t = new Thread(() -> {
                await(startLatch);
                EventExecutor noop = (l, event) -> {
                };
                while (churnRunning.get()) {
                    RegisteredListener rl = new RegisteredListener(new Listener() {
                    }, noop, EventPriority.NORMAL, app, false);
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
