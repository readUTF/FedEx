package gg.mpl.fedex.parcels.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gg.mpl.fedex.parcels.Parcel;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public abstract class AutoParcel extends Parcel {

    private static final Gson GSON = new Gson();

    /**
     * Automatically serialised the data within the class
     * @return Serialised Class
     */
    @Override
    public @NotNull JsonObject getData() {
        return GSON.toJsonTree(this).getAsJsonObject();
    }
}
