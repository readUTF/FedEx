package com.readutf.fedex.listener;

import com.readutf.fedex.FedEx;
import com.readutf.fedex.response.FedExResponse;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ParcelListenerManager {

    FedEx fedEx;
    private HashMap<Method, Object> parcelListeners = new HashMap<>();

    public ParcelListenerManager(FedEx fedEx) {
        this.fedEx = fedEx;
    }

    @SneakyThrows
    public Object registerParcelListeners(Class<?> clazz, Object... classProps) {
        Class<?>[] classes = Arrays.stream(classProps).map(Object::getClass).toArray(value -> new Class<?>[classProps.length]);


        Optional<Constructor<?>> foundConstructors = Arrays.stream(clazz.getConstructors())
                .filter(constructor -> propsMatch(constructor.getParameterTypes(), classes))
                .findFirst();

        if (foundConstructors.isPresent()) {
            Constructor<?> constructor = foundConstructors.get();
            constructor.setAccessible(true);
            Object o = constructor.newInstance(classProps);
            for (final Method method : clazz.getMethods()) {
                if (method.isAnnotationPresent(ParcelListener.class)) {
                    if (method.getParameterTypes().length < 2) {
                        fedEx.debug("Invalid parameters for parcel listener");
                    } else if (method.getParameterTypes()[0] != UUID.class || method.getParameterTypes()[1] != HashMap.class) {
                        fedEx.debug("Parcel listener parameters should be [UUID, JsonObject]");
                    } else {
                        this.parcelListeners.put(method, o);
                        fedEx.debug("(" + clazz.getSimpleName() + ") Registered parcel listener with name " + method.getName());
                    }
                }
            }
            return o;
        } else {
            throw new Exception("Could not find constructors with props");
        }
    }

    @SneakyThrows
    public FedExResponse handleParcel(String name, UUID uuid, HashMap<String, Object> data) {
        for (Map.Entry<Method, Object> methodObjectEntry : parcelListeners.entrySet()) {
            Method method = methodObjectEntry.getKey();
            ParcelListener annotation = method.getAnnotation(ParcelListener.class);
            if (!annotation.value().equalsIgnoreCase(name)) {
                continue;
            }
            if (method.getReturnType() == FedExResponse.class) {
                try {
                    return (FedExResponse) method.invoke(methodObjectEntry.getValue(), uuid, data);
                } catch (IllegalAccessException e) {
                    fedEx.debug("Error occurred invoking parcel listener");
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            } else {
                try {
                    method.invoke(methodObjectEntry.getValue(), uuid, data);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();

                    fedEx.debug("Error occurred invoking parcel listener: " + e.getLocalizedMessage());
                }
            }
        }
        return null;
    }

    public boolean propsMatch(Class<?>[] foundClasses, Class<?>[] providedClasses) {

//        System.out.println("found: " + Arrays.toString(foundClasses));

        if (foundClasses.length == 0 && providedClasses.length == 0) return true;
        if (foundClasses.length != providedClasses.length) return false;
        for (int i = 0; i < foundClasses.length; i++) {
            Class<?> found = foundClasses[i];
            Class<?> provided = providedClasses[i];



            if (found != provided && !found.isAssignableFrom(provided)) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

}
