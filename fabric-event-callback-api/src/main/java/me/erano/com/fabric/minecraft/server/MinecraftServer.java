package me.erano.com.fabric.minecraft.server;

import me.erano.com.fabric.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import me.erano.com.fabric.fabric.api.event.lifecycle.v1.ServerTickEvents;
import me.erano.com.fabric.minecraft.server.players.PlayerList;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

// Minecraft stub'i. Gercek MinecraftServer bir Executor'dur (BlockableEventLoop): baska thread'lerin
// verdigi isler kuyruga girer ve server thread'inde tick sirasinda calisir. Fabric API bunu kullanir:
// MinecraftServerMixin, END_DATA_PACK_RELOAD'u handleAsync(..., (Executor) server) ile server thread'ine devreder.
// Event cagrilari, MinecraftServerMixin'in enjeksiyon noktalariyla ayni yerlerde.
public class MinecraftServer implements Executor {
    private final Queue<Runnable> pendingTasks = new ConcurrentLinkedQueue<>();
    private final PlayerList playerList = new PlayerList();
    private volatile Thread serverThread;
    private long tickCount;

    @Override
    public void execute(Runnable task) {
        pendingTasks.add(task);
    }

    public boolean isSameThread() {
        return Thread.currentThread() == serverThread;
    }

    public PlayerList getPlayerList() {
        return playerList;
    }

    public long getTickCount() {
        return tickCount;
    }

    public void runServer(int ticks) {
        serverThread = Thread.currentThread();
        ServerLifecycleEvents.SERVER_STARTING.invoker().onServerStarting(this); // mixin: initServer() cagrisindan once
        // vanilla: initServer()
        ServerLifecycleEvents.SERVER_STARTED.invoker().onServerStarted(this);   // mixin: buildServerStatus() cagrisinda
        for (int i = 0; i < ticks; i++) {
            tickServer();
        }
        stopServer();
    }

    private void tickServer() {
        tickCount++;
        ServerTickEvents.START_SERVER_TICK.invoker().onStartTick(this);         // mixin: tickChildren() cagrisindan once
        tickChildren();
        ServerTickEvents.END_SERVER_TICK.invoker().onEndTick(this);             // mixin: @At("TAIL")
    }

    // vanilla: dunyalari tick'ler; burada sadece diger thread'lerin birakti isler calistiriliyor.
    private void tickChildren() {
        Runnable task;
        while ((task = pendingTasks.poll()) != null) {
            task.run();
        }
    }

    private void stopServer() {
        ServerLifecycleEvents.SERVER_STOPPING.invoker().onServerStopping(this); // mixin: @At("HEAD")
        ServerLifecycleEvents.SERVER_STOPPED.invoker().onServerStopped(this);   // mixin: @At("TAIL")
    }
}
