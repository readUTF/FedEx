package com.readutf.fedex.parcels;

import com.readutf.fedex.FedEx;
import com.readutf.fedex.response.FedExResponse;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Getter
public abstract class Parcel {
    /**
     * Gets the name of this parcel, used to determine which parcel is which in messaging
     *
     * @return The name
     */
    @NotNull
    public abstract String getName();

    /**
     * Gets the data to be sent as a {@link HashMap}
     *
     * @return The data
     */
    @NotNull
    public abstract HashMap<String, Object> getData();


    /**
     * Called when this parcel is received
     *
     * @return Parcel which will be sent back in response or null if no response is needed
     */
    public abstract FedExResponse onReceive(String channel, @NotNull UUID parcelId, @NotNull HashMap<String, Object> data);

    public FedExResponse onReceive(@NotNull UUID parcelId, @NotNull HashMap<String, Object> data) {
        return null;
    }

    /**
     * Helper function that calls {@link FedEx#sendParcel(Parcel)} with `this`
     */
    public final void send(FedEx fedEx) {
        fedEx.sendParcel(this);
    }

    public FedExResponse sendAndGet(FedEx fedEx) {
        UUID id = UUID.randomUUID();
        CompletableFuture<FedExResponse> future = new CompletableFuture<>();
        fedEx.sendParcel(id, this, future::complete);
        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return new FedExResponse(id, FedExResponse.ResponseType.TIMED_OUT, new HashMap<>());
        }
    }

    /**
     * Helper function that calls {@link FedEx#sendParcel(Parcel)} with `this`
     */
    public final void send(FedEx fedEx, @NotNull Consumer<FedExResponse> responseConsumer) {
        fedEx.sendParcel(this, responseConsumer);
    }

    @Getter
    @Setter
    private boolean selfRun = false;

    @NotNull
    @Override
    public String toString() {
        return getName() + "|" + getData();
    }
}
