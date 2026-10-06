package me.erano.com.bukkit.entity;

import org.jetbrains.annotations.NotNull;

/*
 * Gercek org.bukkit.entity.Player; HumanEntity, Conversable, OfflinePlayer, PluginMessageRecipient
 * gibi arayuzleri extend eder ve yuzlerce metot tasir. Event siniflarinin (PlayerEvent ve alt
 * siniflari) derlenebilmesi icin sadece getName() alindi. Implementasyonu gercekte CraftPlayer'dir
 * (server jar'inda, OBC); bizde spigot-plugin-impl icindeki DemoPlayer.
 */
public interface Player {

    @NotNull
    String getName();
}
