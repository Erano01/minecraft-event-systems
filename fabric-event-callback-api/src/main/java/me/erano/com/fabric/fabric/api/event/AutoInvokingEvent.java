package me.erano.com.fabric.fabric.api.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Dokumantasyon isareti: entrypoint sinifi bu callback'i implement ederse event'e otomatik kaydedilir.
@Target({ElementType.FIELD, ElementType.METHOD})
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoInvokingEvent {
}
