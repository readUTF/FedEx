package com.readutf.fedex;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.readutf.fedex.response.FedExResponse;
import com.readutf.fedex.response.FedExResponseParcel;
import com.readutf.fedex.utils.ClassUtils;
import com.readutf.fedex.utils.Pair;
import com.readutf.fedex.listener.ParcelListenerManager;
import com.readutf.fedex.parcels.Parcel;
import com.readutf.fedex.response.TimeoutTask;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

@SuppressWarnings("unused")
@Getter()
public final class FedEx {

    @Deprecated
    @Getter
    private static FedEx instance;

    @Getter
    private static final Map<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = new HashMap<>();
    private final Map<String, Parcel> parcels = new HashMap<>();
    private final UUID senderId;
    private final String channel;
    private final JedisPool jedisPool;
    private final Gson gson;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Logger logger = Logger.getLogger("FedEx");
    private final ParcelListenerManager parcelListenerManager;
    private boolean active;
    private Thread poolThread;
    private FedExPubSub pubSub;
    @Setter
    private boolean debug = false;

    public FedEx(@NotNull String channel, @NotNull JedisPool jedisPool) {
        instance = this;
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.gson = new Gson();

        senderId = UUID.randomUUID();

        parcelListenerManager = new ParcelListenerManager(this);

        registerParcel(new FedExResponseParcel(null));
        connect();
    }

    private void connect() {
        active = true;
        poolThread = new Thread(() -> jedisPool.getResource().subscribe(pubSub = new FedExPubSub(this), channel));
        poolThread.start();
        new TimeoutTask(new Timer(), this);


//        timeoutTask.start();
    }

    /**
     * Close all messaging channels and disconnect from redis
     */
    public void close() {
        debug("fedex closed");
        active = false;
        if (pubSub != null && pubSub.isSubscribed()) {
            pubSub.unsubscribe();
        }
        poolThread.interrupt();

        jedisPool.close();
    }

    /**
     * Sends a parcel over the network with a consumer waiting for a response parcel
     *
     * @param id               The id
     * @param parcel           The parcel
     * @param responseConsumer The response consumer
     */
    public void sendParcel(@Nullable UUID id, @NotNull Parcel parcel, @Nullable Consumer<FedExResponse> responseConsumer) {
        if (id == null) id = UUID.randomUUID();

        if (responseConsumer != null)
            responseConsumers.put(id, new Pair<>(responseConsumer, System.currentTimeMillis()));

        UUID finalId = id;
        if (parcel.isSelfRun() || parcel.getClass().isAnnotationPresent(SelfRun.class)) {
            Optional.ofNullable(parcels.get(parcel.getName())).ifPresent(parcel1 -> parcel1.onReceive(channel, finalId, parcel.getData()));
        }
        executor.submit(() -> {
            try {
                Jedis resource = jedisPool.getResource();
                resource.publish(channel, senderId.toString() + ";" + parcel.getName() + ";" + parcel.getData() + ";" + finalId);
                debug(channel + ";" + senderId.toString() + ";" + parcel.getName() + ";" + parcel.getData() + ";" + finalId);
                jedisPool.returnResource(resource);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Sends a parcel over the network with a consumer waiting for a response parcel
     *
     * @param id   The parcel id
     * @param name The parcel name
     * @param data Json data to be sent
     */
    public void sendParcel(UUID id, String name, JsonObject data) {
        executor.submit(() -> {
            Jedis resource = jedisPool.getResource();
            resource.publish(channel, senderId.toString() + ";" + name + ";" + data.toString() + ";" + id);
            jedisPool.returnResource(resource);
        });
    }

    /**
     * Sends a parcel over the network with a consumer waiting for a response parcel
     *
     * @param id   The parcel id
     * @param name The parcel name
     * @param data Json data to be sent
     * @param responseConsumer Consumer function that handles the response from the remote server
     */
    public void sendParcel(UUID id, String name, JsonObject data, Consumer<FedExResponse> responseConsumer) {
        responseConsumers.put(id, new Pair<>(responseConsumer, System.currentTimeMillis()));
        executor.submit(() -> {
            Jedis resource = jedisPool.getResource();
            resource.publish(channel, senderId.toString() + ";" + name + ";" + data.toString() + ";" + id);
            jedisPool.returnResource(resource);
        });
    }

    /**
     * Sends a parcel over the network
     *
     * @param id     The id
     * @param parcel The parcel
     */
    public void sendParcel(@NotNull UUID id, @NotNull Parcel parcel) {
        sendParcel(id, parcel, null);
    }

    /**
     * Sends a parcel over the network with a consumer waiting for a response parcel
     *
     * @param parcel           The parcel
     * @param responseConsumer The response consumer
     */
    public void sendParcel(@NotNull Parcel parcel, @NotNull Consumer<FedExResponse> responseConsumer) {
        sendParcel(null, parcel, responseConsumer);
    }

    /**
     * Sends a parcel over the network
     *
     * @param parcel The parcel
     */
    public void sendParcel(@NotNull Parcel parcel) {
        sendParcel(null, parcel, null);
    }

    /**
     * Registers a parcel
     *
     * @param parcel The parcel
     */
    public void registerParcel(@NotNull Parcel parcel) {
        parcels.put(parcel.getName(), parcel);
    }

    /**
     * Registers a parcel by a class, needs a no-arg constructor
     *
     * @param parcelClass The class
     */
    public void registerParcel(@NotNull Class<? extends Parcel> parcelClass) {
        Parcel parcel = ClassUtils.tryGetInstance(parcelClass);
        if (parcel == null) {
            logger.warning("[FedEx] Parcel " + parcelClass.getName() + " could not be registered, please make a no-arg constructor.");
        } else {
            logger.warning("[FedEx] Parcel " + parcelClass.getSimpleName() + " has been registered.");
            parcels.put(parcel.getName(), parcel);
        }
    }


    /**
     * Registers all parcels in the package of `mainClass` using reflection and JarEntries
     *
     * @param mainClass The class
     */
    @SuppressWarnings("unchecked")
    public void registerParcels(@NotNull Class<?> mainClass) {
        ClassUtils.getClassesInPackage(mainClass).stream().filter(Parcel.class::isAssignableFrom).forEach(clazz -> registerParcel((Class<? extends Parcel>) clazz));
    }

    /**
     * Registers all parcels provided by instantiation through reflections
     *
     * @param classes The class
     */
    @SafeVarargs
    public final void registerParcels(Class<? extends Parcel>... classes) {
        Stream.of(classes).forEach(this::registerParcel);
    }

    public void debug(Object s) {
        if (debug) logger.warning((String) s);
    }

}
