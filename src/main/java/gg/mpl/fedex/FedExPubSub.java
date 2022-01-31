package gg.mpl.fedex;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import gg.mpl.fedex.parcels.Parcel;
import gg.mpl.fedex.response.FedExResponse;
import gg.mpl.fedex.response.FedExResponseParcel;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.listener.MessageListener;

import java.util.UUID;

@AllArgsConstructor
final class FedExPubSub implements MessageListener<String> {

    @NotNull
    private final FedEx fedEx;

    @Override
    public void onMessage(CharSequence charSequence, String message) {
        try {
            String[] split = message.split(";");
            if (split.length < 4) {
                FedEx.getInstance().getLogger().severe("invalid parcel received.");
                return;
            }
            UUID senderId = UUID.fromString(split[0]);
            String name = split[1];
            JsonObject jsonObject = new JsonParser().parse(split[2]).getAsJsonObject();
            UUID parcelId = UUID.fromString(split[3]);

            if (fedEx.getSenderId().equals(senderId)) return;

//            FedEx.getInstance().getLogger().severe("received command " + name);

            if (fedEx.getParcels().containsKey(name)) {
                Parcel parcelHandler = fedEx.getParcels().get(name);
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
