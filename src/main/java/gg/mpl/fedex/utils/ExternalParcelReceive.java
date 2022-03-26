package gg.mpl.fedex.utils;

import com.google.gson.JsonObject;
import gg.mpl.fedex.response.FedExResponse;

import java.util.UUID;

public abstract class ExternalParcelReceive {
    public abstract FedExResponse onReceive(UUID parcelId, JsonObject parcelData);
}
