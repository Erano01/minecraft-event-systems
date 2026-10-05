package me.erano.com.forge.event;

import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.eventbus.api.event.RecordEvent;

import java.util.function.BooleanSupplier;

// Gercekte ServerTickEvent'te ayrica MinecraftServer server() var; ClientTickEvent, LevelTickEvent,
// PlayerTickEvent ve RenderTickEvent de ayni Pre/Post record desenini kullanir.
public interface TickEvent {

    interface ServerTickEvent extends TickEvent {
        BooleanSupplier haveTimeSupplier();

        default boolean haveTime() {
            return haveTimeSupplier().getAsBoolean();
        }

        record Pre(BooleanSupplier haveTimeSupplier) implements RecordEvent, ServerTickEvent {
            public static final EventBus<Pre> BUS = EventBus.create(Pre.class);
        }

        record Post(BooleanSupplier haveTimeSupplier) implements RecordEvent, ServerTickEvent {
            public static final EventBus<Post> BUS = EventBus.create(Post.class);
        }
    }
}
