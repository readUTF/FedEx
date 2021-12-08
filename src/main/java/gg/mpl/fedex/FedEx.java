package gg.mpl.fedex;

import com.google.gson.Gson;
import gg.mpl.fedex.parcels.Parcel;
import gg.mpl.fedex.parcels.ParcelListener;
import gg.mpl.fedex.response.FedExResponse;
import gg.mpl.fedex.response.FedExResponseParcel;
import gg.mpl.fedex.response.TimeoutTask;
import gg.mpl.fedex.utils.ClassUtils;
import gg.mpl.fedex.utils.Pair;
import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.logging.Logger;

@Getter
@SuppressWarnings("unused")
public final class FedEx {
    @Getter
    private static FedEx instance;

    private final Executor executor;
    private final Map<String, Parcel> parcels = new HashMap<>();
    private final Map<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = new HashMap<>();
    private final List<ParcelListener> parcelListeners = new ArrayList<>();
    private final UUID senderId;
    private final String channel;
    private final JedisPool jedisPool;
    private final Gson gson;
    private final Jedis jedis;
    private final TimeoutTask timeoutTask;
    private final Logger logger = Logger.getLogger("FedEx");

    private boolean active;

    @Setter
    private boolean debug = false;

    private FedExPubSub pubSub;
    private Jedis publisher;
    private Jedis subscriber;

    public FedEx(String channel, JedisPool jedisPool, Gson gson, Executor executor) {
        instance = this;
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.gson = gson;
        this.executor = executor;

        jedis = jedisPool.getResource();
        timeoutTask = new TimeoutTask();
        senderId = UUID.randomUUID();

        registerParcel(new FedExResponseParcel(null));
        connect();
    }

    public FedEx(String channel, JedisPool jedisPool, Gson gson) {
        this(channel, jedisPool, gson, ForkJoinPool.commonPool());
    }

    public FedEx(String channel, JedisPool jedisPool, Executor executor) {
        this(channel, jedisPool, new Gson(), executor);
    }

    public FedEx(String channel, JedisPool jedisPool) {
        this(channel, jedisPool, new Gson(), ForkJoinPool.commonPool());
    }

    public void connect() {
        active = true;

        executor.execute(() -> getSubscriber().subscribe(pubSub = new FedExPubSub(this), channel));
        timeoutTask.start();
    }

    public void close() {
        active = false;
        if (pubSub != null && pubSub.isSubscribed()) {
            pubSub.unsubscribe();
        }

        jedisPool.close();
    }

    public void sendParcel(UUID id, Parcel parcel, Consumer<FedExResponse> fedexResponse) {
        if (id == null) id = UUID.randomUUID();

        if (fedexResponse != null) responseConsumers.put(id, new Pair<>(fedexResponse, System.currentTimeMillis()));

        UUID finalId = id;
        executor.execute(() -> getPublisher().publish(channel, senderId.toString() + ";" + parcel.getName() + ";" + parcel.getData().toString() + ";" + finalId));
    }

    /**
     * Sends a parcel over the network
     *
     * @param parcel The parcel
     */
    public void sendParcel(Parcel parcel) {
        sendParcel(null, parcel, null);
    }

    /**
     * Sends a parcel over the network with a consumer waiting for a response parcel
     *
     * @param parcel           The parcel
     * @param responseConsumer The response consumer
     */
    public void sendParcel(Parcel parcel, Consumer<FedExResponse> responseConsumer) {
        sendParcel(null, parcel, responseConsumer);
    }

    public void sendParcel(UUID id, Parcel parcel) {
        sendParcel(id, parcel, null);
    }

    /**
     * Registers a parcel
     *
     * @param parcel The parcel
     */
    public void registerParcel(Parcel parcel) {
        parcels.put(parcel.getName(), parcel);
    }

    /**
     * Registers a parcel by a class, needs a no-arg constructor
     *
     * @param parcelClass The class
     */
    public void registerParcel(Class<? extends Parcel> parcelClass) {
        Parcel parcel = ClassUtils.tryGetInstance(parcelClass);
        if (parcel == null) {
            logger.warning("Parcel " + parcelClass.getName() + " could not be registered, please make a no-arg constructor.");
        } else {
            parcels.put(parcel.getName(), parcel);
        }
    }

    /**
     * Registers all parcels in the package of `mainClass` using reflection and JarEntries
     *
     * @param mainClass The class
     */
    @SuppressWarnings("unchecked")
    public void registerParcels(Class<?> mainClass) {
        ClassUtils.getClassesInPackage(mainClass).stream().filter(Parcel.class::isAssignableFrom).forEach(clazz -> registerParcel((Class<? extends Parcel>) clazz));
    }

    public void registerParcelListeners(ParcelListener... parcelListener) {
        parcelListeners.addAll(Arrays.asList(parcelListener));
    }

    public Jedis getPublisher() {
        if (publisher == null) {
            publisher = jedisPool.getResource();
        }

        return publisher;
    }

    public Jedis getSubscriber() {
        if (subscriber == null) subscriber = jedisPool.getResource();

        return subscriber;
    }

    public void debug(String s) {
        if (debug) logger.severe(s);
    }
}
