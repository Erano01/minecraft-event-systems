

~/.gradle/caches/minecraftforge/forgegradle/mavenizer/caches/maven/forge/net/minecraftforge/
eventbus/7.0.1/eventbus-7.0.1.jar
javafmllanguage/26.1.2-64.0.8/javafmllanguage-26.1.2-64.0.8.jar
fmlcore/26.1.2-64.0.8/fmlcore-26.1.2-64.0.8.jar
fmlloader/26.1.2-64.0.8/fmlloader-26.1.2-64.0.8.jar
forge/26.1.2-64.0.8/forge-26.1.2-64.0.8-universal.jar

Spigot'taki gibi kuracaksak yükleme tarafı da gerekiyor. Spigot'ta JavaPluginLoader ve JavaPlugin zincirini yazmıştık, Forge'da onun karşılığı FML. Yani event bus jar'ı tek başına yetmez.

Cache'e baktım, gereken jar'ların hepsi zaten inmiş. MDK'nın build.gradle'ında annotationProcessor 'net.minecraftforge:eventbus-validator:7.0.1' satırı var; bu da event bus sürümünün 7.0.1 olduğunu doğruluyor.

Gereken jar'lar ve Spigot'taki karşılıkları:

┌───────────────────────────────┬─────────────────────────────────────────────────────────────────┬────────────────────────────────────────────┐
│          Forge jar'ı          │                     Ne içeriyor (tahminim)                      │           Spigot'taki karşılığı            │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────────────────────────┤
│ eventbus-7.0.1                │ Bus, listener kaydı, dispatch                                   │ HandlerList, SimplePluginManager.callEvent │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────────────────────────┤
│ javafmllanguage-26.1.2-64.0.8 │ @Mod, FMLModContainer, @EventBusSubscriber ile otomatik kayıt   │ JavaPlugin, JavaPluginLoader               │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────────────────────────┤
│ fmlcore-26.1.2-64.0.8         │ ModLoader, ModContainer, lifecycle event'leri (setup aşamaları) │ PluginManager lifecycle'ı, enablePlugin    │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────────────────────────┤
│ fmlloader-26.1.2-64.0.8       │ Mod keşfi ve annotation taraması                                │ Plugin jar keşfi, plugin.yml okuma         │
├───────────────────────────────┼─────────────────────────────────────────────────────────────────┼────────────────────────────────────────────┤
│ forge-26.1.2-64.0.8-universal │ MinecraftForge.EVENT_BUS (game bus) ve oyun event'leri          │ Bukkit'in oyun event'leri                  │
└───────────────────────────────┴─────────────────────────────────────────────────────────────────┴────────────────────────────────────────────┘