package me.erano.com.forge.fml;

// "Su an hangi mod icin calisiyorum?" bilgisi. Paralel yuklemede her worker farkli bir mod'u isledigi
// icin ThreadLocal: her thread kendi aktif container'ini gorur (JCiP: ThreadLocal ile thread confinement).
public class ModLoadingContext {
    private static final ThreadLocal<ModLoadingContext> context = ThreadLocal.withInitial(ModLoadingContext::new);
    private ModContainer activeContainer;
    private Object languageExtension;

    // Gercekte @Deprecated(forRemoval = true): context artik constructor parametresiyle veriliyor.
    @Deprecated
    public static ModLoadingContext get() {
        return context.get();
    }

    void setActiveContainer(ModContainer container) {
        this.activeContainer = container;
        this.languageExtension = container == null ? null : container.contextExtension.get();
    }

    // Gercekte aktif container yoksa "minecraft" mod'unun container'i doner; burada o container yok.
    public ModContainer getContainer() {
        if (activeContainer == null) {
            throw new IllegalStateException("No active mod container on thread " + Thread.currentThread().getName());
        }
        return activeContainer;
    }

    @SuppressWarnings("unchecked")
    public <T> T extension() {
        return (T) languageExtension;
    }
}
