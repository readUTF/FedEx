package com.readutf.fedex.response;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;

@Getter
public final class FedExResponse {

    private final ResponseType responseType;
    private final HashMap<String, Object> responseData;
    private UUID id;

    public FedExResponse(@NotNull UUID id, @NotNull ResponseType responseType, @NotNull HashMap<String, Object> responseData) {
        this.id = id;
        this.responseType = responseType;
        this.responseData = responseData;
    }

    public FedExResponse(@NotNull HashMap<String, Object> parcelData) {
        this.responseType = ResponseType.valueOf((String) parcelData.get("ResponseType"));
        this.responseData = parcelData;

        this.responseData.remove("ResponseType");
    }

    @NotNull
    public HashMap<String, Object> getResponseData() {
        responseData.put("ResponseType", responseType.name());
        return responseData;
    }

    @Override
    public String toString() {
        return "FedExResponse{" +
                "responseType=" + responseType +
                ", responseData=" + responseData +
                ", id=" + id +
                '}';
    }

    @SuppressWarnings("unused")
    public enum ResponseType {
        SUCCESS, FAILED, TIMED_OUT
    }
}
