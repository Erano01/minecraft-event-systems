package me.erano.com.bukkit.plugin;

import org.jetbrains.annotations.NotNull;

public abstract class PluginBase implements Plugin {
    @Override
    public final int hashCode() {
        return getName().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof Plugin)) {
            return false;
        }
        return getName().equals(((Plugin) obj).getName());
    }

    @Override
    @NotNull
    public final String getName() {
        return getDescription().getName();
    }
}
