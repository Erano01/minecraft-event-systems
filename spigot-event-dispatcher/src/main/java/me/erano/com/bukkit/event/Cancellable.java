package me.erano.com.bukkit.event;

public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean var1);
}
