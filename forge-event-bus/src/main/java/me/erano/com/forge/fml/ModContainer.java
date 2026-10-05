package me.erano.com.forge.fml;

import me.erano.com.forge.eventbus.api.bus.BusGroup;
import me.erano.com.forge.fml.event.IModBusEvent;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

// Bir mod'un kimligi, asamasi ve mod bus'i.
public abstract class ModContainer {
    protected final String modId;
    protected final ModInfo modInfo;
    // volatile degil: worker thread'de whenComplete icinde yazilir, sonraki asamada baska bir worker
    // okur. Aradaki happens-before'u CompletableFuture zinciri saglar (onceki asamanin join'i).
    protected ModLoadingStage modLoadingStage;
    protected Supplier<?> contextExtension;
    protected final Map<ModLoadingStage, Runnable> activityMap = new EnumMap<>(ModLoadingStage.class);
    final Set<ModContainer> dependencies = new HashSet<>();

    protected ModContainer(ModInfo info) {
        this.modId = info.modId();
        this.modInfo = info;
        this.modLoadingStage = ModLoadingStage.CONSTRUCT;
    }

    public abstract Object getMod();

    public final String getModId() {
        return modId;
    }

    public ModInfo getModInfo() {
        return modInfo;
    }

    public ModLoadingStage getCurrentState() {
        return modLoadingStage;
    }

    public BusGroup getModBusGroup() {
        return null;
    }

    protected <T extends IModBusEvent> void acceptEvent(T e) {
    }
}
