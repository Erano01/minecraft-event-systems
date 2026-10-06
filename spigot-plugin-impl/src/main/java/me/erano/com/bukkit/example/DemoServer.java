package me.erano.com.bukkit.example;

import me.erano.com.bukkit.Server;
import me.erano.com.bukkit.plugin.InvalidPluginException;
import me.erano.com.bukkit.plugin.Plugin;
import me.erano.com.bukkit.plugin.PluginDescriptionFile;
import me.erano.com.bukkit.plugin.PluginManager;
import me.erano.com.bukkit.plugin.SimplePluginManager;
import me.erano.com.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/*
 * Gercekte Server'i implemente eden CraftServer'in (server jar'i, OBC) demo karsiligi; bizim
 * kodumuz. Plugin'le ilgili adimlar CraftServer'daki sirayla:
 *  - loadPlugin  ~ CraftServer.loadPlugins: registerInterface(JavaPluginLoader.class) ->
 *    pluginManager.loadPlugin -> "Loading ..." logu -> plugin.onLoad().
 *  - enablePlugins ~ CraftServer.enablePlugin (permission kaydi haric) -> pluginManager.enablePlugin.
 *  - disablePlugins ~ CraftServer.disablePlugins -> pluginManager.disablePlugins().
 *  - isPrimaryThread ~ CraftServer.isPrimaryThread: "Thread.currentThread().equals(console.serverThread)
 *    || console.hasStopped() || RestartCommand.restarting". Bizde serverThread = bu nesneyi
 *    olusturan thread; stop/restart durumu yok.
 */
public class DemoServer implements Server {
    private final Thread serverThread;
    private final Logger logger;
    private final SimplePluginManager pluginManager;

    public DemoServer() {
        this.serverThread = Thread.currentThread();
        this.logger = createConsoleLogger();
        this.pluginManager = new SimplePluginManager(this);
        this.pluginManager.registerInterface(JavaPluginLoader.class);
    }

    @NotNull
    public Plugin loadPlugin(@NotNull PluginDescriptionFile description) throws InvalidPluginException {
        Plugin plugin = this.pluginManager.loadPlugin(description);
        plugin.getLogger().info(String.format("Loading %s", plugin.getDescription().getFullName()));
        plugin.onLoad();
        return plugin;
    }

    public void enablePlugins() {
        for (Plugin plugin : this.pluginManager.getPlugins()) {
            this.pluginManager.enablePlugin(plugin);
        }
    }

    public void disablePlugins() {
        this.pluginManager.disablePlugins();
    }

    @Override
    @NotNull
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    @NotNull
    public PluginManager getPluginManager() {
        return this.pluginManager;
    }

    @Override
    public boolean isPrimaryThread() {
        return Thread.currentThread().equals(this.serverThread);
    }

    // Spigot konsolundaki "[HH:mm:ss LEVEL]: mesaj" bicimi. Listener'larin System.out ciktisiyla
    // sira karismasin diye stdout'a yaziyor ve her kayitta flush ediyor.
    private static Logger createConsoleLogger() {
        Formatter formatter = new Formatter() {
            private final DateTimeFormatter time = DateTimeFormatter.ofPattern("HH:mm:ss");

            @Override
            public String format(LogRecord record) {
                StringBuilder sb = new StringBuilder()
                        .append('[').append(LocalTime.now().format(this.time)).append(' ').append(record.getLevel()).append("]: ")
                        .append(formatMessage(record)).append(System.lineSeparator());
                if (record.getThrown() != null) {
                    StringWriter sw = new StringWriter();
                    record.getThrown().printStackTrace(new PrintWriter(sw));
                    sb.append(sw);
                }
                return sb.toString();
            }
        };
        Handler handler = new StreamHandler(System.out, formatter) {
            @Override
            public synchronized void publish(LogRecord record) {
                super.publish(record);
                flush();
            }
        };
        Logger logger = Logger.getLogger("DemoServer");
        logger.setUseParentHandlers(false);
        logger.addHandler(handler);
        return logger;
    }
}
