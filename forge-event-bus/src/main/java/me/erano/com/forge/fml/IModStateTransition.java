package me.erano.com.forge.fml;

import me.erano.com.forge.fml.event.IModBusEvent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

// Bir asamadan digerine gecis: hangi event uretilecek, hangi thread'lerde dagitilacak, sonunda ne yapilacak.
public interface IModStateTransition {
    ThreadSelector threadSelector();

    Function<ModContainer, IModBusEvent> eventFunction();

    BiFunction<Executor, CompletableFuture<Void>, CompletableFuture<Void>> finalActivityGenerator();

    default CompletableFuture<Void> build(Executor syncExecutor, Executor parallelExecutor) {
        return ModStateTransitionHelper.build(this, syncExecutor, parallelExecutor);
    }
}
