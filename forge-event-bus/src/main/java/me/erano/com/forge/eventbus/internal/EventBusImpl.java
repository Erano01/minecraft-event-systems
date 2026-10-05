package me.erano.com.forge.eventbus.internal;

import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.api.listener.Priority;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VolatileCallSite;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

// Iptal edilemeyen event'ler icin bus. Invoker: tum listener'lari sirayla cagiran tek bir Consumer.
public record EventBusImpl<T extends Event>(
        String busGroupName,
        Class<T> eventType,
        CallSite invokerCallSite,
        ArrayList<EventListener> backingList,
        ArrayList<EventListener> monitorBackingList,
        List<AbstractEventBusImpl<?, ?>> children,
        AtomicBoolean alreadyInvalidated,
        AtomicBoolean shutdownFlag,
        int eventCharacteristics
) implements AbstractEventBusImpl<T, Consumer<T>> {

    public EventBusImpl(String busGroupName, Class<T> eventType, ArrayList<EventListener> backingList, int eventCharacteristics) {
        this(busGroupName, eventType,
                new VolatileCallSite(backingList.isEmpty() ? Constants.MH_NO_OP_CONSUMER : Constants.MH_NULL_CONSUMER),
                backingList, new ArrayList<>(), AbstractEventBusImpl.makeEventChildrenList(eventType, eventCharacteristics),
                new AtomicBoolean(), new AtomicBoolean(), eventCharacteristics);
    }

    @Override
    public EventListener addListener(Consumer<T> listener) {
        return addListener(new EventListenerImpl.ConsumerListener<>(eventType, Priority.NORMAL, listener));
    }

    @Override
    public EventListener addListener(byte priority, Consumer<T> listener) {
        return addListener(priority == Priority.MONITOR
                ? new EventListenerImpl.MonitoringListener<>(eventType, listener)
                : new EventListenerImpl.ConsumerListener<>(eventType, priority, listener));
    }

    @Override
    public boolean post(T event) {
        getInvoker().accept(event);
        return false;
    }

    @Override
    public T fire(T event) {
        getInvoker().accept(event);
        return event;
    }

    @Override
    public boolean hasListeners() {
        return (Object) getInvoker() != Constants.NO_OP_CONSUMER;
    }

    // VolatileCallSite.getTarget() volatile okumadir: buildInvoker()'da setTarget ile yayinlanan
    // invoker'i ve onun yakaladigi listener dizisini guvenle gorur (safe publication).
    @Override
    @SuppressWarnings("unchecked")
    public Consumer<T> maybeGetInvoker() {
        try {
            return (Consumer<T>) (Consumer<?>) invokerCallSite.getTarget().invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void invalidateInvoker() {
        if (alreadyInvalidated.getAcquire()) {
            return;
        }
        invokerCallSite.setTarget(backingList.isEmpty() ? Constants.MH_NO_OP_CONSUMER : Constants.MH_NULL_CONSUMER);
    }

    @Override
    public Consumer<T> buildInvoker() {
        synchronized (backingList) {
            backingList.sort(Constants.PRIORITY_COMPARATOR);
            Consumer<T> invoker = InvokerFactory.createMonitoringInvoker(backingList, monitorBackingList);
            setInvoker(invoker);
            alreadyInvalidated.set(false);
            return invoker;
        }
    }

    @Override
    public void setNoOpInvoker() {
        invokerCallSite.setTarget(Constants.MH_NO_OP_CONSUMER);
    }

    private void setInvoker(Consumer<T> invoker) {
        invokerCallSite.setTarget(MethodHandles.constant(Consumer.class, invoker));
    }
}
