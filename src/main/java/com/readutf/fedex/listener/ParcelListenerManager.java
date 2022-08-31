package com.readutf.fedex.listener;

import com.google.gson.JsonObject;
import com.readutf.fedex.response.FedExResponse;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ParcelListenerManager {

    private HashMap<Method, Object> parcelListeners = new HashMap<>();

    @SneakyThrows
    public void registerParcelListeners(Class<?> clazz, Object... classProps)  {
        Class<?>[] classes = Arrays.stream(classProps).map(Object::getClass).toArray(value -> new Class<?>[classProps.length]);

        Optional<Constructor<?>> foundConstructors = Arrays.stream(clazz.getConstructors())
                .filter(constructor -> propsMatch(constructor.getParameterTypes(), classes))
                .findFirst();

        if(foundConstructors.isPresent()) {
            Constructor<?> constructor = foundConstructors.get();
            constructor.setAccessible(true);
            Object o = constructor.newInstance(classProps);
            for (final Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(ParcelListener.class)) {
                    if (method.getParameterTypes().length < 2) {
                        System.out.println("Invalid parameters for parcel listener");
                    }
                    else if (method.getParameterTypes()[0] != UUID.class || method.getParameterTypes()[1] != JsonObject.class) {
                        System.out.println("Parcel listener parameters should be [UUID, JsonObject]");
                    }
                    else {
                        this.parcelListeners.put(method, o);
                        System.out.println("(" + clazz.getSimpleName() + ") Registered parcel listener with name " + method.getName());
                    }
                }
            }


        } else {
            throw new Exception("Could not find constructors with props");
        }}

    @SneakyThrows
    public FedExResponse handleParcel(String name, UUID uuid, JsonObject data) {
        for (Map.Entry<Method, Object> methodObjectEntry : parcelListeners.entrySet()) {
            Method method = methodObjectEntry.getKey();
            ParcelListener annotation = method.getAnnotation(ParcelListener.class);
            if (!annotation.value().equalsIgnoreCase(name)) {
                continue;
            }
            if(method.getReturnType() == FedExResponse.class) {
                try {
                    return (FedExResponse) method.invoke(methodObjectEntry.getValue(), uuid, data);
                } catch (IllegalAccessException e) {
                    System.out.println("Error occurred invoking parcel listener");
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            } else {
                try {
                    method.invoke(methodObjectEntry.getValue(), uuid, data);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.out.println("Error occurred invoking parcel listener: " + e.getLocalizedMessage());
                }
            }
        }
        return null;
    }

    public boolean propsMatch(Class<?>[] foundClasses, Class<?>[] providedClasses) {

        if(foundClasses.length == 0 && providedClasses.length == 0) return true;
        if(foundClasses.length != providedClasses.length) return false;
        for (int i = 0; i < foundClasses.length; i++) {
            Class<?> found = foundClasses[i];
            Class<?> provided = providedClasses[i];

            if(found != provided && !found.isAssignableFrom(provided)) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

}
