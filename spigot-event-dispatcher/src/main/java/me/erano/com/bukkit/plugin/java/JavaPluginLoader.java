package me.erano.com.bukkit.plugin.java;

import com.google.common.base.Preconditions;
import me.erano.com.bukkit.Server;
import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.EventException;
import me.erano.com.bukkit.event.EventHandler;
import me.erano.com.bukkit.event.Listener;
import me.erano.com.bukkit.event.server.PluginDisableEvent;
import me.erano.com.bukkit.event.server.PluginEnableEvent;
import me.erano.com.bukkit.plugin.EventExecutor;
import me.erano.com.bukkit.plugin.InvalidPluginException;
import me.erano.com.bukkit.plugin.Plugin;
import me.erano.com.bukkit.plugin.PluginDescriptionFile;
import me.erano.com.bukkit.plugin.PluginLoader;
import me.erano.com.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

/*
 * Gercek siniftan cikarilanlar:
 *  - PluginClassLoader / LibraryLoader / loaders listesi: jar'dan sinif yukleme, plugin'ler
 *    arasi sinif paylasimi (getClassByName/setClass/removeClass), disable'da classloader'i kapatma.
 *  - loadPlugin(File)'in dataFolder, depend kontrolu ve server.getUnsafe().checkSupported adimlari.
 *  - createRegisteredListeners'taki @Deprecated event uyarisi (Warning/WarningState, server
 *    config'ine bagli) ve her handler icin CustomTimingsHandler (spigot profiler'i).
 *
 * loadPlugin(PluginDescriptionFile): gercekte bu adimlari PluginClassLoader'in kurucusu yapar:
 * Class.forName(description.getMain()) -> asSubclass(JavaPlugin) -> no-arg constructor ->
 * newInstance; ardindan JavaPlugin'in kurucusu PluginClassLoader.initialize(this) ile
 * JavaPlugin.init(...)'i cagirir. Bizde sinif uygulamanin kendi classloader'indan yukleniyor ve
 * init(...), nesne olustuktan HEMEN SONRA burada cagriliyor (gercekte kurucunun icinde, alt
 * sinifin kurucu govdesinden once). Hata mesajlari birebir.
 */
public final class JavaPluginLoader implements PluginLoader {
    final Server server;
    private final Pattern[] fileFilters = {Pattern.compile("\\.jar$")};

    @Deprecated(since = "1.4.5")
    public JavaPluginLoader(@NotNull Server instance) {
        Preconditions.checkArgument(instance != null, "Server cannot be null");
        this.server = instance;
    }

    @Override
    @NotNull
    public Plugin loadPlugin(@NotNull PluginDescriptionFile description) throws InvalidPluginException {
        Preconditions.checkArgument(description != null, "Description cannot be null");
        JavaPlugin plugin;
        try {
            Class<?> jarClass = Class.forName(description.getMain(), true, getClass().getClassLoader());
            try {
                try {
                    Constructor<? extends JavaPlugin> pluginConstructor = jarClass.asSubclass(JavaPlugin.class).getDeclaredConstructor();
                    try {
                        plugin = pluginConstructor.newInstance();
                    } catch (ExceptionInInitializerError | InvocationTargetException ex) {
                        throw new InvalidPluginException("Exception initializing main class `" + description.getMain() + "'", ex);
                    } catch (IllegalAccessException ex2) {
                        throw new InvalidPluginException("main class `" + description.getMain() + "' constructor must be public", ex2);
                    } catch (IllegalArgumentException ex3) {
                        throw new InvalidPluginException("Could not invoke main class `" + description.getMain() + "' constructor", ex3);
                    } catch (InstantiationException ex4) {
                        throw new InvalidPluginException("main class `" + description.getMain() + "' must not be abstract", ex4);
                    }
                } catch (NoSuchMethodException ex5) {
                    throw new InvalidPluginException("main class `" + description.getMain() + "' must have a public no-args constructor", ex5);
                }
            } catch (ClassCastException ex6) {
                throw new InvalidPluginException("main class `" + description.getMain() + "' must extend JavaPlugin", ex6);
            }
        } catch (ClassNotFoundException ex7) {
            throw new InvalidPluginException("Cannot find main class `" + description.getMain() + "'", ex7);
        }
        plugin.init(this, this.server, description);
        return plugin;
    }

    @Override
    @NotNull
    public Pattern[] getPluginFileFilters() {
        return this.fileFilters.clone();
    }

    @Override
    @NotNull
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(@NotNull Listener listener, @NotNull Plugin plugin) {
        Preconditions.checkArgument(plugin != null, "Plugin can not be null");
        Preconditions.checkArgument(listener != null, "Listener can not be null");
        // Gercekte de sonucu kullanilmayan bir cagri (JADX: "this.server.getPluginManager().useTimings();").
        this.server.getPluginManager().useTimings();
        Map<Class<? extends Event>, Set<RegisteredListener>> map = new HashMap<>();
        try {
            Method[] publicMethods = listener.getClass().getMethods();
            Method[] privateMethods = listener.getClass().getDeclaredMethods();
            Set<Method> methods = new HashSet<>(publicMethods.length + privateMethods.length, 1.0f);
            for (Method method : publicMethods) {
                methods.add(method);
            }
            for (Method method : privateMethods) {
                methods.add(method);
            }
            for (final Method method : methods) {
                final EventHandler eh = method.getAnnotation(EventHandler.class);
                if (eh == null || method.isBridge() || method.isSynthetic()) {
                    continue;
                }
                final Class<?> checkClass;
                if (method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
                    // JADX ciktisinda bu satir kosulsuz gorunuyor; gecerli dalin sonundaki
                    // 'continue' decompile sirasinda kaybolmus. Gercek akis bu.
                    plugin.getLogger().severe(plugin.getDescription().getFullName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + String.valueOf(listener.getClass()));
                    continue;
                }
                final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
                method.setAccessible(true);
                Set<RegisteredListener> eventSet = map.get(eventClass);
                if (eventSet == null) {
                    eventSet = new HashSet<>();
                    map.put(eventClass, eventSet);
                }
                EventExecutor executor = new EventExecutor() {
                    @Override
                    public void execute(@NotNull Listener listener2, @NotNull Event event) throws EventException {
                        try {
                            if (!eventClass.isAssignableFrom(event.getClass())) {
                                return;
                            }
                            method.invoke(listener2, event);
                        } catch (InvocationTargetException ex) {
                            throw new EventException(ex.getCause());
                        } catch (Throwable t) {
                            throw new EventException(t);
                        }
                    }
                };
                eventSet.add(new RegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
            }
            return map;
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().severe("Plugin " + plugin.getDescription().getFullName() + " has failed to register events for " + String.valueOf(listener.getClass()) + " because " + e.getMessage() + " does not exist.");
            return map;
        }
    }

    @Override
    public void enablePlugin(@NotNull Plugin plugin) {
        Preconditions.checkArgument(plugin instanceof JavaPlugin, "Plugin is not associated with this PluginLoader");
        if (!plugin.isEnabled()) {
            plugin.getLogger().info("Enabling " + plugin.getDescription().getFullName());
            JavaPlugin jPlugin = (JavaPlugin) plugin;
            try {
                jPlugin.setEnabled(true);
            } catch (Throwable ex) {
                this.server.getLogger().log(Level.SEVERE, "Error occurred while enabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
            this.server.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        }
    }

    @Override
    public void disablePlugin(@NotNull Plugin plugin) {
        Preconditions.checkArgument(plugin instanceof JavaPlugin, "Plugin is not associated with this PluginLoader");
        if (plugin.isEnabled()) {
            String message = String.format("Disabling %s", plugin.getDescription().getFullName());
            plugin.getLogger().info(message);
            this.server.getPluginManager().callEvent(new PluginDisableEvent(plugin));
            JavaPlugin jPlugin = (JavaPlugin) plugin;
            try {
                jPlugin.setEnabled(false);
            } catch (Throwable ex) {
                this.server.getLogger().log(Level.SEVERE, "Error occurred while disabling " + plugin.getDescription().getFullName() + " (Is it up to date?)", ex);
            }
        }
    }
}
