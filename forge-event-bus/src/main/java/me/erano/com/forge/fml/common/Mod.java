package me.erano.com.forge.fml.common;

import me.erano.com.forge.common.MinecraftForge;
import me.erano.com.forge.eventbus.api.bus.BusGroup;
import me.erano.com.forge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

// Mod'un giris sinifi; value() mods.toml'daki modId ile eslesir.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Mod {
    String value();

    // Sinifin statik @SubscribeEvent metotlarini otomatik kaydeder (bkz. AutomaticEventSubscriber).
    // Gercekte ayrica Dist[] value() ile client/server tarafi secilir.
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface EventBusSubscriber {
        String modid() default "";

        Bus bus() default Bus.BOTH;

        enum Bus {
            FORGE(() -> MinecraftForge.EVENT_BUS),
            MOD(() -> FMLJavaModLoadingContext.get().getModBusGroup()),
            // Her metot icin event tipine gore secilir: IModBusEvent ise mod bus, degilse game bus.
            BOTH(() -> null);

            private final Supplier<BusGroup> busSupplier;

            Bus(Supplier<BusGroup> busSupplier) {
                this.busSupplier = busSupplier;
            }

            public Supplier<BusGroup> bus() {
                return busSupplier;
            }
        }
    }
}
