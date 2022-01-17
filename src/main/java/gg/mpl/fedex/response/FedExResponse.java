package gg.mpl.fedex.response;

import com.google.gson.JsonObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter
public final class FedExResponse {

    private final ResponseType responseType;
    private final JsonObject responseData;
    private UUID id;

    public FedExResponse(@NotNull UUID id, @NotNull ResponseType responseType, @NotNull JsonObject responseData) {
        this.id = id;
        this.responseType = responseType;
        this.responseData = responseData;
    }

    public FedExResponse(@NotNull JsonObject parcelData) {
        this.responseType = ResponseType.valueOf(parcelData.get("ResponseType").getAsString());
        this.responseData = parcelData;

        this.responseData.remove("ResponseType");
    }

    @NotNull
    public JsonObject getResponseData() {
        responseData.addProperty("ResponseType", responseType.name());
        return responseData;
    }

    @SuppressWarnings("unused")
    public enum ResponseType {
        SUCCESS, FAILED, TIMED_OUT
    }
}
