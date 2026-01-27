package org.graph.infrastructure.utils;

import java.util.Base64;

public class Base64Utils {

    public static String encode(String input) {
        return Base64.getEncoder().encodeToString(input.getBytes());
    }

    public static String encode(byte[] input) {
        return Base64.getEncoder().encodeToString(input);
    }

    public static String decode(String input) {
        return new String(Base64.getDecoder().decode(input));
    }

    public static byte[] decodeToBytes(String input) {
        return Base64.getDecoder().decode(input);
    }
}
