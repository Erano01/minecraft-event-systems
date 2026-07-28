package me.erano.com.bukkit.plugin.java;

import me.erano.com.bukkit.plugin.PluginBase;
import me.erano.com.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

/*
 * Gercek org.bukkit.plugin.java.JavaPlugin, PluginClassLoader/Server/PluginDescriptionFile/
 * File(dataFolder)/FileConfiguration/PluginLogger alanlarini tasir; no-arg constructor'i
 * "classLoader instanceof PluginClassLoader" kontrolu yapip PluginClassLoader.initialize(this)
 * cagirir - yani bir plugin.jar'in GERCEK bir classloader tarafindan yuklenmis olmasini
 * ZORUNLU kilar. Bizde jar/classloading pipeline'i olmadigindan (bkz. Plugin.java ve
 * JavaPluginLoader.java basindaki yorumlar), bu init mekanizmasini basit bir
 * "isim dogrudan constructor'dan gelir" seklinde sadelestiriyoruz - asagidaki constructor'a
 * bakiniz, bu bilincli bir sapmadir.
 *
 * getServer().getPluginManager() zincirinin (Server'in TAMAMI kapsam disi, bkz. Plugin.java)
 * yerine, sadece event kaydi icin gereken PluginManager referansini dogrudan tasiyoruz
 * (attachPluginManager/getPluginManager). Gercek Bukkit'te bu baglama islemini
 * JavaPluginLoader.loadPlugin(File) yapar; bizde jar yukleme olmadigindan bu "bootstrap"
 * adimini CLI demo'nun kendisi (bkz. spigot-plugin-impl) ustleniyor, o yuzden
 * attachPluginManager PUBLIC (bilincli sapma). setEnabled(boolean) ise gercekteki gibi
 * PROTECTED kaldi: onu da gercekteki gibi ayni paketteki JavaPluginLoader.enablePlugin/
 * disablePlugin cagiriyor (bkz. plugin.java.JavaPluginLoader).
 */
public abstract class JavaPlugin extends PluginBase {
    private final String name;
    private boolean isEnabled = false;
    private boolean naggable = true;
    private PluginManager pluginManager;

    protected JavaPlugin(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    @Override
    public final String getName() {
        return this.name;
    }

    @Override
    public final boolean isEnabled() {
        return this.isEnabled;
    }

    /*
     * Gercek Bukkit'teki mekanizma birebir: enabled durumu degistiginde onEnable()/onDisable()
     * OTOMATIK cagrilir. Bu, PluginManager.enablePlugin/disablePlugin -> PluginLoader.enable/
     * disablePlugin -> JavaPlugin.setEnabled(boolean) zincirinin bizim CLI'daki karsiligidir
     * (bkz. spigot-plugin-impl icindeki bootstrap).
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

    public final void attachPluginManager(@NotNull PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @NotNull
    protected final PluginManager getPluginManager() {
        if (this.pluginManager == null) {
            throw new IllegalStateException(getName() + " has not been attached to a PluginManager yet");
        }
        return this.pluginManager;
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

    @NotNull
    @Override
    public String toString() {
        return getName();
    }
}
