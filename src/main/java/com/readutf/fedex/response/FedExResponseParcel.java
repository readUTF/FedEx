package com.readutf.fedex.response;

import com.google.gson.JsonObject;
import com.readutf.fedex.FedEx;
import com.readutf.fedex.parcels.Parcel;

import java.util.UUID;

public class FedExResponseParcel extends Parcel {

    FedExResponse response;

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
        if(FedEx.getInstance().getResponseConsumers().containsKey(parcelId)) {
            FedExResponse response = new FedExResponse(data);
            FedEx.getInstance().getResponseConsumers().get(parcelId).accept(response);
        }
        return null;
    }
}
