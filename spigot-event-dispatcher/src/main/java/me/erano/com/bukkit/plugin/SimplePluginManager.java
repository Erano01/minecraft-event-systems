package me.erano.com.bukkit.plugin;

import com.google.common.base.Preconditions;
import me.erano.com.bukkit.Server;
import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.HandlerList;
import me.erano.com.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

/*
 * Event dispatch hatti (callEvent, fireEvent, registerEvents, registerEvent, getEventListeners,
 * getRegistrationClass) ve plugin yasam dongusu (registerInterface, loadPlugin, getPlugin(s),
 * isPluginEnabled, enablePlugin, disablePlugin(s), clearPlugins) gercek siniftan alindi.
 *
 * Gercek siniftan cikarilanlar (event sistemiyle ilgisiz alt sistemler):
 *  - SimpleCommandMap: enablePlugin'deki PluginCommandYamlParser.parse + commandMap.registerAll.
 *  - disablePlugin'deki server.getScheduler().cancelTasks, getServicesManager().unregisterAll,
 *    getMessenger().unregister*PluginChannel, world.removePluginChunkTickets adimlari.
 *  - loadPlugins(File/File[]) + dependencyGraph (MutableGraph) + checkUpdate (update klasoru).
 *  - permissions/defaultPerms/permSubs/defSubs ve tum permission metotlari.
 * loadPlugin dosya adini loader'in file filter'iyla (\.jar$) eslestirir; bizde dosya olmadigindan
 * kayitli loader'lar sirayla deneniyor (tek loader JavaPluginLoader, sonuc ayni).
 */
public final class SimplePluginManager implements PluginManager {
    private final Server server;
    private final Map<Pattern, PluginLoader> fileAssociations = new HashMap<>();
    private final List<Plugin> plugins = new ArrayList<>();
    private final Map<String, Plugin> lookupNames = new HashMap<>();
    private boolean useTimings = false;

    public SimplePluginManager(@NotNull Server instance) {
        this.server = instance;
    }

    @Override
    public void registerInterface(@NotNull Class<? extends PluginLoader> loader) throws IllegalArgumentException {
        if (PluginLoader.class.isAssignableFrom(loader)) {
            try {
                Constructor<? extends PluginLoader> constructor = loader.getConstructor(Server.class);
                PluginLoader instance = constructor.newInstance(this.server);
                Pattern[] patterns = instance.getPluginFileFilters();
                synchronized (this) {
                    for (Pattern pattern : patterns) {
                        this.fileAssociations.put(pattern, instance);
                    }
                }
                return;
            } catch (NoSuchMethodException ex) {
                String className = loader.getName();
                throw new IllegalArgumentException(String.format("Class %s does not have a public %s(Server) constructor", className, className), ex);
            } catch (Exception ex2) {
                throw new IllegalArgumentException(String.format("Unexpected exception %s while attempting to construct a new instance of %s", ex2.getClass().getName(), loader.getName()), ex2);
            }
        }
        throw new IllegalArgumentException(String.format("Class %s does not implement interface PluginLoader", loader.getName()));
    }

    @Override
    @Nullable
    public synchronized Plugin loadPlugin(@NotNull PluginDescriptionFile description) throws InvalidPluginException {
        Preconditions.checkArgument(description != null, "Description cannot be null");
        Plugin result = null;
        for (PluginLoader loader : this.fileAssociations.values()) {
            result = loader.loadPlugin(description);
        }
        if (result != null) {
            this.plugins.add(result);
            this.lookupNames.put(result.getDescription().getName(), result);
            for (String provided : result.getDescription().getProvides()) {
                this.lookupNames.putIfAbsent(provided, result);
            }
        }
        return result;
    }

    @Override
    @Nullable
    public synchronized Plugin getPlugin(@NotNull String name) {
        return this.lookupNames.get(name.replace(' ', '_'));
    }

    @Override
    @NotNull
    public synchronized Plugin[] getPlugins() {
        return this.plugins.toArray(new Plugin[this.plugins.size()]);
    }

    @Override
    public boolean isPluginEnabled(@NotNull String name) {
        Plugin plugin = getPlugin(name);
        return isPluginEnabled(plugin);
    }

    @Override
    public boolean isPluginEnabled(@Nullable Plugin plugin) {
        if (plugin != null && this.plugins.contains(plugin)) {
            return plugin.isEnabled();
        }
        return false;
    }

    @Override
    public void enablePlugin(@NotNull Plugin plugin) {
        if (!plugin.isEnabled()) {
            try {
                plugin.getPluginLoader().enablePlugin(plugin);
            } catch (Throwable ex) {
                this.server.getLogger().log(Level.SEVERE, "Error occurred (in the plugin loader) while enabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
            HandlerList.bakeAll();
        }
    }

    @Override
    public void disablePlugins() {
        Plugin[] plugins = getPlugins();
        for (int i = plugins.length - 1; i >= 0; i--) {
            disablePlugin(plugins[i]);
        }
    }

    @Override
    public void disablePlugin(@NotNull Plugin plugin) {
        if (plugin.isEnabled()) {
            try {
                plugin.getPluginLoader().disablePlugin(plugin);
            } catch (Throwable ex) {
                this.server.getLogger().log(Level.SEVERE, "Error occurred (in the plugin loader) while disabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
            try {
                HandlerList.unregisterAll(plugin);
            } catch (Throwable ex4) {
                this.server.getLogger().log(Level.SEVERE, "Error occurred (in the plugin loader) while unregistering events for " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex4);
            }
        }
    }

    @Override
    public void clearPlugins() {
        synchronized (this) {
            disablePlugins();
            this.plugins.clear();
            this.lookupNames.clear();
            HandlerList.unregisterAll();
            this.fileAssociations.clear();
        }
    }

    @Override
    public void callEvent(@NotNull Event event) {
        if (event.isAsynchronous()) {
            if (Thread.holdsLock(this)) {
                throw new IllegalStateException(event.getEventName() + " cannot be triggered asynchronously from inside synchronized code.");
            }
            if (this.server.isPrimaryThread()) {
                throw new IllegalStateException(event.getEventName() + " cannot be triggered asynchronously from primary server thread.");
            }
        } else if (!this.server.isPrimaryThread()) {
            throw new IllegalStateException(event.getEventName() + " cannot be triggered asynchronously from another thread.");
        }
        fireEvent(event);
    }

    private void fireEvent(@NotNull Event event) {
        HandlerList handlers = event.getHandlers();
        RegisteredListener[] listeners = handlers.getRegisteredListeners();
        for (RegisteredListener registration : listeners) {
            if (registration.getPlugin().isEnabled()) {
                try {
                    registration.callEvent(event);
                } catch (AuthorNagException ex) {
                    Plugin plugin = registration.getPlugin();
                    if (plugin.isNaggable()) {
                        plugin.setNaggable(false);
                        this.server.getLogger().log(Level.SEVERE, String.format("Nag author(s): '%s' of '%s' about the following: %s", plugin.getDescription().getAuthors(), plugin.getDescription().getFullName(), ex.getMessage()));
                    }
                } catch (Throwable ex2) {
                    this.server.getLogger().log(Level.SEVERE, "Could not pass event " + event.getEventName() + " to " + registration.getPlugin().getDescription().getFullName(), ex2);
                }
            }
        }
    }

    @Override
    public void registerEvents(@NotNull Listener listener, @NotNull Plugin plugin) {
        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException("Plugin attempted to register " + String.valueOf(listener) + " while not enabled");
        }
        for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : plugin.getPluginLoader().createRegisteredListeners(listener, plugin).entrySet()) {
            getEventListeners(getRegistrationClass(entry.getKey())).registerAll(entry.getValue());
        }
    }

    @Override
    public void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor, @NotNull Plugin plugin) {
        registerEvent(event, listener, priority, executor, plugin, false);
    }

    @Override
    public void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor, @NotNull Plugin plugin, boolean ignoreCancelled) {
        Preconditions.checkArgument(listener != null, "Listener cannot be null");
        Preconditions.checkArgument(priority != null, "Priority cannot be null");
        Preconditions.checkArgument(executor != null, "Executor cannot be null");
        Preconditions.checkArgument(plugin != null, "Plugin cannot be null");
        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException("Plugin attempted to register " + String.valueOf(event) + " while not enabled");
        }
        if (this.useTimings) {
            getEventListeners(event).register(new TimedRegisteredListener(listener, executor, priority, plugin, ignoreCancelled));
        } else {
            getEventListeners(event).register(new RegisteredListener(listener, executor, priority, plugin, ignoreCancelled));
        }
    }

    @NotNull
    private HandlerList getEventListeners(@NotNull Class<? extends Event> type) {
        try {
            Method method = getRegistrationClass(type).getDeclaredMethod("getHandlerList");
            method.setAccessible(true);
            if (!Modifier.isStatic(method.getModifiers())) {
                throw new IllegalAccessException("getHandlerList must be static");
            }
            return (HandlerList) method.invoke(null);
        } catch (Exception e) {
            throw new IllegalPluginAccessException("Error while registering listener for event type " + type.toString() + ": " + e.toString());
        }
    }

    @NotNull
    private Class<? extends Event> getRegistrationClass(@NotNull Class<? extends Event> clazz) {
        try {
            clazz.getDeclaredMethod("getHandlerList");
            return clazz;
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null && !clazz.getSuperclass().equals(Event.class) && Event.class.isAssignableFrom(clazz.getSuperclass())) {
                return getRegistrationClass(clazz.getSuperclass().asSubclass(Event.class));
            }
            throw new IllegalPluginAccessException("Unable to find handler list for event " + clazz.getName() + ". Static getHandlerList method required!");
        }
    }

    @Override
    public boolean useTimings() {
        return this.useTimings;
    }

    public void useTimings(boolean use) {
        this.useTimings = use;
    }
}
