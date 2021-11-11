package com.readutf.fedex.utils;

import lombok.Getter;

@Getter
public class Pair<K, V> {

    public K key;
    public V value;

    public Pair(K first, V second) {
        this.key = first;
        this.value = second;
    }

}