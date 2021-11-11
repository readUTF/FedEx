package com.readutf.fedex;


import com.google.gson.Gson;
import com.readutf.fedex.parcels.Parcel;
import com.readutf.fedex.parcels.ParcelListener;
import com.readutf.fedex.response.FedExResponse;
import com.readutf.fedex.response.FedExResponseParcel;
import com.readutf.fedex.response.TimeoutTask;
import com.readutf.fedex.utils.ClassUtils;
import com.readutf.fedex.utils.Pair;
import com.readutf.fedex.utils.UnsafeHandler;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Getter
public class FedEx {

    @Getter private static FedEx instance;
    @Getter @Setter private boolean debug = false;

    HashMap<String, Parcel> parcels;
    HashMap<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers;
    List<ParcelListener> parcelListeners;

    UUID senderId;
    String channel;
    JedisPool jedisPool;
    Gson gson;
    Jedis jedis;

    TimeoutTask timeoutTask;

    Logger logger;

    FedExPubSub pubSub;

    public FedEx(String channel, JedisPool jedisPool) {
        instance = this;
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.gson = new Gson();
        this.logger = Logger.getLogger("FedEx");
        this.parcels = new HashMap<>();
        this.responseConsumers = new HashMap<>();
        this.parcelListeners = new ArrayList<>();
        jedis = jedisPool.getResource();
        timeoutTask = new TimeoutTask();
        senderId = UUID.randomUUID();
        registerParcel(FedExResponseParcel.class);
        connect();
        active = true;
    }

    public void connect() {
        ForkJoinPool.commonPool().execute(() -> {
            getSubscriber().subscribe(pubSub = new FedExPubSub(this), channel);
        });
        timeoutTask.start();
    }

    @Getter boolean active;

    public void close() {
        if(pubSub != null && pubSub.isSubscribed()) {
            pubSub.unsubscribe();
        }
        jedisPool.close();
        active = false;
    }

    public void sendParcel(UUID id, Parcel parcel, Consumer<FedExResponse> fedexResponse) {
        if(id == null) id = UUID.randomUUID();
        if(fedexResponse != null) responseConsumers.put(id, new Pair<>(fedexResponse, System.currentTimeMillis()));
        UUID finalId = id;
        ForkJoinPool.commonPool().execute(() -> {
            getPublisher().publish(channel, senderId.toString() + ";" + parcel.getName() + ";" + parcel.getData().toString() + ";" + finalId.toString());
        });
    }

    public void sendParcel(Parcel parcel) {
        sendParcel(null, parcel, null);
    }

    public void sendParcel(Parcel parcel, Consumer<FedExResponse> fedExResponse) {
        sendParcel(null, parcel, fedExResponse);
    }

    public void sendParcel(UUID id, Parcel parcel) {
        sendParcel(id, parcel, null);
    }

    public void registerParcel(Class<? extends Parcel> parcel) {
        Parcel instance = new UnsafeHandler<Parcel>(parcel).getInstance();
        parcels.put(instance.getName(), instance);
    }

    private void registerParcelUnsafe(Class<?> parcel) {
        Parcel instance = new UnsafeHandler<Parcel>(parcel).getInstance();
        parcels.put(instance.getName(), instance);
    }

    public void registerParcels(Class<?> mainClass) {
        ClassUtils.getClassesInPackage(mainClass).stream().filter(Parcel.class::isAssignableFrom).forEach(this::registerParcelUnsafe);
    }

    public void registerParcelListeners(ParcelListener... parcelListener) {
        parcelListeners.addAll(Arrays.asList(parcelListener));
    }

    public void registerParcels(Class<? extends Parcel>... parcels) {
        Arrays.stream(parcels).forEach(this::registerParcelUnsafe);
    }

    private Jedis publisher;
    public Jedis getPublisher() {
        if(publisher == null) {
            publisher = jedisPool.getResource();
        }
        return publisher;
    }

    private Jedis subscriber;
    public Jedis getSubscriber() {
        if(subscriber == null) subscriber = jedisPool.getResource();
        return subscriber;
    }

    public void debug(String s) { if(isDebug()) logger.severe(s);}

}
