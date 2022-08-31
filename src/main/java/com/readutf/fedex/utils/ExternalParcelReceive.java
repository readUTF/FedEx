package com.readutf.fedex.utils;

import com.google.gson.JsonObject;
import com.readutf.fedex.response.FedExResponse;

import java.util.UUID;

public abstract class ExternalParcelReceive {
    public abstract FedExResponse onReceive(UUID parcelId, JsonObject parcelData);
}
