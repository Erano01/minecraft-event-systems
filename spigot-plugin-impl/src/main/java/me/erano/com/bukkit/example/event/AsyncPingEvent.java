package me.erano.com.bukkit.example.event;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/*
 * Gercek Bukkit'te YOK: plugin'in kendi tanimladigi custom event (gercekte de custom event'ler
 * plugin jar'inda yasar ve ayni HandlerList konvansiyonuna uyar). Kasitli olarak sade tutuldu
 * (Cancellable degil, ekstra alan yok): tek amaci HandlerList'in concurrency mekanizmasini
 * (volatile 'handlers' dizisi + synchronized register/unregister/bake) yuksek contention
 * altinda gozlemlemek. Bkz. Main.concurrencyDemo().
 */
public class AsyncPingEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public AsyncPingEvent() {
        super(true);
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
