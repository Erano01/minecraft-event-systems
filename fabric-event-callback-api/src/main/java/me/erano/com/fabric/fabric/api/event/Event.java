package me.erano.com.fabric.fabric.api.event;

import me.erano.com.fabric.minecraft.resources.Identifier;

// Bir event = callback arayuzu tipinde (T) tek bir invoker. Bus yok: her event kendi static alaninda
// durur (orn. ServerTickEvents.END_SERVER_TICK) ve cagiran dogrudan invoker()'i cagirir.
public abstract class Event<T> {
    // Okuma yolunun tamami bu alan: kilitsiz volatile okuma. Her register'da yeniden kurulur ve yazilir.
    protected volatile T invoker;
    public static final Identifier DEFAULT_PHASE = Identifier.fromNamespaceAndPath("fabric", "default");

    public abstract void register(T listener);

    public final T invoker() {
        return invoker;
    }

    public void register(Identifier phase, T listener) {
        register(listener);
    }

    public void addPhaseOrdering(Identifier firstPhase, Identifier secondPhase) {
    }
}
