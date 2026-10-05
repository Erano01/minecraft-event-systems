package me.erano.com.forge.eventbus.api.event;

import me.erano.com.forge.eventbus.internal.Event;

// Immutable event'ler icin: record olarak yazilir, listener'lar degistiremez.
public interface RecordEvent extends Event {
}
