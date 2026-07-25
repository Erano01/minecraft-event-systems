package me.erano.com.bukkit.plugin;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.EventException;
import me.erano.com.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public interface EventExecutor {
    void execute(@NotNull Listener var1, @NotNull Event var2) throws EventException;
}
