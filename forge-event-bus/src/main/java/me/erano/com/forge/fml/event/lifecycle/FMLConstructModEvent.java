package me.erano.com.forge.fml.event.lifecycle;

import me.erano.com.forge.fml.ModContainer;
import me.erano.com.forge.fml.ModLoadingStage;

public class FMLConstructModEvent extends ParallelDispatchEvent {
    public FMLConstructModEvent(ModContainer container, ModLoadingStage stage) {
        super(container, stage);
    }
}
