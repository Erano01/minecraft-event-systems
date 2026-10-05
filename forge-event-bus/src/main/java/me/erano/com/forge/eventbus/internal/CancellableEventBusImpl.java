package me.erano.com.forge.eventbus.internal;

import me.erano.com.forge.eventbus.api.bus.CancellableEventBus;
import me.erano.com.forge.eventbus.api.event.characteristic.Cancellable;
import me.erano.com.forge.eventbus.api.listener.EventListener;
import me.erano.com.forge.eventbus.api.listener.ObjBooleanBiConsumer;
import me.erano.com.forge.eventbus.api.listener.Priority;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VolatileCallSite;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

// Iptal edilebilen event'ler icin bus. Invoker bir Predicate zinciri: ilk 'true' zinciri keser.
public record CancellableEventBusImpl<T extends Event & Cancellable>(
        String busGroupName,
        Class<T> eventType,
        CallSite invokerCallSite,
        ArrayList<EventListener> backingList,
        ArrayList<EventListener> monitorBackingList,
        List<AbstractEventBusImpl<?, ?>> children,
        AtomicBoolean alreadyInvalidated,
        AtomicBoolean shutdownFlag,
        int eventCharacteristics
) implements CancellableEventBus<T>, AbstractEventBusImpl<T, Predicate<T>> {

    public CancellableEventBusImpl(String busGroupName, Class<T> eventType, ArrayList<EventListener> backingList, int eventCharacteristics) {
        this(busGroupName, eventType,
                new VolatileCallSite(backingList.isEmpty() ? Constants.MH_NO_OP_PREDICATE : Constants.MH_NULL_PREDICATE),
                backingList, new ArrayList<>(), AbstractEventBusImpl.makeEventChildrenList(eventType, eventCharacteristics),
                new AtomicBoolean(), new AtomicBoolean(), eventCharacteristics);
    }

    @Override
    public EventListener addListener(Consumer<T> listener) {
        return addListener(new EventListenerImpl.WrappedConsumerListener<>(eventType, Priority.NORMAL, listener));
    }

    @Override
    public EventListener addListener(byte priority, Consumer<T> listener) {
        return addListener(priority == Priority.MONITOR
                ? new EventListenerImpl.MonitoringListener<>(eventType, listener)
                : new EventListenerImpl.WrappedConsumerListener<>(eventType, priority, listener));
    }

    @Override
    public EventListener addListener(byte priority, boolean alwaysCancelling, Consumer<T> listener) {
        if (!alwaysCancelling) {
            throw new IllegalArgumentException("If you never cancel the event, call addListener(byte, Consumer<T>) instead");
        }
        if (priority == Priority.MONITOR) {
            throw new IllegalArgumentException("Monitoring listeners cannot cancel events");
        }
        return addListener(new EventListenerImpl.WrappedConsumerListener<>(eventType, priority, true, listener));
    }

    @Override
    public EventListener addListener(Predicate<T> listener) {
        return addListener(new EventListenerImpl.PredicateListener<>(eventType, Priority.NORMAL, listener));
    }

    @Override
    public EventListener addListener(byte priority, Predicate<T> listener) {
        if (priority == Priority.MONITOR) {
            throw new IllegalArgumentException("Monitoring listeners cannot cancel events");
        }
        return addListener(new EventListenerImpl.PredicateListener<>(eventType, priority, listener));
    }

    @Override
    public EventListener addListener(ObjBooleanBiConsumer<T> monitoringListener) {
        return addListener(new EventListenerImpl.MonitoringListener<>(eventType, monitoringListener));
    }

    @Override
    public boolean post(T event) {
        return getInvoker().test(event);
    }

    @Override
    public T fire(T event) {
        getInvoker().test(event);
        return event;
    }

    @Override
    public boolean hasListeners() {
        return (Object) getInvoker() != Constants.NO_OP_PREDICATE;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Predicate<T> maybeGetInvoker() {
        try {
            return (Predicate<T>) (Predicate<?>) invokerCallSite.getTarget().invokeExact();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public void invalidateInvoker() {
        if (alreadyInvalidated.getAcquire()) {
            return;
        }
        invokerCallSite.setTarget(backingList.isEmpty() ? Constants.MH_NO_OP_PREDICATE : Constants.MH_NULL_PREDICATE);
    }

    @Override
    public Predicate<T> buildInvoker() {
        synchronized (backingList) {
            backingList.sort(Constants.PRIORITY_COMPARATOR);
            Predicate<T> invoker = InvokerFactory.createCancellableMonitoringInvoker(backingList, monitorBackingList);
            invokerCallSite.setTarget(MethodHandles.constant(Predicate.class, invoker));
            alreadyInvalidated.set(false);
            return invoker;
        }
    }

    @Override
    public void setNoOpInvoker() {
        invokerCallSite.setTarget(Constants.MH_NO_OP_PREDICATE);
    }
}
