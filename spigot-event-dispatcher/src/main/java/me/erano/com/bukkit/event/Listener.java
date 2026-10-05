package me.erano.com.bukkit.event;

//observer, listener, subscriber
//Simple interface for tagging all EventListeners - Marker interface
public interface Listener {

    // Bukkit, GoF'taki update() metodunu kayıt anında reflection ile
    // her @EventHandler metodu için üretilen bir EventExecutor adapter'ına dönüştürür;
    // bu adapter, ilgili Method'u ve event tipini closure içinde tutar ve dispatch sırasında
    // RegisteredListener'ın ignoreCancelled filtresinden sonra tip kontrolü yapıp metodu Method.invoke ile çağırır.
    // Bu sebeple burada GoF kitabında yer alan Observer.update() tanımlı değil.
}
