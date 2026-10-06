package me.erano.com.bukkit.plugin;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/*
 * Gercek arayuzden tek sapma loadPlugin'in parametresi: gercekte
 *   Plugin loadPlugin(File file)  +  PluginDescriptionFile getPluginDescription(File file)
 * vardir; jar acilir, icindeki plugin.yml okunur, PluginClassLoader kurulur. Bizde plugin
 * jar'i yok, bu yuzden zincire plugin.yml'in zaten okunmus hali (PluginDescriptionFile) ile
 * giriyoruz ve getPluginDescription(File) yok. Diger 4 metot birebir.
 */
public interface PluginLoader {
    @NotNull
    Plugin loadPlugin(@NotNull PluginDescriptionFile description) throws InvalidPluginException;

    @NotNull
    Pattern[] getPluginFileFilters();

    @NotNull
    Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(@NotNull Listener listener, @NotNull Plugin plugin);

    void enablePlugin(@NotNull Plugin plugin);

    void disablePlugin(@NotNull Plugin plugin);
}
