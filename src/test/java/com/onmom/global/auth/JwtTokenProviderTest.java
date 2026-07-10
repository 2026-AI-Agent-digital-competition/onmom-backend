package com.onmom.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import com.onmom.user.domain.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-for-jwt-token-provider";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createAccessTokenContainsRequiredClaims() throws Exception {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpiration(Duration.ofHours(2));
        JwtTokenProvider tokenProvider = new JwtTokenProvider(properties, objectMapper);

        String accessToken = tokenProvider.createAccessToken(1L, UserRole.MOTHER);

        String[] segments = accessToken.split("\\.");
        assertThat(segments).hasSize(3);

        Map<String, Object> payload = decodePayload(segments[1]);
        assertThat(payload)
                .containsEntry("sub", "1")
                .containsEntry("userId", 1)
                .containsEntry("role", "MOTHER")
                .containsKeys("iat", "exp");
    }

    @Test
    void getAccessTokenExpiresInSecondsReturnsConfiguredSeconds() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpiration(Duration.ofMinutes(30));
        JwtTokenProvider tokenProvider = new JwtTokenProvider(properties, objectMapper);

        assertThat(tokenProvider.getAccessTokenExpiresInSeconds()).isEqualTo(1800);
    }

    @Test
    void getUserIdReturnsUserIdFromValidToken() {
        JwtTokenProvider tokenProvider = tokenProvider(Duration.ofMinutes(30));
        String accessToken = tokenProvider.createAccessToken(1L, UserRole.MOTHER);

        assertThat(tokenProvider.getUserId(accessToken)).isEqualTo(1L);
    }

    @Test
    void rejectsTamperedSignature() {
        JwtTokenProvider tokenProvider = tokenProvider(Duration.ofMinutes(30));
        String accessToken = tokenProvider.createAccessToken(1L, UserRole.MOTHER);
        String[] segments = accessToken.split("\\.");
        String tamperedToken = segments[0] + "." + segments[1] + ".invalid-signature";

        assertErrorCode(() -> tokenProvider.getUserId(tamperedToken), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void rejectsExpiredTokenAtExpirationBoundary() {
        JwtTokenProvider tokenProvider = tokenProvider(Duration.ZERO);
        String accessToken = tokenProvider.createAccessToken(1L, UserRole.MOTHER);

        assertErrorCode(() -> tokenProvider.getUserId(accessToken), ErrorCode.EXPIRED_TOKEN);
    }

    @Test
    void rejectsMalformedToken() {
        JwtTokenProvider tokenProvider = tokenProvider(Duration.ofMinutes(30));

        assertErrorCode(() -> tokenProvider.getUserId("not-a-jwt"), ErrorCode.INVALID_TOKEN);
    }

    @Test
    void rejectsTokenWithoutRequiredRoleClaim() throws Exception {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", "1");
        payload.put("userId", 1L);
        payload.put("iat", Instant.now().getEpochSecond());
        payload.put("exp", Instant.now().plusSeconds(300).getEpochSecond());
        String accessToken = signedToken(header, payload);

        assertErrorCode(
                () -> tokenProvider(Duration.ofMinutes(30)).getUserId(accessToken),
                ErrorCode.INVALID_TOKEN
        );
    }

    @Test
    void rejectsUnsupportedAlgorithmHeader() throws Exception {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "none");
        header.put("typ", "JWT");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", "1");
        payload.put("userId", 1L);
        payload.put("role", "MOTHER");
        payload.put("iat", Instant.now().getEpochSecond());
        payload.put("exp", Instant.now().plusSeconds(300).getEpochSecond());
        String accessToken = signedToken(header, payload);

        assertErrorCode(
                () -> tokenProvider(Duration.ofMinutes(30)).getUserId(accessToken),
                ErrorCode.INVALID_TOKEN
        );
    }

    @Test
    void rejectsMissingJwtSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(" ");
        JwtTokenProvider tokenProvider = new JwtTokenProvider(properties, objectMapper);

        assertErrorCode(
                () -> tokenProvider.createAccessToken(1L, UserRole.MOTHER),
                ErrorCode.JWT_SECRET_NOT_CONFIGURED
        );
    }

    private Map<String, Object> decodePayload(String payloadSegment) throws Exception {
        byte[] decodedPayload = Base64.getUrlDecoder().decode(payloadSegment);
        String payloadJson = new String(decodedPayload, StandardCharsets.UTF_8);
        return objectMapper.readValue(payloadJson, new TypeReference<>() {
        });
    }

    private JwtTokenProvider tokenProvider(Duration expiration) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setAccessTokenExpiration(expiration);
        return new JwtTokenProvider(properties, objectMapper);
    }

    private String signedToken(Map<String, Object> header, Map<String, Object> payload) throws Exception {
        String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(header));
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(payload));
        String unsignedToken = encodedHeader + "." + encodedPayload;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mac.doFinal(unsignedToken.getBytes(StandardCharsets.UTF_8)));
        return unsignedToken + "." + signature;
    }

    private void assertErrorCode(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
