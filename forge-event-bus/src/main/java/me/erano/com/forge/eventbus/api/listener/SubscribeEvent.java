package me.erano.com.forge.eventbus.api.listener;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribeEvent {
    byte priority() default Priority.NORMAL;

    boolean alwaysCancelling() default false;
}
