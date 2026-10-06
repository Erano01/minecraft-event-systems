package me.erano.com.bukkit.plugin;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.Listener;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
 * Gercek org.bukkit.plugin.PluginManager 30 metot bildirir. Burada 16'si var: event kaydi/
 * dispatch (callEvent, registerEvents, registerEvent x2, useTimings) ve bu hatta dokunan plugin
 * yasam dongusu (registerInterface, getPlugin(s), isPluginEnabled x2, loadPlugin, enablePlugin,
 * disablePlugin(s), clearPlugins). enable/disable event sistemiyle dogrudan ilgili:
 * enablePlugin sonda HandlerList.bakeAll(), disablePlugin HandlerList.unregisterAll(plugin) cagirir.
 *
 * Disarida kalanlar:
 *  - loadPlugins(File) / loadPlugins(File[]): plugins/ klasorunu tarayip depend/softdepend/
 *    loadbefore graph'i ile yukleme sirasini cozme. Bizde plugin klasoru/jar yok.
 *  - 13 permission metodu (getPermission, addPermission, subscribeToPermission...):
 *    Permission/Permissible yetkilendirme alt sistemi.
 * loadPlugin'in parametresi File yerine PluginDescriptionFile (bkz. PluginLoader).
 */
public interface PluginManager {
    void registerInterface(@NotNull Class<? extends PluginLoader> cls) throws IllegalArgumentException;

    @Nullable
    Plugin getPlugin(@NotNull String name);

    @NotNull
    Plugin[] getPlugins();

    boolean isPluginEnabled(@NotNull String name);

    @Contract("null -> false")
    boolean isPluginEnabled(@Nullable Plugin plugin);

    @Nullable
    Plugin loadPlugin(@NotNull PluginDescriptionFile description) throws InvalidPluginException;

    void disablePlugins();

    void clearPlugins();

    void callEvent(@NotNull Event event) throws IllegalStateException;

    void registerEvents(@NotNull Listener listener, @NotNull Plugin plugin);

    void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor, @NotNull Plugin plugin);

    void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor, @NotNull Plugin plugin, boolean ignoreCancelled);

    void enablePlugin(@NotNull Plugin plugin);

    void disablePlugin(@NotNull Plugin plugin);

    boolean useTimings();
}
