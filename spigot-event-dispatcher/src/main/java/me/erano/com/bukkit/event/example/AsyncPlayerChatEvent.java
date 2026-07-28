package me.erano.com.bukkit.event.example;

import me.erano.com.bukkit.event.Cancellable;
import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/*
 * Gercek Bukkit'in AsyncPlayerChatEvent'i tam olarak boyle davranir: constructor'da
 * super(true) ile "async" isaretlenir cunku bu event ag paketini isleyen thread'lerden
 * (netty) tetiklenir - ana thread'den DEGIL. PluginManager.callEvent bunu dogrular
 * (bkz. SimplePluginManager.callEvent): main thread'den cagirilirsa IllegalStateException.
 */
public class AsyncPlayerChatEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String playerName;
    private final String message;
    private boolean cancelled;

    public AsyncPlayerChatEvent(@NotNull String playerName, @NotNull String message) {
        super(true);
        this.playerName = playerName;
        this.message = message;
    }

    @NotNull
    public String getPlayerName() {
        return this.playerName;
    }

    @NotNull
    public String getMessage() {
        return this.message;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
