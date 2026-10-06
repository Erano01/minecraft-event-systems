package me.erano.com.bukkit.example;

import me.erano.com.bukkit.event.EventHandler;
import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.Listener;
import me.erano.com.bukkit.event.player.AsyncPlayerChatEvent;
import me.erano.com.bukkit.event.player.PlayerJoinEvent;
import me.erano.com.bukkit.event.server.PluginDisableEvent;
import me.erano.com.bukkit.event.server.PluginEnableEvent;

/*
 * Event tipleri spigot-event-dispatcher'da (spigot-api rolu), Listener implementasyonu ise
 * plugin modulunde - gercek ekosistemdeki gibi.
 */
public class ExampleListener implements Listener {

    // registerEvents onEnable() icinde, PluginEnableEvent ise JavaPluginLoader.enablePlugin'de
    // setEnabled(true)'DAN SONRA firlatildigi icin plugin kendi enable event'ini de alir.
    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        System.out.println("[PluginEnableEvent]  " + event.getPlugin().getName() + " (thread=" + Thread.currentThread().getName() + ")");
    }

    // PluginDisableEvent, setEnabled(false)'TAN ONCE firlatilir: plugin hala enabled oldugundan
    // fireEvent'in isEnabled() filtresine takilmaz, kendi disable event'ini de alir.
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        System.out.println("[PluginDisableEvent] " + event.getPlugin().getName());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoinLowest(PlayerJoinEvent event) {
        System.out.println("[LOWEST]  " + event.getPlayer().getName() + " joined (thread=" + Thread.currentThread().getName() + ")");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onJoinNormal(PlayerJoinEvent event) {
        event.setJoinMessage("Welcome, " + event.getPlayer().getName() + "!");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinMonitor(PlayerJoinEvent event) {
        System.out.println("[MONITOR] final joinMessage: " + event.getJoinMessage());
    }

    // LOW oncelikte sakincali mesajlari iptal ediyoruz.
    @EventHandler(priority = EventPriority.LOW)
    public void onChatFilter(AsyncPlayerChatEvent event) {
        if (event.getMessage().contains("kufur")) {
            event.setCancelled(true);
        }
    }

    // ignoreCancelled = true: event LOW'da iptal edildiyse bu handler HIC calismaz.
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        event.setFormat("[chat] <%1$s> %2$s");
        System.out.println("[NORMAL]  " + event.getPlayer().getName() + " format degistirdi (thread=" + Thread.currentThread().getName() + ")");
    }

    // ignoreCancelled = false (varsayilan): iptal edilse bile calisir, ornegin loglama icin.
    @EventHandler(priority = EventPriority.MONITOR)
    public void onChatAudit(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            System.out.println("[MONITOR] chat from " + event.getPlayer().getName() + " was CANCELLED");
        }
    }
}
