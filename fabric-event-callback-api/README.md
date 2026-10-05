# fabric-event-callback-api

Fabric Loader 0.19.5 ve Fabric API 0.155.3+26.1.2 (Minecraft 26.1.2) event sisteminin ve mod yukleme
mekanizmasinin JADX ile incelenip yeniden uretilmis hali. Concurrency analizi:
[concurrency-model.md](concurrency-model.md).

## Modul ayrimi

| Gercek ekosistem | Bizim modulumuz | Rolu |
|---|---|---|
| `fabric-loader`, `fabric-api-base`, `fabric-lifecycle-events-v1`, `fabric-message-api-v1` | **fabric-event-callback-api** | Event/EventFactory, faz siralamasi, entrypoint yukleme, ornek oyun event'leri |
| Bir mod jar'i (`fabric.mod.json` + entrypoint sinifi) | **fabric-mod-impl** | Tek ornek mod (`examplemod`) ve sunucuyu taklit eden `Main` |

Paket eslemesi: `net.fabricmc` -> `me.erano.com.fabric`, `net.minecraft` -> `me.erano.com.fabric.minecraft`.

## Sinif haritasi

| Gercek sinif | Bizim sinifimiz | Durum |
|---|---|---|
| `fabric.api.event.Event` / `EventFactory` / `AutoInvokingEvent` | `fabric.api.event.*` | birebir (`EventFactory`'nin deprecated profiling metotlari yok) |
| `fabric.impl.base.event.ArrayBackedEvent` / `EventPhaseData` | `fabric.impl.base.event.*` | birebir |
| `fabric.impl.base.event.EventFactoryImpl` | `fabric.impl.base.event.EventFactoryImpl` | **kismi**: hic cagrilmayan `buildEmptyInvoker` yok |
| `fabric.impl.base.toposort.NodeSorting` / `SortableNode` | `fabric.impl.base.toposort.*` | birebir (uyari SLF4J yerine `System.err`) |
| `fabric.api.event.lifecycle.v1.ServerLifecycleEvents` | ayni paket | **kismi**: STARTING/STARTED/STOPPING/STOPPED; data pack ve save event'leri yok |
| `fabric.api.event.lifecycle.v1.ServerTickEvents` | ayni paket | **kismi**: START/END_SERVER_TICK; level tick event'leri yok |
| `fabric.api.message.v1.ServerMessageEvents` | ayni paket | **kismi**: ALLOW_CHAT_MESSAGE ve CHAT_MESSAGE; game/command mesajlari yok |
| `api.ModInitializer` / `DedicatedServerModInitializer` | `api.*` | birebir |
| `loader.api.ModContainer` / `metadata.ModMetadata` / `LanguageAdapter` / `entrypoint.EntrypointContainer` | `loader.api.*` | **kismi**: sadece kullanilan metotlar |
| `loader.api.LanguageAdapterException` / `EntrypointException` | `loader.api.*` | birebir |
| `loader.impl.entrypoint.EntrypointStorage` / `EntrypointContainerImpl` | `loader.impl.entrypoint.*` | **kismi**: 0.3.x eski stil girdiler (`OldEntry`) yok |
| `loader.impl.util.DefaultLanguageAdapter` | `loader.impl.util.DefaultLanguageAdapter` | birebir; siniflar oyun classloader'i yerine context classloader'dan yukleniyor |
| `loader.impl.FabricLoaderImpl` | `loader.impl.FabricLoaderImpl` | **kismi**: sadece mod kesfi, `setupMods`, `invokeEntrypoints`; bagimlilik cozumu, jar-in-jar, Mixin yok |
| `loader.impl.game.minecraft.Hooks` | `loader.impl.game.minecraft.Hooks` | **kismi**: sadece `startServer`; `runDir` ve `prepareModInit` yok |
| `loader.impl.metadata.*` (V1 parser, `LoaderModMetadata`, `EntrypointMetadata`) | `loader.impl.metadata.*` | **kismi**: `fabric.mod.json`'dan sadece `id` ve `entrypoints`, Gson ile |
| `fabric.mixin.message.PlayerListMixin` | `minecraft.server.players.PlayerList` stub'inin icinde | enjekte edilen kod birebir, Mixin altyapisi yok |
| `fabric.mixin.event.lifecycle.MinecraftServerMixin` | `minecraft.server.MinecraftServer` stub'inin icinde | enjeksiyon noktalari birebir, Mixin altyapisi yok |
| `net.minecraft.*` (`Identifier`, `MinecraftServer`, `ServerPlayer`, `PlayerChatMessage`, `ChatType`, `PlayerList`) | `minecraft.*` | Minecraft yerine gecen stub'lar |

## CLI demo

Demo `fabric-mod-impl` modulunde. Proje kok dizininden:

```
./gradlew :fabric-mod-impl:run
```

Loader, classpath'teki `fabric.mod.json`'dan `examplemod`'u bulur. Mod'un `main` entrypoint'i bir sinif
(`ExampleMod`), `server` entrypoint'i ise `ExampleMod::onInitializeServer` bicimindeki statik bir metot.
`Main` su adimlari calistirir:

1. Loader: mod kesfi ve entrypoint'lerin cagiran thread'de, sirayla cagrilmasi.
2. "Netty Server IO" thread'i chat mesajlarini `server.execute(...)` ile server'in is kuyruguna birakir.
3. "Server thread": lifecycle event'leri (sonra kaydedilen `examplemod:early` fazi
   `addPhaseOrdering` sayesinde once calisir), tick event'leri ve chat (`ALLOW_CHAT_MESSAGE` spam'i
   engeller, engellenen mesaj icin `CHAT_MESSAGE` calismaz).
4. Concurrency stres testi: concurrent `register` altinda kilitsiz `invoker()`; hicbir ping ve hicbir
   kayit kaybolmaz.

## Kaynak jar'lar (JADX ile incelenenler)

`~/MinecraftWorkspace/fabric/template-mod-26.1.2` sablonunun build'inde Gradle'in indirdigi jar'lar
(`~/.gradle/caches/modules-2/files-2.1/` altinda):

```
net.fabricmc/fabric-loader/0.19.5/fabric-loader-0.19.5.jar
net.fabricmc.fabric-api/fabric-api-base/2.0.3+ece063234c/fabric-api-base-2.0.3+ece063234c.jar
net.fabricmc.fabric-api/fabric-lifecycle-events-v1/4.1.1+df84eb3d4c/fabric-lifecycle-events-v1-4.1.1+df84eb3d4c.jar
net.fabricmc.fabric-api/fabric-message-api-v1/7.0.5+dae8ce3e4c/fabric-message-api-v1-7.0.5+dae8ce3e4c.jar
```

Sablon build'i Java 25 ister: `JAVA_HOME=/usr/lib/jvm/java-25-openjdk ./gradlew build`.
