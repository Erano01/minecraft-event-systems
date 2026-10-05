package me.erano.com.forge.eventbus.internal;

import me.erano.com.forge.eventbus.api.bus.CancellableEventBus;
import me.erano.com.forge.eventbus.api.event.characteristic.Cancellable;
import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.api.listener.ObjBooleanBiConsumer;
import me.erano.com.forge.eventbus.api.listener.SubscribeEvent;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

// GoF'taki update()'in Forge karsiligi. @SubscribeEvent metodu kayit aninda LambdaMetafactory ile
// GERCEK bir Consumer/Predicate sinifina donusturulur (javac'in lambda icin yaptiginin aynisi).
// Dispatch'te reflection degil, normal bir arayuz cagrisi kalir.
//
// Gercekte ayrica registerStrict (eventbus.api.strictRegistrationChecks) var; burada sadece lenient yol.
final class EventListenerFactory {
    private static final MethodType RETURNS_CONSUMER = MethodType.methodType(Consumer.class);
    private static final MethodType RETURNS_PREDICATE = MethodType.methodType(Predicate.class);
    private static final MethodType RETURNS_MONITOR = MethodType.methodType(ObjBooleanBiConsumer.class);
    private static final MethodType CONSUMER_FI_TYPE = MethodType.methodType(void.class, Object.class);
    private static final MethodType PREDICATE_FI_TYPE = CONSUMER_FI_TYPE.changeReturnType(boolean.class);
    private static final MethodType MONITOR_FI_TYPE = MethodType.methodType(void.class, Object.class, boolean.class);

    // Method -> lambda fabrikasi. Ayni metot icin LambdaMetafactory bir kez calisir; paralel mod
    // yuklemesinde farkli thread'lerden kayit yapilabildigi icin ConcurrentHashMap.
    private static final Map<Method, MethodHandle> LMF_CACHE = new ConcurrentHashMap<>();

    private EventListenerFactory() {
    }

    static Collection<EventListener> register(BusGroupImpl busGroup, MethodHandles.Lookup callerLookup, Class<?> listenerClass, Object listenerInstance) {
        Method[] declaredMethods = listenerClass.getDeclaredMethods();
        if (declaredMethods.length == 0) {
            throw new IllegalArgumentException("No declared methods found in " + listenerClass);
        }
        Class<?> firstValidListenerEventType = null;
        ArrayList<EventListener> listeners = new ArrayList<>();
        for (Method method : declaredMethods) {
            int paramCount;
            Class<?> returnType;
            if ((listenerInstance != null || Modifier.isStatic(method.getModifiers()))
                    && !method.isSynthetic()
                    && (paramCount = method.getParameterCount()) != 0 && paramCount <= 2
                    && ((returnType = method.getReturnType()) == void.class || returnType == boolean.class)
                    && method.isAnnotationPresent(SubscribeEvent.class)) {
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (!Event.class.isAssignableFrom(parameterTypes[0])) {
                    throw new IllegalArgumentException("First parameter of a @SubscribeEvent method must be an event");
                }
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventType = (Class<? extends Event>) parameterTypes[0];
                SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
                listeners.add(registerListener(busGroup, callerLookup, paramCount, returnType, eventType, annotation, method, listenerInstance));
                if (firstValidListenerEventType == null) {
                    firstValidListenerEventType = eventType;
                }
            }
        }
        if (listeners.isEmpty()) {
            throw new IllegalArgumentException("No listeners found in " + listenerClass);
        }
        // Tek listener icin sinif taramasi gereksiz maliyet: dogrudan addListener(lambda) tavsiye edilir.
        if (listeners.size() == 1) {
            throw new IllegalArgumentException("Only a single listener found in " + listenerClass
                    + ". You should directly call addListener() on the EventBus of "
                    + firstValidListenerEventType.getSimpleName() + " instead.");
        }
        return listeners;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static EventListener registerListener(BusGroupImpl busGroup, MethodHandles.Lookup callerLookup, int paramCount,
                                                  Class<?> returnType, Class<? extends Event> eventType,
                                                  SubscribeEvent annotation, Method method, Object listenerInstance) {
        if (paramCount == 1) {
            byte priority = annotation.priority();
            if (returnType == void.class) {
                if (Cancellable.class.isAssignableFrom(eventType)) {
                    CancellableEventBus eventBus = (CancellableEventBus) busGroup.getOrCreateEventBus(eventType);
                    if (annotation.alwaysCancelling()) {
                        return eventBus.addListener(priority, true, createConsumer(callerLookup, method, listenerInstance));
                    }
                    return eventBus.addListener(priority, createConsumer(callerLookup, method, listenerInstance));
                }
                return busGroup.getOrCreateEventBus((Class) eventType).addListener(priority, createConsumer(callerLookup, method, listenerInstance));
            }
            if (!Cancellable.class.isAssignableFrom(eventType)) {
                throw new IllegalArgumentException("Return type boolean is only valid for cancellable events: " + method);
            }
            if (annotation.alwaysCancelling()) {
                throw new IllegalArgumentException("Always cancelling listeners must have a void return type");
            }
            return ((CancellableEventBus) busGroup.getOrCreateEventBus(eventType)).addListener(priority, createPredicate(callerLookup, method, listenerInstance));
        }
        // 2 parametre: (event, boolean wasCancelled) -> iptal-farkinda MONITOR listener.
        if (returnType != void.class) {
            throw new IllegalArgumentException("Cancellation-aware monitoring listeners must have a void return type");
        }
        if (annotation.alwaysCancelling()) {
            throw new IllegalArgumentException("Monitoring listeners cannot cancel events");
        }
        return ((CancellableEventBus) busGroup.getOrCreateEventBus(eventType)).addListener(createMonitor(callerLookup, method, listenerInstance));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> Consumer<T> createConsumer(MethodHandles.Lookup callerLookup, Method callback, Object instance) {
        return (Consumer<T>) instantiate(callerLookup, callback, instance, RETURNS_CONSUMER, CONSUMER_FI_TYPE, "accept");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> Predicate<T> createPredicate(MethodHandles.Lookup callerLookup, Method callback, Object instance) {
        return (Predicate<T>) instantiate(callerLookup, callback, instance, RETURNS_PREDICATE, PREDICATE_FI_TYPE, "test");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> ObjBooleanBiConsumer<T> createMonitor(MethodHandles.Lookup callerLookup, Method callback, Object instance) {
        return (ObjBooleanBiConsumer<T>) instantiate(callerLookup, callback, instance, RETURNS_MONITOR, MONITOR_FI_TYPE, "accept");
    }

    private static Object instantiate(MethodHandles.Lookup callerLookup, Method callback, Object instance,
                                      MethodType factoryReturnType, MethodType fiMethodType, String fiMethodName) {
        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        MethodHandle factory = LMF_CACHE.computeIfAbsent(callback, method ->
                makeFactory(callerLookup, method, isStatic, instance, factoryReturnType, fiMethodType, fiMethodName));
        try {
            return isStatic ? factory.invoke() : factory.invoke(instance);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create listener for " + callback, t);
        }
    }

    // Statik metot: fabrika ()->Consumer. Instance metot: fabrika (instance)->Consumer, yani lambda
    // listener nesnesini yakalar (bound method reference: listener::onEvent).
    private static MethodHandle makeFactory(MethodHandles.Lookup callerLookup, Method callback, boolean isStatic, Object instance,
                                            MethodType factoryReturnType, MethodType fiMethodType, String fiMethodName) {
        try {
            MethodHandle mh = callerLookup.unreflect(callback);
            MethodType factoryType = isStatic
                    ? factoryReturnType
                    : factoryReturnType.insertParameterTypes(0, Objects.requireNonNull(instance).getClass());
            MethodHandle lmf = LambdaMetafactory.metafactory(callerLookup, fiMethodName, factoryType, fiMethodType, mh,
                    isStatic ? mh.type() : mh.type().dropParameterTypes(0, 1)).getTarget();
            return isStatic ? lmf : lmf.asType(factoryType.changeParameterType(0, Object.class));
        } catch (IllegalAccessException e) {
            String msg = "Failed to create listener";
            if (!Modifier.isPublic(callback.getModifiers())) {
                msg += " - is it public?";
            }
            throw new RuntimeException(msg, e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create listener", e);
        }
    }
}
