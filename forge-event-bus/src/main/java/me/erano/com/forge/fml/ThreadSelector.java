package me.erano.com.forge.fml;

import java.util.concurrent.Executor;
import java.util.function.BinaryOperator;

// Bir lifecycle gecisinin event'leri hangi executor'da dagitacagini secer.
public enum ThreadSelector implements BinaryOperator<Executor> {
    SYNC((sync, parallel) -> sync),
    PARALLEL((sync, parallel) -> parallel);

    private final BinaryOperator<Executor> selector;

    ThreadSelector(BinaryOperator<Executor> selector) {
        this.selector = selector;
    }

    @Override
    public Executor apply(Executor sync, Executor parallel) {
        return selector.apply(sync, parallel);
    }
}
