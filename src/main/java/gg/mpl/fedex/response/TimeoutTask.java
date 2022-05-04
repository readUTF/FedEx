package gg.mpl.fedex.response;

import com.google.gson.JsonObject;
import gg.mpl.fedex.FedEx;
import gg.mpl.fedex.utils.Pair;

import java.util.*;
import java.util.function.Consumer;

public final class TimeoutTask extends TimerTask {

    FedEx fedEx;

    public TimeoutTask(Timer timer, FedEx fedEx) {
        this.fedEx = fedEx;
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
                responseConsumerAndTimestamp.getKey().accept(new FedExResponse(uuid, FedExResponse.ResponseType.TIMED_OUT, new JsonObject()));
                toRemove.add(uuid);
            }
        }
        toRemove.forEach(responseConsumers::remove);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
