package me.erano.com.bukkit.plugin;

import org.jetbrains.annotations.NotNull;

/*
 * Gercek org.bukkit.plugin.Plugin arayuzu "TabExecutor"i extend eder ve 26 metot bildirir.
 * Imza duzeyinde bile birebir kopyalamak, hicbiri bu projenin konusu (event dispatch +
 * concurrency + GoF Observer) olmayan, aralarinda yuzlerce ek Bukkit tipi (World, Player,
 * Recipe, BossBar, ScoreboardManager, NamespacedKey...) gerektiren dev alt sistemleri
 * derlensin diye var etmemizi gerektirir. O yuzden burada SADECE plugin yasam donguesu +
 * kimlik ile ilgili kismi (asagida) aliyoruz; geri kalan gercek metotlar ve ait olduklari
 * ekosistem parcasi soyle:
 *
 *  - getDataFolder(), getResource(String), saveResource(String, boolean), saveConfig(),
 *    saveDefaultConfig(), reloadConfig(), getConfig() -> FileConfiguration/YamlConfiguration
 *    zinciri (org.bukkit.configuration.*). Sadece ConfigurationSection arayuzu bile 65+ metot
 *    (getInt/getString/getList/getItemStack/getLocation/getVector/isX...) tasir; SnakeYAML
 *    tabanli plugin.yml/config.yml okuma-yazma katmanidir.
 *
 *  - getDescription() -> PluginDescriptionFile: plugin.yml'den parse edilen isim/versiyon/
 *    yazar/depend/softdepend/permission-default metadata'si. Bizde plugin.yml/jar yok.
 *
 *  - getPluginLoader() -> gercek org.bukkit.plugin.PluginLoader: jar dosyasindan
 *    PluginClassLoader kurup .class dosyalarini yukleyen mekanizma. Bizim JavaPluginLoader
 *    (bkz. plugin.java.JavaPluginLoader) SADECE createRegisteredListeners'i tasiyor, bu
 *    metodun tam karsiligi degil - o yuzden Plugin'e baglanmiyor.
 *
 *  - getServer() -> org.bukkit.Server: TEK BASINA 130+ metot (dunya/entity/oyuncu yonetimi,
 *    BukkitScheduler, ServicesManager, Messenger, ban listeleri, boss bar, scoreboard,
 *    resource pack, crafting/recipe sistemi...). Bu projenin event/concurrency kapsaminin
 *    disinda, sunucu API'sinin tamami.
 *
 *  - getDefaultWorldGenerator(String, String), getDefaultBiomeProvider(String, String) ->
 *    ChunkGenerator/BiomeProvider: world generation pipeline'i, tamamen ayri bir konu.
 *
 *  - getLogger() -> PluginLogger (java.util.logging.Logger'i plugin adiyla prefixleyen ince
 *    bir sarmalayici). Biz System.out/err kullaniyoruz (bkz. SimplePluginManager.fireEvent).
 *
 *  - Plugin'in extend ettigi TabExecutor (-> CommandExecutor.onCommand +
 *    TabCompleter.onTabComplete): komut sistemi (Command, CommandSender, PluginCommand,
 *    SimpleCommandMap). Ayri bir alt sistem, event dispatch'le ilgisi yok.
 *
 * Asagidaki 7 metot ise TAM OLARAK event sisteminin ihtiyac duydugu, gercekte kullandigimiz
 * kisim: RegisteredListener.getPlugin()/HandlerList.register-unregister, SimplePluginManager
 * icindeki isEnabled() kontrolleri (fireEvent, registerEvents, registerEvent) ve
 * JavaPlugin'deki onEnable/onDisable/onLoad yasam donguesu.
 */
public interface Plugin {

    @NotNull
    String getName();

    boolean isEnabled();

    void onLoad();

    void onEnable();

    void onDisable();

    boolean isNaggable();

    void setNaggable(boolean canNag);
}
