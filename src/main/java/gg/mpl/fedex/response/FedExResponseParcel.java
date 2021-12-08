package gg.mpl.fedex.response;

import com.google.gson.JsonObject;
import gg.mpl.fedex.FedEx;
import gg.mpl.fedex.parcels.Parcel;
import gg.mpl.fedex.utils.Pair;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class FedExResponseParcel extends Parcel {
    private final FedExResponse response;

    public FedExResponseParcel(FedExResponse fedExResponse) {
        response = fedExResponse;
    }

    @Override
    public String getName() {
        return "RESPONSE_PARCEL";
    }

    @Override
    public JsonObject getData() {
        return response.getResponseData();
    }

    @Override
    public FedExResponse onReceive(UUID parcelId, JsonObject data) {
        Map<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = FedEx.getInstance().getResponseConsumers();
        if (responseConsumers.containsKey(parcelId)) {
            FedExResponse response = new FedExResponse(data);

            responseConsumers.get(parcelId).getKey().accept(response);
            responseConsumers.remove(parcelId);
        }

        return null;
    }
}