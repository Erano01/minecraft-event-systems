package me.erano.com.bukkit.plugin;

import me.erano.com.bukkit.event.Event;
import me.erano.com.bukkit.event.EventException;
import me.erano.com.bukkit.event.EventHandler;
import me.erano.com.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * Gercek Bukkit'te bu sinif "PluginLoader" arayuzunun bir implementasyonudur (org.bukkit.plugin.java.JavaPluginLoader).
 * Gercek JavaPluginLoader ayrica su sorumluluklari da tasir:
 *  - plugin.yml / PluginDescriptionFile okuma ve dogrulama
 *  - jar dosyasindan PluginClassLoader kurup plugin sinifini instantiate etme
 *  - enablePlugin/disablePlugin cagrilarini plugin yasam donguslune baglama
 * Bunlarin hicbiri event dispatch/concurrency mekanizmasinin parcasi degil (disk I/O,
 * classloading ve plugin yasam donguesu konulari), bu yuzden implement edilmiyor.
 *
 * Burada sadece event kayit hattinin gercek kalbi olan metot birebir tasiniyor:
 * bir Listener'in @EventHandler ile isaretli metotlarini reflection ile bulup her biri
 * icin bir EventExecutor (adapter) ureten mekanizma.
 */
public class JavaPluginLoader {

    @NotNull
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(@NotNull Listener listener, @NotNull Application plugin) {
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
}
