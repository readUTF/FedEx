package gg.mpl.fedex.listener;

import com.google.gson.JsonObject;
import gg.mpl.fedex.response.FedExResponse;
import gg.mpl.fedex.utils.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParcelListenerManager {

    private HashMap<Method, Class<?>> parcelListeners = new HashMap<>();

    public void registerParcelListeners(Class<?> classContainingListeners) {
        boolean found;
        for (Method method : classContainingListeners.getMethods()) {
            if(!method.isAnnotationPresent(ParcelListener.class)) {
                continue;
            }
            found = true;
            if(method.getParameterTypes().length < 2) {
                System.out.println("Invalid parameters for parcel listener");
                continue;
            }
            if(method.getParameterTypes()[0] != UUID.class || method.getParameterTypes()[1] != JsonObject.class) {
                System.out.println("Parcel listener parameters should be [UUID, JsonObject]");
                continue;
            }
            parcelListeners.put(method, classContainingListeners);
        }
    }

    public FedExResponse handleParcel(String name, UUID uuid, JsonObject data) {
        for (Map.Entry<Method, Class<?>> methodClassEntry : parcelListeners.entrySet()) {
            Method method = methodClassEntry.getKey();
            ParcelListener annotation = method.getAnnotation(ParcelListener.class);
            if (!annotation.value().equalsIgnoreCase(name)) {
                continue;
            }
            Class<?> clazz = methodClassEntry.getValue();
            if(method.getReturnType() == FedExResponse.class) {
                try {
                    return (FedExResponse) method.invoke(ClassUtils.tryGetInstance(clazz), uuid, data);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    System.out.println("Error occurred invoking parcel listener: " + e.getMessage());
                }
            } else {
                try {
                    method.invoke(ClassUtils.tryGetInstance(clazz), uuid, data);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    System.out.println("Error occurred invoking parcel listener: " + e.getLocalizedMessage());
                }
            }
        }
        return null;
    }

}
