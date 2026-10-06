package me.erano.com.bukkit.plugin;

import me.erano.com.bukkit.Server;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/*
 * Gercek org.bukkit.plugin.Plugin, TabExecutor'u extend eder ve 20 metot bildirir. Burada
 * event/plugin hattinin kullandigi 11'i var:
 *  - getDescription/getName: kimlik ve log mesajlari (getFullName, getAuthors).
 *  - getPluginLoader: SimplePluginManager.registerEvents/enablePlugin/disablePlugin bununla
 *    loader'a ulasir.
 *  - getServer/getLogger: plugin'in PluginManager'a ve log'a erisimi.
 *  - isEnabled/onLoad/onEnable/onDisable: yasam dongusu (fireEvent her listener icin isEnabled'a bakar).
 *  - isNaggable/setNaggable: fireEvent'teki AuthorNagException dali.
 *
 * Disarida kalanlar:
 *  - getDataFolder, getConfig, getResource, saveConfig, saveDefaultConfig, saveResource,
 *    reloadConfig -> config.yml/FileConfiguration (org.bukkit.configuration.*).
 *  - getDefaultWorldGenerator, getDefaultBiomeProvider -> world generation.
 *  - TabExecutor (onCommand/onTabComplete) -> komut sistemi.
 */
public interface Plugin {
    @NotNull
    PluginDescriptionFile getDescription();

    @NotNull
    PluginLoader getPluginLoader();

    @NotNull
    Server getServer();

    boolean isEnabled();

    void onDisable();

    void onLoad();

    void onEnable();

    boolean isNaggable();

    void setNaggable(boolean canNag);

    @NotNull
    Logger getLogger();

    @NotNull
    String getName();
}
