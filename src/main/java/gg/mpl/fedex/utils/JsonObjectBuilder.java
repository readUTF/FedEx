package gg.mpl.fedex.utils;

import com.google.gson.JsonObject;

public class JsonObjectBuilder {

    JsonObject jsonObject;

    public JsonObjectBuilder() {
        this.jsonObject = new JsonObject();
    }

    public JsonObjectBuilder addProperty(String key, String value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public JsonObjectBuilder addProperty(String key, Boolean value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public JsonObjectBuilder addProperty(String key, Number value) {
        jsonObject.addProperty(key, value);
        return this;
    }

    public JsonObject build() {
        return jsonObject;
    }

}
