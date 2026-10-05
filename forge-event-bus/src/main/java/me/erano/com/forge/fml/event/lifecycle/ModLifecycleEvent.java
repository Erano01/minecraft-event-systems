package me.erano.com.forge.fml.event.lifecycle;

import me.erano.com.forge.fml.ModContainer;
import me.erano.com.forge.fml.event.IModBusEvent;

public abstract class ModLifecycleEvent implements IModBusEvent {
    private final ModContainer container;

    protected ModLifecycleEvent(ModContainer container) {
        this.container = container;
    }

    ModContainer getContainer() {
        return container;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
