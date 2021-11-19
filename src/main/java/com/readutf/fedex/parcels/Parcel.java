package com.readutf.fedex.parcels;

import com.google.gson.JsonObject;
import com.readutf.fedex.FedEx;
import com.readutf.fedex.response.FedExResponse;
import lombok.Getter;

import java.util.UUID;

@Getter
public abstract class Parcel {

    public abstract String getName();
    public abstract JsonObject getData();

    /*
    * Code to be executed when the parcel is received
    * Returns Parcel which will be sent back in response
    * Returns null if no response is needed
    * */

    public abstract FedExResponse onReceive(UUID parcelId, JsonObject data);

    public void send() {
        FedEx.getInstance().sendParcel(this);
    }

}
