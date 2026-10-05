package me.erano.com.forge.fml.event.lifecycle;

import me.erano.com.forge.fml.ModContainer;
import me.erano.com.forge.fml.ModLoadingStage;

public class FMLLoadCompleteEvent extends ParallelDispatchEvent {
    public FMLLoadCompleteEvent(ModContainer container, ModLoadingStage stage) {
        super(container, stage);
    }
}
