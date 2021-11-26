package com.readutf.fedex.utils;

import lombok.Getter;

@Getter
public class Pair<K, V> {

    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

}