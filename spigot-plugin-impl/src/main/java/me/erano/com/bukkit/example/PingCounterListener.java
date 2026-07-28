package me.erano.com.bukkit.example;

import me.erano.com.bukkit.event.EventHandler;
import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.Listener;
import me.erano.com.bukkit.event.example.AsyncPingEvent;

import java.util.concurrent.atomic.AtomicInteger;

public class PingCounterListener implements Listener {
    private final AtomicInteger counter;

    public PingCounterListener(AtomicInteger counter) {
        this.counter = counter;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPing(AsyncPingEvent event) {
        this.counter.incrementAndGet();
    }
}
