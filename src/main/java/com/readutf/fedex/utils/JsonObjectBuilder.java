package com.readutf.fedex.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.HashMap;

public class JsonObjectBuilder {

    private static Gson gson = new Gson();

    HashMap<String, Object> objects;

    public JsonObjectBuilder(String key, Object value) {
        objects = new HashMap<>();
        objects.put(key, value);
    }

    public JsonObjectBuilder() {
        objects = new HashMap<>();
    }

    public JsonObjectBuilder add(String key, Object value) {
        objects.put(key, value);
        return this;
    }

    public JsonObject build() {
        JsonObject jsonObject = new JsonObject();
        return gson.toJsonTree(objects).getAsJsonObject();
    }

}
