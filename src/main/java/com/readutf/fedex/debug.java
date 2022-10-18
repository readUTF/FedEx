package com.readutf.fedex;

import java.util.Arrays;
import java.util.List;

public class debug {

    public static void main(String[] args) {
        List<Object> test = Arrays.asList("test", "test2");
        test(test.stream().toArray());
    }

    public static void test(Object... strings) {
        for (Object string : strings) {
            System.out.println(string);
        }
    }

}
