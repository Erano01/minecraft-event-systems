# Spigot Event Sisteminin Concurrency Modeli

Kaynak: `spigot-26.2.jar` → `META-INF/libraries/spigot-api-26.2-R0.1-SNAPSHOT.jar` (JADX + `javap`).
Terimler *Java Concurrency in Practice* (JCiP) kitabındaki anlamlarıyla kullanıldı.

Spigot event sisteminde thread oluşturan, executor kullanan, atomic wrapper ya da `java.util.concurrent`
koleksiyonu kullanan hiçbir kod yok. Thread safety üç mekanizmayla sağlanıyor:

- Sync event'ler main thread'e confine ediliyor; bu kural çalışma anında kontrol ediliyor.
- Listener listesi immutable bir snapshot olarak `volatile` alan üzerinden yayınlanıyor.
- Bu snapshot'ı üreten mutable yapılar intrinsic lock ile korunuyor.

Stateless → Immutable + Safe Publication → Thread Confinement → Atomic Wrapper → Locking
sıralamasında Immutable + Safe Publication ve Thread Confinement ağırlıkta. Locking yalnızca
yazma tarafında var. Atomic Wrapper adımı hiç kullanılmıyor.

## Dispatch akışı

`callEvent` hiçbir kilit almıyor. Yalnızca çağıran thread'in doğru olup olmadığını kontrol ediyor:

```java
if (event.isAsynchronous()) {
    if (Thread.holdsLock(this))      throw ...; // PluginManager monitörü tutulurken async event fırlatılamaz
    if (server.isPrimaryThread())    throw ...; // async event main thread'de fırlatılamaz
} else if (!server.isPrimaryThread()) throw ...; // sync event yalnızca main thread'de fırlatılabilir
fireEvent(event);
```

`fireEvent` event'in `HandlerList`'inden snapshot dizisini alır. Diziyi düz bir `for` döngüsüyle,
çağıran thread'de ve kilitsiz gezer. Dolayısıyla "async event" ayrı thread'de çalışan event demek
değil; main thread dışından fırlatılması zorunlu olan event demek. Thread'i sağlamak çağıranın işi.

`Thread.holdsLock(this)` kontrolünün amacı bir liveness önlemi gibi görünüyor. `SimplePluginManager`'da
`loadPlugin`, `getPlugin` ve `getPlugins` `synchronized(this)`. Async bir listener main thread'i
beklerken main thread bu monitörü almaya çalışırsa deadlock oluşur. Kontrol, async event'in bu
monitör tutulurken fırlatılmasını baştan engelliyor. (Bu amaç koddan çıkarım; dokümante edilmemiş.)

## State bazında senkronizasyon politikası

| State | Ne tür bir state | Nasıl korunuyor |
|---|---|---|
| `RegisteredListener` (tüm alanlar `final`) | Immutable | Final field'lar sayesinde initialization safety. Senkronizasyon gerekmiyor. |
| `HandlerList.handlers` (`volatile RegisteredListener[]`) | Mutable reference + effectively immutable referent | Volatile ile safe publication. Dizi yayınlandıktan sonra hiç değiştirilmiyor. |
| `HandlerList.handlerslots` (`EnumMap<…, ArrayList>`) | Thread-safe olmayan, instance-confined mutable state | `private final`, dışarı hiç verilmiyor. Her erişim `synchronized(this)` altında; Java Monitor Pattern. |
| `HandlerList.allLists` (`static ArrayList`) | Global mutable state | Static initializer ile yayınlanıyor. Kendi monitörü ile korunuyor. `getHandlerLists()` savunmacı kopya (`clone()`) döndürüyor, liste escape etmiyor. |
| Event nesnesi | Mutable, kısa ömürlü | Thread confinement: fırlatan thread oluşturuyor, listener'lar aynı thread'de değiştiriyor. |
| Sync event'lerin dokunduğu oyun state'i | Paylaşılan mutable state | Main thread'e ad-hoc thread confinement (JCiP'teki Swing EDT örneğinin aynısı). |

**Volatile neden tek başına yetiyor?** `handlers` ile `handlerslots` arasında bir invariant var:
`handlers` ya `null` ya da `handlerslots`'un öncelik sırasına göre düzleştirilmiş kopyası.
JCiP'e göre invariant'a katılan bir değişkeni tek başına `volatile` yapmak yanlış.
Burada çalışmasının sebebi:

- Okuyan taraf `handlerslots`'a hiç dokunmuyor, yalnızca `handlers`'ı okuyor.
- `handlers`'a yapılan her yazma (`null` ile geçersiz kılma ya da `bake()`'in yeni dizi ataması)
  `handlerslots`'u koruyan aynı kilit altında yapılıyor. Böylece invariant hep o kilit altında korunuyor.
- Yazılan değer mevcut değere bağlı değil (`null` ya da sıfırdan kurulmuş bir dizi), yani compound action yok.

JCiP'teki "invariant'ı bir holder nesnesinde topla, holder'ı volatile yap" önerisinin uygulaması bu.
Holder burada snapshot dizisi.

**Okuma yolu:** `getRegisteredListeners()` diziyi okur. Dizi `null` ise `synchronized bake()`
çağırır ve döngüye devam eder. `bake()` bittikten sonra başka bir thread `handlers`'ı yeniden
`null` yapabilir; döngü bu yüzden var. Yeni bir listener eklenmediği sürece okuma tamamen kilitsiz.
Yazma nadir, okuma her event'te olduğu için bu dengeyi bilinçli seçmişler.

**Stale snapshot:** Dispatch sırasında başka bir thread listener silerse
`ConcurrentModificationException` oluşmaz; dispatch eski diziyi gezmeye devam eder. Bunun bedeli
şu: silinen bir listener o event'i yine de bir kez alabilir. Liste için tutarlılık, güncellik
yerine tercih edilmiş.

**Kilit kapsamı ve sırası:** Her `HandlerList` için tek bir mutex var (kaba taneli kilit).
Kilit içindeki işler kısa: listeye ekleme/çıkarma ve `bake()` sırasında dizi kopyalama.
Uzun sürebilecek tek iş olan listener çalıştırma kilit dışında yapılıyor; bu, JCiP'teki
"uzun işi lock dışında yap" kuralına uyuyor. Kilit sırası her zaman `allLists` → `h`
(`bakeAll`, `unregisterAll`, `getRegisteredListeners(Plugin)`). Instance metotlar yalnızca `h`
kilidini alıyor. Ters sırada kilit alan bir yol olmadığı için deadlock oluşmuyor.

**Split ownership:** `HandlerList` listenin yapısından sorumlu. Listener nesnelerinin ve
dokundukları state'in thread safety'si plugin'in sorumluluğunda. Bu, JCiP'teki `ServletContext`
örneğiyle aynı yapı.

## Zayıf noktalar

- **`JavaPlugin.isEnabled` düz `boolean`, volatile değil.** `fireEvent` her listener için
  `getPlugin().isEnabled()` okuyor. Async event'lerde bu okuma main thread dışında yapılıyor, ama
  değer main thread'de senkronizasyon olmadan yazılıyor. Happens-before ilişkisi yok, dolayısıyla
  devre dışı bırakılmış bir plugin async event'leri bir süre daha alabilir. Yine de
  `unregisterAll(plugin)` `synchronized` olduğu için listener'lar kısa sürede snapshot'tan düşüyor.
- **`HandlerList` constructor'ında `this` escape ediyor.** Constructor'ın son satırı
  `allLists.add(this)`. Pratikte güvenli: `handlerslots` bu satırdan önce dolduruluyor ve yayınlama
  `synchronized(allLists)` üzerinden, yani lock ile korunan bir alana yapılıyor. Ama sınıf `final`
  değil; bir alt sınıf kendi alanlarını constructor'da atarsa, nesne yarım haliyle görünür olabilir.
- **`Event.getEventName()`** adı ilk çağrıda hesaplayıp volatile olmayan bir alana yazıyor
  (racy single-check). İki thread aynı anda çağırırsa ikisi de aynı immutable `String`'i yazar;
  zararsız. JCiP'in `String.hashCode` için "kendi kodunuzda taklit etmeyin" dediği desen bu.

## Bu modülle karşılaştırma

`event/HandlerList.java` ve `plugin/SimplePluginManager#callEvent` mantık olarak birebir aynı.
Canlı doğrulama için `spigot-plugin-impl` → `Main.concurrencyDemo()`
kullanılıyor: publisher thread'ler `AsyncPingEvent` fırlatırken churn thread'ler aynı `HandlerList`'e
concurrent `register`/`unregister` yapıyor.

Server tarafı (`spigot-26.2-R0.1-SNAPSHOT.jar`, JADX):

- `CraftServer.isPrimaryThread()`: `Thread.currentThread().equals(console.serverThread) || console.hasStopped() || RestartCommand.restarting`.
  Bizde `DemoServer` kendisini oluşturan thread'i server thread kabul ediyor; stop/restart durumu yok.
- `AsyncPlayerChatEvent`'i oluşturan tek yer `ServerGamePacketListenerImpl.chat(String, PlayerChatMessage, boolean async)`.
  `async` bayrağı çağırandan geliyor: ağ thread'inden gelen sohbet paketi için `true`. Event sonrasında
  `PlayerChatEvent` (eski sync event) için listener varsa, iş bir `Waitable` ile main thread'in
  `processQueue`'suna verilip `waitable.get()` ile bekleniyor; async thread'den main thread'e geçişin yolu bu.
