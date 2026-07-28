package me.erano.com.bukkit.plugin;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/*
 * Gercek org.bukkit.plugin.PluginManager arayuzu 26 metot bildirir. Bunlarin buyuk cogunlugu
 * event dispatch/concurrency ile ilgisiz iki ayri alt sisteme ait:
 *  - Plugin yasam donguesu: registerInterface, getPlugin(s), isPluginEnabled, loadPlugin(s),
 *    enablePlugin, disablePlugin, disablePlugins, clearPlugins -> jar/dosya yukleme ve
 *    bagimlilik cozumleme mekanizmasina ait (PluginLoader + dependency graph). Bizde bu
 *    sorumlulugun kucuk, event-disi bir kismi JavaPluginLoader.enablePlugin/disablePlugin'e
 *    tasindi (bkz. plugin.java.JavaPluginLoader) - PluginManager'a hic girmiyor, cunku gercekte
 *    de PluginManager bunu PluginLoader'a devreder.
 *  - Permission alt sistemi: getPermission, addPermission, removePermission,
 *    getDefaultPermissions, recalculatePermissionDefaults, subscribeToPermission,
 *    unsubscribeFromPermission, getPermissionSubscriptions, subscribeToDefaultPerms,
 *    unsubscribeFromDefaultPerms, getDefaultPermSubscriptions, getPermissions, useTimings ->
 *    Permission/Permissible yetkilendirme mekanizmasina ait.
 * Bunlarin hicbiri bizim calisma konumuz olan event dispatch + concurrency mekanizmasinin
 * parcasi degil, o yuzden implement edilmiyor/dallandirilmiyor. Asagida sadece event dispatch
 * hattini kuran 4 metot birebir aliniyor.
 */
public interface PluginManager {

    void callEvent(@NotNull Event event) throws IllegalStateException;

    void registerEvents(@NotNull Listener listener, @NotNull Plugin plugin);

    void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor, @NotNull Plugin plugin);

    void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor, @NotNull Plugin plugin, boolean ignoreCancelled);
}
