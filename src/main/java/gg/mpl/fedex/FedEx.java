package gg.mpl.fedex;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gg.mpl.fedex.listener.ParcelListener;
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
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Getter
@SuppressWarnings("unused")
public final class FedEx {

    @Deprecated @Getter private static FedEx instance;

    @Getter
    private static final Map<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = new HashMap<>();
    private final Map<String, Parcel> parcels = new HashMap<>();
    private final List<ParcelListener> parcelListeners;
    private final UUID senderId;
    private final String channel;
    private final JedisPool jedisPool;
    private final Gson gson;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private Logger logger = Logger.getLogger("FedEx");

    private boolean active;
    Thread poolThread;

    @Setter
    private boolean debug = false;

    private FedExPubSub pubSub;

    public FedEx(@NotNull String channel, @NotNull JedisPool jedisPool) {
        instance = this;
        this.channel = channel;
        this.jedisPool = jedisPool;
        this.gson = new Gson();

        senderId = UUID.randomUUID();
        parcelListeners = new ArrayList<>();

        registerParcel(new FedExResponseParcel(null));
        connect();
    }

    public void connect() {
        active = true;
        poolThread = new Thread(() -> jedisPool.getResource().subscribe(pubSub = new FedExPubSub(this), channel));
        poolThread.start();
        new TimeoutTask(new Timer(), this);


//        timeoutTask.start();
    }

    public void close() {
        System.out.println("fedex closed");
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
        if (parcel.isSelfRun()) {
            parcels.get(parcel.getName()).onReceive(channel, id, parcel.getData());
        }
        executor.submit(() -> {
            Jedis resource = getJedisPool().getResource();
            resource.publish(channel, senderId.toString() + ";" + parcel.getName() + ";" + parcel.getData() + ";" + finalId);
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

    @SafeVarargs
    public final void registerParcels(Class<? extends Parcel>... classes) {
        Stream.of(classes).forEach(this::registerParcel);
    }

    @SafeVarargs
    public final void registerParcelListeners(@NotNull ParcelListener... parcelListeners) {
        this.parcelListeners.addAll(Arrays.asList(parcelListeners));
    }

    public void debug(Object s) {
        if (debug) System.out.println(s);
    }

}
