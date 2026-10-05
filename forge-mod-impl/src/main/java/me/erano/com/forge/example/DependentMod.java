package me.erano.com.forge.example;

import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.fml.common.Mod;
import me.erano.com.forge.fml.event.lifecycle.FMLCommonSetupEvent;
import me.erano.com.forge.fml.javafmlmod.FMLJavaModLoadingContext;

// examplemod'a bagimli (bkz. Main'deki ModInfo). Her asamada examplemod'un isi bitmeden baslamaz.
@Mod(DependentMod.MOD_ID)
public class DependentMod {
    public static final String MOD_ID = "dependentmod";

    // Parametresiz constructor: context eski yoldan, ThreadLocal uzerinden alinir. Bu thread'de
    // o an hangi mod isleniyorsa onun context'i doner.
    public DependentMod() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        Log.line("DependentMod constructor (ThreadLocal context: " + context.getContainer().getModId() + ")");
        EventBus.create(context.getModBusGroup(), FMLCommonSetupEvent.class)
                .addListener(event -> Log.line("DependentMod commonSetup (examplemod bittikten sonra)"));
    }
}
