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
        System.out.println("tst");
        try {
            String[] split = message.split(";");
            if (split.length < 4) {
                FedEx.getInstance().getLogger().severe("invalid parcel received.");
                return;
            }
            FedEx.getInstance().debug("Received parcel: " + message);
            UUID senderId = UUID.fromString(split[0]);
            String name = split[1];
            JsonObject jsonObject = new JsonParser().parse(split[2]).getAsJsonObject();
            UUID parcelId = UUID.fromString(split[3]);

            FedEx.getInstance().debug(senderId);
            FedEx.getInstance().debug(name);
            FedEx.getInstance().debug(jsonObject);
            FedEx.getInstance().debug(parcelId);

//            FedEx.getInstance().getLogger().severe("received command " + name);

            if (fedEx.getParcels().containsKey(name)) {
                FedEx.getInstance().debug("Found parcel handler");
                Parcel parcelHandler = fedEx.getParcels().get(name);
                if (fedEx.getSenderId().equals(senderId) && !parcelHandler.isSelfRun()) return;
                FedEx.getInstance().debug("Parcel Handled");
                FedExResponse fedExResponse = parcelHandler.onReceive(parcelId, jsonObject);

                if (!name.equalsIgnoreCase("RESPONSE_PARCEL") && fedExResponse != null) {
                    FedEx.getInstance().sendParcel(fedExResponse.getId(), new FedExResponseParcel(fedExResponse));
                }
            }
        } catch (Exception e) {
            FedEx.getInstance().getLogger().severe("Failed to handle redis message");
            e.printStackTrace();
        }
    }
}
