package com.example.wycrimeapp.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing utilities.
 *
 * <p>Why this exists:</p>
 * <ul>
 *     <li>The coursework app previously stored plaintext passwords.</li>
 *     <li>To harden authentication without breaking existing installs, login supports legacy
 *     plaintext values and upgrades them to a secure hash after a successful login.</li>
 * </ul>
 *
 * <p>Storage format:</p>
 * pbkdf2_sha256$<iterations>$<saltBase64>$<hashBase64>
 */
public final class PasswordUtils {

    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;

    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtils() {}

    public static String hashPassword(String plainTextPassword) {
        if (plainTextPassword == null) {
            throw new IllegalArgumentException("Password must not be null");
        }

        byte[] salt = new byte[SALT_BYTES];
        RNG.nextBytes(salt);

        byte[] hash = pbkdf2(plainTextPassword.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS);

        String saltB64 = Base64.encodeToString(salt, Base64.NO_WRAP);
        String hashB64 = Base64.encodeToString(hash, Base64.NO_WRAP);

        return PREFIX + "$" + ITERATIONS + "$" + saltB64 + "$" + hashB64;
    }

    public static boolean verifyPassword(String candidatePlainText, String storedPasswordValue) {
        if (candidatePlainText == null || storedPasswordValue == null) {
            return false;
        }

        // Legacy support: previously stored as plaintext.
        if (!isHashedPassword(storedPasswordValue)) {
            return constantTimeEquals(candidatePlainText, storedPasswordValue);
        }

        try {
            String[] parts = storedPasswordValue.split("\\$");
            if (parts.length != 4) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.decode(parts[2], Base64.NO_WRAP);
            byte[] expectedHash = Base64.decode(parts[3], Base64.NO_WRAP);

            byte[] actualHash = pbkdf2(candidatePlainText.toCharArray(), salt, iterations, expectedHash.length * 8);
            return constantTimeEquals(actualHash, expectedHash);
        } catch (Exception e) {
            // If parsing or crypto fails, treat as invalid.
            return false;
        }
    }

    public static boolean isHashedPassword(String storedPasswordValue) {
        return storedPasswordValue != null && storedPasswordValue.startsWith(PREFIX + "$");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // PBKDF2WithHmacSHA256 should exist on API 26+; if not, fail closed.
            throw new IllegalStateException("Password hashing not available", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return constantTimeEquals(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        int diff = a.length ^ b.length;
        for (int i = 0; i < Math.min(a.length, b.length); i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
