package me.erano.com.forge.fml;

import java.util.List;

// Gercekte bu bilgiler mods.toml (IModInfo) ve jar'in ASM ile taranmasindan (ModFileScanData) gelir.
// Burada jar/classloading katmani olmadigi icin elle veriliyor.
public record ModInfo(String modId, Class<?> modClass, List<String> dependencies, List<Class<?>> eventBusSubscribers) {
}
