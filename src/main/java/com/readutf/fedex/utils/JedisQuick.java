package com.readutf.fedex.utils;

import com.readutf.fedex.FedEx;
import lombok.Setter;
import lombok.SneakyThrows;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public class JedisQuick {

    @Setter
    private boolean debug = false;

    private FedEx fedEx;

    public JedisQuick(FedEx fedEx) {
        this.fedEx = fedEx;
    }

    public void set(String key, String value) {
        try {
            Jedis jedis = getPool().borrowObject();
            handleDebug("set");
            jedis.set(key, value);
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean exists(String key) {
        try {
            Jedis jedis = getPool().borrowObject();
            handleDebug("exists");
            return jedis.exists(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String get(String key) {
        String value = null;
        try {
            Jedis jedis = getPool().borrowObject();
            value = jedis.get(key);
            handleDebug("get");
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public String hget(String key, String field) {
        try {
            Jedis jedis = getPool().borrowObject();
            String result = jedis.hget(key, field);
            handleDebug("hget");
            getPool().returnObject(jedis);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, String> hgetall(String key) {
        try {
            Jedis jedis = getPool().borrowObject();
            Map<String, String> result = jedis.hgetAll(key);
            handleDebug("hgetAll");
            getPool().returnObject(jedis);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    public void addToList(String key, String... values) {
        try {
            Jedis jedis = getPool().borrowObject();
            jedis.sadd(key, values);
            handleDebug("sadd");
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeFromList(String key, String... values) {
        try {
            Jedis jedis = getPool().borrowObject();
            jedis.srem(key, values);
            handleDebug("srem");
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> getList(String key) {
        try {
            Jedis jedis = getPool().borrowObject();
            Set<String> members = jedis.smembers(key);
            handleDebug("smembers");
            getPool().returnObject(jedis);
            return new ArrayList<>(members);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    @SneakyThrows
    public void hset(String key, String field, String value) {
        Jedis jedis = getPool().borrowObject();
        jedis.hset(key, field, value);
        handleDebug("hset");
        getPool().returnObject(jedis);
    }

    public void del(String key) {

        try {
            Jedis jedis = getPool().borrowObject();
            jedis.del(key);
            handleDebug("del");
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void useRedis(Consumer<Jedis> jedisConsumer) {
        try {
            Jedis jedis = getPool().borrowObject();
            jedisConsumer.accept(jedis);
            handleDebug("consumer");
            getPool().returnObject(jedis);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    public <T> T useRedisAndGet(Function<Jedis, T> jedisFunction) {
        Jedis jedis = getPool().borrowObject();
        T resource = jedisFunction.apply(jedis);
        handleDebug("consumer");
        getPool().returnObject(jedis);
        return resource;
    }

    public JedisPool getPool() {
        return fedEx.getJedisPool();
    }

    public void handleDebug(String type) {
        if(!debug) return;
        fedEx.debug(type + " called from: " + Thread.currentThread().getStackTrace()[3]);
    }

}
