package com.onmom.global.auth;

import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.user.domain.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Component
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    public JwtTokenProvider(JwtProperties jwtProperties, ObjectMapper objectMapper) {
        this.jwtProperties = jwtProperties;
        this.objectMapper = objectMapper;
    }

    public String createAccessToken(Long userId, UserRole role) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getAccessTokenExpiration());

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", String.valueOf(userId));
        payload.put("userId", userId);
        payload.put("role", role.name());
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String unsignedToken = base64UrlJson(header) + "." + base64UrlJson(payload);
        return unsignedToken + "." + sign(unsignedToken);
    }

    public long getAccessTokenExpiresInSeconds() {
        return jwtProperties.getAccessTokenExpiration().toSeconds();
    }

    public Long getUserId(String accessToken) {
        String[] segments = accessToken.split("\\.", -1);
        if (segments.length != 3) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String unsignedToken = segments[0] + "." + segments[1];
        if (!MessageDigest.isEqual(
                sign(unsignedToken).getBytes(StandardCharsets.UTF_8),
                segments[2].getBytes(StandardCharsets.UTF_8)
        )) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Map<String, Object> header = parseJsonSegment(segments[0]);
        if (!"HS256".equals(header.get("alg")) || !"JWT".equals(header.get("typ"))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Map<String, Object> payload = parseJsonSegment(segments[1]);
        Object expiration = payload.get("exp");
        if (!(expiration instanceof Number exp)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        if (exp.longValue() <= Instant.now().getEpochSecond()) {
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        }

        Object userId = payload.get("userId");
        if (!(userId instanceof Number numericUserId)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        if (!String.valueOf(numericUserId.longValue()).equals(payload.get("sub"))
                || !(payload.get("iat") instanceof Number)
                || !isValidRole(payload.get("role"))) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        return numericUserId.longValue();
    }

    private String base64UrlJson(Map<String, Object> value) {
        try {
            return base64UrlEncode(objectMapper.writeValueAsBytes(value));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Failed to serialize JWT JSON.", exception);
        }
    }

    private String sign(String unsignedToken) {
        String secret = jwtProperties.getSecret();
        if (!StringUtils.hasText(secret)) {
            throw new BusinessException(ErrorCode.JWT_SECRET_NOT_CONFIGURED);
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return base64UrlEncode(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to sign JWT.", exception);
        }
    }

    private String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private Map<String, Object> parseJsonSegment(String segment) {
        try {
            byte[] decodedJson = Base64.getUrlDecoder().decode(segment);
            return objectMapper.readValue(decodedJson, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private boolean isValidRole(Object role) {
        if (!(role instanceof String roleName)) {
            return false;
        }
        try {
            UserRole.valueOf(roleName);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
