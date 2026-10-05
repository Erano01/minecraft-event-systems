package me.erano.com.forge.example;

import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.fml.common.Mod;
import me.erano.com.forge.fml.event.lifecycle.FMLCommonSetupEvent;
import me.erano.com.forge.fml.javafmlmod.FMLJavaModLoadingContext;

// Hicbir mod'a bagimli degil: examplemod ile AYNI ANDA, baska bir worker thread'inde calisabilir.
@Mod(IndependentMod.MOD_ID)
public class IndependentMod {
    public static final String MOD_ID = "independentmod";

    public IndependentMod(FMLJavaModLoadingContext context) {
        Log.line("IndependentMod constructor");
        EventBus.create(context.getModBusGroup(), FMLCommonSetupEvent.class)
                .addListener(event -> {
                    Log.line("IndependentMod commonSetup basladi");
                    Log.simulateWork();
                    Log.line("IndependentMod commonSetup bitti");
                });
    }
}
