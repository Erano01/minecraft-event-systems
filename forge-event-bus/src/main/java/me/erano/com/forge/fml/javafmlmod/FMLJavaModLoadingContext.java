package me.erano.com.forge.fml.javafmlmod;

import me.erano.com.forge.eventbus.api.bus.BusGroup;
import me.erano.com.forge.fml.ModLoadingContext;

// Mod constructor'ina parametre olarak verilir. Statik get() eski yol: ThreadLocal uzerinden
// o an bu thread'de islenen mod'un context'ini bulur.
public class FMLJavaModLoadingContext extends ModLoadingContext {
    private final FMLModContainer container;

    FMLJavaModLoadingContext(FMLModContainer container) {
        this.container = container;
    }

    public BusGroup getModBusGroup() {
        return container.getModBusGroup();
    }

    @Override
    public FMLModContainer getContainer() {
        return container;
    }

    // Gercekte @Deprecated(forRemoval = true): context artik constructor parametresiyle veriliyor.
    @Deprecated
    public static FMLJavaModLoadingContext get() {
        return ModLoadingContext.get().extension();
    }
}
