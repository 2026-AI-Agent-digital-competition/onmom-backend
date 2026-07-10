package com.onmom.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KakaoClient {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String TOKEN_EXCHANGE_STAGE = "token_exchange";
    private static final String USER_INFO_STAGE = "user_info";
    private static final Logger log = LoggerFactory.getLogger(KakaoClient.class);

    private final RestClient restClient;
    private final KakaoClientProperties properties;

    public KakaoClient(
            RestClient.Builder restClientBuilder,
            KakaoClientProperties properties
    ) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    public KakaoUserInfo authenticate(String authorizationCode) {
        validateOauthConfiguration();
        String accessToken = exchangeAuthorizationCode(authorizationCode);
        return getUserInfo(accessToken);
    }

    private String exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", properties.getClientId());
        formData.add("redirect_uri", properties.getRedirectUri());
        formData.add("code", authorizationCode);
        formData.add("client_secret", properties.getClientSecret());

        try {
            KakaoTokenResponse response = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(new MediaType(MediaType.APPLICATION_FORM_URLENCODED, StandardCharsets.UTF_8))
                    .body(formData)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
                        throw new BusinessException(ErrorCode.KAKAO_AUTHORIZATION_CODE_INVALID);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, clientResponse) -> {
                        logHttpFailure(TOKEN_EXCHANGE_STAGE, clientResponse.getStatusCode());
                        throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
                    })
                    .body(KakaoTokenResponse.class);

            if (response == null || !StringUtils.hasText(response.accessToken())) {
                logInvalidResponse(TOKEN_EXCHANGE_STAGE);
                throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
            }
            return response.accessToken();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            logClientFailure(TOKEN_EXCHANGE_STAGE, exception);
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    private KakaoUserInfo getUserInfo(String accessToken) {
        try {
            KakaoApiResponse response = restClient.get()
                    .uri(USER_INFO_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (request, clientResponse) -> {
                        logHttpFailure(USER_INFO_STAGE, clientResponse.getStatusCode());
                        throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, clientResponse) -> {
                        logHttpFailure(USER_INFO_STAGE, clientResponse.getStatusCode());
                        throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
                    })
                    .body(KakaoApiResponse.class);

            if (response == null || response.id() == null) {
                logInvalidResponse(USER_INFO_STAGE);
                throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
            }
            return response.toUserInfo();
        } catch (BusinessException exception) {
            throw exception;
        } catch (RestClientException exception) {
            logClientFailure(USER_INFO_STAGE, exception);
            throw new BusinessException(ErrorCode.KAKAO_LOGIN_FAILED);
        }
    }

    private void logHttpFailure(String stage, HttpStatusCode statusCode) {
        log.warn("Kakao external call failed: stage={} status={}", stage, statusCode.value());
    }

    private void logClientFailure(String stage, RestClientException exception) {
        log.warn(
                "Kakao external call failed: stage={} exception={}",
                stage,
                exception.getClass().getSimpleName()
        );
    }

    private void logInvalidResponse(String stage) {
        log.warn("Kakao external call returned invalid response: stage={}", stage);
    }

    private void validateOauthConfiguration() {
        if (!StringUtils.hasText(properties.getClientId())
                || !StringUtils.hasText(properties.getClientSecret())
                || !StringUtils.hasText(properties.getRedirectUri())) {
            throw new BusinessException(ErrorCode.KAKAO_OAUTH_NOT_CONFIGURED);
        }
    }

    private record KakaoTokenResponse(
            @JsonProperty("access_token")
            String accessToken
    ) {
    }

    private record KakaoApiResponse(
            Long id,
            @JsonProperty("kakao_account")
            KakaoAccount kakaoAccount,
            KakaoProfileProperties properties
    ) {

        KakaoUserInfo toUserInfo() {
            String nickname = properties == null ? null : properties.nickname();
            String profileImageUrl = properties == null ? null : properties.profileImage();
            String email = kakaoAccount == null ? null : kakaoAccount.email();

            if (kakaoAccount != null && kakaoAccount.profile() != null) {
                if (nickname == null) {
                    nickname = kakaoAccount.profile().nickname();
                }
                if (profileImageUrl == null) {
                    profileImageUrl = kakaoAccount.profile().profileImageUrl();
                }
            }
            return new KakaoUserInfo(String.valueOf(id), email, nickname, profileImageUrl);
        }
    }

    private record KakaoAccount(
            String email,
            KakaoProfile profile
    ) {
    }

    private record KakaoProfile(
            String nickname,
            @JsonProperty("profile_image_url")
            String profileImageUrl
    ) {
    }

    private record KakaoProfileProperties(
            String nickname,
            @JsonProperty("profile_image")
            String profileImage
    ) {
    }
}
