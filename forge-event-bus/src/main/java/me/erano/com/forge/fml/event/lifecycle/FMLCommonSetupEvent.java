package me.erano.com.forge.fml.event.lifecycle;

import me.erano.com.forge.fml.ModContainer;
import me.erano.com.forge.fml.ModLoadingStage;

public class FMLCommonSetupEvent extends ParallelDispatchEvent {
    public FMLCommonSetupEvent(ModContainer container, ModLoadingStage stage) {
        super(container, stage);
    }
}
