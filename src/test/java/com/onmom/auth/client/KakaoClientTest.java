package com.onmom.auth.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoClientTest {

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    private KakaoClientProperties properties;
    private MockRestServiceServer server;
    private KakaoClient client;
    private Logger logger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        server = MockRestServiceServer.bindTo(restClientBuilder).build();
        properties = configuredProperties();
        client = new KakaoClient(restClientBuilder, properties);

        logger = (Logger) LoggerFactory.getLogger(KakaoClient.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void exchangesAuthorizationCodeAndGetsUserInfo() {
        server.expect(requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    MediaType contentType = request.getHeaders().getContentType();
                    assertThat(contentType).isNotNull();
                    assertThat(contentType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)).isTrue();
                    assertThat(contentType.getCharset()).isEqualTo(StandardCharsets.UTF_8);

                    String encodedBody = ((MockClientHttpRequest) request).getBodyAsString();
                    String body = URLDecoder.decode(encodedBody, StandardCharsets.UTF_8);
                    assertThat(body)
                            .contains("grant_type=authorization_code")
                            .contains("client_id=client-id")
                            .contains("redirect_uri=http://localhost:5173/oauth/kakao/callback")
                            .contains("code=authorization-code")
                            .contains("client_secret=client-secret");
                })
                .andRespond(withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER_INFO_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer kakao-access-token"))
                .andRespond(withSuccess("""
                        {
                          "id": 123,
                          "kakao_account": {
                            "email": "onmom@example.com",
                            "profile": {
                              "nickname": "온맘",
                              "profile_image_url": "https://example.com/profile.png"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        KakaoUserInfo userInfo = client.authenticate("authorization-code");

        assertThat(userInfo.providerUserId()).isEqualTo("123");
        assertThat(userInfo.email()).isEqualTo("onmom@example.com");
        assertThat(userInfo.nickname()).isEqualTo("온맘");
        assertThat(userInfo.profileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(warningMessages()).isEmpty();
        server.verify();
    }

    @Test
    void mapsTokenClientErrorToInvalidAuthorizationCode() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withBadRequest());

        assertErrorCode(
                () -> client.authenticate("invalid-code"),
                ErrorCode.KAKAO_AUTHORIZATION_CODE_INVALID
        );
        assertThat(warningMessages()).isEmpty();
        server.verify();
    }

    @Test
    void mapsTokenServerErrorsToKakaoLoginFailure() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withServerError());

        assertErrorCode(
                () -> client.authenticate("authorization-code"),
                ErrorCode.KAKAO_LOGIN_FAILED
        );
        assertThat(warningMessages()).containsExactly(
                "Kakao external call failed: stage=token_exchange status=500"
        );
        assertNoSensitiveInformationInLogs();
        server.verify();
    }

    @Test
    void mapsTokenNetworkErrorsToKakaoLoginFailure() {
        server.expect(requestTo(TOKEN_URL)).andRespond(request -> {
            throw new IOException("connection reset");
        });

        assertErrorCode(
                () -> client.authenticate("authorization-code"),
                ErrorCode.KAKAO_LOGIN_FAILED
        );
        assertThat(warningMessages()).singleElement()
                .asString()
                .contains("stage=token_exchange")
                .contains("exception=");
        assertNoSensitiveInformationInLogs();
        server.verify();
    }

    @Test
    void rejectsSuccessfulTokenResponseWithoutAccessToken() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertErrorCode(
                () -> client.authenticate("authorization-code"),
                ErrorCode.KAKAO_LOGIN_FAILED
        );
        assertThat(warningMessages()).containsExactly(
                "Kakao external call returned invalid response: stage=token_exchange"
        );
        server.verify();
    }

    @Test
    void mapsUserInfoErrorsToKakaoLoginFailure() {
        server.expect(requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access-token\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(USER_INFO_URL)).andRespond(withServerError());

        assertErrorCode(
                () -> client.authenticate("authorization-code"),
                ErrorCode.KAKAO_LOGIN_FAILED
        );
        assertThat(warningMessages()).containsExactly(
                "Kakao external call failed: stage=user_info status=500"
        );
        assertNoSensitiveInformationInLogs();
        server.verify();
    }

    @Test
    void rejectsMissingOauthConfigurationBeforeCallingKakao() {
        properties.setClientSecret(" ");

        assertErrorCode(
                () -> client.authenticate("authorization-code"),
                ErrorCode.KAKAO_OAUTH_NOT_CONFIGURED
        );
        assertThat(warningMessages()).isEmpty();
    }

    private KakaoClientProperties configuredProperties() {
        KakaoClientProperties configuredProperties = new KakaoClientProperties();
        configuredProperties.setClientId("client-id");
        configuredProperties.setClientSecret("client-secret");
        configuredProperties.setRedirectUri("http://localhost:5173/oauth/kakao/callback");
        return configuredProperties;
    }

    private void assertErrorCode(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }

    private List<String> warningMessages() {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private void assertNoSensitiveInformationInLogs() {
        assertThat(warningMessages()).allSatisfy(message -> assertThat(message)
                .doesNotContain("authorization-code")
                .doesNotContain("kakao-access-token")
                .doesNotContain("client-secret")
                .doesNotContain("onmom@example.com"));
    }
}
