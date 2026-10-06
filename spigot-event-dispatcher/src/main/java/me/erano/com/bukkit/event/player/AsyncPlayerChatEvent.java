package me.erano.com.bukkit.event.player;

import me.erano.com.bukkit.entity.Player;
import me.erano.com.bukkit.event.Cancellable;
import me.erano.com.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.IllegalFormatException;
import java.util.Set;

/*
 * 'async' parametre olarak gelir, sabit degildir. Server jar'inda tek cagiran
 * ServerGamePacketListenerImpl.chat(String, PlayerChatMessage, boolean async):
 * ag thread'inden gelen sohbet paketi icin async=true, sunucunun kendi tetikledigi
 * (or. komutla) sohbet icin async=false. SimplePluginManager.callEvent bu bayraga gore
 * dogru thread'de olunup olunmadigini dogrular.
 */
public class AsyncPlayerChatEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancel;
    private String message;
    private String format;
    private final Set<Player> recipients;

    public AsyncPlayerChatEvent(boolean async, @NotNull Player who, @NotNull String message, @NotNull Set<Player> players) {
        super(who, async);
        this.cancel = false;
        this.format = "<%1$s> %2$s";
        this.message = message;
        this.recipients = players;
    }

    @NotNull
    public String getMessage() {
        return this.message;
    }

    public void setMessage(@NotNull String message) {
        this.message = message;
    }

    @NotNull
    public String getFormat() {
        return this.format;
    }

    public void setFormat(@NotNull String format) throws IllegalFormatException, NullPointerException {
        try {
            String.format(format, this.player, this.message);
            this.format = format;
        } catch (RuntimeException ex) {
            ex.fillInStackTrace();
            throw ex;
        }
    }

    @NotNull
    public Set<Player> getRecipients() {
        return this.recipients;
    }

    @Override
    public boolean isCancelled() {
        return this.cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
