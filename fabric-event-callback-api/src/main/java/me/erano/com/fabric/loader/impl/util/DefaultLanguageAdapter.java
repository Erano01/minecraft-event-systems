package me.erano.com.fabric.loader.impl.util;

import me.erano.com.fabric.loader.api.LanguageAdapter;
import me.erano.com.fabric.loader.api.LanguageAdapterException;
import me.erano.com.fabric.loader.api.ModContainer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Uc bicim: "pkg.Class" (no-arg constructor), "pkg.Class::staticField", "pkg.Class::method"
// (metot, istenen arayuze MethodHandleProxies ile uyarlanir). Gercekte sinif, oyun classloader'indan
// (FabricLauncherBase.getLauncher().getTargetClassLoader()) yuklenir; burada thread context classloader.
public final class DefaultLanguageAdapter implements LanguageAdapter {
    public static final DefaultLanguageAdapter INSTANCE = new DefaultLanguageAdapter();

    private DefaultLanguageAdapter() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        String[] methodSplit = value.split("::");
        if (methodSplit.length >= 3) {
            throw new LanguageAdapterException("Invalid handle format: " + value);
        }
        Class<?> c;
        try {
            c = Class.forName(methodSplit[0], true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new LanguageAdapterException(e);
        }

        if (methodSplit.length == 1) {
            if (!type.isAssignableFrom(c)) {
                throw new LanguageAdapterException("Class " + c.getName() + " cannot be cast to " + type.getName() + "!");
            }
            try {
                return (T) c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new LanguageAdapterException(e);
            }
        }

        List<Executable> methodList = new ArrayList<>();
        for (Method m : c.getDeclaredMethods()) {
            if (m.getName().equals(methodSplit[1])) {
                methodList.add(m);
            }
        }
        if (methodSplit[1].equals("<init>")) {
            methodList.addAll(Arrays.asList(c.getDeclaredConstructors()));
        }

        try {
            Field field = c.getDeclaredField(methodSplit[1]);
            if (!Modifier.isStatic(field.getModifiers())) {
                throw new LanguageAdapterException("Field " + value + " must be static!");
            }
            if (!methodList.isEmpty()) {
                throw new LanguageAdapterException("Ambiguous " + value + " - refers to both field and method!");
            }
            if (!type.isAssignableFrom(field.getType())) {
                throw new LanguageAdapterException("Field " + value + " cannot be cast to " + type.getName() + "!");
            }
            return (T) field.get(null);
        } catch (IllegalAccessException e) {
            throw new LanguageAdapterException("Field " + value + " cannot be accessed!", e);
        } catch (NoSuchFieldException e) {
            // metot dalina dus
        }

        if (!type.isInterface()) {
            throw new LanguageAdapterException("Cannot proxy method " + value + " to non-interface type " + type.getName() + "!");
        }
        if (methodList.isEmpty()) {
            throw new LanguageAdapterException("Could not find " + value + "!");
        }
        if (methodList.size() >= 2) {
            throw new LanguageAdapterException("Found multiple method entries of name " + value + "!");
        }
        Executable executable = methodList.getFirst();
        Object object = null;
        if (executable instanceof Method m && !Modifier.isStatic(m.getModifiers())) {
            try {
                object = c.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new LanguageAdapterException(e);
            }
        }
        try {
            MethodHandle handle = executable instanceof Method m
                    ? MethodHandles.lookup().unreflect(m)
                    : MethodHandles.lookup().unreflectConstructor((Constructor<?>) executable);
            if (object != null) {
                handle = handle.bindTo(object);
            }
            return MethodHandleProxies.asInterfaceInstance(type, handle);
        } catch (Exception e) {
            throw new LanguageAdapterException(e);
        }
    }
}
