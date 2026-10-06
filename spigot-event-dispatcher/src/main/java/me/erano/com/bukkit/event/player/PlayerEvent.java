package me.erano.com.bukkit.event.player;

import me.erano.com.bukkit.entity.Player;
import me.erano.com.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class PlayerEvent extends Event {
    protected Player player;

    public PlayerEvent(@NotNull Player who) {
        this.player = who;
    }

    PlayerEvent(@NotNull Player who, boolean async) {
        super(async);
        this.player = who;
    }

    @NotNull
    public final Player getPlayer() {
        return this.player;
    }
}
