package me.erano.com.fabric.fabric.api.message.v1;

import me.erano.com.fabric.fabric.api.event.Event;
import me.erano.com.fabric.fabric.api.event.EventFactory;
import me.erano.com.fabric.minecraft.network.chat.ChatType;
import me.erano.com.fabric.minecraft.network.chat.PlayerChatMessage;
import me.erano.com.fabric.minecraft.server.level.ServerPlayer;

// Iptal kavrami cekirdekte degil, event'in invoker'inda: ALLOW_* event'leri ilk 'false'ta durur.
// Gercekte ayrica ALLOW_GAME_MESSAGE, ALLOW_COMMAND_MESSAGE, GAME_MESSAGE, COMMAND_MESSAGE var.
public final class ServerMessageEvents {
    public static final Event<AllowChatMessage> ALLOW_CHAT_MESSAGE = EventFactory.createArrayBacked(AllowChatMessage.class, handlers -> (message, sender, boundChatType) -> {
        for (AllowChatMessage handler : handlers) {
            if (!handler.allowChatMessage(message, sender, boundChatType)) {
                return false;
            }
        }
        return true;
    });
    public static final Event<ChatMessage> CHAT_MESSAGE = EventFactory.createArrayBacked(ChatMessage.class, handlers -> (message, sender, boundChatType) -> {
        for (ChatMessage handler : handlers) {
            handler.onChatMessage(message, sender, boundChatType);
        }
    });

    private ServerMessageEvents() {
    }

    @FunctionalInterface
    public interface AllowChatMessage {
        boolean allowChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound boundChatType);
    }

    @FunctionalInterface
    public interface ChatMessage {
        void onChatMessage(PlayerChatMessage message, ServerPlayer sender, ChatType.Bound boundChatType);
    }
}
