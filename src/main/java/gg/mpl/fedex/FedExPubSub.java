package gg.mpl.fedex;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.mpl.fedex.parcels.Parcel;
import gg.mpl.fedex.response.FedExResponse;
import gg.mpl.fedex.response.FedExResponseParcel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

@AllArgsConstructor
final class FedExPubSub extends JedisPubSub {

    @NotNull
    private final FedEx fedEx;

    @Override
    public void onMessage(String channel, String message) {
        try {
            String[] split = message.split(";");
            if (split.length < 4) {
                fedEx.getLogger().severe("invalid parcel received.");
                return;
            }
            fedEx.debug("Received parcel: " + message);
            UUID senderId = UUID.fromString(split[0]);
            String name = split[1];
            JsonObject jsonObject = new JsonParser().parse(split[2]).getAsJsonObject();
            UUID parcelId = UUID.fromString(split[3]);

            fedEx.debug(senderId);
            fedEx.debug(name);
            fedEx.debug(jsonObject);
            fedEx.debug(parcelId);

            if (fedEx.getParcelListeners().stream().anyMatch(parcelListener -> !parcelListener.handleParcel(name, parcelId, jsonObject))) {
                return;
            }

            if (fedEx.getParcels().containsKey(name)) {
                fedEx.debug("Found parcel handler");
                Parcel parcelHandler = fedEx.getParcels().get(name);
                if (fedEx.getSenderId().equals(senderId)) return;
                fedEx.debug("Parcel Handled");
                FedExResponse fedExResponse = parcelHandler.onReceive(channel, parcelId, jsonObject);

                if (!name.equalsIgnoreCase("RESPONSE_PARCEL") && fedExResponse != null) {
                    fedEx.sendParcel(fedExResponse.getId(), new FedExResponseParcel(fedExResponse));
                }
            }
        } catch (Exception e) {
            fedEx.getLogger().severe("Failed to handle redis message");
            e.printStackTrace();
        }
    }
}
