package com.example.calculator_vault_androidapp.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for cryptographic operations including PIN hashing and file encryption.
 */
public class CryptoUtils {

    /**
     * Hash a 5-digit PIN using SHA-256 and return hex string.
     * @param pin The PIN to hash
     * @return The SHA-256 hash as a hex string
     */
    public static String hashPin(String pin) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    /**
     * Verify input PIN against stored hash.
     * @param inputPin The PIN entered by user
     * @param storedHash The stored hash to compare against
     * @return true if PIN matches, false otherwise
     */
    public static boolean verifyPin(String inputPin, String storedHash) {
        if (inputPin == null || storedHash == null) {
            return false;
        }
        return hashPin(inputPin).equals(storedHash);
    }

    /**
     * Encrypt data using XOR with PIN.
     * @param data The data to encrypt
     * @param pin The PIN to use as key
     * @return The encrypted data
     */
    public static byte[] encryptData(byte[] data, String pin) {
        if (data == null || pin == null || pin.isEmpty()) {
            return data;
        }
        return xorWithKey(data, pin.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decrypt data using XOR with PIN.
     * @param data The data to decrypt
     * @param pin The PIN to use as key
     * @return The decrypted data
     */
    public static byte[] decryptData(byte[] data, String pin) {
        if (data == null || pin == null || pin.isEmpty()) {
            return data;
        }
        return xorWithKey(data, pin.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * XOR operation with key.
     * @param data The data to XOR
     * @param key The key bytes
     * @return The XORed data
     */
    private static byte[] xorWithKey(byte[] data, byte[] key) {
        byte[] out = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            out[i] = (byte) (data[i] ^ key[i % key.length]);
        }
        return out;
    }

    /**
     * Validate PIN format (must be exactly 5 digits).
     * @param pin The PIN to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidPin(String pin) {
        if (pin == null || pin.length() != 5) {
            return false;
        }
        for (char c : pin.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
