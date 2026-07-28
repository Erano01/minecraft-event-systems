package me.erano.com.bukkit.plugin;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.EventPriority;
import me.erano.com.bukkit.event.HandlerList;
import me.erano.com.bukkit.event.Listener;
import me.erano.com.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/*
 * Gercek org.bukkit.plugin.SimplePluginManager, PluginManager arayuzunun TAMAMINI (26 metot)
 * implemente eder ve ek olarak Server, SimpleCommandMap, dependency graph (MutableGraph),
 * permission/permissible haritalari gibi bircok alan tasir. Bu sinifin implement ETMEDIGIMIZ
 * kisimlari (registerInterface, loadPlugin(s), enablePlugin, disablePlugin, clearPlugins,
 * permission subscription metotlari) sunlari halleder:
 *  - loadPlugin(s)/registerInterface/checkUpdate: jar dosyalarini diskten okuyup
 *    PluginLoader araciligiyla siniflandirma, dependency graph ile yukleme sirasini cozme.
 *  - enablePlugin/disablePlugin/clearPlugins: plugin yasam donguesunu Scheduler,
 *    ServicesManager, Messenger ve World chunk-ticket sistemlerine baglama (bizde bu
 *    donguenun event-disi kismi JavaPluginLoader.enablePlugin/disablePlugin'e tasindi).
 *  - addPermission/subscribeToPermission/calculatePermissionDefault/dirtyPermissibles vb.:
 *    Permission/Permissible yetkilendirme alt sistemi.
 * Bunlarin hicbiri event dispatch/concurrency mekanizmasinin parcasi degil, bu yuzden bu
 * sinifta implement edilmiyor/dallandirilmiyor. Asagida sadece event dispatch hattini kuran
 * metotlar birebir aliniyor.
 *
 * Not: Gercek surumde Thread guvenligi validasyonu icin server.isPrimaryThread() cagirilir.
 * Bizde Server nesnesi olmadigindan, SimplePluginManager'i olusturan thread "primary thread"
 * kabul ediliyor - bu bilincli bir sapma (asagida isPrimaryThread() metoduna bakiniz).
 */
public class SimplePluginManager implements PluginManager {

    private final JavaPluginLoader pluginLoader = new JavaPluginLoader();
    private final Thread primaryThread;
    private boolean useTimings = false;

    public SimplePluginManager() {
        this.primaryThread = Thread.currentThread();
    }

    public boolean isPrimaryThread() {
        return Thread.currentThread() == this.primaryThread;
    }

    @Override
    public void callEvent(@NotNull Event event) {
        if (event.isAsynchronous()) {
            if (Thread.holdsLock(this)) {
                throw new IllegalStateException(event.getEventName() + " cannot be triggered asynchronously from inside synchronized code.");
            }
            if (isPrimaryThread()) {
                throw new IllegalStateException(event.getEventName() + " cannot be triggered asynchronously from primary server thread.");
            }
        } else if (!isPrimaryThread()) {
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
                } catch (Throwable ex) {
                    System.err.println("Could not pass event " + event.getEventName() + " to " + registration.getPlugin().getName() + ": " + ex);
                }
            }
        }
    }

    @Override
    public void registerEvents(@NotNull Listener listener, @NotNull Plugin plugin) {
        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException("Plugin attempted to register " + listener + " while not enabled");
        }
        for (Map.Entry<Class<? extends Event>, Set<RegisteredListener>> entry : this.pluginLoader.createRegisteredListeners(listener, plugin).entrySet()) {
            getEventListeners(getRegistrationClass(entry.getKey())).registerAll(entry.getValue());
        }
    }

    @Override
    public void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor, @NotNull Plugin plugin) {
        registerEvent(event, listener, priority, executor, plugin, false);
    }

    @Override
    public void registerEvent(@NotNull Class<? extends Event> event, @NotNull Listener listener, @NotNull EventPriority priority, @NotNull EventExecutor executor, @NotNull Plugin plugin, boolean ignoreCancelled) {
        // Gercek Bukkit burada Guava'nin Preconditions.checkArgument'ini kullanir; bu
        // modulun bagimliligi olmadigindan ayni davranisi Objects.requireNonNull ile veriyoruz.
        Objects.requireNonNull(listener, "Listener cannot be null");
        Objects.requireNonNull(priority, "Priority cannot be null");
        Objects.requireNonNull(executor, "Executor cannot be null");
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        if (!plugin.isEnabled()) {
            throw new IllegalPluginAccessException("Plugin attempted to register " + event + " while not enabled");
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
            throw new IllegalPluginAccessException("Error while registering listener for event type " + type + ": " + e);
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

    public boolean useTimings() {
        return this.useTimings;
    }

    public void useTimings(boolean use) {
        this.useTimings = use;
    }
}
