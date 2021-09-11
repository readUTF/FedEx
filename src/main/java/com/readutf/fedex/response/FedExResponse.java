package com.readutf.fedex.response;

import com.google.gson.JsonObject;
import lombok.Getter;

import java.util.UUID;

@Getter
public class FedExResponse {

    private UUID id;
    private ResponseType responseType;
    private JsonObject responseData;

    public FedExResponse(UUID id, ResponseType responseType, JsonObject responseData) {
        this.id = id;
        this.responseType = responseType;
        this.responseData = responseData;
    }

    public FedExResponse(JsonObject parcelData) {
        this.responseType = ResponseType.valueOf(parcelData.get("ResponseType").getAsString());
        parcelData.remove("ResponseType");
        this.responseData = parcelData;
    }

    public JsonObject getResponseData() {
        responseData.addProperty("ResponseType", responseType.name());
        return responseData;
    }

    public enum ResponseType {

        SUCCESS,
        FAILED,
        TIMED_OUT

    }

}
