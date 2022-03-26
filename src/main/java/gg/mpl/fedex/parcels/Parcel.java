package gg.mpl.fedex.parcels;

import com.google.gson.JsonObject;
import gg.mpl.fedex.FedEx;
import gg.mpl.fedex.response.FedExResponse;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
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
    public abstract FedExResponse onReceive(@NotNull UUID parcelId, @NotNull JsonObject data);

    /**
     * Helper function that calls {@link FedEx#sendParcel(Parcel)} with `this`
     */
    public final void send() {
        FedEx.getInstance().sendParcel(this);
    }

    /**
     * Helper function that calls {@link FedEx#sendParcel(Parcel)} with `this`
     */
    public final void send(@NotNull Consumer<FedExResponse> responseConsumer) {
        FedEx.getInstance().sendParcel(this, responseConsumer);
    }

    @Getter @Setter
    private boolean selfRun = false;

    @NotNull
    @Override
    public String toString() {
        return getName() + "|" + getData();
    }
}
