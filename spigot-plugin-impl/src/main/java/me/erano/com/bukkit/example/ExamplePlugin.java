package me.erano.com.bukkit.example;

import me.erano.com.bukkit.plugin.java.JavaPlugin;

/*
 * Gercek bir Bukkit plugin'inin ana sinifi (plugin.yml'deki "main:") tam olarak boyle yazilir:
 * public no-arg kurucu, JavaPlugin'i extend et, onEnable()'da Listener'larini
 * getServer().getPluginManager() uzerinden kaydet.
 */
public class ExamplePlugin extends JavaPlugin {

    @Override
    public void onLoad() {
        getLogger().info("onLoad");
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ExampleListener(), this);
        getLogger().info("onEnable: listener kaydedildi");
    }

    @Override
    public void onDisable() {
        getLogger().info("onDisable");
    }
}
