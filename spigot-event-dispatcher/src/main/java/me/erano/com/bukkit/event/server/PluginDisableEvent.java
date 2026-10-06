package me.erano.com.bukkit.event.server;

import me.erano.com.bukkit.event.HandlerList;
import me.erano.com.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PluginDisableEvent extends PluginEvent {
    private static final HandlerList handlers = new HandlerList();

    public PluginDisableEvent(@NotNull Plugin plugin) {
        super(plugin);
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
