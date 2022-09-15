package com.readutf.fedex;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.readutf.fedex.listener.ParcelListenerManager;
import com.readutf.fedex.parcels.Parcel;
import com.readutf.fedex.response.FedExResponse;
import com.readutf.fedex.response.FedExResponseParcel;
import com.readutf.fedex.utils.ClassUtils;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.stream.Stream;

@SuppressWarnings("unused")
@Getter
public class FedEx {

    @Deprecated
    @Getter
    private static FedEx instance;

    @Getter
    private static final Map<UUID, CompletableFuture<FedExResponse>> responseFutures = new HashMap<>();
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
     * Send a parcel through this instance's registered channel
     * @param id Parcel id, used to link send data with response parcel
     * @param parcel Data container
     * @return Response future
     */
    public CompletableFuture<FedExResponse> sendParcel(@Nullable UUID id, @NotNull Parcel parcel) {
        if (id == null) id = UUID.randomUUID();

        CompletableFuture<FedExResponse> future = new CompletableFuture<>();
        responseFutures.put(id, future);

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
        return future;
    }


    public CompletableFuture<FedExResponse> sendParcel(UUID id, String name, JsonObject data) {
        CompletableFuture<FedExResponse> future = new CompletableFuture<>();
        responseFutures.put(id, future);
        System.out.println("trying to publish?");

            Jedis resource = jedisPool.getResource();
            System.out.println("publishing data");
            resource.publish(channel, senderId.toString() + ";" + name + ";" + data.toString() + ";" + id);
            jedisPool.returnResource(resource);

        return future;
    }

    public CompletableFuture<FedExResponse> sendParcel(@NotNull Parcel parcel) {
        return sendParcel(null, parcel);
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
