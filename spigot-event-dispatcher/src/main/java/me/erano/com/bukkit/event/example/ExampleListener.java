package me.erano.com.bukkit.event.example;

import me.erano.com.bukkit.event.EventHandler;
import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.Listener;

public class ExampleListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinLowest(PlayerJoinEvent event) {
        System.out.println("[LOWEST]  " + event.getPlayerName() + " joined (thread=" + Thread.currentThread().getName() + ")");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoinNormal(PlayerJoinEvent event) {
        System.out.println("[NORMAL]  Welcome, " + event.getPlayerName() + "!");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinMonitor(PlayerJoinEvent event) {
        System.out.println("[MONITOR] audit-log: " + event.getPlayerName() + " join processed");
    }

    // LOW oncelikte sakincali mesajlari iptal ediyoruz.
    @EventHandler(priority = EventPriority.LOW)
    public void onChatFilter(AsyncPlayerChatEvent event) {
        if (event.getMessage().contains("kufur")) {
            event.setCancelled(true);
        }
    }

    // ignoreCancelled = true: event LOW'de iptal edildiyse bu handler HIC calismaz.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        System.out.println("<" + event.getPlayerName() + "> " + event.getMessage() + " (thread=" + Thread.currentThread().getName() + ")");
    }

    // ignoreCancelled = false (varsayilan): iptal edilse bile calisir, ornegin loglama icin.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatAudit(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            System.out.println("[MONITOR] chat from " + event.getPlayerName() + " was CANCELLED");
        }
    }
}
