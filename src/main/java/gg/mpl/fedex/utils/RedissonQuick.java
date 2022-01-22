package gg.mpl.fedex.utils;

import lombok.AllArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

public class RedissonQuick {

    private static RedissonClient s_redisson;

    public RedissonQuick(RedissonClient redisson) {
        s_redisson = redisson;
    }

    public static <T> void set(String key, T value) {
        RBucket<Object> bucket = s_redisson.getBucket(key);
        bucket.set(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        RBucket<Object> bucket = s_redisson.getBucket(key);
        Object beforeCast = bucket.get();
        if(beforeCast == null) return null;
        return (T) beforeCast;
    }

    public static void del(String key) {
        s_redisson.getBucket(key).delete();
    }


}
