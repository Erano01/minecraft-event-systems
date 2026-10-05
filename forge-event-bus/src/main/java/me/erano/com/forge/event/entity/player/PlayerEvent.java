package me.erano.com.forge.event.entity.player;

import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.eventbus.api.event.RecordEvent;

// Gercekte PlayerEvent extends LivingEvent (-> EntityEvent) ve getEntity() bir Player dondurur;
// Minecraft siniflari olmadigi icin oyuncu adi (String) kullaniliyor. Gercek arayuzde ~15 alt event var.
public interface PlayerEvent {
    String getEntity();

    record PlayerLoggedInEvent(String getEntity) implements RecordEvent, PlayerEvent {
        public static final EventBus<PlayerLoggedInEvent> BUS = EventBus.create(PlayerLoggedInEvent.class);
    }

    record PlayerLoggedOutEvent(String getEntity) implements RecordEvent, PlayerEvent {
        public static final EventBus<PlayerLoggedOutEvent> BUS = EventBus.create(PlayerLoggedOutEvent.class);
    }
}
