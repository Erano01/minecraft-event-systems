package me.erano.com.bukkit;

import me.erano.com.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/*
 * Gercek org.bukkit.Server 130+ metot bildirir (dunya/oyuncu yonetimi, BukkitScheduler,
 * ServicesManager, Messenger, ban listeleri, recipe sistemi...). Burada SADECE event/plugin
 * hattinin gercekte cagirdigi 3 metot var:
 *  - getPluginManager(): JavaPluginLoader (PluginEnable/DisableEvent, useTimings) ve
 *    plugin'ler (getServer().getPluginManager().registerEvents) kullanir.
 *  - getLogger(): SimplePluginManager.fireEvent hata loglari ve PluginLogger'in parent'i.
 *  - isPrimaryThread(): SimplePluginManager.callEvent'in thread dogrulamasi.
 * Implementasyonu gercekte CraftServer'dir (server jar'inda, OBC); bizde spigot-plugin-impl
 * icindeki DemoServer.
 */
public interface Server {

    @NotNull
    Logger getLogger();

    @NotNull
    PluginManager getPluginManager();

    boolean isPrimaryThread();
}
