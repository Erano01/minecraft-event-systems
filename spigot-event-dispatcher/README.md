# spigot-event-dispatcher

Spigot/Bukkit 26.2 event dispatch mekanizmasinin JADX ile incelenip yeniden uretilmis hali.
Concurrency analizi: [concurrency-model.md](concurrency-model.md).

![Spigot ekosistemi: NMS, OBC ve Bukkit API](spigot-event-system.png)

## GoF Observer eslesmesi

![Bukkit event sistemi Observer UML diyagrami](bukkit-observer-uml.svg)

## Paket yapisi

`me.erano.com.bukkit` = `org.bukkit`. Alt paketler gercek spigot-api ile ayni:
`event`, `event.player`, `event.server`, `entity`, `plugin`, `plugin.java`.

## Sinif haritasi

Kaynak: `spigot-api-26.2-R0.1-SNAPSHOT.jar` (JADX). Guava (`Preconditions`, `ImmutableList`) gercek
spigot-api'deki gibi `api` bagimliligi.

| Gercek sinif (`org.bukkit.`...) | Durum |
|---|---|
| `event.Event`, `event.Listener`, `event.EventPriority`, `event.EventHandler`, `event.Cancellable`, `event.EventException` | birebir |
| `event.HandlerList` | birebir |
| `event.player.PlayerEvent`, `event.player.PlayerJoinEvent`, `event.player.AsyncPlayerChatEvent` | birebir |
| `event.server.ServerEvent`, `event.server.PluginEvent`, `event.server.PluginEnableEvent`, `event.server.PluginDisableEvent` | birebir |
| `plugin.RegisteredListener`, `plugin.TimedRegisteredListener`, `plugin.EventExecutor` | birebir |
| `plugin.AuthorNagException`, `plugin.IllegalPluginAccessException`, `plugin.InvalidPluginException` | birebir |
| `plugin.PluginBase`, `plugin.PluginLogger` | birebir |
| `plugin.SimplePluginManager` | **kismi**: event dispatch hatti + plugin yasam dongusu (`registerInterface`, `loadPlugin`, `enablePlugin` -> `HandlerList.bakeAll()`, `disablePlugin` -> `HandlerList.unregisterAll(plugin)`, `fireEvent`'teki `AuthorNagException` dali) birebir; komut/scheduler/services/messenger/chunk-ticket adimlari, `loadPlugins` + dependency graph ve permission sistemi yok |
| `plugin.PluginManager` | **kismi**: 30 metottan 16'si (`loadPlugins` x2 ve 13 permission metodu yok) |
| `plugin.Plugin` | **kismi**: 20 metottan 11'i (config, world generation, `TabExecutor` yok) |
| `plugin.PluginLoader` | **kismi**: `loadPlugin(File)` yerine `loadPlugin(PluginDescriptionFile)`, `getPluginDescription(File)` yok |
| `plugin.PluginDescriptionFile` | **kismi**: programatik `(name, version, main)` kurucusu + name/version/main/authors/prefix/provides; plugin.yml (SnakeYAML) parse'i yok |
| `plugin.java.JavaPluginLoader` | **kismi**: `createRegisteredListeners`, `enablePlugin`/`disablePlugin` (`PluginEnableEvent`/`PluginDisableEvent` sirasi dahil) birebir; `loadPlugin` PluginClassLoader kurucusunun adimlarini (main sinif -> no-arg kurucu -> `init`) ayni hata mesajlariyla yapiyor; jar/classloading, deprecated-event uyarisi, `CustomTimingsHandler` yok |
| `plugin.java.JavaPlugin` | **kismi**: lifecycle (`setEnabled` -> `onEnable`/`onDisable`), `init`, `getServer`/`getPluginLoader`/`getDescription`/`getLogger`, naggable birebir; file/config/komut/classloader kismi yok, no-arg kurucu `PluginClassLoader` kontrolu yapmiyor |
| `Server` | **kismi**: `getLogger`, `getPluginManager`, `isPrimaryThread` (130+ metottan 3'u) |
| `entity.Player` | **kismi**: sadece `getName` |

Sunucu tarafi (`CraftServer`, `CraftPlayer`) bu modulde yok; `spigot-plugin-impl` icindeki
`DemoServer`/`DemoPlayer` onlarin yerini tutuyor.

## CLI demo

Demo `spigot-plugin-impl` modulunde; bu modul `spigot-event-dispatcher`'a, gercek bir plugin'in
spigot-api'ye bagimli olmasi gibi bagimli. Proje kok dizininden:

```
./gradlew -q :spigot-plugin-impl:run
```

`Main` sunucuyu temsil eder (`DemoServer` = `CraftServer` karsiligi) ve su adimlari calistirir:

0. `ExamplePlugin`'i `PluginDescriptionFile` ile yukler (`onLoad`) ve enable eder:
   `SimplePluginManager.enablePlugin` -> `JavaPluginLoader.enablePlugin` -> `setEnabled(true)` ->
   `onEnable()` (listener kaydi) -> `PluginEnableEvent`.
1. Sync event (`PlayerJoinEvent`), main thread'de, `EventPriority` sirasiyla (`LOWEST` -> `NORMAL` -> `MONITOR`).
2. Async event'i (`AsyncPlayerChatEvent`) main thread'den tetikleme denemesi -> `IllegalStateException`.
3. Async event'i ayri bir thread'den tetikleme + `Cancellable`/`ignoreCancelled` + `setFormat`.
4. Concurrency stres testi: concurrent `register`/`unregister` altinda async dispatch
   (`AsyncPingEvent` plugin'in kendi custom event'i).
5. Plugin'i devre disi birakma: `PluginDisableEvent` -> `onDisable()` -> `HandlerList.unregisterAll(plugin)`.
