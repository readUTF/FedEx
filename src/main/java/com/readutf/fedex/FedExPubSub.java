package com.readutf.fedex;

import com.readutf.fedex.parcels.Parcel;
import com.readutf.fedex.response.FedExResponse;
import com.readutf.fedex.response.FedExResponseParcel;
import com.readutf.uls.Logger;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import redis.clients.jedis.JedisPubSub;

import java.util.HashMap;
import java.util.UUID;

@AllArgsConstructor
final class FedExPubSub extends JedisPubSub {

    private static Logger logger = FedEx.getLoggerFactory().getLogger(FedExPubSub.class);

    @NotNull
    private final FedEx fedEx;

    @Override
    public void onMessage(String channel, String message) {
        try {

            logger.debug("---------- PARCEL START ----------");
            String[] split = message.split(";");
            if (split.length < 4) {
                logger.severe("invalid parcel received.");
                return;
            }
            logger.debug("Received parcel: " + message);
            UUID senderId = UUID.fromString(split[0]);
            String name = split[1];
            HashMap<String, Object> data = fedEx.jsonToMap(split[2]);
            UUID parcelId = UUID.fromString(split[3]);

            logger.debug("name: " + name);
            logger.debug("senderId: " + senderId);
            logger.debug("json: " + data);
            logger.debug("parcelId: " + parcelId);


            FedExResponse handleParcel = fedEx.getParcelListenerManager().handleParcel(name, parcelId, data);
            if (handleParcel != null) {
                fedEx.sendParcel(handleParcel.getId(), new FedExResponseParcel(handleParcel));
            }

            if (fedEx.getParcels().containsKey(name)) {
                Parcel parcelHandler = fedEx.getParcels().get(name);
                Logger parcelLogger = FedEx.getLoggerFactory().getLogger(parcelHandler.getClass());

                parcelLogger.debug("Found parcel handler (selfRun: " + parcelHandler.isSelfRun() + ")");
                if (fedEx.getSenderId().equals(senderId) && !parcelHandler.isSelfRun()) return;
                parcelLogger.debug("Parcel Handled");
                FedExResponse fedExResponse = parcelHandler.onReceive(channel, parcelId, data);

                if (!name.equalsIgnoreCase("RESPONSE_PARCEL") && fedExResponse != null) {
                    fedEx.sendParcel(fedExResponse.getId(), new FedExResponseParcel(fedExResponse));
                }
            }

            logger.debug("---------- PARCEL END ----------");
        } catch (Exception e) {
            logger.severe("Failed to handle redis message");
            e.printStackTrace();
        }
    }
}
