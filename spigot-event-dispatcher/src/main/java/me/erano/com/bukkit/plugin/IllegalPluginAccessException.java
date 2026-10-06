package me.erano.com.bukkit.plugin;

public class IllegalPluginAccessException extends RuntimeException {
    public IllegalPluginAccessException() {
    }

    public IllegalPluginAccessException(String msg) {
        super(msg);
    }
}
