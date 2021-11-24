package com.readutf.fedex.parcels;

import com.google.gson.JsonObject;
import com.readutf.fedex.FedEx;
import com.readutf.fedex.response.FedExResponse;
import lombok.Getter;

import java.util.UUID;
import java.util.function.Consumer;

@Getter
public abstract class Parcel {

    /**
     * Gets the name of this parcel, used to determine which parcel is which in messaging
     *
     * @return The name
     */
    public abstract String getName();

    /**
     * Gets the data to be sent as a {@link JsonObject}
     *
     * @return The data
     */
    public abstract JsonObject getData();

    /**
     * Called when this parcel is received
     *
     * @return Parcel which will be sent back in response or null if no response is needed
     */
    public abstract FedExResponse onReceive(UUID parcelId, JsonObject data);

    /**
     * Helper function that calls {@link FedEx#sendParcel(Parcel)} with `this`
     */
    public final void send() {
        FedEx.getInstance().sendParcel(this);
    }

    /**
     * Helper function that calls {@link FedEx#sendParcel(Parcel)} with `this`
     */
    public final void send(Consumer<FedExResponse> responseConsumer) {
        FedEx.getInstance().sendParcel(this, responseConsumer);
    }

    @Override
    public String toString() {
        return getName() + "|" + getData().toString();
    }
}
