package me.erano.com.bukkit.plugin.java;

import me.erano.com.bukkit.Server;
import me.erano.com.bukkit.plugin.PluginBase;
import me.erano.com.bukkit.plugin.PluginDescriptionFile;
import me.erano.com.bukkit.plugin.PluginLoader;
import me.erano.com.bukkit.plugin.PluginLogger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

/*
 * Gercek siniftan cikarilanlar: file/dataFolder/classLoader/newConfig/configFile alanlari ve
 * bunlara bagli metotlar (getConfig, saveConfig, saveResource, getResource, getClassLoader...),
 * komut sistemi (onCommand, onTabComplete, getCommand), world generation, statik
 * getPlugin(Class)/getProvidingPlugin(Class) (PluginClassLoader'a dayanir).
 *
 * Kurucu: gercek public no-arg kurucu "getClass().getClassLoader() instanceof PluginClassLoader"
 * degilse IllegalStateException firlatir, sonra PluginClassLoader.initialize(this) -> init(...)
 * cagirir. Bizde PluginClassLoader yok; kurucu bos, init(...)'i JavaPluginLoader.loadPlugin
 * cagiriyor (bkz. JavaPluginLoader). Plugin yazari icin fark yok: yine public no-arg kurucu.
 */
public abstract class JavaPlugin extends PluginBase {
    private boolean isEnabled = false;
    private PluginLoader loader = null;
    private Server server = null;
    private PluginDescriptionFile description = null;
    private boolean naggable = true;
    private PluginLogger logger = null;

    public JavaPlugin() {
    }

    @Override
    @NotNull
    public final PluginLoader getPluginLoader() {
        return this.loader;
    }

    @Override
    @NotNull
    public final Server getServer() {
        return this.server;
    }

    @Override
    public final boolean isEnabled() {
        return this.isEnabled;
    }

    @Override
    @NotNull
    public final PluginDescriptionFile getDescription() {
        return this.description;
    }

    /*
     * Enabled durumu degisince onEnable()/onDisable() otomatik cagrilir. Ayni paketteki
     * JavaPluginLoader.enablePlugin/disablePlugin cagirir (protected erisim, paket-ici).
     */
    protected final void setEnabled(boolean enabled) {
        if (this.isEnabled != enabled) {
            this.isEnabled = enabled;
            if (this.isEnabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    final void init(@NotNull PluginLoader loader, @NotNull Server server, @NotNull PluginDescriptionFile description) {
        this.loader = loader;
        this.server = server;
        this.description = description;
        this.logger = new PluginLogger(this);
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public final boolean isNaggable() {
        return this.naggable;
    }

    @Override
    public final void setNaggable(boolean canNag) {
        this.naggable = canNag;
    }

    @Override
    @NotNull
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    @NotNull
    public String toString() {
        return this.description.getFullName();
    }
}
