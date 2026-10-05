package me.erano.com.forge.eventbus.api.event;

import me.erano.com.forge.eventbus.internal.Event;

// Listener'larin state'ini degistirebildigi event'ler icin taban sinif.
// Gercekte MutableEventInternals'i (MonitorAware icin isMonitoring alani) extend eder; burada yok.
public abstract class MutableEvent implements Event {
}
