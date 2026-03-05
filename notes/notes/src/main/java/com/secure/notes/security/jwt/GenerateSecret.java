package com.secure.notes.security.jwt;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class GenerateSecret {

    public static void main(String[] args) {

        byte[] key = new byte[32]; // 256-bit key
        new SecureRandom().nextBytes(key);

        String secret = Base64.getEncoder().encodeToString(key);

        System.out.println(secret);

        byte[] decodedBytes = Base64.getDecoder().decode(secret);

        System.out.println("Decoded byte length: " + decodedBytes.length);

        // Print as text (may not be readable)
        System.out.println("Decoded as text: " + new String(decodedBytes, StandardCharsets.UTF_8));

        // Print each byte value
        System.out.print("Decoded bytes: ");
        for (byte b : decodedBytes) {
            System.out.print(b + " ");
        }
    }
}