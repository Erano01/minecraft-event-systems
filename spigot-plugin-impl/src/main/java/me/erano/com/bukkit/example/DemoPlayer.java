package me.erano.com.bukkit.example;

import me.erano.com.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// Gercekte Player'i implemente eden CraftPlayer'in (server jar'i, OBC) demo karsiligi; bizim kodumuz.
public class DemoPlayer implements Player {
    private final String name;

    public DemoPlayer(@NotNull String name) {
        this.name = name;
    }

    @Override
    @NotNull
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.name;
    }
}
