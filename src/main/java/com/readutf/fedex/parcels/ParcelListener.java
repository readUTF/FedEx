package com.readutf.fedex.parcels;

@FunctionalInterface
public interface ParcelListener {

    void onReceive(Parcel parcel);

}
