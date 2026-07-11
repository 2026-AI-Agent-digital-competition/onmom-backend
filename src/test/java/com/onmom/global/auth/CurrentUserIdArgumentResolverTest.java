package com.onmom.global.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.onmom.global.exception.BusinessException;
import com.onmom.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

class CurrentUserIdArgumentResolverTest {

    private JwtTokenProvider jwtTokenProvider;
    private CurrentUserIdArgumentResolver resolver;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = mock(JwtTokenProvider.class);
        resolver = new CurrentUserIdArgumentResolver(jwtTokenProvider);
    }

    @Test
    void resolvesUserIdFromBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access-token");
        when(jwtTokenProvider.getUserId("access-token")).thenReturn(1L);

        Object userId = resolver.resolveArgument(
                mock(MethodParameter.class),
                null,
                new ServletWebRequest(request),
                null
        );

        assertThat(userId).isEqualTo(1L);
        verify(jwtTokenProvider).getUserId("access-token");
    }

    @Test
    void rejectsMissingAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertErrorCode(
                () -> resolver.resolveArgument(
                        mock(MethodParameter.class),
                        null,
                        new ServletWebRequest(request),
                        null
                ),
                ErrorCode.AUTHENTICATION_REQUIRED
        );
    }

    @Test
    void rejectsInvalidAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "access-token");

        assertErrorCode(
                () -> resolver.resolveArgument(
                        mock(MethodParameter.class),
                        null,
                        new ServletWebRequest(request),
                        null
                ),
                ErrorCode.INVALID_AUTHORIZATION_HEADER
        );
    }

    private void assertErrorCode(Runnable invocation, ErrorCode errorCode) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(errorCode);
    }
}
