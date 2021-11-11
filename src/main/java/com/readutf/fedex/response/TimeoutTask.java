package com.readutf.fedex.response;

import com.google.gson.JsonObject;
import com.readutf.fedex.FedEx;
import com.readutf.fedex.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class TimeoutTask extends Thread {

    public TimeoutTask() {
        setDaemon(true);
    }

    @Override
    public void run() {
        while (FedEx.getInstance().isActive()) {
            List<UUID> toRemove = new ArrayList<>();
            HashMap<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = FedEx.getInstance().getResponseConsumers();
            responseConsumers.forEach((uuid, consumerLongPair) -> {
                if(System.currentTimeMillis() - consumerLongPair.getValue() > 3000) {
                    consumerLongPair.getKey().accept(new FedExResponse(uuid, FedExResponse.ResponseType.TIMED_OUT, new JsonObject()));
                }
                toRemove.add(uuid);
            });
            toRemove.forEach(responseConsumers::remove);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
