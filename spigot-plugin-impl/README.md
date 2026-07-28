# spigot-plugin-impl

`spigot-event-dispatcher`'a bagimli, gercek bir Bukkit plugin jar'inin yerini tutan modul.
Iliski gercek dunyadaki gibi: `spigot-event-dispatcher` = spigot-api, bu modul = onun
uzerine yazilmis bir plugin.

## Icerik

- **`ExamplePlugin`** — `JavaPlugin`'i extend eden plugin ana sinifi. `onEnable()` icinde
  kendi listener'ini kaydediyor (gercek plugin'lerin yaptigi sey).
- **`ExampleListener`** / **`PingCounterListener`** — `spigot-event-dispatcher`'daki event
  tiplerini (`PlayerJoinEvent`, `AsyncPlayerChatEvent`, `AsyncPingEvent`) dinleyen Observer
  implementasyonlari.
- **`Main`** — "sunucu"yu temsil eden bootstrap: plugin'i yukler/enable eder, event'leri fiilen
  tetikler (sync, async, concurrency stres testi), sonra plugin'i disable eder.

Detaylar (mimari, concurrency mekanizmalari, sinif haritasi) icin bkz.
[`spigot-event-dispatcher/README.md`](../spigot-event-dispatcher/README.md).

## Calistirma

```
./gradlew :spigot-event-dispatcher:compileJava :spigot-plugin-impl:compileJava
java -cp spigot-event-dispatcher/build/classes/java/main:spigot-plugin-impl/build/classes/java/main:<annotations-jar> \
     me.erano.com.bukkit.example.Main
```
