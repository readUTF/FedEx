package gg.mpl.fedex;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.mpl.fedex.parcels.Parcel;
import gg.mpl.fedex.response.FedExResponse;
import gg.mpl.fedex.response.FedExResponseParcel;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

final class FedExPubSub extends JedisPubSub {

    private final FedEx fedEx;

    public FedExPubSub(FedEx fedEx) {
        this.fedEx = fedEx;
    }

    @Override
    public void onMessage(String channel, String message) {
        try {
            String[] split = message.split(";");
            if (split.length < 4) {
                System.out.println("invalid parcel received.");
                return;
            }
            UUID senderId = UUID.fromString(split[0]);
            String name = split[1];
            JsonObject jsonObject = JsonParser.parseString(split[2]).getAsJsonObject();
            UUID parcelId = UUID.fromString(split[3]);

            if (fedEx.getSenderId().equals(senderId))
                return;

            if (fedEx.getParcels().containsKey(name)) {
                Parcel parcelHandler = fedEx.getParcels().get(name);
                FedExResponse fedExResponse = parcelHandler.onReceive(parcelId, jsonObject);

                if (!name.equalsIgnoreCase("RESPONSE_PARCEL") && fedExResponse != null) {
                    FedEx.getInstance().sendParcel(fedExResponse.getId(), new FedExResponseParcel(fedExResponse));
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to handle redis message");
            e.printStackTrace();
        }
    }
}
