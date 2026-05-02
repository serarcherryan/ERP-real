package com.erpreal.backend.resident;

import com.erpreal.backend.common.ApiException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
class SensitiveFieldCodec {
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    SensitiveFieldCodec(@Value("${erp.security.field-encryption-key}") String secret) {
        this.keySpec = new SecretKeySpec(sha256Bytes(secret), "AES");
    }

    String encrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            var iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            var encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            var payload = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return "enc:v1:" + Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) {
            throw new ApiException("SENSITIVE_FIELD_ENCRYPTION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR,
                    "Sensitive field encryption failed");
        }
    }

    String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return null;
        }
        if (!encrypted.startsWith("enc:v1:")) {
            return null;
        }
        try {
            var payload = Base64.getDecoder().decode(encrypted.substring("enc:v1:".length()));
            var buffer = ByteBuffer.wrap(payload);
            var iv = new byte[IV_LENGTH];
            buffer.get(iv);
            var cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new ApiException("SENSITIVE_FIELD_DECRYPTION_FAILED", HttpStatus.INTERNAL_SERVER_ERROR,
                    "Sensitive field decryption failed");
        }
    }

    String hash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return sha256(normalize(value));
    }

    String maskPhone(String value) {
        if (value == null || value.length() < 7) {
            return value;
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    String maskIdentityNo(String value) {
        if (value == null || value.length() < 8) {
            return value;
        }
        return value.substring(0, 6) + "********" + value.substring(value.length() - 4);
    }

    private static String normalize(String value) {
        return value.replaceAll("\\s+", "").toLowerCase();
    }

    private static String sha256(String value) {
        return HexFormat.of().formatHex(sha256Bytes(value));
    }

    private static byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
