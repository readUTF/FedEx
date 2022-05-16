package gg.mpl.fedex.listener;

import com.google.gson.JsonObject;
import gg.mpl.fedex.response.FedExResponse;

import java.util.UUID;

public interface ParcelListener {

    FedExResponse handleParcel(String name, UUID parcelId, JsonObject data);

}
