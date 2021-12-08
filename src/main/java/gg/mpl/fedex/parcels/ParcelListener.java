package gg.mpl.fedex.parcels;

@FunctionalInterface
public interface ParcelListener {
    void onReceive(Parcel parcel);
}
