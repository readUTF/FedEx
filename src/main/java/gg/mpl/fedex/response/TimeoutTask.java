package gg.mpl.fedex.response;

import com.google.gson.JsonObject;
import gg.mpl.fedex.FedEx;
import gg.mpl.fedex.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class TimeoutTask extends Thread {
    public TimeoutTask() {
        setDaemon(true);
    }

    @Override
    @SuppressWarnings("BusyWait")
    public void run() {
        while (FedEx.getInstance().isActive()) {
            List<UUID> toRemove = new ArrayList<>();
            Map<UUID, Pair<Consumer<FedExResponse>, Long>> responseConsumers = FedEx.getInstance().getResponseConsumers();
            for (Map.Entry<UUID, Pair<Consumer<FedExResponse>, Long>> entry : responseConsumers.entrySet()) {
                UUID uuid = entry.getKey();
                Pair<Consumer<FedExResponse>, Long> responseConsumerAndTimestamp = entry.getValue();
                if (System.currentTimeMillis() - responseConsumerAndTimestamp.getValue() > 3000) {
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
