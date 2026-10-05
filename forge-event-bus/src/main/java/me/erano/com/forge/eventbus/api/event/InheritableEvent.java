package me.erano.com.forge.eventbus.api.event;

import me.erano.com.forge.eventbus.internal.Event;

// Ust tipe eklenen listener'lar kayit aninda alt tiplerin bus'larina da kopyalanir
// (AbstractEventBusImpl.children). Dispatch aninda instanceof filtresi gerekmez.
public interface InheritableEvent extends Event {
}
