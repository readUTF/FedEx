package com.readutf.fedex.parcels.impl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readutf.fedex.parcels.Parcel;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public abstract class AutoParcel extends Parcel {

    @JsonIgnore
    public static final ObjectMapper objectMapper = new ObjectMapper();

    @Override @JsonIgnore
    public @NotNull HashMap<String, Object> getData() {
        //handled externally
        return new HashMap<>();
    }
}
