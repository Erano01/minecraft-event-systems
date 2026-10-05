package me.erano.com.forge.fml.event;

import me.erano.com.forge.eventbus.api.bus.BusGroup;
import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.eventbus.api.event.InheritableEvent;

// Mod bus'ina ait event'lerin isareti. Mod bus BusGroup'u sadece bu tipteki event'leri kabul eder.
public interface IModBusEvent extends InheritableEvent {
    static <T extends IModBusEvent> EventBus<T> getBus(BusGroup modEventBusGroup, Class<T> eventClass) {
        return EventBus.create(modEventBusGroup, eventClass);
    }
}
