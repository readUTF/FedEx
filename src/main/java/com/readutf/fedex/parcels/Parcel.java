package com.readutf.fedex.parcels;

import com.google.gson.JsonObject;
import com.readutf.fedex.FedEx;
import com.readutf.fedex.response.FedExResponse;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
     * Gets the data to be sent as a {@link JsonObject}
     *
     * @return The data
     */
    @NotNull
    public abstract JsonObject getData();


    /**
     * Called when this parcel is received
     *
     * @return Parcel which will be sent back in response or null if no response is needed
     */
    public abstract FedExResponse onReceive(String channel, @NotNull UUID parcelId, @NotNull JsonObject data);

    public FedExResponse onReceive(@NotNull UUID parcelId, @NotNull JsonObject data) {
        return null;
    }


    /**
     * Helper function that calls {@link FedEx#sendParcel(Parcel)} with `this`
     */
    public final CompletableFuture<FedExResponse> send(FedEx fedEx) {
        return fedEx.sendParcel(this);
    }

    @Getter @Setter
    private boolean selfRun = false;

    @NotNull
    @Override
    public String toString() {
        return getName() + "|" + getData();
    }
}
