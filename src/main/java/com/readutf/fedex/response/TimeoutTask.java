package com.readutf.fedex.response;

import com.readutf.fedex.FedEx;
import com.readutf.fedex.utils.Pair;
import lombok.Getter;

import java.util.*;
import java.util.function.Consumer;

@Getter
public final class TimeoutTask extends TimerTask {

    Timer timer;
    FedEx fedEx;

    public TimeoutTask(Timer timer, FedEx fedEx) {
        this.fedEx = fedEx;
        this.timer = timer;
        timer.scheduleAtFixedRate(this, 0, 1000);
    }

    @Override
    @SuppressWarnings("BusyWait")
    public void run() {
        List<UUID> toRemove = new ArrayList<>();
        Map<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = fedEx.getResponseConsumers();
        for (Map.Entry<UUID, Pair<Consumer<FedExResponse>, Long>> entry : responseConsumers.entrySet()) {
            UUID uuid = entry.getKey();
            Pair<Consumer<FedExResponse>, Long> responseConsumerAndTimestamp = entry.getValue();
            long taken = System.currentTimeMillis() - responseConsumerAndTimestamp.getValue();
            if (taken > 1000) {
                responseConsumerAndTimestamp.getKey().accept(new FedExResponse(uuid, FedExResponse.ResponseType.TIMED_OUT, new HashMap<>()));
                toRemove.add(uuid);
            }
        }
        toRemove.forEach(responseConsumers::remove);
    }
}
