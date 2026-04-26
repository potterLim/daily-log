package com.potterlim.daymark.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

final class AuthenticationTokenSupport {

    private AuthenticationTokenSupport() {
    }

    public static String generateRawToken(SecureRandom secureRandom, int tokenByteLength) {
        byte[] tokenBytes = new byte[tokenByteLength];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(messageDigest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new IllegalStateException("SHA-256 algorithm must be available.", noSuchAlgorithmException);
        }
    }
}
