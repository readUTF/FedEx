package com.readutf.fedex.interfaces;

import com.google.gson.JsonObject;

public abstract class Parcel {

    public Parcel() {
        System.out.println("test");
    }

    public abstract String getName();

    public abstract JsonObject toJson();

}
