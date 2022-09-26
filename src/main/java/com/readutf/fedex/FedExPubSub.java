package com.readutf.fedex;

import com.readutf.fedex.parcels.Parcel;
import com.readutf.fedex.response.FedExResponse;
import com.readutf.fedex.response.FedExResponseParcel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.JedisPubSub;

import java.util.HashMap;
import java.util.UUID;

@AllArgsConstructor
final class FedExPubSub extends JedisPubSub {

    @NotNull
    private final FedEx fedEx;

    @Override
    public void onMessage(String channel, String message) {
        try {

            fedEx.debug("---------- PARCEL START ----------");
            String[] split = message.split(";");
            if (split.length < 4) {
                fedEx.getLogger().severe("invalid parcel received.");
                return;
            }
            fedEx.debug("Received parcel: " + message);
            UUID senderId = UUID.fromString(split[0]);
            String name = split[1];
            HashMap<String, Object> data = fedEx.jsonToMap(split[2]);
            UUID parcelId = UUID.fromString(split[3]);

            fedEx.debug("name: " + name);
            fedEx.debug("senderId: " + senderId);
            fedEx.debug("json: " + data);
            fedEx.debug("parcelId: " + parcelId);


            FedExResponse handleParcel = fedEx.getParcelListenerManager().handleParcel(name, parcelId, data);
            if (handleParcel != null) {
                fedEx.debug("handling response parcel");
                fedEx.sendParcel(handleParcel.getId(), new FedExResponseParcel(handleParcel));
            }

            if (fedEx.getParcels().containsKey(name)) {
                Parcel parcelHandler = fedEx.getParcels().get(name);
                fedEx.debug("Found parcel handler (selfRun: " + parcelHandler.isSelfRun() + ")");
                if (fedEx.getSenderId().equals(senderId) && !parcelHandler.isSelfRun()) return;
                fedEx.debug("Parcel Handled");
                FedExResponse fedExResponse = parcelHandler.onReceive(channel, parcelId, data);

                if (!name.equalsIgnoreCase("RESPONSE_PARCEL") && fedExResponse != null) {
                    fedEx.sendParcel(fedExResponse.getId(), new FedExResponseParcel(fedExResponse));
                }
            }

            fedEx.debug("---------- PARCEL END ----------");
        } catch (Exception e) {
            fedEx.getLogger().severe("Failed to handle redis message");
            e.printStackTrace();
        }
    }
}
