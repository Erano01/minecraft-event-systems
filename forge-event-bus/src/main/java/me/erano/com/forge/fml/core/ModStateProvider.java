package me.erano.com.forge.fml.core;

import me.erano.com.forge.fml.ModLoadingPhase;
import me.erano.com.forge.fml.ModLoadingStage;
import me.erano.com.forge.fml.ModLoadingState;
import me.erano.com.forge.fml.event.lifecycle.FMLCommonSetupEvent;
import me.erano.com.forge.fml.event.lifecycle.FMLConstructModEvent;
import me.erano.com.forge.fml.event.lifecycle.FMLLoadCompleteEvent;

import java.util.List;

// Gercekte ayrica VALIDATE, CONFIG_LOAD, SIDED_SETUP, ENQUEUE_IMC, PROCESS_IMC var; hepsi ayni
// ParallelTransition mekanizmasini kullanir.
public final class ModStateProvider {
    public static final ModLoadingState CONSTRUCT = new ModLoadingState("CONSTRUCT", ModLoadingPhase.GATHER,
            new ParallelTransition(ModLoadingStage.CONSTRUCT, FMLConstructModEvent::new));
    public static final ModLoadingState COMMON_SETUP = new ModLoadingState("COMMON_SETUP", ModLoadingPhase.LOAD,
            new ParallelTransition(ModLoadingStage.COMMON_SETUP, FMLCommonSetupEvent::new));
    public static final ModLoadingState COMPLETE = new ModLoadingState("COMPLETE", ModLoadingPhase.COMPLETE,
            new ParallelTransition(ModLoadingStage.COMPLETE, FMLLoadCompleteEvent::new));

    private ModStateProvider() {
    }

    public static List<ModLoadingState> getAllStates() {
        return List.of(CONSTRUCT, COMMON_SETUP, COMPLETE);
    }
}
