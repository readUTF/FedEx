# FedEx

FedEx is a wrapper for Jedis, it allows you to better send and manage data being sent through redis. The library is especially helpful for packets that are sent multiple times and always consist of the same data/layout.

# Initialising FedEx

Creating a FedEx instance requires 2 things, a channel name and a JedisPool. The messaging channel is where all data will be sent and is best kept unique to each project. Any FedEx instance listening on this channel will attempt to handle any packets sent through.

**Example**:

```java
JedisPool jedis = new JedisPool("localhost", 6379);
FedEx fedEx = new FedEx("example", jedis);
```

# Creating a parcel

Parcels are how data is sent between servers. They need to be registered and handle what happens when data is received, what to do when it's received and what to respond.
To get started, create a new class that extends Parcel. Below is an example of what a simple parcel looks like:

```java
package com.example.net;

import com.google.gson.JsonObject;
import Parcel;
import FedExResponse;

import java.util.UUID;

public class ExampleParcel extends Parcel {

    String message;

    public ExampleParcel(String message) {
        this.message = message;
    }

    @Override
    public String getName() {
        return "EXAMPLE_PARCEL";
    }

    @Override
    public JsonObject getData() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("message", message);
        return jsonObject;
    }

    @Override
    public FedExResponse onReceive(UUID parcelId, JsonObject jsonObject) {
        FedEx.getInstance().getLogger().severe(jsonObject.get("message").getAsString());
        return new FedExResponse(parcelId, FedExResponse.ResponseType.SUCCESS, new JsonObject());
    }
}
```

Data being sent is defined in the getData function, this is called when the parcel is sent. When a parcel with the name **"EXAMPLE_PARCEL"** the onReceiive function is called, this executes only on another instance of fedex. For this to occur, a parcel with the same name needs to be registered on both ends. A parcel can act as a receiver by having the getData function return null and the onReceive do something and vice verse for a sender parcel. Custom data can be sent in response defined by the 3rd paramater in **FedExResponse**

Parcels need to be registered for them to act when data is received. This can be done as follows:

```java
fedEx.registerParcels(ExampleParcel.class);
```

# Sending a parcel

Sending a parcel invoels creating an instance of the parcel, and handing any response received back. By default if no response is received the parcel Consumer will respond with a time out ResponseType. However, if no response is expected this can be ignored.

```java
fedEx.sendParcel(new ExampleParcel("test"), fedExResponse -> FedEx.getInstance().getLogger().severe("example parcel sent successfully"));
```

Custom data sent back can be access in the fedexResponse consumer.
