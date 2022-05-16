package gg.mpl.fedex.utils;

import gg.mpl.fedex.FedEx;
import lombok.SneakyThrows;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.function.Consumer;

public class JedisQuick {

    public static void set(String key, String value) {
        try {
            Jedis jedis = getPool().borrowObject();
            jedis.set(key, value);
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean exists(String key) {
        try {
            Jedis jedis = getPool().borrowObject();
            return jedis.exists(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String get(String key) {
        String value = null;
        try {
            Jedis jedis = getPool().borrowObject();
            value = jedis.get(key);
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static String hget(String key, String field) {
        try {
            Jedis jedis = getPool().borrowObject();
            String result = jedis.hget(key, field);
            getPool().returnObject(jedis);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, String> hgetall(String key) {
        try {
            Jedis jedis = getPool().borrowObject();
            Map<String, String> result = jedis.hgetAll(key);
            getPool().returnObject(jedis);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public static void addToList(String key, String... values) {
        try {
            Jedis jedis = getPool().borrowObject();
            jedis.sadd(key, values);
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void removeFromList(String key, String... values) {
        try {
            Jedis jedis = getPool().borrowObject();
            jedis.srem(key, values);
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getList(String key) {
        try {
            Jedis jedis = getPool().borrowObject();
            Set<String> members = jedis.smembers(key);
            getPool().returnObject(jedis);
            return new ArrayList<>(members);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @SneakyThrows
    public static void hset(String key, String field, String value) {
        Jedis jedis = getPool().borrowObject();
        jedis.hset(key, field, value);
        getPool().returnObject(jedis);
    }

    public static void del(String key) {

        try {
            Jedis jedis = getPool().borrowObject();
            jedis.del(key);
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void useRedis(Consumer<Jedis> jedisConsumer) {
        try {
            Jedis jedis = getPool().borrowObject();
            jedisConsumer.accept(jedis);
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    JedisPool jedisPool;

    public static JedisPool getPool() {
        System.out.println("got pool");
        return FedEx.getInstance().getJedisPool();
    }


}
