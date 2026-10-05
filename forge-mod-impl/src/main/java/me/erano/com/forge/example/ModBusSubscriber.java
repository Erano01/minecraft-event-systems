package me.erano.com.forge.example;

import me.erano.com.forge.eventbus.api.listener.SubscribeEvent;
import me.erano.com.forge.fml.common.Mod;
import me.erano.com.forge.fml.event.lifecycle.FMLLoadCompleteEvent;

// Otomatik kayit: FMLModContainer, mod constructor'indan sonra bu sinifi AutomaticEventSubscriber'a verir.
@Mod.EventBusSubscriber(modid = ExampleMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModBusSubscriber {
    private ModBusSubscriber() {
    }

    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        Log.line("ModBusSubscriber onLoadComplete (@EventBusSubscriber)");
    }
}
