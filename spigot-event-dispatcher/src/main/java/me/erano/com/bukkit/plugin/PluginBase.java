package me.erano.com.bukkit.plugin;

/*
 * Gercek org.bukkit.plugin.PluginBase kucuk bir sinif: sadece equals/hashCode/getName'i
 * finalize eder. Gercek getName() govdesi "return getDescription().getName();" seklindedir -
 * yani plugin.yml'den (PluginDescriptionFile) gelir. Biz PluginDescriptionFile/plugin.yml
 * parsing'ini implement etmedigimizden (bkz. Plugin.java basindaki yorum), getName()'i burada
 * FINAL YAPAMIYORUZ; bu sorumlulugu JavaPlugin'e devrediyoruz (JavaPlugin kendi 'name'
 * alanini dogrudan constructor'dan aliyor). equals/hashCode ise gercek Bukkit ile birebir:
 * bir Plugin'in kimligi tamamen adina dayanir (nesne referansina degil).
 */
public abstract class PluginBase implements Plugin {

    @Override
    public final int hashCode() {
        return getName().hashCode();
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Plugin)) {
            return false;
        }
        return getName().equals(((Plugin) obj).getName());
    }
}
