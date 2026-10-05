package me.erano.com.forge.fml;

// Her mod'un bulundugu asama. Her asamanin kendi DeferredWorkQueue'su var (enqueueWork hedefi).
// Gercekte ayrica SIDED_SETUP, ENQUEUE_IMC, PROCESS_IMC var.
public enum ModLoadingStage {
    ERROR,
    CONSTRUCT,
    COMMON_SETUP,
    COMPLETE,
    DONE;

    // Enum sabitleri class init sirasinda olusur; DeferredWorkQueue'nun statik HashMap'i de bu sirada
    // doldurulur. Class init JVM tarafindan senkronize edildigi icin (JLS 12.4.2) worker thread'leri bu
    // HashMap'i kilitsiz okuyabilir: static initializer ile safe publication.
    private final DeferredWorkQueue deferredWorkQueue = new DeferredWorkQueue(this);

    ModLoadingStage nextState(Throwable exception) {
        return exception != null ? ERROR : values()[ordinal() + 1];
    }

    public DeferredWorkQueue getDeferredWorkQueue() {
        return deferredWorkQueue;
    }
}
