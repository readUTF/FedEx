package gg.mpl.fedex.listener;

import com.google.gson.JsonObject;

import java.util.UUID;

public interface ParcelListener {

    boolean handleParcel(String name, UUID parcelId, JsonObject data);

}
