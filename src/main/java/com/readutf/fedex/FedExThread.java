package com.readutf.fedex;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.readutf.fedex.interfaces.IncomingParcelListener;
import com.readutf.fedex.interfaces.Parcel;
import redis.clients.jedis.JedisPubSub;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.UUID;

public class FedExThread implements Runnable {

    FedEx fedEx;

    public FedExThread(FedEx fedEx) {
        this.fedEx = fedEx;
    }

    boolean stopped;

    @Override
    public void run() {
        System.out.println("started");
        JedisPubSub jedisPubSub = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                System.out.println(message);
                if (stopped) {
                    return;
                }


                String[] parts = message.split(";");
                if(FedEx.get().getId().equals(UUID.fromString(parts[2]))) {
                    return;
                }
                String name = parts[0];
                if (!fedEx.getRegisteredParcels().containsKey(name)) {
                    if (fedEx.isDebug()) System.out.println("no listener found for " + name);
                    return;
                }

                try {
                    for (Class<?> clazz : fedEx.getPacketListeners()) {
                        for (Method method : clazz.getDeclaredMethods()) {
                            if (method.isAnnotationPresent(IncomingParcelListener.class)) {
                                if (JsonObject.class.isAssignableFrom(method.getParameters()[0].getType())) {
                                    IncomingParcelListener incomingParcelListener = method.getAnnotation(IncomingParcelListener.class);
                                    System.out.println("incoming parcel: " + incomingParcelListener.name());
                                    if (!incomingParcelListener.name().equalsIgnoreCase(name)) {
                                        continue;
                                    }
                                    Arrays.stream(method.getParameters()).forEach(parameter -> System.out.println("paramater type: " + parameter.getType().getName()));
                                    method.invoke(clazz.newInstance(), new JsonParser().parse(parts[1]));
                                    System.out.println("----------");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };
        fedEx.getJedis().subscribe(jedisPubSub, fedEx.getChannel());
    }


}
