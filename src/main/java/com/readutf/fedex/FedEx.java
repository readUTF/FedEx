package com.readutf.fedex;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readutf.fedex.parcels.impl.AutoParcel;
import com.readutf.fedex.response.FedExResponse;
import com.readutf.fedex.response.FedExResponseParcel;
import com.readutf.fedex.utils.ClassUtils;
import com.readutf.fedex.utils.Pair;
import com.readutf.fedex.listener.ParcelListenerManager;
import com.readutf.fedex.parcels.Parcel;
import com.readutf.fedex.response.TimeoutTask;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
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
@Getter
public class FedEx {

    @Deprecated
    @Getter
    private static FedEx instance;

    @Getter
    private static final Map<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = new HashMap<>();
    private final Map<String, Parcel> parcels = new HashMap<>();
    private final UUID senderId;
    private final String channel;
    private final JedisPool jedisPool;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper;
    private Logger logger = Logger.getLogger("FedEx");
    private final ParcelListenerManager parcelListenerManager;
    private boolean active;
    private Thread poolThread;
    private FedExPubSub pubSub;
    private TimeoutTask timeoutTask;

    @Setter
    private boolean debug = false;

    public FedEx(@NotNull String channel, @NotNull JedisPool jedisPool, ObjectMapper objectMapper) {
        instance = this;
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        senderId = UUID.randomUUID();

        parcelListenerManager = new ParcelListenerManager(this);

        registerParcel(new FedExResponseParcel(null));
        connect();
    }

    private void connect() {
        active = true;
        poolThread = new Thread(() -> jedisPool.getResource().subscribe(pubSub = new FedExPubSub(this), channel));
        poolThread.start();
        timeoutTask = new TimeoutTask(new Timer(), this);


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
        timeoutTask.getTimer().cancel();
    }

    /**
     * Sends a parcel over the network with a consumer waiting for a response parcel
     *
     * @param id               The id
     * @param parcel           The parcel
     * @param responseConsumer The response consumer
     */
    public void sendParcel(@Nullable UUID id, @NotNull Parcel parcel, @Nullable Consumer<FedExResponse> responseConsumer) {
        try {
            HashMap<String, Object> data = parcel.getData();
            debug("autoparcel? " + (parcel instanceof AutoParcel));
            if (parcel instanceof AutoParcel) {
                debug("is autoparcel");
                debug(parcel);
                data = objectMapper.convertValue(parcel, new TypeReference<HashMap<String, Object>>() {});
                debug("data: " + data);
            }
            HashMap<String, Object> finalData = data;
            debug("Sending parcel: " + id + " with data " + data);
            if (id == null) id = UUID.randomUUID();
            UUID finalId = id;
            if (parcel.isSelfRun() || parcel.getClass().isAnnotationPresent(SelfRun.class)) {
                debug("Parcel is self run so finding local receiver");
                Optional.ofNullable(parcels.get(parcel.getName())).ifPresent(parcel1 -> parcel1.onReceive(channel, finalId, finalData));
            }

            @Nullable UUID finalId1 = id;
            executor.submit(() -> {
                try {
                    debug("Submitting parcel to executor");
                    Jedis resource = jedisPool.getResource();
                    resource.publish(channel, senderId.toString() + ";" + parcel.getName() + ";" + objectMapper.writeValueAsString(finalData) + ";" + finalId);
                    debug(channel + ";" + senderId + ";" + parcel.getName() + ";" + finalData + ";" + finalId);
                    jedisPool.returnResource(resource);
                    debug("Parcel sent");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (responseConsumer != null)
                    responseConsumers.put(finalId1, new Pair<>(responseConsumer, System.currentTimeMillis()));
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a parcel over the network with a consumer waiting for a response parcel
     *
     * @param id   The parcel id
     * @param name The parcel name
     * @param data Json data to be sent
     */
    public void sendParcel(UUID id, String name, HashMap<String, Object> data) {
        executor.submit(() -> {
            Jedis resource = jedisPool.getResource();
            resource.publish(channel, senderId.toString() + ";" + name + ";" + mapToJson(data) + ";" + id);
            jedisPool.returnResource(resource);
        });
    }

    /**
     * Sends a parcel over the network with a consumer waiting for a response parcel
     *
     * @param id               The parcel id
     * @param name             The parcel name
     * @param data             Json data to be sent
     * @param responseConsumer Consumer function that handles the response from the remote server
     */
    public void sendParcel(UUID id, String name, HashMap<String, Object> data, Consumer<FedExResponse> responseConsumer) {
        responseConsumers.put(id, new Pair<>(responseConsumer, System.currentTimeMillis()));
        executor.submit(() -> {
            Jedis resource = jedisPool.getResource();
            resource.publish(channel, senderId.toString() + ";" + name + ";" + mapToJson(data) + ";" + id);
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
        if (debug) debug((String) s);
    }

    @SneakyThrows
    protected HashMap<String, Object> jsonToMap(String json) {
        return objectMapper.readValue(json, new TypeReference<HashMap<String, Object>>() {
        });
    }

    @SneakyThrows
    protected String mapToJson(HashMap<String, Object> data) {
        return objectMapper.writeValueAsString(data);
    }


}
