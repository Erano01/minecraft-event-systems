package me.erano.com.spigot.event;

public interface Cancellable {
    boolean isCancelled();

    void setCancelled(boolean var1);
}
