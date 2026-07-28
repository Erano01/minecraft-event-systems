package me.erano.com.bukkit.event.example;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/*
 * Gercek Bukkit'te her somut Event alt sinifi kendi statik HandlerList'ini tasir ve
 * "public static HandlerList getHandlerList()" metodunu bildirir. Bu metot HICBIR arayuzde
 * tanimli DEGILDIR (statik metotlar Java'da polimorfik degildir, override edilemez) -
 * SimplePluginManager bunu reflection ile arayip cagirir
 * (bkz. SimplePluginManager.getEventListeners/getRegistrationClass). Yani bu compiler'in
 * zorunlu kildigi bir sozlesme degil, sadece bir Bukkit KONVANSIYONU'dur; unutulursa
 * kayit sirasinda calisma zamaninda IllegalPluginAccessException firlar.
 */
public class PlayerJoinEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final String playerName;

    public PlayerJoinEvent(@NotNull String playerName) {
        this.playerName = playerName;
    }

    @NotNull
    public String getPlayerName() {
        return this.playerName;
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
