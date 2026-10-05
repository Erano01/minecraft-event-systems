# Fabric Event Sisteminin Concurrency Modeli

Kaynak: Fabric Loader 0.19.5, Fabric API 0.155.3+26.1.2 (`fabric-api-base`, `fabric-lifecycle-events-v1`,
`fabric-message-api-v1`), JADX ile incelendi. Terimler *Java Concurrency in Practice* (JCiP)
kitabındaki anlamlarıyla kullanıldı.

Fabric'te bus yok. Her event, kendi static alanında duran bir `Event<T>` nesnesi; `T` event'in
callback arayüzü. Event'i tetikleyen kod `EVENT.invoker().onX(...)` çağırıyor. Thread oluşturan,
executor kullanan ya da listener'ları paralel çalıştıran bir kod yok; mod yükleme de tek thread'de
ve sıralı.

Stateless → Immutable + Safe Publication → Thread Confinement → Atomic Wrapper → Locking sıralamasında
okuma yolu tamamen Immutable + Safe Publication'da. Kilit yalnızca kayıt tarafında var; atomic wrapper
kullanılmıyor.

## Event: okuma ve yazma yolu

```java
EVENT.invoker().onX(...)                 // okuma: tek bir volatile okuma, kilit yok
register(phase, listener):               // yazma: synchronized(lock)
    faz dizisini kopyala + yeni listener'ı ekle      (EventPhaseData.addListener, copy-on-write)
    fazları sırayla tek bir YENİ diziye kopyala       (rebuildInvoker)
    invoker = invokerFactory.apply(yeniDizi)          (volatile yazma)
```

- **Invoker hep geçerli.** Her `register` invoker'ı hemen, kilit altında yeniden kuruyor. Geçersiz kılıp
  sonradan tembelce kurma (null işareti) yok; okuma tarafında ne kontrol ne retry gerekiyor. Bedel
  yazma tarafında: her kayıt tüm listener'ları kopyalıyor (O(n)). Kayıtların neredeyse tamamı mod
  başlatılırken yapıldığı için bu kabul edilebilir.
- **Snapshot hiç değişmiyor.** `invokerFactory`'nin ürettiği lambda listener dizisini yakalıyor. O dizi
  yayınlandıktan sonra bir daha yazılmıyor; sonraki kayıtlar yeni dizi oluşturuyor. Tek fazlı event'lerde
  `handlers` doğrudan fazın dizisine işaret ediyor, ama `addListener` da eski diziyi değil kopyasını
  değiştirdiği için bu kural bozulmuyor.
- **Unregister yok.** Listener eklenebiliyor ama çıkarılamıyor. Bu, eşzamanlı silme senaryolarını tamamen
  ortadan kaldırıyor; karşılığında "aboneliği bırakılmamış listener" sorunu tasarım gereği çözülmüyor.
- **Event semantiği invoker'da.** Çekirdekte öncelik, iptal ya da hata yakalama kavramı yok. Her event
  kendi `invokerFactory` lambda'sında listener'ları nasıl çağıracağına karar veriyor: tick event'leri hepsini
  sırayla çağırıyor; `ALLOW_CHAT_MESSAGE` ilk `false`'ta durup `false` dönüyor. Bir listener exception
  fırlatırsa zincir kırılıyor; bunu engelleyen bir katman yok.
- **Sıralama faz grafiğiyle.** Öncelik sayısı yerine isimli fazlar (`Identifier`) ve aralarındaki
  `addPhaseOrdering(a, b)` kenarları var. `NodeSorting` önce güçlü bağlı bileşenleri buluyor (döngüleri
  tespit edip uyarıyor), sonra bileşenler arasında topolojik sıralama yapıyor; sırası belirsiz fazlar
  arasında `Identifier` karşılaştırması belirleyici. Faz grafiği thread-safe değil; sadece event'in kilidi
  altında kullanılıyor.

## State bazında senkronizasyon politikası

| State | Ne tür bir state | Nasıl korunuyor |
|---|---|---|
| `Event.invoker` | Mutable referans, effectively immutable referent | `volatile` ile safe publication |
| `ArrayBackedEvent.handlers`, `phases`, `sortedPhases`, `EventPhaseData.listeners` | Instance-confined mutable state | Özel `lock` nesnesi (`synchronized(lock)`), sadece yazma yolunda |
| `SortableNode` (faz grafiği, `visited` bayrağı) | Thread-safe olmayan yardımcı yapı | Sahibi olan event'in kilidi |
| `EventFactoryImpl.ARRAY_BACKED_EVENTS` | Global küme | Guava `MapMaker().weakKeys()` ile concurrent ve weak bir harita |
| Event static alanları (`ServerTickEvents.END_SERVER_TICK` ...) | Global, bir kez atanan | Class init (static initializer) ile safe publication |
| `EntrypointStorage.entryMap` | Bir kez doldurulan harita | Loader başlarken tek thread'de doldurulup sonra yalnızca okunuyor |
| `EntrypointContainerImpl.instance`, `NewEntry.instanceMap` | Lazy oluşturulan nesneler | `synchronized` metot (lazy init) |
| `MinecraftServer` iş kuyruğu | Thread'ler arası iş aktarımı | Concurrent kuyruk; işler server thread'inde tick sırasında çalışıyor (bu modülde stub) |

`ArrayBackedEvent`'in kilidi `this` değil, `private final Object lock`. Dışarıdan biri event nesnesi
üzerinde `synchronized` kullansa bile kayıt kilidine karışamıyor. JCiP'teki "private lock" deseni.

## Thread modeli

```
main thread       : FabricLoaderImpl.load() → Hooks.startServer → "main" sonra "server" entrypoint'leri, SIRAYLA
                    (mod'lar kayıtlarını burada yapar)
Netty thread'leri : gelen paketi işler, işi server.execute(...) ile server'ın kuyruğuna bırakır
Server thread     : tick döngüsü; lifecycle, tick ve chat event'leri burada tetiklenir
```

- **Mod yükleme sıralı.** `invokeEntrypoints` entrypoint'leri çağıran thread'de, birer birer çağırıyor.
  Bir mod hata verirse diğerleri yine çağrılıyor, hatalar toplanıp sonda birlikte fırlatılıyor.
- **Kayıtlar yayınlamadan önce, event'ler yayınlamadan sonra.** Listener'lar main thread'de kaydedilip
  invoker'lar volatile alanlara yazılıyor; server thread'i daha sonra bu alanları okuyor. Volatile okuma
  güncel invoker'ı ve onun yakaladığı diziyi güvenle görüyor.
- **Server thread'e devir.** Gerçek `MinecraftServer` bir `Executor`. Fabric API bunu kullanıyor:
  `MinecraftServerMixin`, `END_DATA_PACK_RELOAD` event'ini asenkron reload bitince
  `handleAsync(..., (Executor) server)` ile server thread'inde tetikliyor. Event'in hangi thread'de
  çalışacağını kilit değil, işin server'ın kuyruğuna verilmesi belirliyor (thread confinement).
- **Thread kuralı yok.** `invoker()` hangi thread'den çağrıldığını kontrol etmiyor. Event'lerin server
  thread'inde tetiklenmesi, mixin'lerin enjekte edildiği yerlerin zaten server thread'inde çalışmasından
  geliyor; bir mod event'i başka bir thread'den tetiklerse hiçbir şey onu durdurmuyor.

## Zayıf noktalar

- **`EventFactory.invalidate()` kilitsiz.** Tüm event'lerde `update()` çağırıyor; `update()` ise
  `handlers`'ı kilit almadan okuyup invoker'ı yeniden yazıyor. `handlers` volatile değil. Aynı anda bir
  `register` çalışıyorsa eski bir `handlers` dizisiyle kurulan invoker, yeni kurulmuş olanın üzerine
  yazılabilir; o anda eklenen listener invoker'dan düşer (lost update). Metot gerçekte
  `@Deprecated(forRemoval = true)`.
- **Kayıt maliyeti doğrusal.** Her `register` tüm listener'ları kopyalıyor. Oyun sırasında sürekli kayıt
  yapan bir mod, her seferinde bütün diziyi kopyalatır.
- **Listener hatası zinciri kırar.** Çekirdekte hata yakalama yok; bir listener'ın exception'ı aynı
  event'teki sonraki listener'ları ve tetikleyen oyun kodunu etkiler.

Canlı doğrulama: `fabric-mod-impl` → `Main`. 4. adımda publisher thread'ler `invoker()`'ı çağırırken
registrar thread'ler aynı event'e 500 listener ekliyor; sayaç tüm ping'leri görüyor ve hiçbir kayıt
kaybolmuyor.
