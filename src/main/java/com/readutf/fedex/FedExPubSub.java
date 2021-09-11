package com.readutf.fedex;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.readutf.fedex.parcels.Parcel;
import com.readutf.fedex.response.FedExResponse;
import com.readutf.fedex.response.FedExResponseParcel;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class FedExPubSub extends JedisPubSub {

    FedEx fedEx;

    public FedExPubSub(FedEx fedEx) {
        this.fedEx = fedEx;
    }

    @Override
    public void onMessage(String channel, String message) {
        fedEx.debug("Received Parcel With Data: " + message);
        try {
            String[] split = message.split(";");
            if(split.length < 4) {
                System.out.println("invalid parcel received.");
                return;
            }
            UUID senderId = UUID.fromString(split[0]);
            String name = split[1];
            JsonObject jsonObject = new JsonParser().parse(split[2]).getAsJsonObject();
            UUID parselId = UUID.fromString(split[3]);

            if(fedEx.getSenderId().equals(senderId)) return;
            if(fedEx.getParcels().containsKey(name)) {
                Parcel parcelHandler = fedEx.getParcels().get(name);
                FedExResponse fedExResponse = parcelHandler.onReceive(parselId, jsonObject);
                if(!name.equalsIgnoreCase("RESPONSE_PARCEL") && fedExResponse != null) {
                    FedEx.getInstance().sendParcel(fedExResponse.getId(), new FedExResponseParcel(fedExResponse), null);
                }
            }


        } catch (Exception e) {
            System.out.println("Failed to handle redis message");
            e.printStackTrace();
        }

    }
}
