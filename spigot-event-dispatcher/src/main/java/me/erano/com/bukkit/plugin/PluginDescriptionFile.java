package me.erano.com.bukkit.plugin;

import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.Pattern;

/*
 * Gercek sinif plugin.yml'i SnakeYAML ile parse eder (PluginDescriptionFile(InputStream/Reader)
 * -> loadMap) ve ~20 alan tasir: depend/softdepend/loadbefore, commands, permissions,
 * api-version, libraries, load order... Burada event/plugin hattinin kullandigi alanlar var:
 *  - name/rawName/version/main: kimlik, getFullName() (log mesajlari) ve JavaPluginLoader'in
 *    main sinifi bulmasi.
 *  - authors: SimplePluginManager.fireEvent'teki AuthorNagException mesaji.
 *  - prefix: PluginLogger.
 *  - provides: SimplePluginManager.loadPlugin'deki lookupNames kaydi.
 * Kurucu olarak gercekteki programatik 3 parametreli kurucu (name, version, main) birebir
 * alindi; bu kurucu authors'i null birakir (gercekte de oyle).
 */
public final class PluginDescriptionFile {
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9 _.-]+$");
    String rawName;
    private String name;
    private List<String> provides;
    private String main;
    private String version;
    private List<String> authors;
    private String prefix;

    public PluginDescriptionFile(@NotNull String pluginName, @NotNull String pluginVersion, @NotNull String mainClass) {
        this.rawName = null;
        this.name = null;
        this.provides = ImmutableList.of();
        this.main = null;
        this.version = null;
        this.authors = null;
        this.prefix = null;
        this.rawName = pluginName;
        this.name = pluginName;
        if (!VALID_NAME.matcher(this.name).matches()) {
            throw new IllegalArgumentException("name '" + this.name + "' contains invalid characters.");
        }
        this.name = this.name.replace(' ', '_');
        this.version = pluginVersion;
        this.main = mainClass;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    @NotNull
    public List<String> getProvides() {
        return this.provides;
    }

    @NotNull
    public String getVersion() {
        return this.version;
    }

    @NotNull
    public String getMain() {
        return this.main;
    }

    @NotNull
    public List<String> getAuthors() {
        return this.authors;
    }

    @Nullable
    public String getPrefix() {
        return this.prefix;
    }

    @NotNull
    public String getFullName() {
        return this.name + " v" + this.version;
    }

    @ApiStatus.Internal
    @NotNull
    public String getRawName() {
        return this.rawName;
    }
}
