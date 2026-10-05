package me.erano.com.forge.example;

import me.erano.com.forge.common.MinecraftForge;
import me.erano.com.forge.eventbus.api.bus.EventBus;
import me.erano.com.forge.fml.common.Mod;
import me.erano.com.forge.fml.event.lifecycle.FMLCommonSetupEvent;
import me.erano.com.forge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.lang.invoke.MethodHandles;

// Gercek bir Forge mod'unun yazacagi sey. Constructor CONSTRUCT asamasinda bir modloading-worker
// thread'inde cagrilir; burada sadece kayit yapilir, oyun state'ine dokunulmaz.
@Mod(ExampleMod.MOD_ID)
public class ExampleMod {
    public static final String MOD_ID = "examplemod";

    public ExampleMod(FMLJavaModLoadingContext context) {
        Log.line("ExampleMod constructor");

        // Mod bus: lambda (method reference) ile dogrudan kayit. Reflection/LMF gerekmez.
        EventBus.create(context.getModBusGroup(), FMLCommonSetupEvent.class).addListener(this::commonSetup);

        // Game bus: @SubscribeEvent metotlari olan bir nesne. Lookup, LambdaMetafactory'nin
        // GameEventListeners'in metotlarina erisebilmesi icin bizim erisim hakkimizi tasir.
        MinecraftForge.EVENT_BUS.register(MethodHandles.lookup(), new GameEventListeners());
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        Log.line("ExampleMod commonSetup basladi");
        Log.simulateWork();
        Log.line("ExampleMod commonSetup bitti");
        // Thread-safe olmayan is: asama sonunda main thread'de calisacak.
        event.enqueueWork(() -> Log.line("ExampleMod enqueueWork -> registry'ye yaziliyor"));
    }
}
