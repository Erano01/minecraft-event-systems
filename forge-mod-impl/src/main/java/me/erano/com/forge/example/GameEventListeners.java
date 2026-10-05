package me.erano.com.forge.example;

import me.erano.com.forge.event.ServerChatEvent;
import me.erano.com.forge.event.entity.player.PlayerEvent;
import me.erano.com.forge.eventbus.api.listener.Priority;
import me.erano.com.forge.eventbus.api.listener.SubscribeEvent;

// Game bus listener'lari. Her metot kayit aninda LambdaMetafactory ile bir Consumer/Predicate'e donusur.
public class GameEventListeners {

    @SubscribeEvent(priority = Priority.HIGH)
    public void onLoginHigh(PlayerEvent.PlayerLoggedInEvent event) {
        System.out.println("  [HIGH]    " + event.getEntity() + " giris yapti");
    }

    @SubscribeEvent
    public void onLoginNormal(PlayerEvent.PlayerLoggedInEvent event) {
        System.out.println("  [NORMAL]  Hos geldin, " + event.getEntity() + "!");
    }

    @SubscribeEvent(priority = Priority.MONITOR)
    public void onLoginMonitor(PlayerEvent.PlayerLoggedInEvent event) {
        System.out.println("  [MONITOR] audit-log: " + event.getEntity());
    }

    // boolean donus -> Predicate: true donerse event iptal edilir ve zincir burada durur.
    @SubscribeEvent(priority = Priority.HIGH)
    public boolean filterSpam(ServerChatEvent event) {
        boolean spam = event.getMessage().contains("spam");
        System.out.println("  [HIGH]    spam filtresi: " + (spam ? "IPTAL" : "gecti"));
        return spam;
    }

    @SubscribeEvent
    public void broadcast(ServerChatEvent event) {
        System.out.println("  [NORMAL]  <" + event.getUsername() + "> " + event.getMessage());
    }

    // 2 parametre -> iptal-farkinda MONITOR: iptal edilse bile calisir, iptal edemez.
    @SubscribeEvent(priority = Priority.MONITOR)
    public void logChat(ServerChatEvent event, boolean wasCancelled) {
        System.out.println("  [MONITOR] chat-log: \"" + event.getMessage() + "\" iptal=" + wasCancelled);
    }
}
