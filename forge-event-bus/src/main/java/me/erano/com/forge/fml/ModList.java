package me.erano.com.forge.fml;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

// Gercekte ayrica mod dosyalari (IModFileInfo), siralama icin LoadingModList ve crash report kaydi var.
public final class ModList {
    // volatile degil: main thread'de setLoadedMods ile yazilir, worker'lar bu alanlari ancak
    // main thread is gonderdikten SONRA okur. Executor'a gonderim (thenRunAsync) oncesindeki yazmalar
    // gorevin icinde gorunur (java.util.concurrent happens-before garantisi).
    private static List<ModContainer> mods = List.of();
    private static Map<String, ModContainer> indexedMods = Map.of();
    private static List<ModContainer> sortedContainers = List.of();

    private ModList() {
    }

    // Gercekte siralama LoadingModList'teki bagimlilik sirasina gore yapilir; burada verilen sira korunuyor.
    static void setLoadedMods(List<ModContainer> modContainers) {
        mods = modContainers;
        sortedContainers = List.copyOf(modContainers);
        indexedMods = modContainers.stream().collect(Collectors.toUnmodifiableMap(ModContainer::getModId, Function.identity()));
    }

    public static Optional<? extends ModContainer> getModContainerById(String modId) {
        return Optional.ofNullable(indexedMods.get(modId));
    }

    public static boolean isLoaded(String modTarget) {
        return indexedMods.containsKey(modTarget);
    }

    public static int size() {
        return mods.size();
    }

    public static List<ModContainer> getLoadedMods() {
        return sortedContainers;
    }
}
