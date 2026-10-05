package me.erano.com.fabric.loader.api;

// Entrypoint tanimindaki metni ("com.example.Mod", "com.example.Mod::field") bir nesneye cevirir.
public interface LanguageAdapter {
    <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException;
}
