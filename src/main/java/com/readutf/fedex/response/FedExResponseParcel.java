package com.readutf.fedex.response;

import com.google.gson.JsonObject;
import com.readutf.fedex.utils.Pair;
import com.readutf.fedex.parcels.Parcel;
import com.readutf.fedex.FedEx;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class FedExResponseParcel extends Parcel {

    private final FedExResponse response;

    public FedExResponseParcel(FedExResponse fedExResponse) {
        response = fedExResponse;
    }

    @Override
    public @NotNull String getName() {
        return "RESPONSE_PARCEL";
    }

    @Override
    public @NotNull JsonObject getData() {
        return response.getResponseData();
    }

    @Override
    public FedExResponse onReceive(String channel, @NotNull UUID parcelId, @NotNull JsonObject data) {
        Map<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = FedEx.getResponseConsumers();
        if (responseConsumers.containsKey(parcelId)) {
            FedExResponse response = new FedExResponse(data);

            responseConsumers.get(parcelId).getKey().accept(response);
            responseConsumers.remove(parcelId);
        }

        return null;
    }
}
