package me.erano.com.forge.fml.javafmlmod;

import me.erano.com.forge.common.MinecraftForge;
import me.erano.com.forge.eventbus.api.bus.BusGroup;
import me.erano.com.forge.eventbus.api.bus.CancellableEventBus;
import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.eventbus.api.event.characteristic.Cancellable;
import me.erano.com.forge.eventbus.api.listener.ObjBooleanBiConsumer;
import me.erano.com.forge.eventbus.api.listener.SubscribeEvent;
import me.erano.com.forge.eventbus.internal.Event;
import me.erano.com.forge.fml.ModContainer;
import me.erano.com.forge.fml.common.Mod;
import me.erano.com.forge.fml.event.IModBusEvent;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

// @Mod.EventBusSubscriber siniflarinin statik @SubscribeEvent metotlarini kaydeder.
// Gercekte siniflar jar'in ASM ile taranmasindan (ModFileScanData) bulunur ve lookup icin JDK'nin
// IMPL_LOOKUP'i kullanilir; burada sinif listesi ModInfo'dan gelir, lookup privateLookupIn ile alinir.
public final class AutomaticEventSubscriber {
    private static final MethodType RETURNS_CONSUMER = MethodType.methodType(Consumer.class);
    private static final MethodType RETURNS_PREDICATE = MethodType.methodType(Predicate.class);
    private static final MethodType RETURNS_MONITOR = MethodType.methodType(ObjBooleanBiConsumer.class);
    private static final MethodType CONSUMER_FI_TYPE = MethodType.methodType(void.class, Object.class);
    private static final MethodType PREDICATE_FI_TYPE = CONSUMER_FI_TYPE.changeReturnType(boolean.class);
    private static final MethodType MONITOR_FI_TYPE = MethodType.methodType(void.class, Object.class, boolean.class);

    private AutomaticEventSubscriber() {
    }

    public static void inject(ModContainer mod, List<Class<?>> targets) {
        for (Class<?> target : targets) {
            Mod.EventBusSubscriber annotation = target.getAnnotation(Mod.EventBusSubscriber.class);
            if (annotation == null) {
                continue;
            }
            String modId = annotation.modid().isEmpty() ? mod.getModId() : annotation.modid();
            if (modId.equals(mod.getModId())) {
                registerLenient(annotation.bus().bus().get(), target);
            }
        }
    }

    private static void registerLenient(BusGroup busGroup, Class<?> listenerClass) {
        int listenersCount = 0;
        for (Method method : listenerClass.getDeclaredMethods()) {
            int paramCount;
            Class<?> returnType;
            if (Modifier.isStatic(method.getModifiers()) && !method.isSynthetic()
                    && (paramCount = method.getParameterCount()) != 0 && paramCount <= 2
                    && ((returnType = method.getReturnType()) == void.class || returnType == boolean.class)
                    && method.isAnnotationPresent(SubscribeEvent.class)) {
                Class<?> eventType = method.getParameterTypes()[0];
                if (!Event.class.isAssignableFrom(eventType)) {
                    throw new IllegalArgumentException("First parameter of a @SubscribeEvent method must be an event");
                }
                registerListener(busGroup, paramCount, returnType, eventType, method.getAnnotation(SubscribeEvent.class), method);
                listenersCount++;
            }
        }
        if (listenersCount == 0) {
            throw new IllegalArgumentException("No listeners found in " + listenerClass);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerListener(BusGroup busGroup, int paramCount, Class<?> returnType, Class<?> eventType,
                                         SubscribeEvent annotation, Method method) {
        if (busGroup == null) {
            busGroup = IModBusEvent.class.isAssignableFrom(eventType)
                    ? FMLJavaModLoadingContext.get().getModBusGroup()
                    : MinecraftForge.EVENT_BUS;
        }
        byte priority = annotation.priority();
        if (paramCount == 2) {
            CancellableEventBus.create(busGroup, (Class) eventType).addListener((ObjBooleanBiConsumer) create(method, RETURNS_MONITOR, MONITOR_FI_TYPE, "accept"));
        } else if (returnType == boolean.class) {
            CancellableEventBus.create(busGroup, (Class) eventType).addListener(priority, (Predicate) create(method, RETURNS_PREDICATE, PREDICATE_FI_TYPE, "test"));
        } else if (Cancellable.class.isAssignableFrom(eventType) && annotation.alwaysCancelling()) {
            CancellableEventBus.create(busGroup, (Class) eventType).addListener(priority, true, (Consumer) create(method, RETURNS_CONSUMER, CONSUMER_FI_TYPE, "accept"));
        } else {
            EventBus.create(busGroup, (Class) eventType).addListener(priority, (Consumer) create(method, RETURNS_CONSUMER, CONSUMER_FI_TYPE, "accept"));
        }
    }

    private static Object create(Method callback, MethodType factoryReturnType, MethodType fiMethodType, String fiMethodName) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(callback.getDeclaringClass(), MethodHandles.lookup());
            MethodHandle mh = lookup.unreflect(callback);
            return LambdaMetafactory.metafactory(lookup, fiMethodName, factoryReturnType, fiMethodType, mh, mh.type())
                    .getTarget().invoke();
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create listener for " + callback, t);
        }
    }
}
