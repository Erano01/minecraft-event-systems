package me.erano.com.forge.eventbus.api.event.characteristic;

import me.erano.com.forge.eventbus.internal.EventCharacteristic;

// Marker: setCancelled()/isCancelled() yok. Iptal, listener'in donus degeriyle
// (Predicate -> true) ifade edilir ve zinciri orada durdurur; bkz. InvokerFactory.
public interface Cancellable extends EventCharacteristic {
}
