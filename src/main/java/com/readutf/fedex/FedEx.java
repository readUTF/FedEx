package com.readutf.fedex;

import com.readutf.fedex.interfaces.IncomingParcelListener;
import com.readutf.fedex.interfaces.Parcel;
import com.readutf.fedex.utils.ClassUtils;
import lombok.Getter;

import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;

import redis.clients.jedis.Jedis;
import sun.misc.Unsafe;

public class FedEx {

    @Getter private List<Class<?>> packetListeners = new ArrayList<>();
    @Getter private HashMap<String, Parcel> registeredParcels = new HashMap<>();
    @Getter private boolean debug;
    @Getter private Thread redisThread;
    @Getter private Jedis jedis;
    @Getter private Jedis jedisPublisher;
    @Getter private String channel;
    @Getter private Logger logger;

    @Getter private UUID id;

    private static FedEx instance;

    public FedEx(String address, int port, boolean auth, String password, String channel) {
        instance = this;
        id = UUID.randomUUID();
        this.logger = Logger.getLogger(getClass().getName());

        jedis = new Jedis(address, port);
        jedisPublisher = new Jedis(address, port);
        if(auth) {
            jedis.auth(password);
            jedisPublisher.auth(password);
        }

        this.channel = channel;

        redisThread = new Thread(new FedExThread(this));
        redisThread.start();


    }

    public static FedEx get() {return instance;}

//    public void registerAll(Class<?> mainClass, String pathToPackage) {
//        try {
//            List<Class<?>> classes = ClassUtils.getClasses(mainClass.getProtectionDomain().getCodeSource().getLocation().getPath(), pathToPackage);
//            System.out.println(classes);
//            classes.forEach(aClass -> {
//                System.out.println(aClass.getName());
//                registerParcel(aClass);
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void sendParcel(Parcel parcel) {
        jedisPublisher.publish(channel, parcel.getName() +";" + parcel.toJson() + ";" + id.toString());
    }

    public void registerParcelListener(Class<?> clazz) {
//        if(!clazz.isAnnotationPresent(IncomingParcelListener.class)) {
//            return;
//        }
        if(Arrays.stream(clazz.getMethods()).noneMatch(method -> method.isAnnotationPresent(IncomingParcelListener.class))) {
            return;
        }
        packetListeners.add(clazz);
    }

    public void registerParcel(Class<?> clazz)  {
        if(clazz == null) return;
        if(!clazz.getSuperclass().equals(Parcel.class)) {
            return;
        }

        System.out.println(clazz.getName() + " is a parcel");

        Parcel parcel = null;
        try {
            parcel = (Parcel) unsafe.allocateInstance(clazz);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(parcel.getName().equalsIgnoreCase("") || parcel.getName() == null) {
            logger.severe("[" + clazz.getName() + "] Invalid name.");
            return;
        }

        if(parcel == null) return;
        System.out.println("registered parcel: " + parcel);
        registeredParcels.put(parcel.getName(), parcel);

    }

    static Unsafe unsafe;
    static {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            unsafe = (Unsafe) singleoneInstanceField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
