package com.readutf.fedex.response;

import com.google.gson.JsonObject;
import com.readutf.fedex.FedEx;
import com.readutf.fedex.utils.Pair;

import java.util.*;
import java.util.function.Consumer;

public class TimeoutTask extends Thread {

    public TimeoutTask() {
        setDaemon(true);
    }

    @Override
    public void run() {
        while (FedEx.getInstance().isActive()) {
            List<UUID> toRemove = new ArrayList<>();
            Map<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = FedEx.getInstance().getResponseConsumers();
            for (Map.Entry<UUID, Pair<Consumer<FedExResponse>, Long>> entry : responseConsumers.entrySet()) {
                UUID uuid = entry.getKey();
                Pair<Consumer<FedExResponse>, Long> responseConsumerAndTimestamp = entry.getValue();
                if(System.currentTimeMillis() - responseConsumerAndTimestamp.getValue() > 3000) {
                    responseConsumerAndTimestamp.getKey().accept(new FedExResponse(uuid, FedExResponse.ResponseType.TIMED_OUT, new JsonObject()));
                }

                toRemove.add(uuid);
            }

            toRemove.forEach(responseConsumers::remove);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
