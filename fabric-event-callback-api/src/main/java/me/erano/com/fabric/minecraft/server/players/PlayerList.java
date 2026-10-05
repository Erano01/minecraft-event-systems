package me.erano.com.fabric.minecraft.server.players;

import me.erano.com.fabric.fabric.api.message.v1.ServerMessageEvents;
import me.erano.com.fabric.minecraft.network.chat.ChatType;
import me.erano.com.fabric.minecraft.network.chat.PlayerChatMessage;
import me.erano.com.fabric.minecraft.server.level.ServerPlayer;

// Minecraft stub'i. Event cagrilari, Fabric API'nin PlayerListMixin'inin broadcastChatMessage'in
// basina enjekte ettigi kodun aynisi (@At("HEAD"), cancellable = true).
public class PlayerList {
    public void broadcastChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound boundChatType) {
        // --- PlayerListMixin.onSendChatMessage ---
        if (!ServerMessageEvents.ALLOW_CHAT_MESSAGE.invoker().allowChatMessage(message, sender, boundChatType)) {
            return; // ci.cancel()
        }
        ServerMessageEvents.CHAT_MESSAGE.invoker().onChatMessage(message, sender, boundChatType);
        // --- vanilla: mesaji tum oyunculara gonder (stub'da yok) ---
    }
}
