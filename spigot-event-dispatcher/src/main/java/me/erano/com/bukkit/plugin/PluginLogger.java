package me.erano.com.bukkit.plugin;

import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/*
 * JADX ciktisinda prefix dali "SelectorUtils.PATTERN_HANDLER_PREFIX + prefix + \"] \"" olarak
 * gorunuyor: JADX, degeri "[" olan bir derleme-zamani sabitini ilgisiz bir kutuphanenin
 * (plexus-utils) ayni degerli sabitine baglamis. Gercek ifade "[" + prefix + "] ".
 */
public class PluginLogger extends Logger {
    private String pluginName;

    public PluginLogger(@NotNull Plugin context) {
        super(context.getClass().getCanonicalName(), null);
        String prefix = context.getDescription().getPrefix();
        this.pluginName = prefix != null ? "[" + prefix + "] " : "[" + context.getDescription().getName() + "] ";
        setParent(context.getServer().getLogger());
        setLevel(Level.ALL);
    }

    @Override
    public void log(@NotNull LogRecord logRecord) {
        logRecord.setMessage(this.pluginName + logRecord.getMessage());
        super.log(logRecord);
    }
}
