package me.erano.com.bukkit.event.player;

import me.erano.com.bukkit.entity.Player;
import me.erano.com.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
 * Her somut Event alt sinifi kendi statik HandlerList'ini tasir ve
 * "public static HandlerList getHandlerList()" bildirir. Bu metot HICBIR arayuzde tanimli
 * degildir (statik metotlar override edilemez); SimplePluginManager onu reflection ile arar
 * (getEventListeners/getRegistrationClass). Compiler'in zorunlu kildigi bir sozlesme degil,
 * bir Bukkit konvansiyonu: unutulursa kayit aninda IllegalPluginAccessException firlar.
 */
public class PlayerJoinEvent extends PlayerEvent {
    private static final HandlerList handlers = new HandlerList();
    private String joinMessage;

    public PlayerJoinEvent(@NotNull Player playerJoined, @Nullable String joinMessage) {
        super(playerJoined);
        this.joinMessage = joinMessage;
    }

    @Nullable
    public String getJoinMessage() {
        return this.joinMessage;
    }

    public void setJoinMessage(@Nullable String joinMessage) {
        this.joinMessage = joinMessage;
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
