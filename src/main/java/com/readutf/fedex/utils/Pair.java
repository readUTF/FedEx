package com.readutf.fedex.utils;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public final class Pair<K, V> {
    @NotNull
    private final K key;
    @NotNull
    private final V value;
}