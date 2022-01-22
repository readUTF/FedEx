package gg.mpl.fedex;

import com.google.gson.Gson;
import gg.mpl.fedex.parcels.Parcel;
import gg.mpl.fedex.response.FedExResponse;
import gg.mpl.fedex.response.FedExResponseParcel;
import gg.mpl.fedex.response.TimeoutTask;
import gg.mpl.fedex.utils.ClassUtils;
import gg.mpl.fedex.utils.Pair;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.config.Config;

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
    private final List<Consumer<Parcel>> parcelListeners = new ArrayList<>();
    private final UUID senderId;
    private final String channel;
    private final Gson gson;
    private final TimeoutTask timeoutTask;
    private final RedissonClient redisson;
    private Logger logger = Logger.getLogger("FedEx");

    private boolean active;

    @Setter
    private boolean debug = false;

    private FedExPubSub pubSub;

    public FedEx(@NotNull String channel, @NotNull Config redisConfig, @NotNull Gson gson, @NotNull Executor executor, Logger logger) {
        instance = this;
        this.channel = channel;
        this.gson = gson;
        this.executor = executor;
        this.redisson = Redisson.create(redisConfig);

        timeoutTask = new TimeoutTask();
        senderId = UUID.randomUUID();

        registerParcel(new FedExResponseParcel(null));
        connect();
    }

    public FedEx(@NotNull String channel, @NotNull Config redisConfig, @NotNull Gson gson) {
        this(channel, redisConfig, gson, ForkJoinPool.commonPool(), Logger.getLogger("FedEx"));
    }

    public FedEx(@NotNull String channel, @NotNull Config redisConfig, @NotNull Executor executor) {
        this(channel, redisConfig, new Gson(), executor, Logger.getLogger("FedEx"));
    }

    public FedEx(@NotNull String channel, @NotNull Config redisConfig, Logger logger) {
        this(channel, redisConfig, new Gson(), ForkJoinPool.commonPool(), logger);
    }

    public void connect() {
        active = true;

        new Thread(() -> {
            RTopic rTopic = redisson.getTopic(channel);
            rTopic.addListener(String.class, pubSub = new FedExPubSub(this));
//            jedisPool.getResource().subscribe(pubSub = new FedExPubSub(this), channel);
        }).start();


        timeoutTask.start();
    }

    public void close() {
        active = false;
        redisson.shutdown();
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
        new Thread(() -> {
            FedEx.getInstance().getLogger().severe("sending parcel: " + parcel.getName());
            RTopic topic = redisson.getTopic(channel);
            topic.publish(senderId.toString() + ";" + parcel.getName() + ";" + parcel.getData() + ";" + finalId);
            FedEx.getInstance().getLogger().severe("sent parcel: " + parcel.getName());
        }).start();
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
    public void registerParcels(@NotNull Class<?> mainClass) {
        ClassUtils.getClassesInPackage(mainClass).stream().filter(Parcel.class::isAssignableFrom).forEach(clazz -> registerParcel((Class<? extends Parcel>) clazz));
    }

    @SafeVarargs
    public final void registerParcelListeners(@NotNull Consumer<Parcel>... parcelConsumers) {
        parcelListeners.addAll(Arrays.asList(parcelConsumers));
    }
    public void debug(@NotNull String s) {
        if (debug) logger.severe(s);
    }
}
