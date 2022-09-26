package com.readutf.fedex.parcels.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.readutf.fedex.parcels.Parcel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

@Getter @RequiredArgsConstructor
public abstract class AutoParcel extends Parcel {

    private final ObjectMapper objectMapper;
    /**
     * Automatically serialised the data within the class
     * @return Serialised Class
     */
    @Override
    public @NotNull HashMap<String, Object> getData() {
        return objectMapper.convertValue(this, new TypeReference<HashMap<String, Object>>() {});
    }
}
