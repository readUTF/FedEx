package com.readutf.fedex.utils;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeHandler<T> {

    Class<?> clazz;

    public UnsafeHandler(Class<?> clazz) {
        this.clazz = clazz;
    }

    public T getInstance() {
        try {
            return (T) unsafe.allocateInstance(clazz);
        } catch (Exception e) {
            return null;
        }

    }

    static Unsafe unsafe;
    static {
        try {
            Field singleoneInstanceField = Unsafe.class.getDeclaredField("theUnsafe");
            singleoneInstanceField.setAccessible(true);
            unsafe = (Unsafe) singleoneInstanceField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
