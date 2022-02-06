package gg.mpl.fedex.utils;

import gg.mpl.fedex.FedEx;
import redis.clients.jedis.Jedis;

import java.util.function.Consumer;

public class JedisQuick {
    
    public static void set(String key, String value) {
        try {
            Jedis jedis = FedEx.getInstance().getJedisPool().borrowObject();
            jedis.set(key, value);
            FedEx.getInstance().getJedisPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        String value = null;
        try {
            Jedis jedis = FedEx.getInstance().getJedisPool().borrowObject();
            value = jedis.get(key);
            FedEx.getInstance().getJedisPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static void del(String key) {

        try {
            Jedis jedis = FedEx.getInstance().getJedisPool().borrowObject();
            jedis.del(key);
            FedEx.getInstance().getJedisPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void useRedis(Consumer<Jedis> jedisConsumer) {
        try {
            Jedis jedis = FedEx.getInstance().getJedisPool().borrowObject();
            jedisConsumer.accept(jedis);
            FedEx.getInstance().getJedisPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
