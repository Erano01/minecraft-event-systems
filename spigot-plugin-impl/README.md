# spigot-plugin-impl

`spigot-event-dispatcher`'a bagimli, gercek bir Bukkit plugin jar'inin yerini tutan modul.
Iliski gercek dunyadaki gibi: `spigot-event-dispatcher` = spigot-api, bu modul = onun
uzerine yazilmis bir plugin.

## Icerik

- **`ExamplePlugin`**: `JavaPlugin`'i extend eden plugin ana sinifi. Public no-arg kurucu,
  `onEnable()` icinde `getServer().getPluginManager().registerEvents(...)` (gercek plugin'lerin yaptigi sey).
- **`ExampleListener`** / **`PingCounterListener`**: `PlayerJoinEvent`, `AsyncPlayerChatEvent`,
  `PluginEnableEvent`/`PluginDisableEvent` ve `AsyncPingEvent`'i dinleyen Observer implementasyonlari.
- **`event.AsyncPingEvent`**: plugin'in kendi custom event'i (gercek Bukkit'te yok), concurrency testi icin.
- **`DemoServer`** / **`DemoPlayer`**: gercekte server jar'inda olan `CraftServer` / `CraftPlayer`'in
  demo karsiliklari (bizim kodumuz).
- **`Main`**: plugin'i yukler/enable eder, event'leri fiilen tetikler (sync, async, concurrency
  stres testi), sonra plugin'i disable eder.

Detaylar (sinif haritasi, concurrency) icin bkz.
[`spigot-event-dispatcher/README.md`](../spigot-event-dispatcher/README.md).

## Calistirma

```
./gradlew -q :spigot-plugin-impl:run
```
