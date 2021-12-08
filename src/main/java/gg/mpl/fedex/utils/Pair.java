package gg.mpl.fedex.utils;

import lombok.Data;

@Data
public final class Pair<K, V> {
    private final K key;
    private final V value;
}