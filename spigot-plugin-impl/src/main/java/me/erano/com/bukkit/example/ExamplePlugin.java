package me.erano.com.bukkit.example;

import me.erano.com.bukkit.plugin.java.JavaPlugin;

/*
 * Gercek bir Bukkit plugin'inin ana sinifi (plugin.yml'deki "main:") tam olarak boyle
 * yazilir: JavaPlugin'i extend et, onEnable()'da kendi Listener'larini kaydet. Tek fark:
 * gercekte "getServer().getPluginManager().registerEvents(...)" yazilir, bizde Server'in
 * tamami kapsam disi oldugundan JavaPlugin.getPluginManager() kisayolunu kullaniyoruz
 * (bkz. JavaPlugin.java basindaki yorum).
 */
public class ExamplePlugin extends JavaPlugin {

    public ExamplePlugin() {
        super("example-plugin");
    }

    @Override
    public void onEnable() {
        getPluginManager().registerEvents(new ExampleListener(), this);
        System.out.println(getName() + " enabled.");
    }

    @Override
    public void onDisable() {
        System.out.println(getName() + " disabled.");
    }
}
