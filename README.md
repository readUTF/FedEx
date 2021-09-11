# FedEx
FedEx is a wrapper for Jedis, it allows you to better send and manage data being sent through redis. The library is especially helpful for packets that are sent multiple times and always consist of the same data/layout.

# Initialising FedEx
Creating a FedEx instance requires 2 things, a channel name and a JedisPool. The messaging channel is where all data will be sent and is best kept unique to each project. Any FedEx instance listening on this channel will attempt to handle any packets sent through.
