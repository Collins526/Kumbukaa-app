package com.kumbukaa.config;

import com.kumbukaa.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class JwtTokenProvider {

    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    private static final String SIGNATURE_ALGORITHM = "HmacSHA256";

    private final byte[] secretKey;
    private final long accessTokenExpirationMillis;
    private final long refreshTokenExpirationMillis;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration}") long accessTokenExpirationMillis,
            @Value("${app.jwt.refresh.expiration}") long refreshTokenExpirationMillis) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must be configured in application properties");
        }
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenExpirationMillis = accessTokenExpirationMillis;
        this.refreshTokenExpirationMillis = refreshTokenExpirationMillis;
    }

    public String createAccessToken(User user) {
        return createToken(user, accessTokenExpirationMillis, TOKEN_TYPE_ACCESS);
    }

    public String createRefreshToken(User user) {
        return createToken(user, refreshTokenExpirationMillis, TOKEN_TYPE_REFRESH);
    }

    public boolean validateAccessToken(String token) {
        TokenDetails details = parseToken(token);
        return details != null
                && TOKEN_TYPE_ACCESS.equals(details.type)
                && details.expiresAt > System.currentTimeMillis();
    }

    public Long getUserId(String token) {
        TokenDetails details = parseToken(token);
        return details != null ? details.userId : null;
    }

    private String createToken(User user, long ttlMillis, String type) {
        long issuedAt = System.currentTimeMillis();
        long expiresAt = issuedAt + ttlMillis;
        String payload = String.format("%d:%s:%d:%d:%s", user.getId(), user.getEmail(), issuedAt, expiresAt, type);
        String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = computeSignature(encodedPayload);
        return encodedPayload + "." + signature;
    }

    private TokenDetails parseToken(String token) {
        if (token == null || !token.contains(".")) {
            return null;
        }

        String[] parts = token.split("\\.");
        if (parts.length != 2) {
            return null;
        }

        String encodedPayload = parts[0];
        String signature = parts[1];
        if (!signature.equals(computeSignature(encodedPayload))) {
            return null;
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }

        String[] payloadParts = payload.split(":");
        if (payloadParts.length != 5) {
            return null;
        }

        try {
            long userId = Long.parseLong(payloadParts[0]);
            // payloadParts[1] = email (not stored), payloadParts[2] = issuedAt (not stored)
            long expiresAt = Long.parseLong(payloadParts[3]);
            String type = payloadParts[4];
            return new TokenDetails(userId, expiresAt, type);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String computeSignature(String message) {
        try {
            Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(new SecretKeySpec(secretKey, SIGNATURE_ALGORITHM));
            byte[] macBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(macBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to compute token signature", ex);
        }
    }

    private static final class TokenDetails {
        private final long userId;
        private final long expiresAt;
        private final String type;

        private TokenDetails(long userId, long expiresAt, String type) {
            this.userId = userId;
            this.expiresAt = expiresAt;
            this.type = type;
        }
    }
}
