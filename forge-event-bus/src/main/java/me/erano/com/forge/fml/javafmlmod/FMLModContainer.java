package me.erano.com.forge.fml.javafmlmod;

import me.erano.com.forge.eventbus.api.bus.BusGroup;
import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.fml.ModContainer;
import me.erano.com.forge.fml.ModInfo;
import me.erano.com.forge.fml.ModLoadingStage;
import me.erano.com.forge.fml.event.IModBusEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

// @Mod sinifini yukleyen container. Gercekte mod sinifi ayri bir module layer'dan Class.forName ile
// yuklenir; burada ModInfo'da hazir Class olarak geliyor.
public class FMLModContainer extends ModContainer {
    private final BusGroup eventBusGroup;
    private final Class<?> modClass;
    private final FMLJavaModLoadingContext context;
    // CONSTRUCT asamasinda bir worker'da yazilir; sonraki okumalar future zinciri sayesinde gorur.
    private Object modInstance;

    public FMLModContainer(ModInfo info) {
        super(info);
        this.context = new FMLJavaModLoadingContext(this);
        this.activityMap.put(ModLoadingStage.CONSTRUCT, this::constructMod);
        // Her mod'un KENDI bus grubu: mod'lar birbirinin lifecycle event'lerini gormez.
        this.eventBusGroup = BusGroup.create("modBusFor" + info.modId(), IModBusEvent.class);
        this.contextExtension = () -> context;
        this.modClass = info.modClass();
    }

    private void constructMod() {
        try {
            Constructor<?> constructor;
            try {
                constructor = modClass.getDeclaredConstructor(FMLJavaModLoadingContext.class);
            } catch (NoSuchMethodException e) {
                constructor = modClass.getDeclaredConstructor();
            }
            modInstance = constructor.getParameterCount() == 0 ? constructor.newInstance() : constructor.newInstance(context);
            AutomaticEventSubscriber.inject(this, modInfo.eventBusSubscribers());
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to create mod instance " + modId, e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create mod instance " + modId, e);
        }
    }

    @Override
    public Object getMod() {
        return modInstance;
    }

    @Override
    public BusGroup getModBusGroup() {
        return eventBusGroup;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends IModBusEvent> void acceptEvent(T e) {
        EventBus<T> eventBus = IModBusEvent.getBus(eventBusGroup, (Class<T>) e.getClass());
        eventBus.post(e);
    }
}
