package gg.mpl.fedex.listener;

import com.google.gson.JsonObject;

import java.util.UUID;

public class TestListener {

    @ParcelListener("test")
    public void handleTest(UUID id, JsonObject data) {
        System.out.println("test");
    }

}
