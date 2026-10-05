# Forge Event Sisteminin Concurrency Modeli

Kaynak: Forge 26.1.2-64.0.8 (`eventbus-7.0.1`, `fmlcore`, `javafmllanguage`, `forge-universal`), JADX ile incelendi.
Terimler *Java Concurrency in Practice* (JCiP) kitabındaki anlamlarıyla kullanıldı.

Forge iki ayrı concurrency problemini çözüyor. Event bus, Spigot'taki gibi "okuması çok, yazması az bir
listener listesi" problemini çözüyor; yöntem aynı ama daha modern. Mod yükleme tarafında ise Spigot'ta
karşılığı olmayan bir şey var: lifecycle event'leri `ForkJoinPool` üzerinde paralel dağıtılıyor ve
sıralama kilitlerle değil `CompletableFuture` grafiğiyle sağlanıyor.

Stateless → Immutable + Safe Publication → Thread Confinement → Atomic Wrapper → Locking sıralamasında
event bus ilk iki adımda ve yazma tarafındaki kilitte, mod yükleme ise thread confinement'ta yoğunlaşıyor.
Spigot'tan farklı olarak atomic wrapper ve `java.util.concurrent` koleksiyonları da kullanılıyor.

## Event bus: dispatch akışı

```java
post(event) → getInvoker().test/accept(event)
getInvoker():  invokerCallSite.getTarget()   // kilitsiz volatile okuma
               null ise buildInvoker()       // synchronized(backingList): sırala, lambda zinciri kur, setTarget
addListener / removeListener:                // synchronized(backingList): listeyi değiştir, setTarget(NULL)
```

Spigot'taki `volatile RegisteredListener[] handlers` + `bake()` deseninin aynısı. Farklar:

- **Snapshot dizi değil, fonksiyon.** `InvokerFactory` listener listesini tek bir `Consumer`/`Predicate`'e
  derliyor. 0–4 listener için açık yazılmış lambda'lar, fazlası için dizi üzerinde döngü üretiliyor.
  Üretilen lambda yakaladığı diziyi bir daha değiştirmiyor; yani snapshot yine immutable.
- **Volatile alan yerine `VolatileCallSite`.** `getTarget()`/`setTarget()` volatile semantiğine sahip;
  safe publication'ı bu sağlıyor. Hedef, sabit bir değer döndüren bir `MethodHandle`
  (`MethodHandles.constant`). Neden düz bir volatile alan kullanılmadığı koddan anlaşılmıyor.
- **Retry döngüsü yok.** Spigot'ta `bake()` sonrası dizi yeniden okunuyordu; burada `buildInvoker()`
  kurduğu invoker'ı doğrudan döndürüyor. Bunun bedeli: aynı anda birkaç thread invoker'ı geçersiz
  bulursa her biri kilidi sırayla alıp yeniden kuruyor. Gereksiz iş ama zararsız.
- **Dispatch maliyeti.** Dispatch anında `Method.invoke`, `isEnabled` kontrolü ya da `instanceof`
  filtresi yok. Listener'lar kayıt anında `LambdaMetafactory` ile gerçek `Consumer` sınıflarına
  dönüştürülüyor; event kalıtımı da kayıt anında, listener'ları alt tiplerin bus'larına kopyalayarak
  çözülüyor.

## State bazında senkronizasyon politikası

| State | Ne tür bir state | Nasıl korunuyor |
|---|---|---|
| Listener kayıtları (`EventListenerImpl.*` record'ları) | Immutable | Record'un final alanları, initialization safety |
| Invoker lambda'sı | Effectively immutable | `VolatileCallSite` ile safe publication |
| `backingList`, `monitorBackingList`, `children` | Instance-confined mutable state | Hepsi `backingList`'in monitörü ile korunuyor (yazma yolu) |
| `shutdownFlag`, `alreadyInvalidated` | Bağımsız boolean bayraklar | `AtomicBoolean`; `startup`/`shutdown` CAS ile idempotent |
| `BusGroupImpl.eventBuses` | Paylaşılan harita | `ConcurrentHashMap`, oluşturma `putIfAbsent` ile |
| `BUS_GROUP_NAMES`, `LMF_CACHE` | Global harita/küme | `ConcurrentHashMap.newKeySet()` / `computeIfAbsent` |
| `ModLoadingContext` (aktif mod) | Thread başına state | `ThreadLocal` |
| `DeferredWorkQueue.tasks` | Worker'lardan main thread'e iş aktarımı | `ConcurrentLinkedDeque` |
| `ModContainer.modLoadingStage`, `FMLModContainer.modInstance` | Volatile olmayan alanlar | `CompletableFuture` zincirinin happens-before'u |
| `DeferredWorkQueue.workQueues` (static `HashMap`) | Global, bir kez doldurulan | Class init (enum constructor'ı) ile safe publication |

**Kilit kapsamı:** `buildInvoker()` kilit içinde listeyi sıralayıp lambda kuruyor; bu kısa bir iş.
Listener çalıştırma tamamen kilit dışında. Kalıtım zincirinde `addListener` önce kendi kilidini, sonra
çocuk bus'ların kilidini alıyor. Sıra her zaman üst tipten alt tipe olduğu için deadlock oluşmuyor.

## Mod yükleme: thread confinement ve future grafiği

```
ModLoader (main thread)
  └─ her mod için: allOf(bağımlılıkların future'ları).thenRunAsync(…, ForkJoinPool "modloading-worker-N")
       ├─ ModLoadingContext.setActiveContainer(mod)   // ThreadLocal
       ├─ activityMap (CONSTRUCT ise mod constructor'ı + @EventBusSubscriber kaydı)
       └─ mod.acceptEvent(FMLCommonSetupEvent)          // modun kendi bus'ına post
  └─ tüm mod'lar bitince: DeferredWorkQueue.runTasks()  // sync executor = main thread
  └─ main thread bu sırada waitForTransition içinde sync executor kuyruğunu boşaltıyor
```

- **Sıralama kilitle değil, grafikle.** Bağımsız mod'lar aynı anda farklı worker'larda çalışıyor;
  bağımlı mod, bağımlılığının future'ı tamamlanmadan başlamıyor. Demoda `examplemod` ile
  `independentmod` aynı anda çalışıyor, `dependentmod` ise `examplemod`'u bekliyor.
- **Volatile olmayan alanlar yine de güvenli.** Bir future'ın tamamlanması ile ona bağlı aşamanın
  başlaması arasında happens-before ilişkisi var (`java.util.concurrent` garantisi). `modLoadingStage`
  ve `modInstance` bir worker'da yazılıp sonraki aşamada başka bir worker'da okunabiliyor.
- **Thread-safe olmayan işler main thread'e devrediliyor.** `enqueueWork` işi hemen çalıştırmıyor,
  aşamanın kuyruğuna ekliyor. Aşama sonunda kuyruk main thread'de sırayla çalışıyor. JCiP'teki
  Swing `invokeLater` örneğinin aynısı: paylaşılan state'e dokunan iş tek bir thread'e confine ediliyor.
- **Aktif mod bilgisi `ThreadLocal`.** Parametresiz mod constructor'ları `FMLJavaModLoadingContext.get()`
  ile kendi context'lerine ulaşabiliyor, çünkü her worker kendi aktif container'ını görüyor. Bu yol
  gerçekte deprecated; JCiP'in "ThreadLocal gizli bir parametreye dönüşebilir" uyarısının örneği.
  Yeni yol context'i constructor parametresi olarak vermek.

## Zayıf noktalar

- **`BusGroupImpl.getOrCreateEventBus` yarışı.** Yeni bus kilit dışında oluşturuluyor ve oluşturma
  sırasında üst tiplerin `children` listesine ekleniyor. Bu ekleme üst bus'ın kilidi alınmadan yapılıyor;
  üst bus'ın `backingList`'i de kilitsiz okunuyor. `children` normalde `backingList` monitörüyle korunan
  bir liste; burada bu kural çiğneniyor. Aynı tip için iki thread aynı anda bus oluşturursa, yarışı
  kaybeden bus haritaya girmiyor ama `children`'da kalıyor. Paralel CONSTRUCT aşamasında birden fazla
  mod game bus'a aynı anda kayıt yaptığı için bu yarış teorik değil.
- **Game bus'ta thread kuralı yok.** Spigot `isPrimaryThread()` ile yanlış thread'den fırlatılan event'i
  reddediyordu. Forge event bus'ı hangi thread'den `post` edildiğini kontrol etmiyor; game event'lerin
  doğru thread'den post edilmesi tamamen çağıranın, yani Forge'un patch'lediği Minecraft kodunun
  sorumluluğunda.
- **`MutableEvent` korumasız.** Listener'lar event'i senkronizasyon olmadan değiştiriyor. Güvenli, çünkü
  bir event'in dispatch'i tek thread'de gerçekleşiyor; event'i başka thread'e veren listener bu
  güvenceyi bozar.
- **Worker isimleri.** `ModWorkManager` thread'e `"modloading-worker-" + getPoolIndex()` adını veriyor.
  Factory içinde thread henüz havuza kaydolmadığı için `getPoolIndex()` hep 0 dönüyor (JDK 25'te
  denendi); tüm worker'lar aynı adı alıyor. Bu modülde sayaçla değiştirildi.

## Spigot ile karşılaştırma

| | Spigot | Forge |
|---|---|---|
| Listener deposu | Event sınıfı başına static `HandlerList` | `BusGroup` içinde event tipi başına `EventBus` |
| Okuma yolu | `volatile RegisteredListener[]`, retry döngüsü | `VolatileCallSite` içinde derlenmiş lambda |
| `update()` karşılığı | Reflection ile `EventExecutor` + `Method.invoke` | `LambdaMetafactory` ile üretilmiş `Consumer`/`Predicate` |
| İptal | `setCancelled`; diğer listener'lar `ignoreCancelled` ile eler | Listener `true` döner, zincir durur, sadece MONITOR çalışır |
| Thread kuralı | `isPrimaryThread()` ile zorunlu | Yok, çağıranın sorumluluğu |
| Plugin/mod yükleme | Main thread'de sırayla | `ForkJoinPool` + `CompletableFuture` grafiği, paralel |
| Kullanılan JUC araçları | Yok | `ConcurrentHashMap`, `AtomicBoolean`, `ConcurrentLinkedDeque`, `ForkJoinPool`, `CompletableFuture`, `ThreadLocal` |

Canlı doğrulama: `forge-mod-impl` → `Main`. 1. adım paralel lifecycle'ı ve `enqueueWork`'ü, 4. adım
concurrent `addListener`/`removeListener` altında kilitsiz `post`'u çalıştırıyor.
