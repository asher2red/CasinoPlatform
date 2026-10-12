package com.casino.platform.provider.security;

import com.casino.platform.provider.exception.ProviderAuthenticationException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * HMAC-SHA256 signature validator for provider authentication.
 *
 * Signature format:
 * HMAC-SHA256(method + url + timestamp + nonce + body, secretKey)
 */
@Component
public class HmacSignatureValidator {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Validate HMAC signature.
     *
     * @param method HTTP method (GET, POST, etc.)
     * @param url Request URL path (e.g., /api/provider-wallet/v1/DEMO/balance)
     * @param timestamp Request timestamp in milliseconds
     * @param nonce Unique nonce
     * @param body Request body (empty string if no body)
     * @param providedSignature Signature from request header
     * @param secretKey Provider's secret key
     * @throws ProviderAuthenticationException if signature is invalid
     */
    public void validateSignature(
            String method,
            String url,
            Long timestamp,
            String nonce,
            String body,
            String providedSignature,
            String secretKey) {

        // Build message to sign
        String message = buildSignatureMessage(method, url, timestamp, nonce, body);

        // Calculate expected signature
        String expectedSignature = calculateHmacSignature(message, secretKey);

        // Constant-time comparison to prevent timing attacks
        if (!constantTimeEquals(expectedSignature, providedSignature)) {
            throw new ProviderAuthenticationException("Invalid HMAC signature");
        }
    }

    /**
     * Build message string for signing.
     * Format: method + url + timestamp + nonce + body
     */
    private String buildSignatureMessage(String method, String url, Long timestamp, String nonce, String body) {
        return method + url + timestamp + nonce + body;
    }

    /**
     * Calculate HMAC-SHA256 signature.
     */
    private String calculateHmacSignature(String message, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            );
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new ProviderAuthenticationException("Failed to calculate HMAC signature", e);
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        return MessageDigest.isEqual(aBytes, bBytes);
    }
}
