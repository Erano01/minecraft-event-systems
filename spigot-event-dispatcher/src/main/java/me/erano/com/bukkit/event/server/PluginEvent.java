package me.erano.com.bukkit.event.server;

import me.erano.com.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public abstract class PluginEvent extends ServerEvent {
    private final Plugin plugin;

    public PluginEvent(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    @NotNull
    public Plugin getPlugin() {
        return this.plugin;
    }
}
