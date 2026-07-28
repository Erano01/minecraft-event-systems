package me.erano.com.bukkit.plugin.java;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.EventException;
import me.erano.com.bukkit.event.EventHandler;
import me.erano.com.bukkit.event.Listener;
import me.erano.com.bukkit.plugin.EventExecutor;
import me.erano.com.bukkit.plugin.Plugin;
import me.erano.com.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * Gercek org.bukkit.plugin.java.JavaPluginLoader, "PluginLoader" arayuzunun bir
 * implementasyonudur ve ayrica su sorumluluklari da tasir:
 *  - plugin.yml / PluginDescriptionFile okuma ve dogrulama
 *  - jar dosyasindan PluginClassLoader kurup plugin sinifini instantiate etme (loadPlugin)
 *  - enablePlugin/disablePlugin sirasinda PluginEnableEvent/PluginDisableEvent firlatma ve
 *    PluginClassLoader'i kapatip sinif kayitlarini temizleme
 * plugin.yml/jar/classloading kismi (disk I/O, classloading) event dispatch/concurrency
 * mekanizmasinin parcasi degil - implement edilmiyor. Ama enablePlugin/disablePlugin'in
 * "ayni pakette oldugu icin JavaPlugin.setEnabled(boolean)'i protected olarak cagirabilme"
 * mekanizmasi dogrudan bizim Plugin yasam donguesu (isEnabled/onEnable/onDisable, bkz.
 * JavaPlugin.java) ile ilgili oldugundan BIREBIR korunuyor (sadece Logger/ClassLoader/
 * PluginEnableEvent-PluginDisableEvent kismi cikarildi, yorumla isaretlendi).
 */
public class JavaPluginLoader {

    @NotNull
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(@NotNull Listener listener, @NotNull Plugin plugin) {
        Map<Class<? extends Event>, Set<RegisteredListener>> map = new HashMap<>();

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
                System.err.println(plugin.getName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + listener.getClass());
                continue;
            }

            final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);

            Set<RegisteredListener> eventSet = map.get(eventClass);
            if (eventSet == null) {
                eventSet = new HashSet<>();
                map.put(eventClass, eventSet);
            }

            // Gercek Bukkit burada ayrica Deprecated/@Warning kontrolu yapip plugin
            // yazarlarini uyariyor (AuthorNagException) ve her handler icin ayri bir
            // "CustomTimingsHandler" olusturuyor. Bunlar profiling/uyari altyapisina ait,
            // event dispatch mekanizmasinin bir parcasi degil - implement edilmiyor.
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
    }

    /*
     * Gercek versiyonda ClassLoader kaydi, PluginEnableEvent firlatma ve Logger cagrilari da
     * var (bkz. sinif basi yorum) - onlar cikarildi. Kalan kalp: ayni paket icinde oldugumuz
     * icin JavaPlugin.setEnabled(protected)'i dogrudan cagirabiliyoruz, tipki gercekte oldugu
     * gibi.
     */
    public void enablePlugin(@NotNull JavaPlugin plugin) {
        if (!plugin.isEnabled()) {
            plugin.setEnabled(true);
        }
    }

    public void disablePlugin(@NotNull JavaPlugin plugin) {
        if (plugin.isEnabled()) {
            plugin.setEnabled(false);
        }
    }
}
